package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.ComponentType
import com.discord.api.botuikit.TextDisplayComponent
import com.discord.models.botuikit.MessageComponent

data class TextDisplayMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    val id: Int,
    val content: String,
) : MessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: TextDisplayComponent,
            index: Int
        ): TextDisplayMessageComponent {
            return component.run {
                TextDisplayMessageComponent(
                    type,
                    index,
                    id,
                    content
                )
            }
        }
    }
}
