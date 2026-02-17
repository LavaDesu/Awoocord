package moe.lava.awoocord.zinnia

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.patcher.component3
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.utils.accessField
import com.discord.databinding.WidgetChannelMembersListItemUserBinding
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListViewHolderMember
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.MessageEntry

private val ChannelMembersListViewHolderMember.binding
        by accessField<WidgetChannelMembersListItemUserBinding>()

data class Colours(
    val fgP: Int,
    val bgP: Int,
    val fgO: Int,
    val bgO: Int,
)

@AliucordPlugin
class Zinnia : Plugin() {
    companion object { const val NAME = "RoleBlocks" }

    init {
        settingsTab = SettingsTab(ZinniaSettings.Page::class.java, SettingsTab.Type.PAGE)
    }

    override fun start(context: Context) {
        patchMemberList()
        patchMessageAuthor()
    }

    override fun stop(context: Context) { patcher.unpatchAll() }

    private fun patchMemberList() {
        // Patches the method that configures the username in members list
        patcher.after<ChannelMembersListViewHolderMember>(
            "bind",
            ChannelMembersListAdapter.Item.Member::class.java,
            Function0::class.java,
        ) { (_, member: ChannelMembersListAdapter.Item.Member) ->
            val presenceTextView = binding.d
            val usernameView = binding.f
            val usernameTextView = usernameView.j.c

            if (presenceTextView.visibility == View.VISIBLE) {
                usernameView.layoutParams = (usernameView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    bottomMargin = 2.dp
                }
            }

            APCAUtil.configureOn(usernameTextView, member.color)
        }
    }

    private fun patchMessageAuthor() {
        // Configures for message author username
        patcher.after<WidgetChatListAdapterItemMessage>(
            "onConfigure",
            Int::class.javaPrimitiveType!!,
            ChatListEntry::class.java,
        ) { (_, _: Int, entry: MessageEntry) ->
            val username = itemView.findViewById<TextView?>("chat_list_adapter_item_text_name")
                ?: return@after
            APCAUtil.configureOn(username, entry.author?.color)
        }

        // Configures for reply preview username
        patcher.after<WidgetChatListAdapterItemMessage>(
            "configureReplyPreview",
            MessageEntry::class.java,
        ) { (_, entry: MessageEntry) ->
            val referencedAuthor = entry.replyData?.messageEntry?.author
            val replyUsername = itemView.findViewById<TextView?>("chat_list_adapter_item_text_decorator_reply_name")
                ?: return@after
            APCAUtil.configureOn(replyUsername, referencedAuthor?.color)
        }
    }
}
