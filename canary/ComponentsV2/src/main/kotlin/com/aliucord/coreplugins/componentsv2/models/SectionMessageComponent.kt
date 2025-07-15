package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.ComponentType
import com.discord.api.botuikit.SectionComponent
import com.discord.models.botuikit.MessageComponent

data class SectionMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    val id: Int,
    val components: List<MessageComponent>,
    val accessory: MessageComponent?,
) : MessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: SectionComponent,
            index: Int,
            components: List<MessageComponent>,
        ): SectionMessageComponent {
            return component.run {
                val realComponents = components.toMutableList()
                val accessory = realComponents.removeAt(realComponents.lastIndex)
                SectionMessageComponent(
                    type,
                    index,
                    id,
                    realComponents,
                    accessory,
                )
            }
        }
    }
}
