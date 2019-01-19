package net.slashOmega.juktaway.fragment.main.tab

import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class InteractionsFragment: BaseFragment() {
    override var tabId = TabManager.INTERACTIONS_TAB_ID

    override suspend fun taskExecute() {
        runCatching {
            currentClient.timeline.mention(
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