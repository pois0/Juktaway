package net.slash_omega.juktaway

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.MenuItem
import android.widget.TextView
import net.slash_omega.juktaway.adapter.SimplePagerAdapter
import net.slash_omega.juktaway.fragment.mute.SourceFragment
import net.slash_omega.juktaway.fragment.mute.UserFragment
import net.slash_omega.juktaway.fragment.mute.WordFragment
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_mute.*

/**
 * Created on 2018/08/27.
 */

class MuteActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_mute)

        actionBar?.apply {
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

        val tabs = arrayListOf<TextView>(tab_user, tab_source, tab_word)
        tabs.forEachIndexed { i, tab -> tab.setOnClickListener { pager.currentItem = i } }

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
        if (item?.itemId == android.R.id.home) finish()
        return true
    }
}