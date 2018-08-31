package info.justaway.adapter

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager

import java.util.ArrayList
import kotlin.reflect.KClass

/**
 * タブの切替毎に必要なFragmentを取得するためのAdapterクラス
 */
class SimplePagerAdapter(context: FragmentActivity, viewPager: ViewPager) : FragmentPagerAdapter(context.supportFragmentManager) {
    private val mContext: Context
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
        mContext = context
    }

    override fun getItem(position: Int): Fragment {
        val info = mTabs[position]
        return Fragment.instantiate(mContext, info.clazz.qualifiedName, info.args)
    }

    /**
     * タブ内に起動するActivity、引数、タイトルを設定する
     *
     * @param clazz 起動するv4.Fragmentクラス
     * @param args  v4.Fragmentに対する引数
     */
    fun <T: Fragment> addTab(clazz: KClass<T>, args: Bundle?) {
        val info = TabInfo(clazz, args)
        mTabs.add(info)
    }

    override fun getCount(): Int {
        // タブ数
        return mTabs.size
    }
}