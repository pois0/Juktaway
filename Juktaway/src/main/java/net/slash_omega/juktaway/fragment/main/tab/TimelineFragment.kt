package net.slash_omega.juktaway.fragment.main.tab

import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.homeTimeline
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

class TimelineFragment: BaseFragment() {
    override suspend fun getNewStatuses(additional: Boolean) = runCatching {
        currentClient.timeline.homeTimeline(count = preferences.api.pageCount, maxId = getRequestMaxId(additional)).await()
    }.getOrNull()
}