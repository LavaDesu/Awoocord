import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

version = "8.8.0"
description = "Beta backport of ComponentsV2"

android {
    namespace = "moe.lava.corenary.componentsv2"
}

aliucord {
    // Changelog of your plugin
    changelog.set("""
        TODO {fixed}
        ======================
        * File component
        * SelectV2: searching
        * SelectV2: showing selected items in chat list
        
        Changelog {added marginTop}
        ======================
        # 8.8.0
        * Fix a possible weird crash

        # 8.7.0
        * Prevent ViewRaw crash
        * Add a CV2 tag to distinguish new embeds (will not be in core)

        # 7.15.1
        * Fix broken reply preview >w<

        # 7.15.0
        * Initial release >w<
    """.trimIndent())

    deploy.set(true)
}

apply {
    plugin(libs.plugins.shadow.get().pluginId)
}

val shadowDir = File(buildDir, "intermediates/shadowed")

tasks.register<ShadowJar>("relocateJar") {
    val task = tasks.findByName("compileDebugKotlin")!!
    from(task.outputs)
//    relocate("com.discord.api.botuikit", "moe.lava.awoocanary.componentsv2.botuikit") {
//        exclude("com.discord.api.botuikit.ComponentType")
//    }
    relocate("com.aliucord.coreplugins.componentsv2", "moe.lava.corenary.componentsv2")
    relocate("com.aliucord.coreplugins.ComponentsV2", "moe.lava.corenary.ComponentsV2")
    archiveClassifier.set("shadowed")
    destinationDirectory.set(File(buildDir, "intermediates"))
}

tasks.register<Sync>("copyShadowed") {
    val reloc = tasks.findByName("relocateJar")!! as ShadowJar
    dependsOn(reloc)
    from(zipTree(reloc.archiveFile))
    into(shadowDir)
}

project.afterEvaluate {
    tasks.compileDex {
        val copyShadowed = tasks.findByName("copyShadowed")!! as Sync
        dependsOn(copyShadowed)
        input.setFrom(shadowDir)
    }
}
