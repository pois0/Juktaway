package net.slash_omega.juktaway.fragment.profile

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.View
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.userTimelineByUserId
import jp.nephy.penicillin.extensions.await
import kotlinx.android.synthetic.main.pull_to_refresh_list.view.*
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient


/**
 * Created on 2018/11/18.
 */
internal class UserTimelineFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { StatusAdapter(activity!!) }
    override val layout = R.layout.pull_to_refresh_list
    private var mMaxId = 0L
    private var mReload = false
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    override fun showList() {
        mFooter.visibility = View.GONE
        mSwipeRefreshLayout.isRefreshing = true
        launch {
            val action = currentClient.timeline.run {
                if (mMaxId > 0 && !mReload) {
                    userTimelineByUserId(user.id, maxId = mMaxId, count = preferences.api.pageCount)
                } else {
                    userTimelineByUserId(user.id, count = preferences.api.pageCount)
                }
            }

            action.runCatching { await() }.onSuccess { response ->
                if (response.isEmpty()) return@onSuccess

                if (mReload) {
                    mAdapter.clear()
                    mMaxId = response.last().id - 1
                    mAdapter.addAllSuspend(response)
                    mReload = false
                } else {
                    mMaxId = response.last().id - 1
                    mAdapter.extensionAddAllFromStatuses(response)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }

            mSwipeRefreshLayout.isRefreshing = false
            finishLoading()
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

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) = launch { mAdapter.removeStatus(event.statusId!!) }

    private fun reload() {
        mReload = true
        mMaxId = 0
        showList()
    }

    override fun View.init() {
        mListView.onItemClickListener = StatusClickListener(activity!!)
        mListView.onItemLongClickListener = StatusLongClickListener(activity!!)
        mSwipeRefreshLayout = swipe_refresh_layout.apply {
            setOnRefreshListener { reload() }
        }
    }
}
