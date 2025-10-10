package moe.lava.awoocord.zinnia

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.utils.accessField
import com.discord.databinding.WidgetChannelMembersListItemUserBinding
import com.discord.stores.StoreStream
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListViewHolderMember
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.MessageEntry
import kotlin.math.abs

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

    private val localSettings = ZinniaSettings

    init {
        settingsTab = SettingsTab(ZinniaSettings.Page::class.java, SettingsTab.Type.PAGE)
    }

    override fun start(context: Context) {
        patchMemberList()
        patchMessageAuthor()
    }

    override fun stop(context: Context) { patcher.unpatchAll() }

    private fun configureOn(view: TextView, colour: Int?) {
        when (localSettings.mode) {
            Mode.Block -> configureBlock(view, colour ?: Color.BLACK)
            Mode.RoleDot -> configureRoleDot(view, colour ?: Color.BLACK)
        }
    }

    private fun configureRoleDot(view: TextView, colour: Int) { }

    private fun configureBlock(view: TextView, colourP: Int) {
        val isLight = StoreStream.getUserSettingsSystem().theme == "light"
        var colour = colourP
        val bcol = GradientDrawable()
        bcol.cornerRadius = 4.dp.toFloat()
        view.background = bcol

        if (colour == Color.BLACK) {
            if (localSettings.blockAlsoDefault) {
                colour = if (isLight && !localSettings.blockInverted) Color.WHITE else Color.BLACK
            } else {
                view.background = null
                view.setPadding(0, 0, 0, 0)
                return
            }
        }
        view.setPadding(4.dp, 0, 4.dp, 0)

        var (preferred, other) = if (isLight) {
            Color.WHITE to Color.BLACK
        } else {
            Color.BLACK to Color.WHITE
        }
        when (localSettings.blockMode) {
            BlockMode.InvertedThemeOnly -> preferred = other
            BlockMode.WhiteOnly -> preferred = Color.WHITE
            BlockMode.BlackOnly -> preferred = Color.BLACK
            else -> {}
        }

        val colours = if (!localSettings.blockInverted) {
            Colours(
                fgP = preferred,
                fgO = other,
                bgP = colour,
                bgO = colour,
            )
        } else {
            Colours(
                fgP = colour,
                fgO = colour,
                bgP = preferred,
                bgO = other,
            )
        }

        val usePreferred = when (localSettings.blockMode) {
            BlockMode.ApcaOnly -> isApca(colours)
            BlockMode.WcagOnly -> isWcag(colours)
            BlockMode.ApcaLightWcagDark -> if (isLight) isApca(colours) else isWcag(colours)
            BlockMode.WcagLightApcaDark -> if (isLight) isWcag(colours) else isApca(colours)
            BlockMode.ThemeOnly,
            BlockMode.InvertedThemeOnly,
            BlockMode.WhiteOnly,
            BlockMode.BlackOnly -> true
        }

        if (usePreferred) {
            view.setTextColor(colours.fgP)
            bcol.setColor(colours.bgP)
        } else {
            view.setTextColor(colours.fgO)
            bcol.setColor(colours.bgO)
        }
    }

    private fun isApca(c: Colours): Boolean {
        val cPref = abs(APCA.contrast(c.fgP, c.bgP))
        val cOth = abs(APCA.contrast(c.fgO, c.bgO))
        return cPref > localSettings.blockApcaThreshold || cPref > cOth
    }

    private fun isWcag(c: Colours): Boolean {
        val cPref = ColorUtils.calculateContrast(c.fgP, c.bgP)
        val cOth = ColorUtils.calculateContrast(c.fgO, c.bgO)
        return cPref > localSettings.blockWcagThreshold || cPref > cOth
    }

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

            configureOn(usernameTextView, member.color)
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
            configureOn(username, entry.author?.color)
        }

        // Configures for reply preview username
        patcher.after<WidgetChatListAdapterItemMessage>(
            "configureReplyPreview",
            MessageEntry::class.java,
        ) { (_, entry: MessageEntry) ->
            val referencedAuthor = entry.replyData?.messageEntry?.author
            val replyUsername = itemView.findViewById<TextView?>("chat_list_adapter_item_text_decorator_reply_name")
                ?: return@after
            configureOn(replyUsername, referencedAuthor?.color)
        }
    }
}
