package net.slash_omega.juktaway

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ListView
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.toJsonArray
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.lists.list
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.TwitterList
import net.slash_omega.juktaway.adapter.SubscribeUserListAdapter
import net.slash_omega.juktaway.event.AlertDialogEvent
import net.slash_omega.juktaway.event.model.DestroyUserListEvent
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_choose_user_lists.*
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.model.*
import net.slash_omega.juktaway.twitter.currentClient
import org.jetbrains.anko.collections.forEachWithIndex

/**
 * Created on 2018/08/29.
 */
class ChooseUserListsActivity: DividedFragmentActivity() {
    private lateinit var mAdapter: SubscribeUserListAdapter
    private lateinit var initial: List<UserListWithRegistered>

    @SuppressLint("UseSparseArrays")
    override fun onCreate(savedInstanceState: Bundle?) {
        launch {
            //TODO
            val lists = currentClient.lists.list().await()
            initial = lists.map { UserListWithRegistered(it) }
            mAdapter.addAll(initial.map { it.copy() })
            UserListCache.userLists = lists.toMutableList()
        }

        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_choose_user_lists)

        actionBar?.run {
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
            val addList = mutableListOf<TwitterList>()
            val removeList = mutableListOf<TwitterList>()
            (0 until mAdapter.count)
                    .map { mAdapter.getItem(it)!! }
                    .forEachWithIndex { i, lr ->
                        if (lr.isRegistered == initial[i].isRegistered.not()) {
                            if (lr.isRegistered) addList.add(lr.userList)
                            else removeList.add(lr.userList)
                        }
                    }
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("add", addList.map{ it.toJsonString() }.toTypedArray())
                putExtra("remove", removeList.map{ it.toJsonString() }.toTypedArray())
            })
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