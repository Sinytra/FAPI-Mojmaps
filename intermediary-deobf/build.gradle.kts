plugins {
    java
    id("dev.architectury.loom")
}

val versionMc: String by project
val versionYarn: String by project
val versionFabricLoader: String by project
val versionUpstream: String by project

repositories {
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = versionMc)
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:$versionYarn:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.20.5+build.3")
    })

    modImplementation("net.fabricmc:fabric-loader:$versionFabricLoader")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionUpstream")
}
