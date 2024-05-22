plugins {
    java
    `java-base`
    `java-library`
    `kotlin-dsl`
}

repositories {
    // The org.jetbrains.kotlin.jvm plugin requires a repository
    // where to download the Kotlin compiler dependencies from.
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
        name = "Architectury"
        url = uri("https://maven.architectury.dev/")
    }
    mavenLocal()
}

dependencies {
    implementation("dev.architectury:architectury-loom:1.6-SNAPSHOT")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("dev.architectury:at:1.0.1")

    implementation("commons-codec:commons-codec:1.17.0")

    // Remapping
    implementation("dev.architectury:mercury:0.1.+")
    implementation("org.cadixdev:mercury-mixin:0.1.1-SNAPSHOT:all") {
        isTransitive = false
    }
    implementation("net.fabricmc:access-widener:2.1.0")
    compileOnly("net.fabricmc:tiny-remapper:0.10.+")

    // Update
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    implementation("codechicken:DiffPatch:1.5.0.+")
}