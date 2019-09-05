package net.slash_omega.juktaway

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_license.*
import net.slash_omega.juktaway.util.ThemeUtil


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

        license_text.loadUrl("file:///android_asset/licenses.html")
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }
}
