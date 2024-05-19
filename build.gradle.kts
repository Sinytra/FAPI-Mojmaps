import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
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
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
}

val versionMc: String by project
val versionForge: String by project
val versionYarn: String by project

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

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = versionMc)
    neoForge(group = "net.neoforged", name = "neoforge", version = versionForge)
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:$versionYarn:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.20.5+build.3")
    })
}

val remote = "upstream"
val localBranch = "fabric/$versionMc"
val remoteBranch = "$remote/$versionMc"

tasks.register("setup") {
    group = "sinytra"

    doFirst {
        val git = Git.open(rootDir)
        val list = git.branchList().setContains(localBranch).call()
        if (list.isNotEmpty()) {
            return@doFirst
        }

        // Add upstream remote
        git.remoteAdd()
            .setName("upstream")
            .setUri(URIish("https://github.com/FabricMC/fabric"))
            .call()
        git.fetch()
            .setRemote("upstream")
            .call()
        // Set up remote tracking branch
        git.branchCreate()
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            .setName(localBranch)
            .setStartPoint(remoteBranch)
            .call()
    }
}

tasks.register("update") {
    group = "sinytra"

    doFirst {
        val git = Git.open(rootDir)
        // Ensure we're up to date
//        git.fetch().setRemote(remote).call() TODO

        val currentCommit = git.repository.parseCommit(git.repository.resolve(localBranch))
        val latestCommit = git.repository.parseCommit(git.repository.resolve(remoteBranch))
        println("CURRENT: ${currentCommit.abbreviate(8).name()} ${currentCommit.shortMessage}")
        println("LATEST: ${latestCommit.abbreviate(8).name()} ${latestCommit.shortMessage}")

        if (currentCommit.equals(latestCommit)) {
            println("UP TO DATE")
        } else {
            
        }
    }
}

