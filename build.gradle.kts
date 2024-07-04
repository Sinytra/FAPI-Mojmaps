import com.google.common.base.CaseFormat
import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerWriter
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingLayer
import net.fabricmc.loom.configuration.providers.mappings.mojmap.MojangMappingsSpec
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftMetadataProvider
import net.fabricmc.loom.util.Constants
import net.fabricmc.loom.util.download.Download
import net.fabricmc.loom.util.download.DownloadBuilder
import net.fabricmc.loom.util.srg.Tsrg2Writer
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.adapter.MappingNsRenamer
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.tree.MemoryMappingTree
import org.sinytra.gradle.RemapSourceDirectory
import java.nio.file.FileSystems
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText
import java.util.function.Function

plugins {
    java
    id("dev.architectury.loom")
}

val versionMc: String by project
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
}

val ignoredProjects = listOf("fabric-api-bom", "fabric-api-catalog")
val projectNames = file("fabric-api-upstream").list()?.filter { it.startsWith("fabric-") }?.let { it - ignoredProjects } ?: emptyList()

// TODO Move to setup script
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
val yarnMappings: Configuration by configurations.creating

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = versionMc)
    mappings("net.fabricmc:yarn:$versionYarn:v2")
    yarnMappings("net.fabricmc:yarn:$versionYarn:mergedv2")

    modImplementation("net.fabricmc:fabric-loader:$versionFabricLoader")

    // Remapping dep
    mercuryClasspath("org.junit.jupiter:junit-jupiter-api:5.8.1")
    mercuryClasspath("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    mercuryClasspath("org.mockito:mockito-core:5.4.0")
}

val downloadMojmaps by tasks.registering {
    val outputDir = project.layout.buildDirectory.dir(name).get()
    val outputFile = outputDir.file("mojmap.tsrg")
    inputs.property("versionMc", versionMc)
    outputs.file(outputFile)
    extra["outputFile"] = outputFile.asFile

    doLast {
        val cache = project.layout.buildDirectory.dir("tmp/$name").get()
        val provider =
            MinecraftMetadataProvider::class.java.declaredConstructors[0].apply { isAccessible = true }.newInstance(
                MinecraftMetadataProvider.Options(
                    versionMc,
                    Constants.VERSION_MANIFESTS,
                    Constants.EXPERIMENTAL_VERSIONS,
                    null,
                    cache.file("version_manifest.json").asFile.toPath(),
                    cache.file("experimental_version_manifest.json").asFile.toPath(),
                    cache.file("minecraft-info.json").asFile.toPath()
                ),
                Function<String, DownloadBuilder> { Download.create(it) }
            ) as MinecraftMetadataProvider

        val clientMappingsPath = cache.file("mojang/client.txt").asFile.toPath()
        val serverMappingsPath = cache.file("mojang/server.txt").asFile.toPath()

        val clientMappings = provider.versionMeta.download("client_mappings")
        Download.create(clientMappings.url)
            .sha1(clientMappings.sha1)
            .downloadPath(clientMappingsPath)
        val serverMappings = provider.versionMeta.download("server_mappings")
        Download.create(serverMappings.url)
            .sha1(serverMappings.sha1)
            .downloadPath(serverMappingsPath)

        val mojMaps = MojangMappingLayer(
            versionMc,
            clientMappingsPath,
            serverMappingsPath,
            true,
            project.logger,
            MojangMappingsSpec.SilenceLicenseOption { true })
        val mappings = MemoryMappingTree()
        mojMaps.visit(mappings)
        outputFile.asFile.toPath().writeText(
            Tsrg2Writer.serialize(mappings),
            Charsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }
}

val createMappings by tasks.registering(GenerateMergedMappingsTask::class) {
    group = "sinytra"
    dependsOn(downloadMojmaps)
    inputYarnMappings.set { yarnMappings.singleFile }
    inputMojangMappings.set { downloadMojmaps.get().extra["outputFile"] as File }
}

val remapUpstreamSources by tasks.registering {
    group = "sinytra"
}

val projectRoots = projectNames.map { file("fabric-api-upstream/$it") }

projectNames.forEach { projectName ->
    val taskName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, projectName)

    val remapTask = tasks.register("remap${taskName}UpstreamSources", RemapSourceDirectory::class) {
        group = "sinytra"

        projectRoot.set(file("fabric-api-upstream/$projectName"))
        classpath.from(
            configurations["compileClasspath"],
            configurations["mercuryClasspath"],
            configurations["modCompileClasspathMapped"]
        )
        sourcepath.from(projectRoots)
        mappingFile.set(createMappings.flatMap { it.outputFile })

        sourceNamespace.set(MappingsNamespace.NAMED.toString())
        targetNamespace.set(MappingsNamespace.MOJANG.toString())
        outputDir.set(file("fabric-api-mojmap"))
    }

    remapUpstreamSources.configure {
        dependsOn(remapTask)
    }
}

sourceSets.main {
    resources.srcDirs("src/generated/resources")
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

@CacheableTask
open class GenerateMergedMappingsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputYarnMappings: RegularFileProperty = project.objects.fileProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputMojangMappings: RegularFileProperty = project.objects.fileProperty()

    @get:OutputFile
    val outputFile: RegularFileProperty =
        project.objects.fileProperty().convention(project.layout.buildDirectory.file("$name/output.tsrg"))

    @TaskAction
    fun execute() {
        val officialToMojangTree = MemoryMappingTree()
        val officialToNamedTree = MemoryMappingTree()

        val renamer = MappingNsRenamer(officialToMojangTree, mapOf(MappingsNamespace.NAMED.toString() to MappingsNamespace.MOJANG.toString()))
        MappingReader.read(inputMojangMappings.get().asFile.toPath(), renamer)

        FileSystems.newFileSystem(inputYarnMappings.asFile.get().toPath()).use {
            val mappings = it.getPath("mappings", "mappings.tiny")
            MappingReader.read(mappings, officialToNamedTree)
        }

        val combined = MemoryMappingTree()
        val namedSelector = MappingDstNsReorder(combined, MappingsNamespace.NAMED.toString())
        officialToNamedTree.accept(namedSelector)
        officialToMojangTree.accept(combined)

        val filtered = MemoryMappingTree()
        val toOfiSource = MappingSourceNsSwitch(filtered, MappingsNamespace.OFFICIAL.toString())
        val toMojSource = MappingSourceNsSwitch(toOfiSource, MappingsNamespace.NAMED.toString(), true)
        combined.accept(toMojSource)

        outputFile.get().asFile.parentFile.resolve("output.tsrg")
            .toPath().writeText(Tsrg2Writer.serialize(filtered), Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        
        outputFile.get().asFile.parentFile.resolve("yarn.tsrg")
            .toPath().writeText(Tsrg2Writer.serialize(officialToNamedTree), Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
}