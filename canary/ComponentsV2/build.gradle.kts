import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

version = "7.15.0"
description = "Beta backport of ComponentsV2"

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
        # 7.15.0
        * Initial release >w<
    """.trimIndent())

    excludeFromUpdaterJson.set(false)
}

//apply(plugin = "com.gradleup.shadow")
apply(plugin = "com.github.johnrengelman.shadow") // remove when gradle 8

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
