package net.slash_omega.juktaway

import android.app.Activity
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.view.MenuItem
import net.slash_omega.juktaway.util.ThemeUtil

class PerformanceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()

        actionBar?.run {
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

    class SettingsFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val preferenceManager = preferenceManager ?: return
            preferenceManager.sharedPreferencesName = "settings"

            addPreferencesFromResource(R.xml.pref_performance)

            (findPreference("user_icon_size") as? ListPreference)?.run {
                summary = entry
                SettingsActivity.SettingsFragment.changedPreference(this)
            }

            (findPreference("page_count") as? ListPreference)?.run {
                summary = entry
                SettingsActivity.SettingsFragment.changedPreference(this)
            }
        }
    }
}
