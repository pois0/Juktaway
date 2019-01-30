package net.slash_omega.juktaway.fragment.main.tab

import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.mentionsTimeline
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient

class InteractionsFragment: BaseFragment() {
    override var tabId = TabManager.INTERACTIONS_TAB_ID

    override suspend fun taskExecute() {
        runCatching {
            currentClient.timeline.mentionsTimeline(
                    count = BasicSettings.pageCount,
                    maxId = mMaxId.takeIf { it > 0 && !mReloading }?.minus(1),
                    options = *arrayOf("tweet_mode" to "extended")
            ).await()
        }.onSuccess { statuses ->
            statuses.forEach { if (mMaxId <= 0L || mMaxId > it.id) mMaxId = it.id }
            if (mReloading) {
                clear()
                mAdapter?.addAllFromStatuses(statuses)
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
            } else {
                mAdapter?.extensionAddAllFromStatuses(statuses)
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }
        }.onFailure {
            it.printStackTrace()
            mReloading = false
            mPullToRefreshLayout.setRefreshComplete()
            mListView.visibility = View.VISIBLE
        }

        finishLoad()
    }
}