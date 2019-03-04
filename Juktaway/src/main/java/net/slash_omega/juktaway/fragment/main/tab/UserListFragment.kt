package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.listTimeline
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient

class UserListFragment: BaseFragment() {
    var userListId = 0L

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (userListId == 0L) userListId = arguments?.getLong("userListId") ?: 0
        super.onActivityCreated(savedInstanceState)
    }

    override suspend fun taskExecute() {
        val statuses = runCatching {
            currentClient.timeline.run {
                if (mMaxId > 0 && !mReloading) listTimeline(userListId, maxId = mMaxId - 1, count = BasicSettings.pageCount)
                else listTimeline(userListId, count = BasicSettings.pageCount)
            }.await()
        }.getOrNull()

        when {
            statuses.isNullOrEmpty() -> {
                mReloading = false
                mSwipeRefreshLayout.isRefreshing = false
                mListView.visibility = View.VISIBLE
            }
            mReloading -> {
                clear()
                statuses.lastOrNull { mMaxId <= 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                mAdapter?.extensionAddAllFromStatuses(statuses)
                mReloading = false
                mSwipeRefreshLayout.isRefreshing = false
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