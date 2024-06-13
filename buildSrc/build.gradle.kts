plugins {
    java
    `java-base`
    `java-library`
    `kotlin-dsl`
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
        name = "Architectury"
        url = uri("https://maven.architectury.dev/")
    }
    maven { 
        name = "Sinytra"
        url = uri("https://maven.su5ed.dev/releases")
    }
}

dependencies {
    implementation("dev.architectury:architectury-loom:1.6-SNAPSHOT")

    implementation("com.google.guava:guava:33.2.0-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.fabricmc:mapping-io:0.6.1")

    // Remapping
    implementation("org.sinytra:mercury:0.1.+")
    implementation("org.sinytra:mercury-mixin:0.2.0-SNAPSHOT:all") {
        isTransitive = false
    }
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("net.fabricmc:lorenz-tiny:4.0.2")
    compileOnly("net.fabricmc:tiny-remapper:0.10.+")
}