package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.userTimelineByUserId
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.twitter.currentClient

/**
 * Created on 2019/02/25.
 */

class UserFragment: BaseFragment() {
    private var userId = 0L

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (userId == 0L) userId = arguments?.getLong("userId") ?: 0
        super.onActivityCreated(savedInstanceState)
    }

    override suspend fun taskExecute() {
        val statuses = runCatching {
            currentClient.timeline.userTimelineByUserId(userId).await()
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
    }
}