version = "1.0.1"
description = "More lenient message grouping"

android {
    namespace = "moe.lava.awoocord.bocchi"
}

aliucord {
    // Changelog of your plugin
    changelog.set("""
        # 1.0.1
        * Hide blank space w.r.t attachments and embeds

        # 1.0.0
        * Initial release >w<
    """.trimIndent())

    deploy.set(true)
}
