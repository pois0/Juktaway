package net.slash_omega.juktaway.fragment.main.tab

import jp.nephy.penicillin.endpoints.favorites
import jp.nephy.penicillin.endpoints.favorites.list
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentClient

class FavoritesFragment: BaseFragment() {
    override suspend fun getNewStatuses(loadType: LoadStatusesType) = runCatching {
        currentClient.favorites.list(maxId = loadType.requestMaxId, sinceId = loadType.requestSinceId, count = preferences.api.pageCount).await()
    }.getOrNull()
}