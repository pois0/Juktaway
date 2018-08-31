package info.justaway

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.MenuItem
import android.widget.TextView
import info.justaway.adapter.SimplePagerAdapter
import info.justaway.fragment.mute.SourceFragment
import info.justaway.fragment.mute.UserFragment
import info.justaway.fragment.mute.WordFragment
import info.justaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_mute.*

/**
 * Created on 2018/08/27.
 */

class MuteActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_mute)

        actionBar.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        pager.offscreenPageLimit = 3

        SimplePagerAdapter(this, pager).apply {
            addTab(UserFragment::class, null)
            addTab(SourceFragment::class, null)
            addTab(WordFragment::class, null)
            notifyDataSetChanged()
        }

        val colorBlue = ThemeUtil.getThemeTextColor(R.attr.holo_blue)
        val colorWhite = ThemeUtil.getThemeTextColor(R.attr.text_color)

        val tabs = arrayListOf<TextView>(tab_user, tab_source, tab_word).apply {
            forEachIndexed { i, tab -> tab.setOnClickListener { pager.currentItem = i } }
        }

        tabs[0].setTextColor(colorBlue)
        pager.addOnPageChangeListener(object: ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                tabs.forEachIndexed { i, tab ->
                    tab.setTextColor(if (i == position) colorBlue else colorWhite)
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.home) finish()
        return true
    }
}