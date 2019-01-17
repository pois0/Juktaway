package net.slashOmega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class UserListFragment: BaseFragment() {
    override var tabId = 0L

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (tabId == 0L) tabId = arguments?.getLong("userListId") ?: 0
        super.onActivityCreated(savedInstanceState)
    }

    override suspend fun taskExecute() {
        val statuses = runCatching {
            currentClient.lists.run {
                if (mMaxId > 0 && !mReloading) timeline(tabId, maxId = mMaxId - 1, count = BasicSettings.pageCount)
                else timeline(tabId, count = BasicSettings.pageCount)
            }.await()
        }.getOrNull()

        when {
            statuses.isNullOrEmpty() -> {
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
                mListView.visibility = View.VISIBLE
            }
            mReloading -> {
                clear()
                statuses.lastOrNull { mMaxId <= 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                mAdapter?.extensionAddAllFromStatuses(statuses)
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
            }
            else -> {
                for (status in statuses) {
                    if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id
                }
                mAdapter?.extensionAddAllFromStatuses(statuses)
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }
        }

        finishLoad()
    }
}