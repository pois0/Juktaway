package net.slash_omega.juktaway

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.MenuItem
import net.slash_omega.juktaway.util.ThemeUtil

/**
 * Created on 2018/08/29.
 */
class SettingsActivity: Activity() {
    class SettingsFragment: PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            if (preferenceManager == null) return
            preferenceManager.sharedPreferencesName = "settings"

            addPreferencesFromResource(R.xml.pref_general)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.setTheme(this)
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content,
                SettingsFragment()).commit()

        actionBar?.run{
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}