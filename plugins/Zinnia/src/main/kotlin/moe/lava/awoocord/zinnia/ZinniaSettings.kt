package moe.lava.awoocord.zinnia

import android.view.View
import android.view.ViewGroup
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.settings.delegate
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class Mode {
    RoleDot,
    Block,
}

enum class BlockMode {
    ApcaLightWcagDark,
    WcagLightApcaDark,
    ApcaOnly,
    WcagOnly,
    ThemeOnly,
    InvertedThemeOnly,
    WhiteOnly,
    BlackOnly,
}

class SettingsDelegateEnum<T : Enum<T>>(
    private val defaultValue: T,
    private val settings: SettingsAPI,
    private val deserialiser: (String) -> T,
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T =
        deserialiser(settings.getString(property.name, defaultValue.name))

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        settings.setString(property.name, value.name)
}

inline fun <reified T : Enum<T>> SettingsAPI.delegateEnum(
    defaultValue: T
) = SettingsDelegateEnum(defaultValue, this) { enumValueOf<T>(it) }

private inline fun <T : View> T.addTo(parent: ViewGroup, block: T.() -> Unit = {}) =
    apply {
        block()
        parent.addView(this)
    }

object ZinniaSettings {
    private val api = SettingsAPI(Zinnia.NAME)

    var mode by api.delegateEnum(Mode.Block)

    var dotKeepNameColour by api.delegate(false)

    var blockAlsoDefault by api.delegate(true)
    var blockInverted by api.delegate(false)
    var blockMode by api.delegateEnum(BlockMode.ApcaLightWcagDark)
    var blockApcaThreshold by api.delegate(75.0)
    var blockWcagThreshold by api.delegate(4.5)

    @Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")
    class Page : SettingsPage() {
        private lateinit var manager: RadioManager
        private lateinit var mRoleDot: CheckedSetting
        private lateinit var mBlock: CheckedSetting

        private val checks = mutableListOf<CheckedSetting>()

        private fun createRadio(newMode: BlockMode, text: String, subtext: String? = null): CheckedSetting {
            return Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.RADIO, text, subtext).addTo(linearLayout) {
                isChecked = blockMode == newMode
                setOnCheckedListener {
                    for (check in checks) check.isChecked = false
                    blockMode = newMode
                    isChecked = true
                }
                checks.add(this)
            }
        }

        override fun onViewBound(view: View) {
            super.onViewBound(view)
            setActionBarTitle(Zinnia.NAME)
            setPadding(0)

            val ctx = requireContext()
            linearLayout.run {
                val blockSettings = mutableListOf<CheckedSetting>()
                val roleDotSettings = mutableListOf<CheckedSetting>()

                addHeader(ctx, "Text colour")
                createRadio(BlockMode.ApcaLightWcagDark, "Automatic", "Adjusts text colour based on role colour")
                createRadio(BlockMode.ThemeOnly, "By theme", "Adjusts text colour based on theme")
                createRadio(BlockMode.InvertedThemeOnly, "By theme (inverted)", "Same as above, but inverted")
                createRadio(BlockMode.WhiteOnly, "White", "Force text colour to be white")
                createRadio(BlockMode.BlackOnly, "Black", "Force text colour to be black")

                /*
                addHeader(ctx, "Mode")

                mBlock = Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.RADIO,
                    "Block mode",
                    "Wraps the username in a coloured block",
                ).addTo(this) {
                    isChecked = mode == Mode.Block
                    setOnCheckedListener {
                        mode = Mode.Block
                        mRoleDot.isChecked = false
                    }
                }

                mRoleDot = Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.RADIO,
                    "Role dot mode",
                    "Adds a coloured role dot next to the username, similar to how Discord does it in their new accessibility settings",
                ).addTo(this) {
                    isChecked = mode == Mode.RoleDot
                    setOnCheckedListener {
                        mode = Mode.RoleDot
                        mBlock.isChecked = false
                    }
                }
                */

                addHeader(ctx, "Block Settings")
                Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.SWITCH,
                    "Also block up default colours",
                    "Blocks up usernames that have no role colour",
                ).addTo(this) {
                    isChecked = blockAlsoDefault
                    setOnCheckedListener {
                        blockAlsoDefault = !blockAlsoDefault
                    }
                    blockSettings.add(this)
                }

                Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.SWITCH,
                    "Invert block colours",
                    "By default, the role colour is applied as the block background. Turning this setting on instead makes the block black or white, and the text stays coloured.",
                ).addTo(this) {
                    isChecked = blockInverted
                    setOnCheckedListener {
                        blockInverted = !blockInverted
                    }
                    blockSettings.add(this)
                }
            }
        }
    }
}
