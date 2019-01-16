package net.slashOmega.juktaway.fragment.main.tab

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class TimelineFragment: BaseFragment() {
    override var tabId = TabManager.TIMELINE_TAB_ID

    override suspend fun taskExecute() {
        runCatching {
            currentClient.timeline.home(
                    count = BasicSettings.pageCount,
                    maxId = mMaxId.takeIf { it > 0 && !mReloading }?.minus(1),
                    options = *arrayOf("tweet_mode" to "extended")
            ).await()
        }.onSuccess { statuses ->
            if (mReloading) {
                clear()
                mMaxId = statuses.last().id
                mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
            } else {
                mMaxId = statuses.last().id
                mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }
        }.onFailure {
            mReloading = false
            mPullToRefreshLayout.setRefreshComplete()
            mListView.visibility = View.VISIBLE
        }

        finishLoad()
    }
}