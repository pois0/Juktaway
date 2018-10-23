package net.slashOmega.juktaway.adapter.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import net.slashOmega.juktaway.fragment.main.tab.BaseFragment
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.UserListCache
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Created on 2018/10/20.
 */
class MainPagerAdapter(val mContext: FragmentActivity, val mViewPager: ViewPager) : FragmentPagerAdapter(mContext.supportFragmentManager) {
    /**
     * タブ内のActivity、引数を設定する。
     *
     * @param mClass    タブに表示するFragment
     * @param args     タブに表示するFragmentに対する引数
     * @param tabTitle タブのタイトル
     * @param id       タブの識別子、タブの取得に利用する
     */
    private class TabInfo(val mClass: KClass<out Fragment>, val args: Bundle?, var tabTitle: String, var id: Long, val searchWord: String? = null)

    private val mTabs = ArrayList<TabInfo>()

    init {
        mViewPager.adapter = this
    }

    override fun getItem(p: Int)
            = mTabs[p].let { Fragment.instantiate(mContext, it.mClass.jvmName, it.args) as BaseFragment}

    override fun getItemId(p: Int) = mTabs[p].id

    override fun getItemPosition(o: Any) = POSITION_NONE

    override fun getPageTitle(position: Int) = mTabs[position].apply {
        if (tabTitle == "-" && args != null) {
            UserListCache.getUserList(args.getInt("userListId").toLong())?.let {
                tabTitle = if (it.user.id == AccessTokenManager.getUserId()) it.name else it.fullName
            }
        }
    }.tabTitle

    override fun getCount() = mTabs.size

    fun findFragmentByPosition(pos: Int) = instantiateItem(mViewPager, pos) as BaseFragment

    fun findPositionById(id: Long) = mTabs.indexOfFirst { it.id == id }

    fun findPositionBySearchWord(str: String)
            = mTabs.indexOfFirst { it.id <= TabManager.SEARCH_TAB_ID && it.searchWord == str }

    fun findFragmentById(id: Long)
            = mTabs.indexOfFirst { it.id == id }.takeIf { it == -1 }?.let { instantiateItem(mViewPager, it) }

    fun addTab(clazz : KClass<out Fragment>, args: Bundle?, tabTitle: String, id: Long, searchWord: String? = null) {
        mTabs.add(TabInfo(clazz, args, tabTitle, id, searchWord))
    }

    fun clearTab() { mTabs.clear() }
}