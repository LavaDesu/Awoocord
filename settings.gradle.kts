rootProject.name = "Awoocord"

// This file sets what projects are included. Every time you add a new project, you must add it
// to the includes below.

// Plugins are included like this
include(
    "AlignThreads",
    "Scout"
)

rootProject.children.forEach {
    // Change kotlin to java if you'd rather use java
    it.projectDir = file("plugins/${it.name}")
}
