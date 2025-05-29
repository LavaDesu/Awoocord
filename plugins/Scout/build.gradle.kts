version = "1.1.0"
description = "Backported and improved search functionality"

aliucord {
    // Changelog of your plugin
    changelog.set("""
        # 1.1.0 - Look out, Scout has:updates
        * Add "has:forward" and "has:poll" filters
        * Add "exclude:" filter. It is the opposite of "has:" and filters out matching elements

        # 1.0.1
        * Fix not being able to search more than one page with sort:old

        # 1.0.0
        * Initial release >w<
    """.trimIndent())

    excludeFromUpdaterJson.set(false)
}
