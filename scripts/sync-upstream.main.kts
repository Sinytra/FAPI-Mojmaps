@file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
@file:Import("shared.main.kts")

import org.eclipse.jgit.api.CherryPickResult
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.merge.ContentMergeStrategy
import org.eclipse.jgit.merge.ResolveMerger
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

val logger: Logger = LoggerFactory.getLogger("SyncUpstream")
val fileChangeFilter = listOf(".java", ".accesswidener")

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
        
        sGit.checkout()
            .setName(tempLocalBranch)
            .call()

        update(sGit)
    }
}

fun update(sGit: Git): Boolean {
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
            return false
        }
        if (needsRemap(sGit)) {
            updateToCommit(sGit, nextCommit)
        } else {
            simpleUpdate(sGit, nextCommit)
        }
        return true
    }
    return false
}

fun setupMappedBranch(sGit: Git) {
    logger.info("=== SETTING UP MAPPED BRANCH FOR THE FIRST TIME ===")

    logger.debug("Checking out branch {}", tempLocalBranch)
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
    val result = sGit.cherryPick()
        .setNoCommit(true)
        .include(commit)
        .setContentMergeStrategy(ContentMergeStrategy.THEIRS)
        .call()
    if (result.status != CherryPickStatus.OK) {
        tryResolveIssues(result, sGit)
    }

    // Copy mapped sources
    logger.info("Copying remapped sources")
    mappedSourcesDir.copyRecursively(submoduleDir, true)

    // Commit changes
    finishUpdate(sGit, commit)
}

fun simpleUpdate(sGit: Git, commit: RevCommit) {
    logger.info("CHERRY-PICKING COMMIT ${commit.shortName()}")

    // Checkout mojmap branch
    logger.info("Checking out branch $tempMappedBranch")
    sGit.checkout()
        .setName(tempMappedBranch)
        .call()

    logger.info("Loading original changes from commit")
    val result = sGit.cherryPick()
        .setNoCommit(true)
        .include(commit)
        .setContentMergeStrategy(ContentMergeStrategy.THEIRS)
        .call()
    if (result.status != CherryPickStatus.OK) {
        tryResolveIssues(result, sGit)
    }

    // Commit changes
    finishUpdate(sGit, commit)
}

fun finishUpdate(sGit: Git, commit: RevCommit) {
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

fun tryResolveIssues(result: CherryPickResult, sGit: Git) {
    if (result.status == CherryPickStatus.CONFLICTING) {
        if (result.failingPaths != null && result.failingPaths.values.all { it == ResolveMerger.MergeFailureReason.COULD_NOT_DELETE }) {
            result.failingPaths.forEach { (path) -> Path(path).deleteIfExists() }
            return
        }
        val list = sGit.diff().call()
            .filter { it.changeType == DiffEntry.ChangeType.DELETE }
        if (list.isNotEmpty()) {
            sGit.rm().also { cmd -> list.forEach { cmd.addFilepattern(it.oldPath) } }.call()
            list.forEach { submoduleDir.resolve(it.oldPath).delete() }
        }
        if (sGit.diff().call().isEmpty()) {
            return
        }
    }
    logger.error("Error cherrypicking changes, aborting update")
    sGit.reset()
        .setMode(ResetCommand.ResetType.HARD)
        .setRef(tempMappedBranch)
        .call()
    throw RuntimeException("Error cherrypicking changes")
}

fun findNextCommit(git: Git, currentCommit: RevCommit, branchHead: ObjectId): RevCommit? {
    RevWalk(git.repository).use { revWalk ->
        revWalk.markStart(git.repository.parseCommit(branchHead))
        revWalk.markUninteresting(currentCommit)
        revWalk.sort(RevSort.REVERSE, true)

        return revWalk.firstOrNull { currentCommit in it.parents }
    }
}

fun needsRemap(git: Git): Boolean {
    return showChangedFiles(git, git.repository.resolve("HEAD^^{tree}"), git.repository.resolve("HEAD^{tree}"))
        .any { f -> fileChangeFilter.any(f.newPath::endsWith) }
}

fun showChangedFiles(git: Git, oldHead: ObjectId, head: ObjectId): List<DiffEntry> {
    return git.repository.newObjectReader().use { reader ->
        val oldTreeIter = CanonicalTreeParser()
        oldTreeIter.reset(reader, oldHead)
        val newTreeIter = CanonicalTreeParser()
        newTreeIter.reset(reader, head)
        git.diff()
            .setNewTree(newTreeIter)
            .setOldTree(oldTreeIter)
            .call()
    }
}
