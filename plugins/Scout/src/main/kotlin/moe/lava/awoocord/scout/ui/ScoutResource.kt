package moe.lava.awoocord.scout.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

class ScoutResource(private val resources: Resources) {
    companion object {
        const val SORT_FILTER = 0xfffffff0.toInt()
        const val SORT_ANSWER = 0xfffffff1.toInt()
        const val EXCLUDE_FILTER = 0xfffffff2.toInt()
    }

    fun getId(name: String, type: String) =
        resources.getIdentifier(name, type, "moe.lava.awoocord.scout")

    @DrawableRes fun getDrawableId(name: String) =
        getId(name, "drawable")

    fun getDrawable(@DrawableRes id: Int) =
        ResourcesCompat.getDrawable(resources, id, null)

    fun getDrawable(name: String) =
        getDrawable(getDrawableId(name))
}
