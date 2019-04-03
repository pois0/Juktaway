package net.slash_omega.juktaway.adapter.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import net.slash_omega.juktaway.MainActivity
import net.slash_omega.juktaway.fragment.main.tab.*
import net.slash_omega.juktaway.model.*
import net.slash_omega.juktaway.twitter.currentIdentifier
import org.jetbrains.anko.bundleOf
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Created on 2018/10/20.
 */
class MainPagerAdapter(private val mContext: MainActivity, private val mViewPager: ViewPager) : FragmentStatePagerAdapter(mContext.supportFragmentManager) {
    /**
     * タブ内のActivity、引数を設定する。
     *
     * @param mClass    タブに表示するFragment
     * @param args     タブに表示するFragmentに対する引数
     * @param tabTitle タブのタイトル
     */
    private class TabInfo(val mClass: KClass<out Fragment>, val args: Bundle?, var tabTitle: String)

    private val mTabs = mutableListOf<TabInfo>()

    private fun bundleBase(tab: Tab, pos: Int) = bundleOf("reloadInterval" to tab.autoReload, "position" to pos)

    init {
        mViewPager.adapter = this
        mViewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {}
            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

            override fun onPageSelected(position: Int) {
                mContext.currentTabPosition = position
            }
        })
    }

    override fun getItem(p: Int)
                = mTabs[p].let { Fragment.instantiate(mContext, it.mClass.jvmName, it.args) as BaseFragment }

    override fun getItemPosition(o: Any) = POSITION_NONE

    override fun getPageTitle(position: Int) = mTabs[position].apply {
        if (tabTitle == "-" && args != null) {
            UserListCache.getUserList(args.getInt("userListId").toLong()).let {
                tabTitle = if (it.user.id == currentIdentifier.userId) it.name else it.fullName
            }
        }
    }.tabTitle

    override fun getCount() = mTabs.size

    fun findFragmentByPosition(pos: Int) = instantiateItem(mViewPager, pos) as BaseFragment

    fun addTab(tab: Tab, pos: Int) {
        val base = bundleBase(tab, pos)
        val info = when (tab.type) {
            HOME_TAB_ID -> TabInfo(TimelineFragment::class, base, tab.displayString)
            MENTION_TAB_ID -> TabInfo(InteractionsFragment::class, base, tab.displayString)
            //DM_TAB_ID -> TabInfo(DirectMessagesFragment::class, null, tab.displayString)
            FAVORITE_TAB_ID -> TabInfo(FavoritesFragment::class, base, tab.displayString)
            SEARCH_TAB_ID -> TabInfo(SearchFragment::class, base.apply {
                    putString("searchWord", tab.word)
                }, tab.displayString)
            LIST_TAB_ID -> TabInfo(UserListFragment::class, base.apply {
                putLong("userListId", tab.id)
            }, tab.displayString)
            USER_TAB_ID -> TabInfo(UserFragment::class, base.apply {
                putLong("userId", tab.id)
            }, tab.displayString)
            else -> return
        }
        mTabs.add(info)
    }

    fun clearTab() { mTabs.clear() }
}