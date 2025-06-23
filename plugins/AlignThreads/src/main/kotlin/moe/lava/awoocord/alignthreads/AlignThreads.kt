package moe.lava.awoocord.alignthreads

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.utils.DimenUtils
import com.discord.widgets.chat.list.actions.`WidgetChatListActions$binding$2`
import com.lytefast.flexinput.R

@AliucordPlugin(requiresRestart = true)
@Suppress("unused")
class AlignThreads : Plugin() {
    override fun start(ctx: Context) {
        patcher.after<`WidgetChatListActions$binding$2`>("invoke", View::class.java)
        { (_, view: View) ->
            val id = Utils.getResId("dialog_chat_actions_start_thread", "id")
            val threadTextView = view.findViewById<TextView>(id)
            val size = DimenUtils.dpToPx(24)
            val icon = ContextCompat.getDrawable(threadTextView.context, R.e.ic_thread)!!
            icon.setBounds(0, 0, size, size)
            threadTextView.setCompoundDrawables(icon, null, null, null)
        }
    }

    override fun stop(ctx: Context) {
        patcher.unpatchAll()
    }
}
