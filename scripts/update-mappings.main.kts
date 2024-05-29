@file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
@file:Import("shared.main.kts")

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("SyncUpstream")

Git.open(rootDir).use { git ->
    initSubmodule(git).use { sGit ->
        updateToCommit(sGit)
    }
}

fun updateToCommit(sGit: Git) {
    logger.info("UPDATING MAPPED SOURCES")

    // Checkout yarn branch commit
    logger.info("Checking out branch $tempLocalBranch")
    sGit.checkout()
        .setName(tempLocalBranch)
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

    // Copy mapped sources
    logger.info("Copying remapped sources")
    mappedSourcesDir.copyRecursively(submoduleDir, true)

    // Commit changes
    logger.info("Commiting changes")
    sGit.add()
        .addFilepattern(".")
        .call()
    sGit.commit()
        .setMessage("Update mapped sources")
        .setSign(false)
        .call()

    logger.info("Pushing branch to root repository")
    sGit.push()
        .setRemote(localRemote)
        .setRefSpecs(RefSpec("$tempMappedBranch:$mappedBranch"))
        .call()

    logger.info("=== DONE UPDATING SOURCES ===")
}