version = "0.2.2"
description = "Display name styles"

android {
    namespace = "moe.lava.awoocord.dns"
}

aliucord {
    // Changelog of your plugin
    changelog.set("""
        # 0.2.2
        * Make font a bit bolder for those on old devices (Android 8 or older) — unfortunately won't be as good as intended but this is the best I could do
        * Fix font flashing and replies' text overlapping

        # 0.2.1
        * Fix app breaking sometimes
        * Actually disable the plugin if it is disabled
        * Removed log spam

        # 0.2.0
        * Apply styles in DMs list

        # 0.1.0
        * Initial release >w<
    """.trimIndent())

    deploy.set(true)
}
