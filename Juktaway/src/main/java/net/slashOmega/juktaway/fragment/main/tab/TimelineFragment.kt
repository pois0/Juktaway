package net.slashOmega.juktaway.fragment.main.tab

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentIdentifier
import twitter4j.Paging

class TimelineFragment: BaseFragment() {
    override var tabId = TabManager.TIMELINE_TAB_ID

    override fun isSkip(row: Row): Boolean = !row.isStatus
            || row.status?.retweetedStatus?.user?.id == currentIdentifier.userId

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = withContext(Dispatchers.Default) {
                try {
                    TwitterManager.twitter.getHomeTimeline(Paging().also {
                        if (mMaxId > 0 && !mReloading) {
                            it.maxId = mMaxId - 1
                            it.count = BasicSettings.pageCount
                        }
                    })
                } catch (e: OutOfMemoryError) {
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            when {
                statuses.isNullOrEmpty() -> {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                }
                mReloading -> {
                    clear()
                    statuses.lastOrNull { mMaxId <= 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                    mAdapter?.addAllFromStatusesSuspend(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                }
                else -> {
                    statuses.lastOrNull { mMaxId <= 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                    mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }

            finishLoad()
        }
    }
}