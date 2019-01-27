package net.slash_omega.juktaway.util

import android.app.Activity
import android.content.res.Resources
import android.util.TypedValue
import android.widget.TextView
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.settings.BasicSettings

object ThemeUtil {
    private var sTheme: Resources.Theme? = null

    fun setTheme(activity: Activity) {
        activity.setTheme(
                if (BasicSettings.themeName == "black")
                    R.style.BlackTheme
                else R.style.WhiteTheme)
        sTheme = activity.theme
    }

    fun setThemeTextColor(view: TextView, resourceId: Int) {
        sTheme?.run {
            val outValue = TypedValue()
            resolveAttribute(resourceId, outValue, true)
            view.setTextColor(outValue.data)
        }
    }

    fun getThemeTextColor(resourceId: Int): Int {
        val outValue = TypedValue()
        sTheme?.run { resolveAttribute(resourceId, outValue, true) }
        return outValue.data
    }
}