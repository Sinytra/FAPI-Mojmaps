@file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.util.*
import java.util.concurrent.TimeUnit

val rootDir = File(".")
val logger: Logger = LoggerFactory.getLogger("SyncUpstream")

if (!File(rootDir, "build.gradle.kts").exists()) {
    logger.error("Unexpected root directory {}", rootDir.absolutePath)
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

val submoduleDir = File("fabric-api-upstream")
val mappedSourcesDir = File("fabric-api-mojmap")

fun RevCommit.shortName() = "${abbreviate(8).name()} $shortMessage"

fun Git.branchExists(name: String, remote: Boolean = false) =
    repository.exactRef("refs/${if (remote) "remotes" else "heads"}/$name") != null

Git.open(rootDir).use { git ->
    initSubmodule(git).use { sGit ->
        // Ensure we're up to date
        git.fetch().setRemote(upstreamRemote).call()

        if (!sGit.branchExists(tempMappedBranch)) {
            if (git.branchExists(originMappedBranch, true)) {
                if (!git.branchExists(mappedBranch)) {
                    logger.info("Pulling remote mapped branch from $originRemote")
                    git.branchCreate()
                        .setName(mappedBranch)
                        .setStartPoint(originMappedBranch)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                        .call()
                }

                logger.info("Creating branch $tempMappedBranch")
                sGit.checkout()
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setCreateBranch(true)
                    .setForceRefUpdate(true)
                    .setName(tempMappedBranch)
                    .setStartPoint(localMappedBranch)
                    .call()
            } else {
                setupMappedBranch(sGit)
            }
        }

        update(sGit)
    }
}

fun update(sGit: Git) {
    val currentCommit = sGit.repository.parseCommit(sGit.repository.resolve(tempLocalBranch))
    val latestCommit = sGit.repository.parseCommit(sGit.repository.resolve(upstreamFabricBranch))
    logger.info("CURRENT: ${currentCommit.shortName()}")
    logger.info("LATEST: ${latestCommit.shortName()}")

    if (currentCommit.equals(latestCommit)) {
        logger.info("UP TO DATE")
    } else {
        val commitDistance = sGit.log().addRange(currentCommit, latestCommit).call().count()

        logger.warn("OUTDATED - WE ARE $commitDistance COMMITS BEHIND")

        val nextCommit = findNextCommit(sGit, currentCommit, latestCommit) ?: run {
            logger.info("NO UPDATES FOUND")
            return
        }

        updateToCommit(sGit, nextCommit)
    }
}

fun setupMappedBranch(sGit: Git) {
    logger.info("=== SETTING UP MAPPED BRANCH FOR THE FIRST TIME ===")

    logger.debug("Checking out branch $tempLocalBranch")
    sGit.checkout()
        .setName(tempLocalBranch)
        .call()

    logger.debug("Remapping upstream sources using Gradle")
    // Invoke gradle remap
    runCommand("gradlew.bat", "remapUpstreamSources")

    logger.info("Creating branch $tempMappedBranch")
    sGit.checkout()
        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
        .setCreateBranch(true)
        .setForceRefUpdate(true)
        .setName(tempMappedBranch)
        .setStartPoint(tempLocalBranch)
        .call()

    logger.info("Copying remapped sources")
    mappedSourcesDir.copyRecursively(submoduleDir, true)

    logger.info("Commiting changes")
    sGit.add()
        .addFilepattern(".")
        .call()
    sGit.commit()
        .setMessage("Remap to Mojang mappings")
        .setSign(false)
        .call()

    logger.info("Pushing branch to root repository")
    sGit.push()
        .setRemote(localRemote)
        .setRefSpecs(RefSpec("$tempMappedBranch:$mappedBranch"))
        .call()

    logger.info("=== DONE SETTING UP MAPPED BRANCH ===")
}

fun updateToCommit(sGit: Git, commit: RevCommit) {
    logger.info("UPDATING TO ${commit.shortName()}")

    // Checkout yarn branch commit
    logger.info("Checking out commit")
    sGit.checkout()
        .setName(commit.name)
        .call()

    // Remap sources
    logger.info("Remapping yarn sources using Gradle")
    if (mappedSourcesDir.exists()) mappedSourcesDir.deleteRecursively()
    runCommand("gradlew.bat", "remapUpstreamSources")

    // Checkout mojmap branch
    logger.info("Checking out branch $tempMappedBranch")
    sGit.checkout()
        .setName(tempMappedBranch)
        .call()

    // Load initial changes
    logger.info("Loading original changes from commit")
    sGit.cherryPick()
        .setNoCommit(true)
        .include(commit)
        .call()

    // Copy mapped sources
    logger.info("Copying remapped sources")
    mappedSourcesDir.copyRecursively(submoduleDir, true)

    // Commit changes
    logger.info("Commiting changes")
    sGit.add()
        .addFilepattern(".")
        .call()

    sGit.commit()
        .setAuthor(commit.authorIdent)
        .setCommitter(commit.authorIdent)
        .setMessage(commit.fullMessage)
        .setSign(false)
        .call()

    // Rebase branch
    logger.info("Updating branch $localBranch to next commit")
    sGit.checkout()
        .setName(tempLocalBranch)
        .call()
    sGit.rebase()
        .setUpstream(commit)
        .call()
    sGit.push()
        .setRefSpecs(RefSpec("$tempLocalBranch:$localBranch"))
        .call()

    logger.info("Updating mapped branch $mappedBranch")
    sGit.push()
        .setRemote(localRemote)
        .setRefSpecs(RefSpec("$tempMappedBranch:$mappedBranch"))
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

fun findNextCommit(git: Git, currentCommit: RevCommit, branchHead: ObjectId): RevCommit? {
    RevWalk(git.repository).use { revWalk ->
        revWalk.markStart(git.repository.parseCommit(branchHead))
        revWalk.markUninteresting(currentCommit)
        revWalk.sort(RevSort.REVERSE, true)

        return revWalk.firstOrNull { currentCommit in it.parents }
    }
}

fun initSubmodule(git: Git): Git {
    if (!submoduleDir.exists()) {
        logger.info("Initializing submodule")
        Git.init().setDirectory(submoduleDir).call()
    }
    val sGit = Git.open(submoduleDir)

    if (!sGit.branchExists(tempLocalBranch)) {
        logger.info("Creating submodule remote tracking branch $tempLocalBranch")
        initSubmoduleBranch(sGit, git)
    }

    if (!git.branchExists(localBranch)) {
        logger.info("INITIALIZING ROOT FABRIC REMOTE TRACKING BRANCH")
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