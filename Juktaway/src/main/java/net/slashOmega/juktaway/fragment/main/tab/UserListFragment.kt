package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.util.LongSparseArray
import android.view.View
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class UserListFragment: BaseFragment() {
    companion object {
        private class UserListStatusesTask(f: UserListFragment) : AsyncTask<Void, Void, ResponseList<Status>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg params: Void): ResponseList<twitter4j.Status>? = ref.get()?.run {
                try {
                    val twitter = TwitterManager.getTwitter()
                    twitter.getUserListStatuses(tabId, Paging().also {
                        if (mMaxId > 0 && !mReloading) {
                            it.maxId = mMaxId - 1
                            it.count = BasicSettings.pageCount
                        } else {
                            twitter.getUserListMembers(tabId, 0).forEach { mMembers.append(it.id, true) }
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
                if (statuses == null || statuses.size == 0) {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                    finishLoad()
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

                        // 最初のツイートに登場ユーザーをStreaming APIからの取り込み対象にすることでAPI節約!!!
                        mMembers.append(status.user.id, true)

                        mAdapter!!.extensionAdd(Row.newStatus(status))
                    }
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
                finishLoad()
            }}
        }
    }

    override var tabId = 0L

    private val mMembers = LongSparseArray<Boolean>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (tabId == 0L) tabId = arguments?.getLong("userListId") ?: 0
        super.onActivityCreated(savedInstanceState)
    }

    override fun isSkip(row: Row): Boolean = mMembers.get(row.status!!.user.id) == null

    override fun taskExecute() { UserListStatusesTask(this).execute() }
}