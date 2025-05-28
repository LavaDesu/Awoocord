package moe.lava.awoocord.scout

import com.discord.utilities.search.query.FilterType

object FilterTypeExtension {
    lateinit var BEFORE: FilterType
    lateinit var DURING: FilterType
    lateinit var AFTER: FilterType
    lateinit var SORT: FilterType
    lateinit var dates: Array<FilterType>
    lateinit var values: Array<FilterType>
}

