package moe.lava.awoocord.scout

import android.content.Context
import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.discord.BuildConfig
import com.discord.databinding.WidgetSearchSuggestionsItemHasBinding
import com.discord.restapi.RequiredHeadersInterceptor
import com.discord.restapi.RequiredHeadersInterceptor.HeadersProvider
import com.discord.restapi.RestAPIBuilder
import com.discord.simpleast.core.parser.*
import com.discord.stores.StoreSearch
import com.discord.stores.StoreSearchInput
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload
import com.discord.utilities.mg_recycler.SingleTypePayload
import com.discord.utilities.rest.RestAPI.AppHeadersProvider
import com.discord.utilities.search.network.`SearchFetcher$getRestObservable$3`
import com.discord.utilities.search.network.SearchQuery
import com.discord.utilities.search.query.FilterType
import com.discord.utilities.search.query.node.QueryNode
import com.discord.utilities.search.query.node.answer.HasAnswerOption
import com.discord.utilities.search.query.node.answer.HasNode
import com.discord.utilities.search.query.node.content.ContentNode
import com.discord.utilities.search.query.node.filter.FilterNode
import com.discord.utilities.search.query.parsing.QueryParser
import com.discord.utilities.search.strings.SearchStringProvider
import com.discord.utilities.search.suggestion.SearchSuggestionEngine
import com.discord.utilities.search.suggestion.entries.*
import com.discord.utilities.search.validation.SearchData
import com.discord.widgets.search.suggestions.WidgetSearchSuggestionsAdapter
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.lytefast.flexinput.R
import moe.lava.awoocord.scout.api.SearchAPIInterface
import moe.lava.awoocord.scout.parsing.*
import moe.lava.awoocord.scout.ui.*
import java.util.regex.Pattern

@AliucordPlugin()
@Suppress("unused", "unchecked_cast")
class Scout : Plugin() {
    lateinit var scoutRes: ScoutResource
    lateinit var ssProvider: ScoutSearchStringProvider
    lateinit var searchApi: SearchAPIInterface

    init {
        needsResources = true
    }

    override fun load(context: Context) {
        scoutRes = ScoutResource(resources)
        ssProvider = ScoutSearchStringProvider(context)
        searchApi = buildSearchApi(context)
    }

    override fun start(context: Context) {
        extendFilterType()
        extendHasAnswerOption()
        patchHasAnswerOption()
        patchHasNode()
        patchQuery()
        patchQueryParser()
        patchSearchUI(context)
    }

    override fun stop(context: Context) {
        resetFilterType()
        resetHasAnswerOption()
        patcher.unpatchAll()
    }

    // Creates a new custom search API implementation, for the extra `min_id` param in search queries
    private fun buildSearchApi(context: Context): SearchAPIInterface {
        @Suppress("cast_never_succeeds")
        val appHeadersProvider = AppHeadersProvider.INSTANCE as HeadersProvider
        val requiredHeadersInterceptor = RequiredHeadersInterceptor(appHeadersProvider)
        val persistentCookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
        val restAPIBuilder = RestAPIBuilder(BuildConfig.HOST_API, persistentCookieJar)

        return RestAPIBuilder.`build$default`(
            restAPIBuilder,
            SearchAPIInterface::class.java,
            false,
            0L,
            listOf(requiredHeadersInterceptor),
            "client_base",
            false,
            null,
            102,
            null
        ) as SearchAPIInterface
    }

    private var origFilterTypes: Array<FilterType>? = null
    // Creates new pseudo-values of the `FilterType` enum for date filters
    @Suppress("LocalVariableName")
    private fun extendFilterType() {
        val cls = FilterType::class.java
        val constructor = cls.declaredConstructors[0]
        constructor.isAccessible = true

        val field = cls.getDeclaredField("\$VALUES")
        field.isAccessible = true
        val values = field.get(null) as Array<FilterType>
        origFilterTypes = origFilterTypes ?: values
        var nextIdx = values.size

        val EXCLUDE = constructor.newInstance("EXCLUDE", nextIdx++) as FilterType
        val BEFORE = constructor.newInstance("BEFORE", nextIdx++) as FilterType
        val DURING = constructor.newInstance("DURING", nextIdx++) as FilterType
        val AFTER = constructor.newInstance("AFTER", nextIdx++) as FilterType
        val SORT = constructor.newInstance("SORT", nextIdx) as FilterType
        FilterTypeExtension.EXCLUDE = EXCLUDE
        FilterTypeExtension.BEFORE = BEFORE
        FilterTypeExtension.DURING = DURING
        FilterTypeExtension.AFTER = AFTER
        FilterTypeExtension.SORT = SORT
        FilterTypeExtension.dates = arrayOf(BEFORE, DURING, AFTER)
        FilterTypeExtension.values = arrayOf(EXCLUDE, BEFORE, DURING, AFTER, SORT)

        val newValues = values.toMutableList()
        newValues.addAll(FilterTypeExtension.values)
        field.set(null, newValues.toTypedArray())
    }

    private fun resetFilterType() {
        if (origFilterTypes == null)
            return logger.error("No unpatched filter types?", null)

        val cls = FilterType::class.java
        val field = cls.getDeclaredField("\$VALUES")
        field.isAccessible = true
        field.set(null, origFilterTypes)
        origFilterTypes = null
    }

    private var origHasAnswerOptions: Array<HasAnswerOption>? = null
    // Creates new pseudo-values of the `HasAnswerOption` enum for poll and forwarded filters
    @Suppress("LocalVariableName")
    private fun extendHasAnswerOption() {
        val cls = HasAnswerOption::class.java
        val constructor = cls.declaredConstructors[0]
        constructor.isAccessible = true

        val field = cls.getDeclaredField("\$VALUES")
        field.isAccessible = true
        val values = field.get(null) as Array<HasAnswerOption>
        origHasAnswerOptions = origHasAnswerOptions ?: values
        var nextIdx = values.size

        val POLL = constructor.newInstance("POLL", nextIdx++, "poll") as HasAnswerOption
        val SNAPSHOT = constructor.newInstance("SNAPSHOT", nextIdx, "snapshot") as HasAnswerOption
        HasAnswerOptionExtension.POLL = POLL
        HasAnswerOptionExtension.SNAPSHOT = SNAPSHOT
        HasAnswerOptionExtension.values = arrayOf(POLL, SNAPSHOT)

        val newValues = values.toMutableList()
        newValues.addAll(HasAnswerOptionExtension.values)
        field.set(null, newValues.toTypedArray())
    }

    private fun resetHasAnswerOption() {
        if (origHasAnswerOptions == null)
            return logger.error("No unpatched 'has' options?", null)

        val cls = HasAnswerOption::class.java
        val field = cls.getDeclaredField("\$VALUES")
        field.isAccessible = true
        field.set(null, origHasAnswerOptions)
        origHasAnswerOptions = null
    }

    // Patches various methods that use HasAnswerOption to include our new options
    private fun patchHasAnswerOption() {
        patcher.before<HasAnswerOption.Companion>(
            "getOptionFromString",
            String::class.java,
            SearchStringProvider::class.java
        ) { param ->
            val str = param.args[0] as String
            if (str == ssProvider.hasPollString)
                param.result = HasAnswerOptionExtension.POLL
            else if (str == ssProvider.hasForwardString)
                param.result = HasAnswerOptionExtension.SNAPSHOT
        }

        patcher.before<HasAnswerOption>(
            "getLocalizedInputText",
            SearchStringProvider::class.java
        ) { param ->
            if (this == HasAnswerOptionExtension.POLL)
                param.result = ssProvider.hasPollString
            else if (this == HasAnswerOptionExtension.SNAPSHOT)
                param.result = ssProvider.hasForwardString
        }

        patcher.instead<QueryParser.Companion>(
            "createHasAnswerRegex",
            SearchStringProvider::class.java
        ) { param ->
            val ossProvider = param.args[0] as SearchStringProvider

            val matches = HasAnswerOption.values().joinToString("|") { it.getLocalizedInputText(ossProvider) }
            "^\\s*($matches)"
        }

        // Patch to set icons
        patcher.before<WidgetSearchSuggestionsAdapter.HasViewHolder>(
            "onConfigure",
            Int::class.java,
            MGRecyclerDataPayload::class.java,
        ) { param ->
            val suggestion = (param.args[1] as SingleTypePayload<HasSuggestion>).data
            val option = suggestion.hasAnswerOption

            val resID = when (option) {
                HasAnswerOptionExtension.POLL -> "baseline_poll_24"
                HasAnswerOptionExtension.SNAPSHOT -> "baseline_forward_to_inbox_24"
                else -> null
            }

            resID?.let {
                val bindingField = this::class.java.getDeclaredField("binding")
                bindingField.isAccessible = true
                val binding = bindingField.get(this) as WidgetSearchSuggestionsItemHasBinding

                binding.d.text = option.getLocalizedInputText(null)
                binding.b.setOnClickListener {
                    WidgetSearchSuggestionsAdapter.HasViewHolder.`access$getAdapter$p`(this).onHasClicked.invoke(option)
                }

                binding.c.setImageDrawable(scoutRes.getDrawable(it))

                param.result = null
            }
        }

        patcher.instead<SearchSuggestionEngine>(
            "getHasSuggestions",
            CharSequence::class.java,
            FilterType::class.java,
            SearchStringProvider::class.java,
        ) { param ->
            val query = param.args[0] as CharSequence
            val filterType = param.args[1] as FilterType
            val ossProvider = param.args[2] as SearchStringProvider

            if (filterType != FilterType.HAS && filterType != FilterTypeExtension.EXCLUDE)
                return@instead listOf<Any>()

            val res = mutableListOf<HasSuggestion>()
            for (opt in HasAnswerOption.values()) {
                val filterText = opt.getLocalizedInputText(ossProvider)

                if (filterText.contains(query))
                    res.add(HasSuggestion(opt))
            }
            res.toList()
        }

    }

    // Patching HasNode related methods for our exclude: filter type
    private fun patchHasNode() {
        patcher.instead<HasNode>("getValidFilters") {
            setOf(FilterTypeExtension.EXCLUDE, FilterType.HAS)
        }

        // Patch updateQuery to either include or exclude our has option
        patcher.instead<HasNode>(
            "updateQuery",
            SearchQuery.Builder::class.java,
            SearchData::class.java,
            FilterType::class.java,
        ) { param ->
            val builder = param.args[0] as SearchQuery.Builder?
            val filterType = param.args[2] as FilterType

            checkNotNull(builder) { "queryBuilder" }

            val field = HasNode::class.java.getDeclaredField("hasAnswerOption")
            field.isAccessible = true
            val opt = field.get(this) as HasAnswerOption

            if (filterType == FilterType.HAS)
                builder.appendParam("has", opt.restParamValue);
            else if (filterType == FilterTypeExtension.EXCLUDE)
                builder.appendParam("has", "-" + opt.restParamValue);
        }

        // Patching the behaviour when the has suggestion is clicked
        patcher.before<StoreSearchInput>(
            "onHasClicked",
            HasAnswerOption::class.java,
            CharSequence::class.java,
            CharSequence::class.java,
            List::class.java,
        ) { param ->
            val opt = param.args[0] as HasAnswerOption
            val hasFilterText = param.args[1] as CharSequence
            val filterAnswer = param.args[2] as CharSequence
            val query = param.args[3] as List<QueryNode>

            val replaceAndPublish = StoreSearchInput::class.java.getDeclaredMethod(
                "replaceAndPublish",
                Int::class.javaPrimitiveType!!,
                List::class.java,
                List::class.java
            )
            replaceAndPublish.isAccessible = true

            val getAnswerReplacementStart = StoreSearchInput::class.java.getDeclaredMethod(
                "getAnswerReplacementStart",
                List::class.java,
            )
            getAnswerReplacementStart.isAccessible = true

            logger.info(query.joinToString("|") { it.text })

            val replacementIdx = getAnswerReplacementStart.invoke(this, query) as Int
            val previousFilterText = query[replacementIdx]
            val filterNode = if (previousFilterText.text == ssProvider.excludeFilterString)
                FilterNode(FilterTypeExtension.EXCLUDE, ssProvider.excludeFilterString)
            else
                FilterNode(FilterType.HAS, hasFilterText)

            replaceAndPublish.invoke(this, replacementIdx, listOf(filterNode, HasNode(opt, filterAnswer)), query)
        }
    }

    // Patches the search query to also insert `min_id`, required for searching "after:" and "during:"
    private fun patchQuery() {
        patcher.patch(
            `SearchFetcher$getRestObservable$3`::class.java.getDeclaredMethod("call", Integer::class.java),
            PreHook { param ->
                val self = param.thisObject as `SearchFetcher$getRestObservable$3`<*, *>
                val retryAttempts = param.args[0] as Int?
                val params = self.`$searchQuery`.params

                var minID = params["min_id"]
                var maxID = params["max_id"]
                val sortOrder = params["sort_order"]
                self.`$oldestMessageId`?.let {
                    if (sortOrder?.getOrNull(0) == "asc")
                        minID = listOf(it.toString())
                    else
                        maxID = listOf(it.toString())
                }

                param.result = if (self.`$searchTarget`.type == StoreSearch.SearchTarget.Type.GUILD)
                    searchApi.searchGuildMessages(
                        self.`$searchTarget`.id,
                        minID,
                        maxID,
                        params["author_id"],
                        params["mentions"],
                        params["channel_id"],
                        params["has"],
                        params["content"],
                        retryAttempts,
                        self.`$searchQuery`.includeNsfw,
                        listOf("timestamp"),
                        sortOrder
                    )
                else
                    searchApi.searchChannelMessages(
                        self.`$searchTarget`.id,
                        minID,
                        maxID,
                        params["author_id"],
                        params["mentions"],
                        params["has"],
                        params["content"],
                        retryAttempts,
                        self.`$searchQuery`.includeNsfw,
                        listOf("timestamp"),
                        sortOrder
                    )
            }
        )
    }

    // Patch parser for date parsing
    private fun patchQueryParser() {
        patcher.after<QueryParser>(SearchStringProvider::class.java) {
            // We need to access and insert into the rules before the rest
            val field = Parser::class.java.getDeclaredField("rules").apply { isAccessible = true }
            val rules = field.get(this) as ArrayList<Rule<Context, QueryNode, Any>>
            rules.addAll(0, listOf(
                UserIdNode.getUserIdRule(),
                DateNode.getBeforeRule(ssProvider.beforeFilterString),
                DateNode.getDuringRule(ssProvider.duringFilterString),
                DateNode.getAfterRule(ssProvider.afterFilterString),
                DateNode.getDateRule(),
                SortNode.getFilterRule(ssProvider.sortFilterString),
                SortNode.getSortRule(ssProvider),
                SimpleParserRule(Pattern.compile("^\\s*?${ssProvider.excludeFilterString}:", 64)) { _, _, obj ->
                    ParseSpec(FilterNode(FilterTypeExtension.EXCLUDE, ssProvider.excludeFilterString), obj)
                }
            ))
        }
    }

    // This is probably the worst bit of this plugin
    private fun patchSearchUI(context: Context) {
        // Run when a filter suggestion is clicked
        // Most of the code is copied from its implementation
        // Patch needed to support the new filter types
        patcher.before<StoreSearchInput>(
            "onFilterClicked",
            FilterType::class.java,
            SearchStringProvider::class.java,
            List::class.java,
        ) { param ->
            val filter = param.args[0] as FilterType
            if (filter !in FilterTypeExtension.values)
                return@before; // Exit if not an extended filter type

            val replaceAndPublish = StoreSearchInput::class.java.getDeclaredMethod(
                "replaceAndPublish",
                Int::class.javaPrimitiveType!!,
                List::class.java,
                List::class.java
            )
            replaceAndPublish.isAccessible = true

            val getAnswerReplacementStart = StoreSearchInput::class.java.getDeclaredMethod(
                "getAnswerReplacementStart",
                List::class.java,
            )
            getAnswerReplacementStart.isAccessible = true

            // Original implementation
            val filterNode = FilterNode(filter, ssProvider.stringFor(filter))
            val list = (param.args[2] as List<QueryNode>).toMutableList()
            val lastIndex = if (list.isEmpty()) {
                0
            } else if (list.last() is ContentNode)
                list.lastIndex
            else
                list.size

            // Open a Date Picker
            if (filter in FilterTypeExtension.dates) {
                replaceAndPublish.invoke(this, lastIndex, listOf(filterNode), list)
                DatePickerFragment.open(Utils.appActivity.supportFragmentManager) {
                    replaceAndPublish.invoke(this,
                        getAnswerReplacementStart.invoke(this, list),
                        listOf(filterNode, DateNode(it)),
                        list
                    );
                }
            }

            if (filter == FilterTypeExtension.SORT)
                replaceAndPublish.invoke(this,
                    lastIndex,
                    listOf(filterNode, SortNode(ssProvider.sortOldString)),
                    list
                );

            if (filter == FilterTypeExtension.EXCLUDE)
                replaceAndPublish.invoke(this,
                    lastIndex,
                    listOf(filterNode),
                    list
                );

            param.result = null
        }

        // Patch to set icons
        @Suppress("ResourceType")
        patcher.before<WidgetSearchSuggestionsAdapter.FilterViewHolder>(
            "getIconDrawable",
            Context::class.java,
            FilterType::class.java
        ) { param ->
            val type = param.args[1] as FilterType
            val (isDiscord, resID) = when (type) {
                FilterTypeExtension.BEFORE -> true to R.e.ic_history_white_24dp
                FilterTypeExtension.DURING -> false to scoutRes.getDrawableId("baseline_clock_24")
                FilterTypeExtension.AFTER -> false to scoutRes.getDrawableId("baseline_update_24")
                FilterTypeExtension.SORT -> true to R.e.ic_sort_white_24dp
                FilterTypeExtension.EXCLUDE -> false to scoutRes.getDrawableId("baseline_do_disturb_on_24")
                else -> false to null
            }

            resID?.let {
                val res = if (isDiscord) context.resources else resources
                param.result = ResourcesCompat.getDrawable(res, it, null)
            }
        }

        // Patch for retrieving sample filter answer/placeholder
        patcher.before<WidgetSearchSuggestionsAdapter.FilterViewHolder>(
            "getAnswerText",
            FilterType::class.java
        ) { param ->
            val type = param.args[0] as FilterType
            if (type in FilterTypeExtension.dates)
                param.result = ssProvider.getIdentifier("search_answer_date")
            if (type == FilterTypeExtension.SORT)
                param.result = ScoutResource.SORT_ANSWER
            if (type == FilterTypeExtension.EXCLUDE)
                param.result = ssProvider.getIdentifier("search_answer_has")
        }

        // Patch for retrieving filter name
        patcher.before<WidgetSearchSuggestionsAdapter.FilterViewHolder>(
            "getFilterText",
            FilterType::class.java
        ) { param ->
            val type = param.args[0] as FilterType
            val res = when (type) {
                FilterTypeExtension.EXCLUDE -> ScoutResource.EXCLUDE_FILTER
                FilterTypeExtension.BEFORE -> ssProvider.getIdentifier("search_filter_before")
                FilterTypeExtension.DURING -> ssProvider.getIdentifier("search_filter_during")
                FilterTypeExtension.AFTER -> ssProvider.getIdentifier("search_filter_after")
                FilterTypeExtension.SORT -> ScoutResource.SORT_FILTER
                else -> null
            }
            res?.let { param.result = it }
        }

        // Patch formatting utils to use our custom lowercase strings
        // This is called by FilterViewHolder.onConfigure, using the results from getAnswerText and getFilterText
        patcher.patch(
            b.a.k.b::class.java.getDeclaredMethod("c",
                Resources::class.java,
                Int::class.javaPrimitiveType!!,
                Array::class.java,
                Function1::class.java
            ),
            PreHook { param ->
                val resID = param.args[1] as Int
                val objArr = param.args[2] as Array<*>
                val override = when (resID) {
                    ScoutResource.SORT_FILTER -> ssProvider.sortFilterString
                    ScoutResource.SORT_ANSWER -> ssProvider.sortOldString
                    ScoutResource.EXCLUDE_FILTER -> ssProvider.excludeFilterString
                    else -> null
                }
                override?.let {
                    // Why invoke? Becuase I can't for the life of me get Function1 to cast properly
                    param.result = b.a.k.b::class.java.getDeclaredMethod("g",
                        CharSequence::class.java,
                        Array::class.java,
                        Function1::class.java
                    ).invoke(null, it, objArr.copyOf(), param.args[3])
                }
            }
        )

        // Patch to add our new filters into the initial suggestions
        patcher.after<SearchSuggestionEngine>(
            "getFilterSuggestions",
            CharSequence::class.java,
            SearchStringProvider::class.java,
            Boolean::class.javaPrimitiveType!!,
        ) { param ->
            val query = param.args[0] as CharSequence
            val res = (param.result as List<SearchSuggestion>).toMutableList()
            for (type in FilterTypeExtension.values) {
                val st = ssProvider.stringFor(type) + ":"

                if (st.contains(query))
                    res.add(FilterSuggestion(type))
            }
            param.result = res.toList()
        }
    }
}
