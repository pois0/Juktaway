package net.slash_omega.juktaway.fragment.main.tab

import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.mentionsTimeline
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

class InteractionsFragment: BaseFragment() {
    override suspend fun getNewStatuses(loadType: LoadStatusesType) = runCatching {
        currentClient.timeline.mentionsTimeline(count = preferences.api.pageCount, sinceId = loadType.requestSinceId,
                maxId = loadType.requestMaxId).await()
    }.getOrNull()
}
