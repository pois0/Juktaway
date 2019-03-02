package net.slash_omega.juktaway.fragment.profile

import android.support.v4.widget.SwipeRefreshLayout
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
import net.slash_omega.juktaway.settings.BasicSettings
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
            val timeline = runCatching {
                currentClient.timeline.run {
                    if (mMaxId > 0 && mReload) userTimelineByUserId(user.id, maxId = mMaxId, count = BasicSettings.pageCount)
                    else userTimelineByUserId(user.id, count = BasicSettings.pageCount)
                }.await()
            }.getOrNull()

            timeline?.takeIf { it.isNotEmpty() }?.run {
                if (mReload) {
                    mAdapter.clear()
                    mMaxId = lastOrNull()?.id ?: 0
                    mAdapter.addAllFromStatusesSuspend(this)
                    mReload = false
                } else {
                    mMaxId = lastOrNull()?.id ?: 0
                    mAdapter.extensionAddAllFromStatuses(this)
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
        mSwipeRefreshLayout = sr_layout.apply {
            setOnRefreshListener { reload() }
        }
    }
}