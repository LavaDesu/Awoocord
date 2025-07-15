package com.aliucord.coreplugins.componentsv2.selectsheet

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discord.utilities.mg_recycler.MGRecyclerAdapterSimple
import com.discord.utilities.mg_recycler.MGRecyclerViewHolder

internal class SelectSheetAdapter(recycler: RecyclerView, val viewModel: SelectSheetViewModel)
    : MGRecyclerAdapterSimple<SelectSheetItem>(recycler) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MGRecyclerViewHolder<*, SelectSheetItem> {
        return SelectSheetItemViewHolder(this)
    }
}
