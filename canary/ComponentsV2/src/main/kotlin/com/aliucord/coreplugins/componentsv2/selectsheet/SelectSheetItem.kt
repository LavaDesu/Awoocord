package com.aliucord.coreplugins.componentsv2.selectsheet

import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.member.GuildMember
import com.discord.models.user.User
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload

sealed class SelectSheetItem(
    private val type: Int,
    val id: Long,
) : MGRecyclerDataPayload {
    override fun getKey() = id.toString()
    override fun getType() = type

    abstract val checked: Boolean
    abstract val disabled: Boolean
    abstract fun copy(checked: Boolean = this.checked, disabled: Boolean = this.disabled) : SelectSheetItem

    internal data class UserSelectItem(
        val user: User,
        val member: GuildMember,
        override val checked: Boolean,
        override val disabled: Boolean = false,
    ) : SelectSheetItem(1, user.id) {
        override fun copy(checked: Boolean, disabled: Boolean): SelectSheetItem = copy(checked = checked, disabled = disabled, user = user)
    }

    internal data class RoleSelectItem(
        val role: GuildRole,
        override val checked: Boolean,
        override val disabled: Boolean = false,
    ) : SelectSheetItem(2, role.id) {
        override fun copy(checked: Boolean, disabled: Boolean): SelectSheetItem = copy(checked = checked, disabled = disabled, role = role)
    }

    internal data class ChannelSelectItem(
        val channel: Channel,
        override val checked: Boolean,
        override val disabled: Boolean = false,
    ) : SelectSheetItem(3, channel.id) {
        override fun copy(checked: Boolean, disabled: Boolean): SelectSheetItem = copy(checked = checked, disabled = disabled, channel = channel)
    }
}
