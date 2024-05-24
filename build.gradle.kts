import com.google.common.base.CaseFormat
import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerWriter
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.sinytra.gradle.RemapSourceDirectory

plugins {
    java
    id("dev.architectury.loom")
}

val versionMc: String by project
val versionForge: String by project
val versionYarn: String by project
val versionFabricLoader: String by project
val versionUpstream: String by project

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

val submoduleDir = file("fabric-api-upstream")
val remote = "upstream"
val localBranch = "fabric/$versionMc"
val remoteBranch = "$remote/$versionMc"
val mappedBranch = "mojmap/$versionMc"
val ignoredProjects = listOf("fabric-api-bom", "fabric-api-catalog")
val projectNames = file("fabric-api-upstream").list()?.filter { it.startsWith("fabric-") }?.let { it - ignoredProjects } ?: emptyList()

val generateMergedAccessWidener by tasks.registering(GenerateMergedAccessWidenerTask::class) {
    group = "sinytra"

    inputFiles.from(projectNames
        .flatMap {
            listOf(
                file("fabric-api-upstream/$it/src/client/resources"),
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

val mercuryClasspath: Configuration by configurations.creating

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = versionMc)
    neoForge(group = "net.neoforged", name = "neoforge", version = versionForge)
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:$versionYarn:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.20.5+build.3")
    })

    modImplementation("net.fabricmc:fabric-loader:$versionFabricLoader")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionUpstream")

    // Remapping dep
    mercuryClasspath("org.junit.jupiter:junit-jupiter-api:5.8.1")
    mercuryClasspath("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    mercuryClasspath("org.mockito:mockito-core:5.4.0")
}

val remapUpstreamSources by tasks.registering {
    group = "sinytra"
}

projectNames.filter { it == "fabric-rendering-v1" }.forEach { projectName ->
    val taskName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, projectName)

    val remapTask = tasks.register("remap${taskName}UpstreamSources", RemapSourceDirectory::class) {
        group = "sinytra"

        projectRoot.set(file("fabric-api-upstream/$projectName"))
        classpath.from(configurations["minecraftNamedCompile"], configurations["mercuryClasspath"])
        classpath.from(project(":intermediary-deobf").configurations["modCompileClasspathMapped"])

        sourceNamespace.set(MappingsNamespace.NAMED.toString())
        targetNamespace.set(MappingsNamespace.MOJANG.toString())
        outputDir.set(file("fabric-api-mojmap"))
    }

    remapUpstreamSources.configure {
        dependsOn(remapTask)
    }
}

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