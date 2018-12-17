package net.slashOmega.juktaway.fragment.main.tab

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.model.*
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging

class FavoritesFragment: BaseFragment() {
    override var tabId = TabManager.FAVORITES_TAB_ID

    override fun isSkip(row: Row): Boolean = !row.isFavorite || row.source?.id != AccessTokenManager.getUserId()

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = withContext(Dispatchers.Default) {
                try {
                    TwitterManager.twitter.getFavorites(Paging().also {
                        if (mMaxId > 0 && !mReloading) {
                            it.maxId = mMaxId - 1
                            it.count = BasicSettings.pageCount
                        }
                    })
                } catch (e: OutOfMemoryError) {
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            when {
                statuses.isNullOrEmpty() -> {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                }
                mReloading -> {
                    clear()
                    for (status in statuses) {
                        FavRetweetManager.setFav(status.id)
                        if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id
                    }
                    mAdapter?.addAllFromStatusesSuspend(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                }
                else -> {
                    for (status in statuses) {
                        FavRetweetManager.setFav(status.id)
                        if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id
                    }
                    mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }

            finishLoad()
        }
    }
}