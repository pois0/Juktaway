package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.userTimelineByUserId
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.settings.preferences
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

    override suspend fun getNewStatuses(loadType: LoadStatusesType) = runCatching {
        currentClient.timeline.userTimelineByUserId(userId, count = preferences.api.pageCount,
                maxId = loadType.requestMaxId, sinceId = loadType.requestSinceId).await()
    }.getOrNull()?.also { res ->
        res.firstOrNull()?.let { TabManager.refreshUserTab(it.user) }
    }
}