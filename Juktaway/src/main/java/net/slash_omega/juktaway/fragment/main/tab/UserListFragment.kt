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

    override suspend fun getNewStatuses(additional: Boolean) = runCatching {
        currentClient.timeline.listTimeline(userListId, maxId = getRequestMaxId(additional), count = preferences.api.pageCount).await()
    }.getOrNull()
}