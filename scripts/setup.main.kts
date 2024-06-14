@file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r", "com.google.code.gson:gson:2.10.1")

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
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
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

val rootDir = File(".")
val submoduleDir = File("fabric-api-upstream")
val mappedSourcesDir = File("fabric-api-mojmap")
val logger: Logger = LoggerFactory.getLogger("Shared")

if (!File(rootDir, "build.gradle.kts").exists()) {
    logger.error("Unexpected root directory {}", rootDir.absolutePath)
    throw RuntimeException()
}

val properties = Properties().also { p -> File("gradle.properties").bufferedReader().use(p::load) }

val upstreamBranch: String by properties
val upstreamRemote = "upstream"
val upstreamFabricBranch = "$upstreamRemote/$upstreamBranch"

val localRemote = "root"
val localBranch = "fabric/$upstreamBranch"
val mappedBranch = "mojmap/$upstreamBranch"
val localMappedBranch = "$localRemote/$mappedBranch"
val tempLocalBranch = "temp/$localBranch"
val tempMappedBranch = "temp/$mappedBranch"

val originRemote = "origin"
val originMappedBranch = "$originRemote/$mappedBranch"

val fileChangeFilter = listOf(".java", ".accesswidener")

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

fun setupTask() {
    val customProperty = "custom"
    val interfacesProperty = "loom:injected_interfaces"
    val architecturyCommonJson = File("src/generated/resources/architectury.common.json")
    
    setupOnSubmoduleBranch()
    
    val foundInterfaces = mutableMapOf<String, MutableSet<String>>()
    
    logger.info("Looking for injected interfaces...")
    (submoduleDir.listFiles() ?: emptyArray())
        .filter { it.name.startsWith("fabric-") }
        .map {
            it to listOf(
                it.resolve("src/main/resources/fabric.mod.json"),
                it.resolve("src/client/resources/fabric.mod.json")
            ).filter(File::exists)
        }
        .filter { (_, files) -> files.isNotEmpty() }
        .forEach { (proj, files) ->
            files.forEach readFile@ { mod ->
                val json = mod.bufferedReader().use(JsonParser::parseReader).asJsonObject
                val custom = json.getAsJsonObject(customProperty)?.asJsonObject ?: return@readFile
                val interfaces = custom.getAsJsonObject(interfacesProperty)?.asJsonObject ?: return@readFile
    
                logger.info("Adding injected interfaces from project ${proj.name}")
                interfaces.entrySet().forEach { (key, value) ->
                    val list = foundInterfaces.getOrPut(key, ::mutableSetOf)
                    value.asJsonArray.forEach { list += it.asString }
                }
            }
        }
    
    foundInterfaces.forEach { (t, u) ->
        logger.info("Class $t")
        u.forEach { logger.info("\t $it") }
    }
    
    data class ArchitecturyCommonJson(val injectedInterfaces: Map<String, MutableSet<String>>)
    
    logger.info("Writing architectury.common.json")
    architecturyCommonJson.parentFile.mkdirs()
    val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create()
    val serialized: String = gson.toJson(ArchitecturyCommonJson(foundInterfaces))
    architecturyCommonJson.writeText(serialized)
}

fun syncUpstreamTask() {
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
}

fun updateToCommitTask() {
    Git.open(rootDir).use { git ->
        initSubmodule(git).use { sGit ->
            updateToCommit(sGit)
        }
    }
}

if (args.isEmpty()) {
    logger.warn("No arguments provided")
    logger.warn("Available tasks: setup, sync, update")
} else {
    when (args[0]) {
        "setup" -> setupTask()
        "sync" -> syncUpstreamTask()
        "update" -> updateToCommitTask()
    }
}