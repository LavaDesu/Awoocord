import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

version = "7.16.0"
description = "Beta backport of SlashCommandsFix"

aliucord {
    changelog.set("""
        # 7.16.0
        * Initial port >w< thanks @jedenastka
    """.trimIndent())

    excludeFromUpdaterJson.set(false)
}

//apply(plugin = "com.gradleup.shadow")
apply(plugin = "com.github.johnrengelman.shadow") // remove when gradle 8

val shadowDir = File(buildDir, "intermediates/shadowed")

tasks.register<ShadowJar>("relocateJar") {
    val javaTask = tasks.findByName("compileDebugJavaWithJavac")!!
    val kotlinTask = tasks.findByName("compileDebugKotlin")!!
    from(javaTask.outputs, kotlinTask.outputs)
    relocate("com.aliucord.coreplugins.slashcommandsfix", "moe.lava.corenary.slashcommandsfix")
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
