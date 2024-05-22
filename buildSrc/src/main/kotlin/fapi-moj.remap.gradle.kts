import com.google.gson.JsonParser
import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerVisitor
import net.fabricmc.accesswidener.AccessWidenerWriter
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.task.service.MappingsService
import net.fabricmc.loom.task.service.SourceRemapperService
import net.fabricmc.loom.util.DeletingFileVisitor
import net.fabricmc.loom.util.FileSystemUtil
import net.fabricmc.loom.util.SourceRemapper
import net.fabricmc.loom.util.ZipUtils
import net.fabricmc.loom.util.service.BuildSharedServiceManager
import net.fabricmc.loom.util.service.UnsafeWorkQueueHelper
import net.fabricmc.mappingio.tree.MappingTreeView
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.mercury.Mercury
import org.cadixdev.mercury.mixin.MixinRemapper
import org.cadixdev.mercury.remapper.MercuryRemapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.writeBytes

val sourceSets = extensions.getByType<SourceSetContainer>()

val versionFabricLoader: String by project
val excludedProjects = listOf("fabric-api-bom", "fabric-api-catalog")
val projectNames = file("fabric-api-upstream").list()!!.filter { it.startsWith("fabric-") } - excludedProjects

val prepareRemap by tasks.registering(Copy::class) {
    group = "sinytra"

    projectNames.forEach { from("fabric-api-upstream/$it") { into(it) } }

    destinationDir = file("fabric-api-yarn")
}

val remapTask = tasks.register("remapUpstreamSources", RemapSourceDirectory::class) {
    group = "sinytra"

    projectRoots.from(projectNames.map { file("fabric-api-upstream/$it") })
//    projectRoots.from("fabric-api-upstream/fabric-game-rule-api-v1")
    classpath.from(configurations[JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME])

    sourceNamespace.set(MappingsNamespace.NAMED.toString())
    targetNamespace.set(MappingsNamespace.MOJANG.toString())
    outputDir.set(file("fabric-api-mojmap"))
}

abstract class RemapSourceDirectory : DefaultTask() {
    @get:SkipWhenEmpty
    @get:InputFiles
    val projectRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFiles
    val classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFiles
    val sourcepath: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Input
    val sourceNamespace: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val targetNamespace: Property<String> = project.objects.property(String::class.java)

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Inject
    abstract val buildEventsListenerRegistry: BuildEventsListenerRegistry

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    private val serviceManagerProvider: Provider<BuildSharedServiceManager> =
        BuildSharedServiceManager.createForTask(this, buildEventsListenerRegistry)

    private val sourceMappingService by lazy(::createSourceMapper)

    @Synchronized
    fun createSourceMapper(): SourceRemapperService {
        val serviceManager = serviceManagerProvider.get().get()
        val to = targetNamespace.get()
        val from = sourceNamespace.get()
        val javaCompileRelease = SourceRemapper.getJavaCompileRelease(project)
        return SourceRemapperService::class.java.getDeclaredConstructor(
            MappingsService::class.java,
            ConfigurableFileCollection::class.java,
            Int::class.java
        ).apply { isAccessible = true }.newInstance(
            MappingsService.createDefault(project, serviceManager, from, to),
            classpath,
            javaCompileRelease
        ) as SourceRemapperService
    }

    @TaskAction
    fun run() {
        val mappingService by lazy {
            MappingsService.createDefault(
                project,
                serviceManagerProvider.get().get(),
                sourceNamespace.get(),
                targetNamespace.get()
            )
        }

        val sourcePaths = projectRoots.files
            .map { it.resolve("src") }
            .flatMap {
                listOf(
                    it.resolve("main/java"),
                    it.resolve("client/java"),
                    it.resolve("testmod/java"),
                    it.resolve("testmodClient/java")
                )
            }
            .filter(File::exists)

        val workQueue: WorkQueue = workerExecutor.noIsolation()
        val javaRelease = SourceRemapper.getJavaCompileRelease(project)

        projectRoots.forEach { projectRoot ->
            val output = outputDir.dir(projectRoot.name).get()

            workQueue.submit(MappingWorkAction::class) {
                inputFile.set(projectRoot)
                outputDir.set(output)
                this.mappingServiceUuid.set(UnsafeWorkQueueHelper.create(mappingService))
                this.sourceMappingServiceUuid.set(UnsafeWorkQueueHelper.create(sourceMappingService))
                this.sourcePaths.from(sourcePaths)
                javaCompileRelease.set(javaRelease)
            }
        }
    }
}

interface MappingWorkParameters : WorkParameters {
    val inputFile: RegularFileProperty
    val outputDir: DirectoryProperty
    val mappingServiceUuid: Property<String>
    val sourceMappingServiceUuid: Property<String>
    val sourcePaths: ConfigurableFileCollection
    val javaCompileRelease: Property<Int>
}

abstract class MappingWorkAction : WorkAction<MappingWorkParameters> {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(MappingWorkAction::class.java)
        val mercurySupplier =
            SourceRemapperService::class.java.getDeclaredMethod("createMercury").apply { isAccessible = true }
        val mappingSetMethod =
            SourceRemapperService::class.java.getDeclaredMethod("getMappings").apply { isAccessible = true }
    }

    private val sourceMappingService =
        UnsafeWorkQueueHelper.get(parameters.sourceMappingServiceUuid, SourceRemapperService::class.java)
    private val mappingService = UnsafeWorkQueueHelper.get(parameters.mappingServiceUuid, MappingsService::class.java)

    override fun execute() {
        val inputFile = parameters.inputFile.asFile.get()
        println("Remapping project ${inputFile.name}")

        val mappingSet = mappingSetMethod.invoke(sourceMappingService) as MappingSet
        val mercury = mercurySupplier.invoke(sourceMappingService) as Mercury
        mercury.isGracefulClasspathChecks = false
        mercury.isGracefulJavadocClasspathChecks = true
        mercury.setSourceCompatibilityFromRelease(parameters.javaCompileRelease.get())
        mercury.isFlexibleAnonymousClassMemberLookups = true
        mercury.sourcePath += parameters.sourcePaths.files.map(File::toPath)

        mercury.processors.clear()
        mercury.processors.add(MixinRemapper.create(mappingSet))
        mercury.processors.add(MercuryRemapper.create(mappingSet))

        val sourceRoots = mutableListOf<File>()
        val resourceRoots = mutableListOf<File>()

        inputFile.resolve("src").listFiles()?.forEach {
            sourceRoots += it.resolve("java")
            resourceRoots += it.resolve("resources")
        }

        remapProject(sourceRoots, resourceRoots, mappingSet, mercury)

        println("Finished remapping ${inputFile.name}")
    }

    private fun remapProject(
        sourceRoots: Collection<File>,
        resourceRoots: Collection<File>,
        mappingSet: MappingSet,
        mercury: Mercury
    ) {
        sourceRoots.filter(File::exists).forEach { sourceRoot ->
            val destDir = parameters.outputDir.get().file("${sourceRoot.parentFile.name}/${sourceRoot.name}").asFile
            destDir.mkdirs()

            prepareMixinMappings(sourceRoot.toPath(), mappingSet, mercury.sourcePath, mercury.classPath)

            remapSourcesJar(sourceRoot.toPath(), destDir.toPath(), mercury)
        }
        resourceRoots.filter(File::exists).forEach { resourceRoot ->
            val fmj = resourceRoot.resolve("fabric.mod.json")
            if (fmj.exists()) {
                val json = fmj.bufferedReader().use(JsonParser::parseReader).asJsonObject
                val accessWidener = json.get("accessWidener")?.asString
                if (accessWidener != null) {
                    val awPath = resourceRoot.resolve(accessWidener).toPath()
                    val destDir = parameters.outputDir.get().file("${resourceRoot.parentFile.name}/${resourceRoot.name}").asFile
                    destDir.mkdirs()

                    val mappedPath = destDir.resolve(accessWidener).toPath()

                    val version: Int = awPath.bufferedReader().use { input -> AccessWidenerReader.readVersion(input) }
                    val writer = AccessWidenerWriter(version)
                    val awRemapper = MappingAccessWidenerRemapper(
                        writer,
                        mappingService.memoryMappingTree,
                        MappingsNamespace.NAMED.toString(),
                        MappingsNamespace.MOJANG.toString(),
                        MappingsNamespace.NAMED.toString()
                    )
                    val reader = AccessWidenerReader(awRemapper)

                    awPath.bufferedReader().use(reader::read)
                    mappedPath.writeBytes(writer.write())
                }
            }
        }
    }

    private fun prepareMixinMappings(
        inputFile: Path,
        mappingSet: MappingSet,
        sourcepath: Collection<Path>,
        classpath: Collection<Path>
    ) {
        val mercury = Mercury()
        mercury.setSourceCompatibilityFromRelease(parameters.javaCompileRelease.get())
        mercury.isFlexibleAnonymousClassMemberLookups = true
        mercury.processors.add(MixinRemapper.create(mappingSet))
        mercury.sourcePath += sourcepath
        mercury.classPath += classpath

        val dstDir = Files.createTempDirectory("fabric-loom-dst")

        try {
            mercury.rewrite(inputFile, dstDir)
        } finally {
            Files.walkFileTree(dstDir, DeletingFileVisitor())
        }
    }

    private fun remapSourcesJar(source: Path, destination: Path, mercury: Mercury) {
        if (source == destination) {
            throw UnsupportedOperationException("Cannot remap in place")
        }

        var srcPath: Path? = source
        var isSrcTmp = false

        // Create a temp directory with all of the sources
        if (!Files.isDirectory(source)) {
            isSrcTmp = true
            srcPath = Files.createTempDirectory("fabric-loom-src")
            ZipUtils.unpackAll(source, srcPath)
        }

        if (!Files.isDirectory(destination) && Files.exists(destination)) {
            Files.delete(destination)
        }

        try {
            val dstFs = if (Files.isDirectory(destination)) null else FileSystemUtil.getJarFileSystem(destination, true)

            val dstPath = if (dstFs != null) dstFs.get().getPath("/") else destination

            try {
                mercury.rewrite(srcPath, dstPath)
            } catch (e: Exception) {
                LOGGER.warn("Could not remap $source fully!", e)
            }

            SourceRemapper.copyNonJavaFiles(
                srcPath,
                dstPath,
                LOGGER,
                source
            )
        } finally {
            if (isSrcTmp) {
                Files.walkFileTree(srcPath, DeletingFileVisitor())
            }
        }
    }
}

class MappingAccessWidenerRemapper(
    private val delegate: AccessWidenerVisitor,
    private val remapper: MappingTreeView,
    private val fromNamespace: String,
    toNamespace: String,
    private val toHeaderNamespace: String
) : AccessWidenerVisitor {
    private val fromNamespaceOrdinal = remapper.getNamespaceId(fromNamespace)
    private val toNamespaceOrdinal = remapper.getNamespaceId(toNamespace)

    override fun visitHeader(namespace: String) {
        require(fromNamespace == namespace) {
            ("Cannot remap access widener from namespace '" + namespace + "'."
                    + " Expected: '" + this.fromNamespace + "'")
        }

        delegate.visitHeader(toHeaderNamespace)
    }

    override fun visitClass(name: String, access: AccessWidenerReader.AccessType, transitive: Boolean) {
        delegate.visitClass(
            remapper.mapClassName(name, fromNamespaceOrdinal, toNamespaceOrdinal),
            access,
            transitive
        )
    }

    override fun visitMethod(
        owner: String,
        name: String,
        descriptor: String,
        access: AccessWidenerReader.AccessType,
        transitive: Boolean
    ) {
        delegate.visitMethod(
            remapper.mapClassName(owner, fromNamespaceOrdinal, toNamespaceOrdinal),
            remapper.getMethod(owner, name, descriptor, fromNamespaceOrdinal)?.getDstName(toNamespaceOrdinal)
                ?: name,
            remapper.mapDesc(descriptor, fromNamespaceOrdinal, toNamespaceOrdinal),
            access,
            transitive
        )
    }

    override fun visitField(
        owner: String,
        name: String,
        descriptor: String,
        access: AccessWidenerReader.AccessType,
        transitive: Boolean
    ) {
        delegate.visitField(
            remapper.mapClassName(owner, fromNamespaceOrdinal, toNamespaceOrdinal),
            remapper.getField(owner, name, descriptor, fromNamespaceOrdinal)?.getDstName(toNamespaceOrdinal)
                ?: name,
            remapper.mapDesc(descriptor, fromNamespaceOrdinal, toNamespaceOrdinal),
            access,
            transitive
        )
    }
}