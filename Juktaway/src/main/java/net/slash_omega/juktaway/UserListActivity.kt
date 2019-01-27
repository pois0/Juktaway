package net.slash_omega.juktaway

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.models.TwitterList
import net.slash_omega.juktaway.adapter.SimplePagerAdapter
import net.slash_omega.juktaway.fragment.list.UserListStatusesFragment
import net.slash_omega.juktaway.fragment.list.UserMemberFragment
import net.slash_omega.juktaway.model.UserListCache
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_user_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.currentClient
import org.jetbrains.anko.toast

/**
 * Created on 2018/08/27.
 */
class UserListActivity: FragmentActivity() {
    companion object {
        private var mCurrentPosition = 0
        private var mColorBlue: Int = 0
        private var mColorWhite: Int = 0
        private const val MENU_CREATE = 1
        private const val MENU_DESTROY = 2
    }

    private val mUserList by lazy { intent.getStringExtra("userList").toJsonObject().parse(TwitterList::class)}
    private var mIsFollowing: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_user_list)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        mIsFollowing = mUserList.following
        mColorBlue = ThemeUtil.getThemeTextColor(R.attr.holo_blue)
        mColorWhite = ThemeUtil.getThemeTextColor(R.attr.text_color)
        users_label.setTextColor(mColorBlue)

        /*
         * スワイプで動かせるタブを実装するのに最低限必要な実装
         */
        val args = Bundle().apply { putLong("listId", mUserList.id) }
        SimplePagerAdapter(this, list_pager).apply {
            addTab(UserMemberFragment::class, args)
            addTab(UserListStatusesFragment::class, args)
        }.notifyDataSetChanged()
        list_pager.addOnPageChangeListener (object: ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                (if (position == 0) users_label else tweets_label).setTextColor(mColorBlue)
                (if (mCurrentPosition == 0) users_label else tweets_label).setTextColor(mColorWhite)
                mCurrentPosition = position
            }
        })

        users_label.setOnClickListener { list_pager.currentItem = 0 }
        tweets_label.setOnClickListener { list_pager.currentItem = 1 }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.run {
            add(Menu.NONE, MENU_CREATE, Menu.NONE, R.string.menu_create_user_list_subscription)
            add(Menu.NONE, MENU_DESTROY, Menu.NONE, R.string.menu_destroy_user_list_subscription)
            true
        } ?: false
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return menu?.run { findItem(MENU_CREATE)?.let { create -> findItem(MENU_DESTROY)?.let{ destroy ->
            create.isVisible = !mIsFollowing
            destroy.isVisible = mIsFollowing
            super.onPrepareOptionsMenu(menu)
        }}} ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem?) = if(item == null) false
        else when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            MENU_CREATE -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val res = runCatching { currentClient.lists.subscribe(mUserList.id).await() }.isSuccess
                    if (res) {
                        toast(R.string.toast_create_user_list_subscription_success)
                        mIsFollowing = true
                        UserListCache.userLists.add(0, mUserList)
                    } else {
                        toast(R.string.toast_create_user_list_subscription_failure)
                    }
                }
                true
            }
            MENU_DESTROY -> {
                GlobalScope.launch(Dispatchers.Main) {
                    val res = runCatching { currentClient.lists.unsubscribe(mUserList.id).await() }.isSuccess
                    if (res) {
                        toast(R.string.toast_destroy_user_list_subscription_success)
                        mIsFollowing = false
                        UserListCache.userLists.remove(mUserList)
                    } else {
                        toast(R.string.toast_destroy_user_list_subscription_failure)
                    }
                }
                true
            }
            else -> false
        }
}