package net.slash_omega.juktaway.fragment.main.tab

import jp.nephy.penicillin.core.request.action.JsonObjectApiAction
import jp.nephy.penicillin.endpoints.search
import jp.nephy.penicillin.endpoints.search.search
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.Search
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

class SearchFragment: BaseFragment() {
    private var action: JsonObjectApiAction<Search>? = null

    private val searchWord by lazy { arguments?.getString("searchWord") ?: "" }

    override suspend fun getNewStatuses(loadType: LoadStatusesType) = runCatching {
        (action?.takeIf { loadType.limitMax }
                ?: currentClient.search.search("$searchWord exclude:retweets",
                        count = preferences.api.pageCount,  sinceId = loadType.requestSinceId)
        ).await()
    }.getOrNull()?.also {
        if (!loadType.limitMin) {
            action = it.next
            hasNext = it.hasNext
        }
    }?.result?.statuses
}
