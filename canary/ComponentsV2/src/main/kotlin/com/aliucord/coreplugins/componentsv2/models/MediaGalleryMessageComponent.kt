package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.*
import com.discord.models.botuikit.MessageComponent

data class MediaGalleryMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    val id: Int,
    val items: List<MediaGalleryItem>,
    // Set by ContainerComponentView to tell MediaGalleryComponentView it is contained
    var markedContained: Boolean = false,
) : MessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: MediaGalleryComponent,
            index: Int
        ): MediaGalleryMessageComponent {
            return component.run {
                MediaGalleryMessageComponent(
                    type,
                    index,
                    id,
                    items,
                )
            }
        }
    }
}
