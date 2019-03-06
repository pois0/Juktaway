package net.slash_omega.juktaway.adapter.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import net.slash_omega.juktaway.fragment.main.tab.*
import net.slash_omega.juktaway.model.*
import net.slash_omega.juktaway.twitter.currentIdentifier
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Created on 2018/10/20.
 */
class MainPagerAdapter(private val mContext: FragmentActivity, private val mViewPager: ViewPager) : FragmentStatePagerAdapter(mContext.supportFragmentManager) {
    /**
     * タブ内のActivity、引数を設定する。
     *
     * @param mClass    タブに表示するFragment
     * @param args     タブに表示するFragmentに対する引数
     * @param tabTitle タブのタイトル
     */
    private class TabInfo(val mClass: KClass<out Fragment>, val args: Bundle?, var tabTitle: String)

    private val mTabs = mutableListOf<TabInfo>()

    init {
        mViewPager.adapter = this
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

//    fun findPositionById(id: Long) = mTabs.indexOfFirst { it.id == id }
//
//    fun findPositionBySearchWord(str: String)
//            = mTabs.indexOfFirst { it.id <= TabManager.OLD_SEARCH_TAB_ID && it.searchWord == str }
//
//    fun addTab(clazz : KClass<out Fragment>, args: Bundle?, tabTitle: String, id: Long, searchWord: String? = null) {
//        mTabs.add(TabInfo(clazz, args, tabTitle, id, searchWord))
//    }

    fun addTab(tab: Tab) {
        val info = when (tab.type) {
            HOME_TAB_ID -> TabInfo(TimelineFragment::class, null, tab.displayString)
            MENTION_TAB_ID -> TabInfo(InteractionsFragment::class, null, tab.displayString)
            //DM_TAB_ID -> TabInfo(DirectMessagesFragment::class, null, tab.displayString)
            FAVORITE_TAB_ID -> TabInfo(FavoritesFragment::class, null, tab.displayString)
            SEARCH_TAB_ID -> TabInfo(SearchFragment::class, Bundle().apply {
                    putString("searchWord", tab.word)
                }, tab.displayString)
            LIST_TAB_ID -> TabInfo(UserListFragment::class, Bundle().apply {
                putLong("userListId", tab.id)
            }, tab.displayString)
            else -> return
        }
        mTabs.add(info)
    }

    fun clearTab() { mTabs.clear() }
}