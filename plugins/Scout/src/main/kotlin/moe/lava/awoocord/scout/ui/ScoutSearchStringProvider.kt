package moe.lava.awoocord.scout.ui

import android.content.Context
import com.discord.utilities.search.query.FilterType
import moe.lava.awoocord.scout.FilterTypeExtension

private fun String.decapitalise(context: Context) =
    this.replaceFirstChar { it.lowercase(context.resources.configuration.locales[0]) }

class ScoutSearchStringProvider(private val context: Context) {
    fun getIdentifier(name: String) =
        context.resources.getIdentifier(name, "string", "com.discord")
    fun getString(name: String) =
        context.getString(getIdentifier(name))

    fun stringFor(type: FilterType) = when (type) {
        FilterTypeExtension.BEFORE -> beforeFilterString
        FilterTypeExtension.DURING -> duringFilterString
        FilterTypeExtension.AFTER -> afterFilterString
        FilterTypeExtension.SORT -> sortFilterString
        else -> throw IllegalArgumentException("invalid extended filter type")
    }

    // Surprising!! Discord has localised strings of these
    val beforeFilterString: String
        get() = getString("search_filter_before")
    val duringFilterString: String
        get() = getString("search_filter_during")
    val afterFilterString: String
        get() = getString("search_filter_after")
    val sortFilterString: String
        get() = getString("sort").decapitalise(context)
    val sortOldString: String
        get() = getString("search_oldest_short").decapitalise(context)
}
