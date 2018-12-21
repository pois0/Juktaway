package net.slashOmega.juktaway

import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_license.*
import net.slashOmega.juktaway.util.ThemeUtil



/**
 * Created on 2018/12/18.
 */
class LicenseActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_license)

        actionBar?.run{
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        license_text.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml(getString(R.string.license), Html.FROM_HTML_MODE_COMPACT)
                else Html.fromHtml(getString(R.string.license))
        license_text.movementMethod = ScrollingMovementMethod()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }
}