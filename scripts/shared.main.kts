@file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.URIish
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.util.*
import java.util.concurrent.TimeUnit

val rootDir = File(".")
val submoduleDir = File("fabric-api-upstream")
val mappedSourcesDir = File("fabric-api-mojmap")
val sharedLogger: Logger = LoggerFactory.getLogger("Shared")

if (!File(rootDir, "build.gradle.kts").exists()) {
    sharedLogger.error("Unexpected root directory {}", rootDir.absolutePath)
    throw RuntimeException()
}

val properties = Properties().also { p -> File("gradle.properties").bufferedReader().use(p::load) }

val versionMc: String by properties

val upstreamRemote = "upstream"
val upstreamFabricBranch = "$upstreamRemote/$versionMc"

val localRemote = "root"
val localBranch = "fabric/$versionMc"
val mappedBranch = "mojmap/$versionMc"
val localMappedBranch = "$localRemote/$mappedBranch"
val tempLocalBranch = "temp/$localBranch"
val tempMappedBranch = "temp/$mappedBranch"

val originRemote = "origin"
val originMappedBranch = "$originRemote/$mappedBranch"

fun RevCommit.shortName() = "${abbreviate(8).name()} $shortMessage"

fun Git.branchExists(name: String, remote: Boolean = false) =
    repository.exactRef("refs/${if (remote) "remotes" else "heads"}/$name") != null

fun setupOnSubmoduleBranch() {
    Git.open(rootDir).use { git ->
        initSubmodule(git).use { sGit ->
            sGit.checkout()
                .setName(tempLocalBranch)
                .call()
        }
    }
}

fun initSubmodule(git: Git): Git {
    if (!submoduleDir.exists()) {
        sharedLogger.info("Initializing submodule")
        Git.init().setDirectory(submoduleDir).call()
    }
    val sGit = Git.open(submoduleDir)

    if (!sGit.branchExists(tempLocalBranch)) {
        sharedLogger.info("Creating submodule remote tracking branch $tempLocalBranch")
        initSubmoduleBranch(sGit, git)
    }

    if (!git.branchExists(localBranch)) {
        sharedLogger.info("INITIALIZING ROOT FABRIC REMOTE TRACKING BRANCH")
        sGit.checkout()
            .setName(localBranch)
            .call()
        sGit.push()
            .setRemote(localRemote)
            .call()
    }

    if (git.remoteList().call().none { it.name == upstreamRemote }) {
        git.remoteAdd()
            .setName(upstreamRemote)
            .setUri(URIish("https://github.com/FabricMC/fabric"))
            .call()
    }

    return sGit
}

fun initSubmoduleBranch(git: Git, rootGit: Git) {
    // Add upstream remote
    git.remoteAdd()
        .setName(upstreamRemote)
        .setUri(URIish("https://github.com/FabricMC/fabric"))
        .call()
    // Add root remote
    git.remoteAdd()
        .setName(localRemote)
        .setUri(URIish(rootDir.toURI().toURL()))
        .call()
    listOf(upstreamRemote, localRemote).forEach { git.fetch().setRemote(it).call() }
    // Set up remote tracking branch
    git.checkout()
        .setCreateBranch(true)
        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
        .setName(tempLocalBranch)
        .setStartPoint(if (rootGit.branchExists(localBranch)) "root/$localBranch" else upstreamFabricBranch)
        .call()
}

fun runCommand(vararg args: String) {
    val process = ProcessBuilder(*args)
        .directory(rootDir)
        .redirectOutput(Redirect.INHERIT)
        .redirectError(Redirect.INHERIT)
        .start()
    process.waitFor(60, TimeUnit.MINUTES)
    if (process.exitValue() != 0) {
        throw RuntimeException("Error running command ${listOf(args)}")
    }
}