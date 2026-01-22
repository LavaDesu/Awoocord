@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.aliucord.com/releases")
        maven("https://maven.aliucord.com/snapshots")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliucord.com/releases")
        maven("https://maven.aliucord.com/snapshots")
    }
}

rootProject.name = "Awoocord"

val plugins = mapOf(
    "ComponentsV2Beta" to "canary/ComponentsV2",
    "SlashCommandsFixBeta" to "canary/SlashCommandsFix",
    "Clump" to "plugins/Bocchi",
    "Scout" to "plugins/Scout",
    "RoleBlocks" to "plugins/Zinnia",
)

include(*plugins.keys.toTypedArray())

rootProject.children.forEach { project ->
    plugins[project.name]?.let {
        project.projectDir = file(it)
    }
}
