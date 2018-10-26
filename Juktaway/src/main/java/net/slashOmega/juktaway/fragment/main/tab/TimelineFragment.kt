package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import android.view.View
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
    companion object {
        private class HomeTimelineTask(f: TimelineFragment): AsyncTask<Void, Void, ResponseList<Status>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg p0: Void?): ResponseList<twitter4j.Status>? = ref.get()?.run {
                try {
                    TwitterManager.getTwitter().getHomeTimeline(Paging().also {
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
                mFooter.visibility = View.GONE
                if (statuses == null || statuses.isEmpty()) {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                    return
                }
                if (mReloading) {
                    clear()
                    for (status in statuses) {
                        if (mMaxId <= 0L || mMaxId > status.id) {
                            mMaxId = status.id
                        }
                        mAdapter!!.add(Row.newStatus(status))
                    }
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                } else {
                    for (status in statuses) {
                        if (mMaxId <= 0L || mMaxId > status.id) {
                            mMaxId = status.id
                        }
                        mAdapter!!.extensionAdd(Row.newStatus(status))
                    }
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }}
        }
    }

    override var tabId = TabManager.TIMELINE_TAB_ID

    override fun isSkip(row: Row): Boolean = !row.isStatus
            || row.status?.retweetedStatus?.user?.id == AccessTokenManager.getUserId()

    override fun taskExecute() { HomeTimelineTask(this).execute() }
}