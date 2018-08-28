package info.justaway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.Menu
import android.view.MenuItem
import info.justaway.adapter.RegisterListAdapter
import info.justaway.model.UserListWithRegistered
import info.justaway.task.RegisterUserListsLoader
import info.justaway.util.ThemeUtil
import kotlinx.android.synthetic.main.list.*
import twitter4j.ResponseList
import twitter4j.UserList
import java.util.*

private typealias UsersResponseLists = ArrayList<ResponseList<UserList>>

class RegisterUserListActivity : FragmentActivity(), LoaderManager.LoaderCallbacks<UsersResponseLists> {

    private lateinit var mAdapter: RegisterListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.list)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        list_list.adapter = RegisterListAdapter(this,
                R.layout.row_subscribe_user_list, intent.getLongExtra("userId", -1))

        supportLoaderManager.initLoader(0, Bundle(1).apply {
                    putLong("userId", intent.getLongExtra("userId", -1))
                }, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.register_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.create_user_list ->
                startActivity(Intent(this, CreateUserListActivity::class.java))
        }
        return true
    }

    override fun onCreateLoader(arg0: Int, arg1: Bundle?)
            = RegisterUserListsLoader(this, arg1!!.getLong("userId"))


    @SuppressLint("UseSparseArrays")
    override fun onLoadFinished(arg0: Loader<UsersResponseLists>, responseLists: UsersResponseLists?) {
        responseLists?.let { lists ->
            val registeredMap = lists[1].associateBy({it.id}, {true})
            lists[0].forEach {
                mAdapter.add(UserListWithRegistered().apply {
                    isRegistered = registeredMap[it.id] != null
                    userList = it
                })
            }
        }
    }

    override fun onLoaderReset(arg0: Loader<ArrayList<ResponseList<UserList>>>) {}
}