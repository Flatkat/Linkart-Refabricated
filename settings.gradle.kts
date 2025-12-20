pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        gradlePluginPortal()
    }
}

plugins {
  id("dev.kikugie.stonecutter") version "0.8-alpha.14"
}

stonecutter {
    create(rootProject) {
        versions("1.21.1", "1.21.2", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.10", "1.21.11")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "linkart-refabricated"