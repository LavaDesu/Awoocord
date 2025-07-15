package com.aliucord.coreplugins.componentsv2

import com.aliucord.api.PatcherAPI
import com.aliucord.coreplugins.isComponentV2
import com.aliucord.patcher.*
import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.member.GuildMember
import com.discord.models.message.Message
import com.discord.stores.StoreMessageReplies.MessageState
import com.discord.stores.StoreMessageState
import com.discord.stores.StoreThreadMessages
import com.discord.widgets.chat.list.entries.BotUiComponentEntry
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages

fun patchMessageItems(patcher: PatcherAPI) {
    @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY", "LocalVariableName", "UnusedVariable")
    patcher.patch(WidgetChatListModelMessages.Companion::class.java.declaredMethods.find { it.name == "getMessageItems" }!!)
    {(
         param,
         channel: Channel,
         guildMembers: Map<Long, GuildMember>,
         guildRoles: Map<Long, GuildRole>,
         _blockedRelationships: Map<Long, Int>?,
         _referencedChannel: Channel?,
         _threadStoreState: StoreThreadMessages.ThreadState?,
         _message: Message,
         state: StoreMessageState.State?,
         _repliedMessages: Map<Long, MessageState>?,
         _isBlockedExpanded: Boolean,
         _isMinimal: Boolean,
     ) ->
        @Suppress("UNCHECKED_CAST")
        val result = (param.result as MutableList<ChatListEntry>)
        val meId = param.args[15] as Long
        result.forEachIndexed { index, entry ->
            if (entry is BotUiComponentEntry && entry.message.isComponentV2) {
                val fields = BotUiComponentV2Entry.V2Fields(state, meId, channel, guildMembers, guildRoles)
                result[index] = BotUiComponentV2Entry.fromV1(entry, fields)
            }
        }
    }
}
