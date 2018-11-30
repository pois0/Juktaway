package net.slashOmega.juktaway.fragment.list


import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.list_guruguru.view.*

import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.PagableResponseList
import twitter4j.User
import java.lang.ref.WeakReference

class UserMemberFragment : Fragment() {
    companion object {
        private class UserListMembersTask(f: UserMemberFragment): AsyncTask<Long, Void, PagableResponseList<User>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg params: Long?): PagableResponseList<User>? = ref.get()?.run { params[0]?.let {
                try {
                    val userListsMembers = TwitterManager.twitter.getUserListMembers(it, mCursor)
                    mCursor = userListsMembers.nextCursor
                    userListsMembers
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }}

            override fun onPostExecute(userListsMembers: PagableResponseList<User>?) { ref.get()?.run {
                mFooter.visibility = View.GONE
                if (userListsMembers == null) return
                for (user in userListsMembers) {
                    mAdapter.add(user)
                }
                if (userListsMembers.hasNext()) {
                    mAutoLoader = true
                }
                mListView.visibility = View.VISIBLE
            }}
        }
    }

    private lateinit var mAdapter: UserAdapter
    private var mListId: Long = 0L
    private var mCursor: Long = -1L
    private lateinit var mListView: ListView
    private lateinit var mFooter: ProgressBar
    private var mAutoLoader = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.list_guruguru, container, false)?.apply {
        arguments?.getLong("listId")?.let { mListId = it }

        mAdapter = UserAdapter(activity, R.layout.row_user)

        // リストビューの設定
        mListView = list_view.apply {
            visibility = View.GONE
            adapter = mAdapter
            setOnScrollListener(object : AbsListView.OnScrollListener {

                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    // 最後までスクロールされたかどうかの判定
                    if (totalItemCount == firstVisibleItem + visibleItemCount) {
                        additionalReading()
                    }
                }
            })
        }

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(mListView)

        mFooter = guruguru
        UserListMembersTask(this@UserMemberFragment).execute(mListId)
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        UserListMembersTask(this).execute(mListId)
    }
}
