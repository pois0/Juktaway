package net.slashOmega.juktaway.fragment.main.tab

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.twitter.currentIdentifier

class TimelineFragment: BaseFragment() {
    override var tabId = TabManager.TIMELINE_TAB_ID

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = runCatching {
                (if (mMaxId > 0 && !mReloading) currentClient.timeline.home(maxId = mMaxId - 1, count = BasicSettings.pageCount)
                        else currentClient.timeline.home()).await()
            }.onFailure { it.printStackTrace() }.getOrNull()

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