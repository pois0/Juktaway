package net.slashOmega.juktaway.fragment.main.tab

import android.util.Log
import android.view.View
import io.ktor.http.fullPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class TimelineFragment: BaseFragment() {
    override var tabId = TabManager.TIMELINE_TAB_ID

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = runCatching {
                (if (mMaxId > 0 && !mReloading) currentClient.timeline.home(maxId = mMaxId - 1, count = BasicSettings.pageCount, options = *arrayOf("tweet_mode" to "extended"))
                        else currentClient.timeline.home(count = BasicSettings.pageCount, options = *arrayOf("tweet_mode" to "extended"))).await()
            }.onFailure { it.printStackTrace() }.getOrNull()

            when {
                statuses.isNullOrEmpty() -> {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                }
                mReloading -> {
                    clear()
                    mMaxId = statuses.last().id
                    mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                }
                else -> {
                    mMaxId = statuses.last().id
                    mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }

            finishLoad()
        }
    }
}