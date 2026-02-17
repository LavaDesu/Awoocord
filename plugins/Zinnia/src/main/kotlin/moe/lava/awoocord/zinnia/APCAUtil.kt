package moe.lava.awoocord.zinnia

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.aliucord.utils.DimenUtils.dp
import com.discord.stores.StoreStream
import kotlin.math.abs

internal object APCAUtil {
    private val settings = ZinniaSettings

    internal fun configureOn(view: TextView, colour: Int?) {
        when (settings.mode) {
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
            if (settings.blockAlsoDefault) {
                colour = if (isLight && !settings.blockInverted) Color.WHITE else Color.BLACK
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
        when (settings.blockMode) {
            BlockMode.InvertedThemeOnly -> preferred = other
            BlockMode.WhiteOnly -> preferred = Color.WHITE
            BlockMode.BlackOnly -> preferred = Color.BLACK
            BlockMode.Unchanged -> preferred = colourP
            else -> {}
        }

        val colours = if (!settings.blockInverted) {
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

        val usePreferred = when (settings.blockMode) {
            BlockMode.ApcaOnly -> isApca(colours)
            BlockMode.WcagOnly -> isWcag(colours)
            BlockMode.ApcaLightWcagDark -> if (isLight) isApca(colours) else isWcag(colours)
            BlockMode.WcagLightApcaDark -> if (isLight) isWcag(colours) else isApca(colours)
            BlockMode.ThemeOnly,
            BlockMode.InvertedThemeOnly,
            BlockMode.WhiteOnly,
            BlockMode.BlackOnly,
            BlockMode.Unchanged -> true
        }

        if (usePreferred) {
            view.setTextColor(colours.fgP)
            bcol.setColor(ColorUtils.setAlphaComponent(colours.bgP, settings.alpha))
            bcol.alpha = settings.alpha
        } else {
            view.setTextColor(colours.fgO)
            bcol.setColor(ColorUtils.setAlphaComponent(colours.bgO, settings.alpha))
            bcol.alpha = settings.alpha
        }
    }

    private fun isApca(c: Colours): Boolean {
        val cPref = abs(APCA.contrast(c.fgP, c.bgP))
        val cOth = abs(APCA.contrast(c.fgO, c.bgO))
        return cPref > settings.blockApcaThreshold || cPref > cOth
    }

    private fun isWcag(c: Colours): Boolean {
        val cPref = ColorUtils.calculateContrast(c.fgP, c.bgP)
        val cOth = ColorUtils.calculateContrast(c.fgO, c.bgO)
        return cPref > settings.blockWcagThreshold || cPref > cOth
    }

}
