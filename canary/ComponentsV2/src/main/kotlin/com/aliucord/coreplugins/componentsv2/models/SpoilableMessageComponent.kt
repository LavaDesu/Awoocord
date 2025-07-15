package com.aliucord.coreplugins.componentsv2.models

import com.discord.models.botuikit.MessageComponent

interface SpoilableMessageComponent : MessageComponent {
    val id: Int
    val spoiler: Boolean
}
