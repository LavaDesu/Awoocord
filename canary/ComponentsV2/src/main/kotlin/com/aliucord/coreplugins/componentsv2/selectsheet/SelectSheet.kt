package com.aliucord.coreplugins.componentsv2.selectsheet

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.SimpleItemAnimator
import com.aliucord.Utils
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.models.SelectV2MessageComponent
import com.discord.app.AppBottomSheet
import com.discord.utilities.view.extensions.ViewExtensions
import com.discord.utilities.view.recycler.MaxHeightRecyclerView
import com.discord.widgets.botuikit.views.select.`SelectComponentBottomSheet$binding$2`
import com.lytefast.flexinput.R
import b.a.k.b as FormatUtils

internal class SelectSheet : AppBottomSheet {
    val entry: BotUiComponentV2Entry?
    val component: SelectV2MessageComponent?

    private lateinit var header: ConstraintLayout
    private lateinit var placeholder: TextView
    private lateinit var recycler: MaxHeightRecyclerView
    private lateinit var select: TextView
    private lateinit var subtitle: TextView

    private lateinit var adapter: SelectSheetAdapter

    constructor(entry: BotUiComponentV2Entry, component: SelectV2MessageComponent) {
        this.entry = entry
        this.component = component
    }
    constructor() {
        this.entry = null
        this.component = null
    }

    override fun getContentViewResId() = Utils.getResId("widget_select_component_bottom_sheet", "layout")

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        val viewModel = ViewModelProvider(this).get(SelectSheetViewModel::class.java)

        `SelectComponentBottomSheet$binding$2`.INSTANCE.invoke(view).run {
            header = a
            placeholder = b
            recycler = c
            select = d
            subtitle = e
        }
        adapter = SelectSheetAdapter(recycler, viewModel)
        recycler.adapter = adapter
        (recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        select.setOnClickListener { viewModel.submit() }
        viewModel.onUpdate = ::configureUI
        viewModel.onRequestDismiss = ::dismiss
        if (entry != null && component != null)
            viewModel.configure(entry, component)
        else
            viewModel.state?.let { configureUI(it) }
    }

    private fun configureUI(state: SelectSheetViewModel.ViewState) {
        placeholder.text = state.placeholder
        subtitle.visibility = if (state.isMultiSelect) View.VISIBLE else View.GONE

        if (state.isMultiSelect) {
             subtitle.text =
                FormatUtils.k(
                    this,
                    R.h.message_select_component_select_requirement,
                    arrayOf(state.minSelections),
                    null,
                    4
                )
        }
        select.visibility = if (state.isMultiSelect) View.VISIBLE else View.INVISIBLE
        select.isClickable = state.isValidSelection
        ViewExtensions.setEnabledAlpha(select, state.isValidSelection, 0.3f)
        adapter.setData(state.items)
    }
}
