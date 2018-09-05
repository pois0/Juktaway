package net.slashOmega.juktaway

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import net.slashOmega.juktaway.adapter.SimplePagerAdapter
import net.slashOmega.juktaway.fragment.list.UserListStatusesFragment
import net.slashOmega.juktaway.fragment.list.UserMemberFragment
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserListCache
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_user_list.*
import twitter4j.UserList
import java.lang.ref.WeakReference

/**
 * Created on 2018/08/27.
 */
class UserListActivity: FragmentActivity() {
    companion object {
        private var mCurrentPosition = 0
        private var mColorBlue: Int = 0
        private var mColorWhite: Int = 0
        private val MENU_CREATE = 1
        private val MENU_DESTROY = 2

        private class CreateMenu(activity: UserListActivity): AsyncTask<Void, Void, Boolean>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg p0: Void?): Boolean {
                return ref.get()?.run {
                    try {
                        TwitterManager.getTwitter().createUserListSubscription(mUserList.id)
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: false
            }

            override fun onPostExecute(res: Boolean) {
                if (res) {
                    MessageUtil.showToast(R.string.toast_create_user_list_subscription_success)
                    ref.get()?.let {
                        it.mIsFollowing = true
                        UserListCache.userLists?.apply {
                            add(0, it.mUserList)
                        }
                    }
                } else {
                    MessageUtil.showToast(R.string.toast_create_user_list_subscription_failure)
                }
            }
        }

        private class DestroyMenu(activity: UserListActivity): AsyncTask<Void, Void, Boolean>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg p0: Void?): Boolean {
                return ref.get()?.run {
                    try {
                        TwitterManager.getTwitter().destroyUserListSubscription(mUserList.id)
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: false
            }

            override fun onPostExecute(res: Boolean) {
                if (res) {
                    MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_success)
                    ref.get()?.let {
                        it.mIsFollowing = false
                        UserListCache.userLists?.apply { remove(it.mUserList) }
                    }
                } else {
                    MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_failure)
                }
            }
        }
    }

    private lateinit var mUserList: UserList
    private var mIsFollowing: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_user_list)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        (intent.getSerializableExtra("userList") as UserList?)?.let { ul ->
            mIsFollowing = ul.isFollowing
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
        }

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return item?.let { when (it.itemId) {
            R.id.home -> {
                finish()
                true
            }
            MENU_CREATE -> {
                CreateMenu(this).execute()
                true
            }
            MENU_DESTROY -> {
                DestroyMenu(this).execute()
                true
            }
            else -> false
        }} ?: false
    }
}