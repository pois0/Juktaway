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

import de.greenrobot.event.EventBus
import kotlinx.android.synthetic.main.list_guruguru.view.*
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class UserListStatusesFragment : Fragment() {
    companion object {
        private class UserListTask(f: UserListStatusesFragment) : AsyncTask<Long, Void, ResponseList<Status>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg params: Long?): ResponseList<twitter4j.Status>? = ref.get()?.run { params[0]?.let {
                try {
                    TwitterManager.getTwitter().getUserListStatuses(it, Paging().apply {
                        if (mMaxId > 0) {
                            maxId = mMaxId - 1
                            count = BasicSettings.pageCount
                        }
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }}

            override fun onPostExecute(statuses: ResponseList<twitter4j.Status>?) { ref.get()?.run {
                mFooter.visibility = View.GONE

                if (statuses == null || statuses.size == 0) return

                for (status in statuses) {
                    if (mMaxId == 0L || mMaxId > status.id) {
                        mMaxId = status.id
                    }
                    mAdapter.add(Row.newStatus(status))
                }
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }}
        }
    }

    private lateinit var mAdapter: StatusAdapter
    private lateinit var mListView: ListView
    private var mListId: Long = 0
    private lateinit var mFooter: ProgressBar
    private var mAutoLoader = false
    private var mMaxId = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.list_guruguru, container, false)?.apply {
        arguments?.getLong("listId")?.let { mListId = it }
        mAdapter = StatusAdapter(activity!!, R.layout.row_tweet)

        // リストビューの設定
        mListView = list_view.apply {
            visibility = View.GONE
            adapter = mAdapter
            onItemClickListener = StatusClickListener(activity!!)
            onItemLongClickListener = StatusLongClickListener(activity!!)
            setOnScrollListener(object : AbsListView.OnScrollListener {

                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    // 最後までスクロールされたかどうかの判定
                    if (totalItemCount == firstVisibleItem + visibleItemCount) { additionalReading() }
                }
            })
        }

        mFooter = guruguru

        UserListTask(this@UserListStatusesFragment).execute(mListId)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    fun onEventMainThread(event: StatusActionEvent) { mAdapter.notifyDataSetChanged() }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) { mAdapter.removeStatus(event.statusId!!) }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        UserListTask(this).execute(mListId)
    }
}
