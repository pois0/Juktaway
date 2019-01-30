package net.slash_omega.juktaway.fragment.main.tab

import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.homeTimeline
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient

class TimelineFragment: BaseFragment() {
    override var tabId = TabManager.TIMELINE_TAB_ID

    override suspend fun taskExecute() {
        runCatching {
            currentClient.timeline.homeTimeline (
                    count = BasicSettings.pageCount,
                    maxId = mMaxId.takeIf { it > 0 && !mReloading }?.minus(1),
                    options = *arrayOf("tweet_mode" to "extended")
            ).await()
        }.onSuccess { statuses ->
            if (mReloading) {
                clear()
                mMaxId = statuses.last().id
                mAdapter?.extensionAddAllFromStatuses(statuses)
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
            } else {
                mMaxId = statuses.last().id
                mAdapter?.extensionAddAllFromStatuses(statuses)
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