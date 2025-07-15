package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.*

data class ThumbnailMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    override val id: Int,
    val media: UnfurledMediaItem,
    val description: String?,
    override val spoiler: Boolean,
) : SpoilableMessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: ThumbnailComponent,
            index: Int
        ): ThumbnailMessageComponent {
            return component.run {
                ThumbnailMessageComponent(
                    type,
                    index,
                    id,
                    media,
                    description,
                    spoiler,
                )
            }
        }
    }
}
