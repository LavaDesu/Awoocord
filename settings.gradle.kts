rootProject.name = "Awoocord"

val canaryPlugins = arrayOf("ComponentsV2")

include(
    "AlignThreads",
    "Scout",
    *canaryPlugins,
)

rootProject.children.forEach {
    val isCanary = it.name in canaryPlugins
    val dir = if (isCanary) "canary" else "plugins"
    val name = it.name
    if (isCanary) it.name += "Beta"
    it.projectDir = file("${dir}/${name}")
}
