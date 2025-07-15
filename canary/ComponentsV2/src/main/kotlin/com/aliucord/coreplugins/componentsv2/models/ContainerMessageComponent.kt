package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.ComponentType
import com.discord.api.botuikit.ContainerComponent
import com.discord.models.botuikit.MessageComponent

data class ContainerMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    override val id: Int,
    val components: List<MessageComponent>,
    val accentColor: Int?,
    override val spoiler: Boolean,
) : SpoilableMessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: ContainerComponent,
            index: Int,
            components: List<MessageComponent>,
        ): ContainerMessageComponent {
            components.forEach {
                if (it is MediaGalleryMessageComponent)
                    it.markedContained = true
            }
            return component.run {
                ContainerMessageComponent(
                    type,
                    index,
                    id,
                    components,
                    accentColor,
                    spoiler,
                )
            }
        }
    }
}
