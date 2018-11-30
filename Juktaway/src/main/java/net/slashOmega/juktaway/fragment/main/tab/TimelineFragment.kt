package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class TimelineFragment: BaseFragment() {
    override var tabId = TabManager.TIMELINE_TAB_ID

    override fun isSkip(row: Row): Boolean = !row.isStatus
            || row.status?.retweetedStatus?.user?.id == AccessTokenManager.getUserId()

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = async(Dispatchers.Default) {
                try {
                    TwitterManager.twitter.getHomeTimeline(Paging().also {
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
            }.await()

            if (statuses.isNullOrEmpty()){
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
                mListView.visibility = View.VISIBLE
                finishLoad()
                return@launch
            }

            if (mReloading) {
                clear()
                for (status in statuses) {
                    if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id
                }
                mAdapter!!.addAllFromStatuses(statuses)
                mReloading = false
                mPullToRefreshLayout.setRefreshComplete()
            } else {
                for (status in statuses) {
                    if (mMaxId <= 0L || mMaxId > status.id) {
                        mMaxId = status.id
                    }
                }
                mAdapter!!.extensionAddAllFromStatuses(statuses)
                mAutoLoader = true
                mListView.visibility = View.VISIBLE
            }

            finishLoad()
        }
    }
}