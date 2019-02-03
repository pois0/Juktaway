package net.slash_omega.juktaway

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.lists.membershipsByUserId
import jp.nephy.penicillin.endpoints.lists.ownerships
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.adapter.RegisterListAdapter
import net.slash_omega.juktaway.model.UserListWithRegistered
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.list.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.currentClient
import org.jetbrains.anko.startActivity

class RegisterUserListActivity: DividedFragmentActivity() {
    private lateinit var mAdapter: RegisterListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        launch {
            currentClient.runCatching {
                lists.ownerships(count = 200).await().result.lists to
                        lists.membershipsByUserId(intent.getLongExtra("userId", -1)).await().result.lists
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