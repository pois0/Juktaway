package net.slashOmega.juktaway

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.KeyboardUtil
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_user_search.*
import twitter4j.ResponseList
import twitter4j.User
import java.lang.ref.WeakReference

/**
 * Created on 2018/08/29.
 */
class UserSearchActivity: FragmentActivity() {
    companion object {
        private class UserSearchTask(activity: UserSearchActivity) : AsyncTask<String, Void, ResponseList<User>>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg params: String): ResponseList<User>? {
                return try {
                    ref.get()?.run { TwitterManager.twitter.searchUsers(params[0], mPage) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(users: ResponseList<User>?) {
                ref.get()?.run {
                    guruguru.visibility = View.GONE
                    if (users == null) {
                        MessageUtil.showToast(R.string.toast_load_data_failure)
                        return
                    }
                    for (user in users) {
                        mAdapter.add(user)
                    }
                    if (users.size == 20) {
                        mAutoLoading = true
                        mPage++
                    }
                    list_view.visibility = View.VISIBLE
                }
            }
        }
    }

    private var mSearchWord: String? = null
    private var mPage = 1
    private lateinit var mAdapter: UserAdapter
    private var mAutoLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_user_search)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        list_view.visibility = View.GONE
        mAdapter = UserAdapter(this, R.layout.row_user)
        list_view.adapter = mAdapter

        search.setOnClickListener { search() }
        search_text.setOnKeyListener { _, code, e ->
            if (e.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                search()
                true
            } else false
        }

        intent.getStringExtra("query")?.let {
            search_text.setText(it)
            search.performClick()
        } ?: KeyboardUtil.showKeyboard(search_text)

        list_view.setOnScrollListener(object: AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                // 最後までスクロールされたかどうかの判定
                if (totalItemCount == firstVisibleItem + visibleItemCount) {
                    additionalReading()
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    private fun search() {
        KeyboardUtil.hideKeyboard(search_text)
        if (search_text.text == null) return
        mAdapter.clear()
        mPage = 1
        list_view.visibility = View.GONE
        guruguru.visibility = View.VISIBLE
        mSearchWord = search_text.text.toString()
        UserSearchTask(this).execute(mSearchWord)
    }

    private fun additionalReading() {
        if (!mAutoLoading) {
            return
        }
        guruguru.visibility = View.VISIBLE
        mAutoLoading = false
        UserSearchTask(this).execute(mSearchWord)
    }
}