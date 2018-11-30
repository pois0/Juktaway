package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import android.view.View
import net.slashOmega.juktaway.model.*
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class FavoritesFragment: BaseFragment() {
    companion object {
        private class FavoritesTask(f: FavoritesFragment): AsyncTask<Void, Void, ResponseList<Status>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg p0: Void?): ResponseList<twitter4j.Status>? = ref.get()?.run {
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

            override fun onPostExecute(statuses: ResponseList<twitter4j.Status>?) { ref.get()?.run {
                if (statuses.isNullOrEmpty()) {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                    finishLoad()
                    return
                }
                if (mReloading) {
                    clear()
                    for (status in statuses) {
                        FavRetweetManager.setFav(status.id)
                        if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id
                    }
                    mAdapter?.addAllFromStatuses(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                } else {
                    for (status in statuses) {
                        FavRetweetManager.setFav(status.id)
                        if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id
                    }
                    mAdapter?.extensionAddAllFromStatuses(statuses)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
                finishLoad()
            }}
        }
    }

    override var tabId = TabManager.FAVORITES_TAB_ID

    override fun isSkip(row: Row): Boolean = !row.isFavorite || row.source?.id != AccessTokenManager.getUserId()

    override fun taskExecute() { FavoritesTask(this).execute() }
}