@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:Import("shared.main.kts")

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

val rootDir = File(".")
val logger: Logger = LoggerFactory.getLogger("Setup")
val upstreamDir = File("fabric-api-upstream")
val customProperty = "custom"
val interfacesProperty = "loom:injected_interfaces"
val architecturyCommonJson = File("src/generated/resources/architectury.common.json")

setupOnSubmoduleBranch()

val foundInterfaces = mutableMapOf<String, MutableSet<String>>()

logger.info("Looking for injected interfaces...")
(upstreamDir.listFiles() ?: emptyArray())
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