package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.core.request.action.JsonObjectApiAction
import jp.nephy.penicillin.endpoints.search
import jp.nephy.penicillin.endpoints.search.search
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.Search
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.twitter.currentClient

class SearchFragment: BaseFragment() {
    private var action: JsonObjectApiAction<Search>? = null

    override var mSearchWord: String = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (mSearchWord.isEmpty()) mSearchWord = runCatching { arguments?.getString("searchWord")?.split(":")?.get(1) }.getOrNull() ?: ""
        super.onActivityCreated(savedInstanceState)
    }

    override suspend fun taskExecute() {
        runCatching {
            (action?.takeUnless { mReloading } ?: currentClient.search.search("$mSearchWord exclude:retweets"))
                    .await()
        }.onSuccess { qr ->
            if(mReloading) {
                clear()
                mAdapter?.extensionAddAllFromStatuses(qr.result.statuses)
                mReloading = false
                mAutoLoader = qr.hasNext
                action = qr.takeIf { it.hasNext }?.next
                mSwipeRefreshLayout.isRefreshing = false
            } else {
                mAdapter?.extensionAddAllFromStatuses(qr.result.statuses)
                mAutoLoader = true
                if (qr.hasNext) action = qr.next
                mListView.visibility = View.VISIBLE
            }
        }.onFailure {
            it.printStackTrace()
            mReloading = false
            mSwipeRefreshLayout.isRefreshing = false
            mListView.visibility = View.VISIBLE
            action = null
        }
        finishLoad()
    }
}