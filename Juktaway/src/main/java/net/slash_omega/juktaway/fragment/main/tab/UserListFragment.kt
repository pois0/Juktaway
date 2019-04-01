package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.listTimeline
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

class UserListFragment: BaseFragment() {
    var userListId = 0L

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (userListId == 0L) userListId = arguments?.getLong("userListId") ?: 0
        super.onActivityCreated(savedInstanceState)
    }

    override suspend fun getNewStatuses(loadType: LoadStatusesType) = runCatching {
        currentClient.timeline.listTimeline(userListId, maxId = loadType.requestMaxId, sinceId = loadType.requestSinceId,
                count = preferences.api.pageCount).await()
    }.getOrNull()
}