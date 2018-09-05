package net.slashOmega.juktaway

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.Loader
import android.view.MenuItem
import android.widget.ListView
import de.greenrobot.event.EventBus
import net.slashOmega.juktaway.adapter.SubscribeUserListAdapter
import net.slashOmega.juktaway.event.AlertDialogEvent
import net.slashOmega.juktaway.event.model.DestroyUserListEvent
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.UserListCache
import net.slashOmega.juktaway.model.UserListWithRegistered
import net.slashOmega.juktaway.task.UserListsLoader
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_choose_user_lists.*
import twitter4j.ResponseList
import twitter4j.UserList
import java.util.*

/**
 * Created on 2018/08/29.
 */
class ChooseUserListsActivity: FragmentActivity(), android.support.v4.app.LoaderManager.LoaderCallbacks<ResponseList<UserList>> {

    private lateinit var mAdapter: SubscribeUserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_choose_user_lists)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        val listView = findViewById<ListView>(R.id.list)
        mAdapter = SubscribeUserListAdapter(this, R.layout.row_subscribe_user_list)
        listView.adapter = mAdapter

        button_cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        button_save.setOnClickListener { _ ->
            val checkMap = HashMap<Long, Boolean>()
            val checkList = ArrayList<UserList>()
            for (i in 0 until mAdapter.count) {
                val userListWithRegistered = mAdapter.getItem(i)
                userListWithRegistered?.userList?.let {
                    if (userListWithRegistered.isRegistered) {
                        checkMap[it.id] = true
                        checkList.add(it)
                    }
                }
            }
            val tabMap = HashMap<Long, Boolean>()
            val tabs = ArrayList<TabManager.Tab>()
            for (tab in TabManager.loadTabs()) {
                if (tabMap[tab.id] != null) continue
                if (tab.id < 0 || checkMap[tab.id] != null) {
                    tabs.add(tab)
                    tabMap[tab.id] = true
                }
            }
            for (userList in checkList) {
                if (tabMap[userList.id] != null) continue
                val tab = TabManager.Tab(userList.id).apply {
                    name = if (userList.user.id == AccessTokenManager.getUserId()) userList.name
                            else userList.fullName
                }
                tabs.add(tab)
                tabMap[tab.id] = true
            }
            TabManager.saveTabs(tabs)
            setResult(Activity.RESULT_OK)
            finish()
        }

        supportLoaderManager.initLoader<ResponseList<UserList>>(0, null, this)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    fun onEventMainThread(event: AlertDialogEvent) { event.dialogFragment.show(supportFragmentManager, "dialog") }

    fun onEventMainThread(event: DestroyUserListEvent) {
        val userListWithRegistered = mAdapter.findByUserListId(event.userListId)
        if (userListWithRegistered != null) {
            mAdapter.remove(userListWithRegistered)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onCreateLoader(arg0: Int, arg1: Bundle?): Loader<ResponseList<UserList>> = UserListsLoader(this)

    override fun onLoadFinished(arg0: Loader<ResponseList<UserList>>, userLists: ResponseList<UserList>?) {
        userLists?.forEach {
            mAdapter.add(UserListWithRegistered().apply {
                isRegistered = TabManager.hasTabId(it.id)
                userList = it
            })
        }
        UserListCache.userLists = userLists
    }

    override fun onLoaderReset(arg0: Loader<ResponseList<UserList>>) {}
}