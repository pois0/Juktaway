package net.slash_omega.juktaway.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import java.util.ArrayList
import kotlin.reflect.KClass

/**
 * タブの切替毎に必要なFragmentを取得するためのAdapterクラス
 */
class SimplePagerAdapter(private val context: FragmentActivity, viewPager: ViewPager): FragmentPagerAdapter(context.supportFragmentManager) {
    private val mTabs = ArrayList<TabInfo>()

    private class TabInfo
    /**
     * タブ内のActivity、引数を設定する。
     *
     * @param clazz タブ内のv4.Fragment
     * @param args  タブ内のv4.Fragmentに対する引数
     */
    internal constructor(internal val clazz: KClass<out Fragment>, internal val args: Bundle?)

    init {
        viewPager.adapter = this
    }

    override fun getItem(position: Int) = mTabs[position].let { Fragment.instantiate(context, it.clazz.qualifiedName, it.args) }!!

    /**
     * タブ内に起動するActivity、引数、タイトルを設定する
     *
     * @param clazz 起動するv4.Fragmentクラス
     * @param args  v4.Fragmentに対する引数
     */
    fun <T: Fragment> addTab(clazz: KClass<T>, args: Bundle?) {
        mTabs.add(TabInfo(clazz, args))
    }

    override fun getCount() = mTabs.size
}
