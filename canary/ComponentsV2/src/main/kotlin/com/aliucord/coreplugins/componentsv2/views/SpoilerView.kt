package com.aliucord.coreplugins.componentsv2.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.models.SpoilableMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.discord.stores.StoreMessageState
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R

/**
 * A view that can be spoilered.
 *
 * @param ctx Context
 * @param type 1 for full (spoiler text and button), 2 for mini (eye icon)
 */
@SuppressLint("ViewConstructor")
internal class SpoilerView(ctx: Context, type: Int) : ConstraintLayout(ctx) {
    companion object {
        fun constraintLayoutParamsAround(viewId: Int) =
            LayoutParams(0, 0).apply {
                topToTop = viewId
                bottomToBottom = viewId
                startToStart = viewId
                endToEnd = viewId
            }
    }

    private val spoilerView = ConstraintLayout(ctx).addTo(this) {
        visibility = GONE
        setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.theme_chat_spoiler_bg))
        layoutParams = LayoutParams(0, 0).apply {
            bottomToBottom = PARENT_ID
            endToEnd = PARENT_ID
            startToStart = PARENT_ID
            topToTop = PARENT_ID
        }
        isClickable = true

        when (type) {
            1 -> {
                CardView(ctx).addTo(this) {
                    elevation = ctx.resources.getDimension(R.d.app_elevation)
                    setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundFloating))
                    radius = 16.dp.toFloat()

                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        startToStart = PARENT_ID
                        endToEnd = PARENT_ID
                        topToTop = PARENT_ID
                        bottomToBottom = PARENT_ID
                    }

                    TextView(ctx, null, 0, R.i.UiKit_TextView_H2).addTo(this) {
                        setText(R.h.spoiler)
                        isAllCaps = true
                        setPadding(8.dp, 4.dp, 8.dp, 4.dp)
                        setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorTextNormal))
                        layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            marginStart = 4.dp
                            marginEnd = 4.dp
                        }
                    }
                }
            }
            2 -> {
                ImageView(ctx).addTo(this) {
                    setImageResource(R.e.ic_spoiler)
                    layoutParams = LayoutParams(0, 0).apply {
                        startToStart = PARENT_ID
                        endToEnd = PARENT_ID
                        topToTop = PARENT_ID
                        bottomToBottom = PARENT_ID
                        dimensionRatio = "1:1"
                        matchConstraintPercentWidth = 0.5f
                    }
                }
            }
            else -> throw IllegalArgumentException("Invalid spoiler view type")
        }
    }

    fun configure(entry: BotUiComponentV2Entry, component: SpoilableMessageComponent, key: String? = null) {
        configure(component.spoiler, entry.state, entry.message.id, Pair(component.id, key))
    }

    fun configure(
        isSpoiler: Boolean,
        state: StoreMessageState.State?,
        messageId: Long,
        key: Pair<Int, String?>,
    ) {
        val (id, strKey) = key
        val spoiled = if (strKey != null)
            state?.visibleSpoilerEmbedMap?.get(id)?.contains(strKey) ?: false
        else
            state?.visibleSpoilerEmbedMap?.containsKey(id) ?: false

        spoilerView.setOnClickListener {
            spoilerView.setOnClickListener(null)
            spoilerView.animate()
                .withEndAction {
                    if (strKey != null)
                        StoreStream.getMessageState().revealSpoilerEmbedData(messageId, id, strKey)
                    else
                        StoreStream.getMessageState().revealSpoilerEmbed(messageId, id)
                }
                .alpha(0f)
        }
        spoilerView.visibility = if (isSpoiler && !spoiled) VISIBLE else GONE
        spoilerView.alpha = 1f
    }
}
