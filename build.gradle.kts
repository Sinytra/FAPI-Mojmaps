import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerWriter
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    }
}

plugins {
    java
    id("dev.architectury.loom")
    id("fapi-moj.remap")
}

val versionMc: String by project
val versionForge: String by project
val versionYarn: String by project
val versionFabricLoader: String by project

group = "org.sinytra"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
    maven {
        name = "Mojank"
        url = uri("https://libraries.minecraft.net/")
    }
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        name = "Sinytra"
        url = uri("https://maven.su5ed.dev/releases")
    }
    mavenLocal()
}

val excludedProjects = listOf("fabric-api-bom", "fabric-api-catalog")
val projectNames = file("fabric-api-upstream").list()!!.filter { it.startsWith("fabric-") } - excludedProjects

val generateMergedAccessWidener by tasks.registering(GenerateMergedAccessWidenerTask::class) {
    group = "sinytra"

    inputFiles.from(projectNames
        .flatMap {
            listOf(
                file("fabric-api-upstream/$it/src/main/resources"),
                file("fabric-api-upstream/$it/src/testmod/resources")
            )
        }
        .flatMap { it.listFiles { f -> f.name.endsWith(".accesswidener") }?.toList() ?: emptyList() })

    output.set(layout.buildDirectory.dir(name).get().file("merged.accesswidener"))
}

// Ugly way to make loom not explode during sync
if (!generateMergedAccessWidener.get().output.get().asFile.exists()) {
    generateMergedAccessWidener.get().output.get().asFile.parentFile.mkdirs()
    generateMergedAccessWidener.get().run()
}

loom.accessWidenerPath.set(generateMergedAccessWidener.flatMap(GenerateMergedAccessWidenerTask::output))

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = versionMc)
    neoForge(group = "net.neoforged", name = "neoforge", version = versionForge)
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:$versionYarn:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.20.5+build.3")
    })

    modImplementation("net.fabricmc:fabric-loader:$versionFabricLoader")

    // Remapping dep
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.8.1")
    compileOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly("org.mockito:mockito-core:5.4.0")
}

val submoduleDir = file("fabric-api-upstream")
val remote = "upstream"
val localBranch = "fabric/$versionMc"
val remoteBranch = "$remote/$versionMc"
val mappedBranch = "mojmap/$versionMc"

tasks.register("setup") {
    group = "sinytra"

    doFirst {
        val git = Git.open(rootDir)

        val list = git.branchList().call()
        if (list.any { it.name == "refs/heads/$mappedBranch" }) {
            println("FOUND EXISTING MAPPED BRANCH, RETURNING")
            return@doFirst
        }

        // Init repo
        if (!submoduleDir.exists()) {
            logger.lifecycle("Initializing submodule")
            initSubmodule()
        }
    }
}

fun initSubmodule() {
    Git.init().setDirectory(submoduleDir).call()
    val git = Git.open(submoduleDir)

    val list = git.branchList().call()
    if (list.any { it.name == "refs/heads/$localBranch" }) {
        logger.lifecycle("FOUND EXISTING RAW BRANCH, RETURNING")
        return
    }

    // Add upstream remote
    git.remoteAdd()
        .setName(remote)
        .setUri(URIish("https://github.com/FabricMC/fabric"))
        .call()
    // Add root remote
    git.remoteAdd()
        .setName("root")
        .setUri(URIish(rootDir.toURI().toURL()))
        .call()
    git.fetch()
        .setRemote(remote)
        .call()
    // Set up remote tracking branch
    git.checkout()
        .setCreateBranch(true)
        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
        .setName(localBranch)
        .setStartPoint(remoteBranch)
        .call()

    git.close()
}

tasks.register("update") {
    group = "sinytra"

    doFirst {
        val git = Git.open(rootDir)
        // Ensure we're up to date
//        git.fetch().setRemote(remote).call() TODO

        val currentCommit = git.repository.parseCommit(git.repository.resolve(localBranch))
        val latestCommit = git.repository.parseCommit(git.repository.resolve(remoteBranch))
        println("CURRENT: ${currentCommit.shortName()}")
        println("LATEST: ${latestCommit.shortName()}")

        if (currentCommit.equals(latestCommit)) {
            println("UP TO DATE")
        } else {
            val commitDistance = git.log().addRange(currentCommit, latestCommit).call().count()

            println("OUTDATED - WE ARE $commitDistance COMMITS BEHIND")

            val nextCommit = findNextCommit(git, currentCommit, latestCommit) ?: run {
                logger.error("NO UPDATES FOUND")
                return@doFirst
            }
            println("UPDATING TO ${nextCommit.shortName()}")
        }
    }
}

fun findNextCommit(git: Git, currentCommit: RevCommit, branchHead: ObjectId): RevCommit? {
    RevWalk(git.repository).use { revWalk ->
        revWalk.markStart(git.repository.parseCommit(branchHead))
        revWalk.markUninteresting(currentCommit)
        revWalk.sort(RevSort.REVERSE, true)

        return revWalk.firstOrNull { currentCommit in it.parents }
    }
}

fun RevCommit.shortName() = "${abbreviate(8).name()} $shortMessage"

abstract class GenerateMergedAccessWidenerTask : DefaultTask() {
    @SkipWhenEmpty
    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    val output: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun run() {
        val aw = AccessWidenerWriter()

        inputFiles.forEach { file ->
            val reader = AccessWidenerReader(aw)
            file.bufferedReader().use(reader::read)
        }

        output.get().asFile.writeBytes(aw.write())
    }
}