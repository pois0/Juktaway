package net.slashOmega.juktaway

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.view.MenuItem
import net.slashOmega.juktaway.util.ThemeUtil

/**
 * Created on 2018/08/29.
 */
class SettingsActivity: Activity() {
    class SettingsFragment: PreferenceFragment() {
        companion object {
            fun changedPreference(longTapPreference: ListPreference) {
                longTapPreference.setOnPreferenceChangeListener { pref, newValue ->
                    (pref as ListPreference).apply {
                        entries?.let {
                            summary = it[findIndexOfValue(newValue as String)]
                        }
                    }
                    true
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            if (preferenceManager == null) return
            preferenceManager.sharedPreferencesName = "settings"

            addPreferencesFromResource(R.xml.pref_general)

            (findPreference("display_account_name") as ListPreference?)?.apply {
                summary = entry
                setOnPreferenceChangeListener { pref, newValue ->
                    (pref as ListPreference).apply {
                        entries?.let {
                            summary = it[findIndexOfValue(newValue as String)]
                        }
                    }
                    true
                }
            } ?: return

            (findPreference("font_size") as ListPreference?)?.apply {
                summary = "$value pt"
                setOnPreferenceChangeListener { pref, newValue ->
                    pref.summary = newValue.toString() + " pt"
                    true
                }
            } ?: return
            (findPreference("long_tap") as ListPreference?)?.apply {
                summary = entry
                changedPreference(this)
            } ?: return
            (findPreference("themeName") as ListPreference?)?.apply {
                summary = entry
                setOnPreferenceChangeListener { pref, newValue ->
                    (pref as ListPreference).apply {
                        entries?.let {
                            summary = it[findIndexOfValue(newValue as String)]
                        }
                        fragmentManager?.let {
                            SettingsActivity.ThemeSwitchDialogFragment().show(it, "dialog")
                        }
                    }
                    true
                }
            }
        }
    }

    class ThemeSwitchDialogFragment: DialogFragment() {
        private var mActivity: SettingsActivity? = null

        override fun onAttach(context: Context) {
            super.onAttach(context)

            mActivity = context as SettingsActivity
        }

        override fun onCreateDialog(savedInstanceState:Bundle): Dialog {
            return AlertDialog.Builder(mActivity)
                    .setMessage(R.string.confirm_theme_apply_finish)
                    .setPositiveButton(getString(R.string.button_yes)) { _, _ ->
                        mActivity?.applyTheme()
                        dismiss()
                    }
                    .setNegativeButton(getString(R.string.button_no)) { _, _ -> dismiss() }
                    .create()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.setTheme(this)
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content,
                SettingsFragment()).commit()

        actionBar?.run{
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    fun applyTheme() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}