rootProject.name = "Awoocord"

val plugins = mapOf(
    "ComponentsV2Beta" to "canary/ComponentsV2",
    "SlashCommandsFixBeta" to "canary/SlashCommandsFix",
    "Scout" to "plugins/Scout",
    "RoleBlocks" to "plugins/Zinnia",
)

include(*plugins.keys.toTypedArray())

rootProject.children.forEach { project ->
    plugins[project.name]?.let {
        project.projectDir = file(it)
    }
}
