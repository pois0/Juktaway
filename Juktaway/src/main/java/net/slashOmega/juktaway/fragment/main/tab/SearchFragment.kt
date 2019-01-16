package net.slashOmega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.core.request.action.JsonObjectApiAction
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.Search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.twitter.currentClient

class SearchFragment: BaseFragment() {
    private var action: JsonObjectApiAction<Search>? = null

    override var tabId = 0L

    override var mSearchWord: String = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (mSearchWord.isEmpty()) mSearchWord = arguments?.getString("searchWord")?.removeRange(0, 7) ?: ""
        tabId = TabManager.SEARCH_TAB_ID - Math.abs(mSearchWord.hashCode())
        super.onActivityCreated(savedInstanceState)
    }

    override suspend fun taskExecute() {
        runCatching {
            (action?.takeUnless { mReloading } ?: currentClient.search.search("$mSearchWord exclude:retweets"))
                    .await()
        }.onSuccess { qr ->
            if(mReloading) {
                clear()
                mAdapter?.addAllFromStatusesSuspend(qr.result.statuses)
                mReloading = false
                if (qr.hasNext) {
                    action = qr.next
                    mAutoLoader = true
                } else {
                    action = null
                    mAutoLoader = false
                }
                mPullToRefreshLayout.setRefreshComplete()
            } else {
                mAdapter?.extensionAddAllFromStatusesSuspend(qr.result.statuses)
                mAutoLoader = true
                if (qr.hasNext) action = qr.next
                mListView.visibility = View.VISIBLE
            }
        }.onFailure {
            it.printStackTrace()
            mReloading = false
            mPullToRefreshLayout.setRefreshComplete()
            mListView.visibility = View.VISIBLE
            action = null
        }
        finishLoad()
    }
}