package net.slashOmega.juktaway.fragment.main.tab

import android.util.Log
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class InteractionsFragment: BaseFragment() {
    override var tabId = TabManager.INTERACTIONS_TAB_ID

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = runCatching {
                currentClient.timeline.run {
                    if (mMaxId > 0 && !mReloading) mention(count = BasicSettings.pageCount, maxId = mMaxId - 1)
                    else mention(count = BasicSettings.pageCount)
                }.await()
            }.getOrNull()

            if (statuses.isNullOrEmpty()) {
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
                mListView.visibility = View.VISIBLE
                finishLoad()
                return@launch
            }

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
            finishLoad()
        }
    }
}