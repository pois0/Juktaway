package net.slashOmega.juktaway.fragment.profile

import android.util.Log
import android.view.View
import de.greenrobot.event.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.event.action.StatusActionEvent



/**
 * Created on 2018/11/18.
 */
internal class UserTimelineFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { StatusAdapter(activity!!) }
    override val layout = R.layout.pull_to_refresh_list
    private var mMaxId = 0L
    private var mReload = false
    private lateinit var mPullToRefreshLayout: PullToRefreshLayout

    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val job = async(Dispatchers.Default) {
                TwitterManager.getTwitter().getUserTimeline(user.id, Paging().apply {
                    if (mMaxId > 0) {
                        maxId = mMaxId
                        count = BasicSettings.pageCount
                    }
                })
            }

            mFooter.visibility = View.GONE

            job.await()?.takeIf { it.isNotEmpty() }?.run {
                Log.d("aw;eofi", "hioef")
                if (mReload) {
                    mAdapter.clear()
                    forEach {
                        if (mMaxId == 0L || mMaxId > it.id) mMaxId = it.id
                        mAdapter.add(Row.newStatus(it))
                    }
                    mReload = false
                    mPullToRefreshLayout.setRefreshComplete()
                } else {
                    forEach {
                        if (mMaxId == 0L || mMaxId > it.id) mMaxId = it.id
                        mAdapter.extensionAdd(Row.newStatus(it))
                    }
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }
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

    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        mAdapter.removeStatus(event.statusId!!)
    }

    fun onRefreshStarted(view: View) {
        mReload = true
        mMaxId = 0
        showList()
    }

    override fun View.init() {
        mPullToRefreshLayout = findViewById(R.id.ptr_layout)
    }
}