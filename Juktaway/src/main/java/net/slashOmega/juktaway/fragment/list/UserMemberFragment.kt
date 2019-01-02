package net.slashOmega.juktaway.fragment.list

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import android.widget.ProgressBar
import jp.nephy.penicillin.core.PenicillinCursorJsonObjectAction
import jp.nephy.penicillin.models.CursorUsers
import kotlinx.android.synthetic.main.list_guruguru.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.UserAdapter
import net.slashOmega.juktaway.twitter.currentClient

class UserMemberFragment : Fragment() {
    private lateinit var mAdapter: UserAdapter
    private var mListId: Long = 0L
    private var mCursor: PenicillinCursorJsonObjectAction<CursorUsers>? = null
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
        applyListMembers(mListId)
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        applyListMembers(mListId)
    }

    private fun applyListMembers(listId: Long) {
        GlobalScope.launch {
            val resp = runCatching {
                (mCursor ?: currentClient.list.members(listId)).await()
            }.getOrNull()
            mFooter.visibility = View.GONE
            if (resp == null) return@launch
            mCursor = resp.next()
            mAdapter.addAll(resp.result.users)
            if (resp.result.nextCursor < 0) mAutoLoader = true
            mListView.visibility = View.VISIBLE
        }
    }
}
