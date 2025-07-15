package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.ComponentType
import com.discord.api.botuikit.SeparatorComponent
import com.discord.models.botuikit.MessageComponent

data class SeparatorMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    val divider: Boolean,
    val spacing: Int, // 1 = small padding, 2 = large padding
) : MessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: SeparatorComponent,
            index: Int
        ): SeparatorMessageComponent {
            return component.run {
                SeparatorMessageComponent(
                    type,
                    index,
                    divider,
                    spacing,
                )
            }
        }
    }
}
