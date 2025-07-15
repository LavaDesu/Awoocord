package com.aliucord.coreplugins.componentsv2

import com.aliucord.Logger
import com.discord.api.botuikit.*

// Values added by smali patch
object ComponentV2Type {
    lateinit var USER_SELECT: ComponentType
    lateinit var ROLE_SELECT: ComponentType
    lateinit var MENTIONABLE_SELECT: ComponentType
    lateinit var CHANNEL_SELECT: ComponentType
    lateinit var SECTION: ComponentType
    lateinit var TEXT_DISPLAY: ComponentType
    lateinit var THUMBNAIL: ComponentType
    lateinit var MEDIA_GALLERY: ComponentType
    lateinit var FILE: ComponentType
    lateinit var SEPARATOR: ComponentType
    lateinit var CONTAINER: ComponentType

    var newValues: Array<ComponentType>? = null
    private var oldValues: Array<ComponentType>? = null
    @Suppress("UNCHECKED_CAST", "UNUSED_CHANGED_VALUE")
    fun make() {
        if (oldValues != null)
            return
        oldValues = ComponentType.values()

        val cls = ComponentType::class.java
        val constructor = cls.declaredConstructors[0]
        constructor.isAccessible = true

        val field = cls.getDeclaredField("\$VALUES")
        field.isAccessible = true
        val values = ComponentType.values()
        var nextIdx = values.size

        USER_SELECT = constructor.newInstance("USER_SELECT", nextIdx++, 5, UserSelectComponent::class.java) as ComponentType
        ROLE_SELECT = constructor.newInstance("ROLE_SELECT", nextIdx++, 6, RoleSelectComponent::class.java) as ComponentType
        MENTIONABLE_SELECT = constructor.newInstance("MENTIONABLE_SELECT", nextIdx++, 7, MentionableSelectComponent::class.java) as ComponentType
        CHANNEL_SELECT = constructor.newInstance("CHANNEL_SELECT", nextIdx++, 8, ChannelSelectComponent::class.java) as ComponentType
        SECTION = constructor.newInstance("SECTION", nextIdx++, 9, SectionComponent::class.java) as ComponentType
        TEXT_DISPLAY = constructor.newInstance("TEXT_DISPLAY", nextIdx++, 10, TextDisplayComponent::class.java) as ComponentType
        THUMBNAIL = constructor.newInstance("THUMBNAIL", nextIdx++, 11, ThumbnailComponent::class.java) as ComponentType
        MEDIA_GALLERY = constructor.newInstance("MEDIA_GALLERY", nextIdx++, 12, MediaGalleryComponent::class.java) as ComponentType
        FILE = constructor.newInstance("FILE", nextIdx++, 13, FileComponent::class.java) as ComponentType
        SEPARATOR = constructor.newInstance("SEPARATOR", nextIdx++, 14, SeparatorComponent::class.java) as ComponentType
        CONTAINER = constructor.newInstance("CONTAINER", nextIdx++, 17, ContainerComponent::class.java) as ComponentType

        newValues = arrayOf(USER_SELECT, ROLE_SELECT, MENTIONABLE_SELECT, CHANNEL_SELECT, SECTION, TEXT_DISPLAY, THUMBNAIL, MEDIA_GALLERY, FILE, SEPARATOR, CONTAINER)
        field.set(null, values + newValues!!)
    }

    fun unmake(logger: Logger) {
        if (oldValues == null)
            return logger.error("No unpatched component types?", null)

        val cls = ComponentType::class.java
        val field = cls.getDeclaredField("\$VALUES")
        field.isAccessible = true
        field.set(null, oldValues)
        oldValues = null
    }
}
