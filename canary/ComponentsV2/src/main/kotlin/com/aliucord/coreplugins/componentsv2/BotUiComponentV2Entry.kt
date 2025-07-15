package com.aliucord.coreplugins.componentsv2

import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.botuikit.MessageComponent
import com.discord.models.member.GuildMember
import com.discord.models.message.Message
import com.discord.stores.StoreMessageState
import com.discord.widgets.chat.list.entries.BotUiComponentEntry

@Suppress("EqualsOrHashCode")
class BotUiComponentV2Entry(
    message: Message, appId: Long, guildId: Long?, components: MutableList<out MessageComponent>,
    private val v2Fields: V2Fields
) : BotUiComponentEntry(message, appId, guildId, components) {
    data class V2Fields(
        val state: StoreMessageState.State?,
        val meId: Long,
        val channel: Channel,
        val guildMembers: Map<Long, GuildMember>,
        val guildRoles: Map<Long, GuildRole>,
        // val channelNames: Map<Long, String>,
    )

    companion object {
        fun fromV1(entry: BotUiComponentEntry, fields: V2Fields) =
            entry.run { BotUiComponentV2Entry(message, applicationId, guildId, messageComponents, fields) }
    }

    val state get() = v2Fields.state
    val meId get() = v2Fields.meId
    val channel get() = v2Fields.channel
    val guildMembers get() = v2Fields.guildMembers
    val guildRoles get() = v2Fields.guildRoles

    override fun equals(other: Any?) =
        super.equals(other) && if (other is BotUiComponentV2Entry) this.v2Fields == other.v2Fields else true

    override fun toString() =
        "AliuV2" + super.toString() + "& " + v2Fields.toString()
}
