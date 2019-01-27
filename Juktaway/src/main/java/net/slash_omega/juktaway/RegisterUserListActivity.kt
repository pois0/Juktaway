package net.slash_omega.juktaway

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.Menu
import android.view.MenuItem
import jp.nephy.penicillin.endpoints.lists
import net.slash_omega.juktaway.adapter.RegisterListAdapter
import net.slash_omega.juktaway.model.UserListWithRegistered
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.currentClient
import org.jetbrains.anko.startActivity

class RegisterUserListActivity : FragmentActivity() {
    companion object {
        private var job: Job? = null
    }

    private lateinit var mAdapter: RegisterListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        job = GlobalScope.launch(Dispatchers.Main) {
            currentClient.runCatching {
                lists.ownerships(count = 200).await().result.lists to
                        lists.memberships(intent.getLongExtra("userId", -1)).await().result.lists
            }.getOrNull()?.let { (own, member) ->
                val registeredMap = member.associateBy({it.id}, {true})
                own.forEach {
                    mAdapter.add(UserListWithRegistered().apply {
                        isRegistered = registeredMap[it.id] != null
                        userList = it
                    })
                }
            }
        }

        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.list)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        mAdapter = RegisterListAdapter(this,
                R.layout.row_subscribe_user_list, intent.getLongExtra("userId", -1))

        list_list.adapter = mAdapter
    }

    override fun onStop() {
        job?.cancel()
        job = null
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.register_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.create_user_list -> startActivity<CreateUserListActivity>()
        }
        return true
    }
}