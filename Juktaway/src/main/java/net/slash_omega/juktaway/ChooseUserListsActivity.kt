package net.slash_omega.juktaway

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.MenuItem
import android.widget.ListView
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.models.TwitterList
import net.slash_omega.juktaway.adapter.SubscribeUserListAdapter
import net.slash_omega.juktaway.event.AlertDialogEvent
import net.slash_omega.juktaway.event.model.DestroyUserListEvent
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.model.UserListCache
import net.slash_omega.juktaway.model.UserListWithRegistered
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_choose_user_lists.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.twitter.currentIdentifier
import java.util.*

/**
 * Created on 2018/08/29.
 */
class ChooseUserListsActivity: FragmentActivity() {
    companion object {
        var job: Job? = null
    }

    private lateinit var mAdapter: SubscribeUserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        job = GlobalScope.launch(Dispatchers.Main) {
            val lists = currentClient.lists.list().await()
            lists.forEach {
                mAdapter.add(UserListWithRegistered().apply {
                    isRegistered = TabManager.hasTabId(it.id)
                    userList = it
                })
            }
            UserListCache.userLists = lists.toMutableList()
        }

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
            val checkList = ArrayList<TwitterList>()
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
                    name = if (userList.user.id == currentIdentifier.userId) userList.name
                            else userList.fullName
                }
                tabs.add(tab)
                tabMap[tab.id] = true
            }
            TabManager.saveTabs(tabs)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onStop() {
        job?.cancel()
        job = null
        super.onStop()
    }

    fun onEventMainThread(event: AlertDialogEvent) { event.dialogFragment.show(supportFragmentManager, "dialog") }

    fun onEventMainThread(event: DestroyUserListEvent) {
        mAdapter.findByUserListId(event.userListId)?.let {
            mAdapter.remove(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}