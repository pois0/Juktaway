package net.slash_omega.juktaway.fragment.main.tab

import android.view.View
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.mentionsTimeline
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

class InteractionsFragment: BaseFragment() {
    override suspend fun getNewStatuses(additional: Boolean) = runCatching {
        currentClient.timeline.mentionsTimeline(count = preferences.api.pageCount, maxId = getRequestMaxId(additional)).await()
    }.getOrNull()
}