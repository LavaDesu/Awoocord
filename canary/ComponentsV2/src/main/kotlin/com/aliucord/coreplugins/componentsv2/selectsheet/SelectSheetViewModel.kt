package com.aliucord.coreplugins.componentsv2.selectsheet

import androidx.lifecycle.ViewModel
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.SelectV2MessageComponent
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.api.botuikit.ComponentType
import com.discord.restapi.RestAPIParams.ComponentInteractionData.SelectComponentInteractionData
import com.discord.stores.StoreStream

const val ENTRY_LIMIT = 15

internal class SelectSheetViewModel() : ViewModel() {
    data class ViewState(
        val placeholder: String,
        val items: List<SelectSheetItem>,
        val isMultiSelect: Boolean,
        val minSelections: Int,
        val maxSelections: Int,
        val isValidSelection: Boolean,
    )

    private data class SubmissionData(
        val applicationId: Long,
        val guildId: Long?,
        val channelId: Long,
        val messageId: Long,
        val messageFlags: Long,
        val index: Int,
        val customId: String,
        val type: ComponentType,
    )

    var onUpdate: ((ViewState) -> Unit)? = null
    var onRequestDismiss: (() -> Unit)? = null

    var state: ViewState? = null
        set(value) {
            field = value
            value?.let { onUpdate?.invoke(it) }
        }

    private var submissionData: SubmissionData? = null

    fun configure(entry: BotUiComponentV2Entry, component: SelectV2MessageComponent) {
        var entryCount = 0
        val items = mutableListOf<SelectSheetItem>()
        val users = StoreStream.getUsers().users
        if (component.type in listOf(ComponentV2Type.USER_SELECT, ComponentV2Type.MENTIONABLE_SELECT)) {
            for (member in entry.guildMembers.values) {
                entryCount += 1
                if (entryCount > ENTRY_LIMIT)
                    break
                val user = users[member.userId]!!
                val isDefault = component.defaultValues.any { it.id == member.userId }
                items.add(SelectSheetItem.UserSelectItem(user, member, isDefault))
            }
        }
        if (component.type in listOf(ComponentV2Type.ROLE_SELECT, ComponentV2Type.MENTIONABLE_SELECT)) {
            for (role in entry.guildRoles.values) {
                entryCount += 1
                if (entryCount > ENTRY_LIMIT)
                    break
                val isDefault = component.defaultValues.any { it.id == role.id }
                items.add(SelectSheetItem.RoleSelectItem(role, isDefault))
            }
        }
        // TODO: is the guildID check needed? as in, can server side allow this component?
        if (component.type == ComponentV2Type.CHANNEL_SELECT && entry.guildId != null) {
            val channels = StoreStream.getChannels().getChannelsForGuild(entry.guildId!!)!!
            for (channel in channels.values) {
                entryCount += 1
                if (entryCount > ENTRY_LIMIT)
                    break
                val isDefault = component.defaultValues.any { it.id == channel.id }
                items.add(SelectSheetItem.ChannelSelectItem(channel, isDefault))
            }
        }

        val min = component.minValues
        val max = component.maxValues
        state = ViewState(
            component.placeholder,
            items,
            isMultiSelect = max > 1,
            minSelections = min,
            maxSelections = max,
            isValidSelection = false,
        )
        submissionData = SubmissionData(
            entry.applicationId,
            entry.guildId,
            entry.message.channelId,
            entry.message.id,
            entry.message.flags,
            component.index,
            component.customId,
            component.type,
        )
    }

    fun onItemSelect(item: SelectSheetItem) {
        val state = state ?: return
        var checkedCount = 0
        var newItems = state.items.map {
            val res = if (it == item)
                item.copy(checked = !item.checked)
            else
                it
            if (res.checked)
                checkedCount += 1
            res
        }
        val isMaxed = checkedCount == state.maxSelections
        newItems = newItems.map {
            it.copy(disabled = isMaxed && !it.checked)
        }
        this.state = state.copy(
            items = newItems,
            isValidSelection = checkedCount in state.minSelections..state.maxSelections
        )

        if (!state.isMultiSelect)
            submit()
    }

    fun submit() {
        // val companion = StoreStream.Companion
        // companion.localActionComponentState.setSelectComponentSelection(this.componentContext.getMessageId(), this.componentIndex, u.toList(set))
        val state = state ?: return
        val submissionData = submissionData ?: return

        val selected = state.items.filter { it.checked }.map { it.id.toString() }
        submissionData.run {
            StoreStream.getInteractions().sendComponentInteraction(
                applicationId,
                guildId,
                channelId,
                messageId,
                index,
                SelectComponentInteractionData(
                    type,
                    customId,
                    selected,
                ),
                messageFlags
            )
        }
        onRequestDismiss?.invoke()
    }
}
