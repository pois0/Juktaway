package net.slash_omega.juktaway.util

import android.app.Activity
import android.content.res.Resources
import android.util.TypedValue
import android.widget.ImageButton
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.settings.preferences

object ThemeUtil {
    private var sTheme: Resources.Theme? = null

    fun setTheme(activity: Activity) {
        activity.setTheme(
                if (preferences.display.general.theme == "black")
                    R.style.BlackTheme
                else R.style.WhiteTheme)
        sTheme = activity.theme
    }

    fun setThemeTextColor(view: ImageButton, resourceId: Int) {
        sTheme?.run {
            view.setColorFilter(TypedValue().also {
                resolveAttribute(resourceId, it, true)
            }.data)
        }
    }

    fun getThemeTextColor(resourceId: Int): Int {
        val outValue = TypedValue()
        sTheme?.run { resolveAttribute(resourceId, outValue, true) }
        return outValue.data
    }
}
