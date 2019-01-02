package net.slashOmega.juktaway.fragment.main.tab

import android.os.Bundle
import android.support.v4.util.LongSparseArray
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.Paging

class UserListFragment: BaseFragment() {
    override var tabId = 0L

    private val mMembers = LongSparseArray<Boolean>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (tabId == 0L) tabId = arguments?.getLong("userListId") ?: 0
        super.onActivityCreated(savedInstanceState)
    }

    override fun isSkip(row: Row): Boolean = mMembers.get(row.status!!.user.id) == null

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = try {
                val twitter = TwitterManager.twitter
                withContext(Dispatchers.Default) {
                    twitter.getUserListStatuses(tabId, Paging().also {
                        if (mMaxId > 0 && !mReloading) {
                            it.maxId = mMaxId - 1
                            it.count = BasicSettings.pageCount
                        } else {
                            twitter.getUserListMembers(tabId, 0).forEach { mMembers.append(it.id, true) }
                        }
                    })
                }
            } catch (e: OutOfMemoryError) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            when {
                statuses.isNullOrEmpty() -> {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                }
                mReloading -> {
                    clear()
                    statuses.lastOrNull { mMaxId <= 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                    mAdapter?.addAllFromStatusesSuspend(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                }
                else -> {
                    for (status in statuses) {
                        if (mMaxId <= 0L || mMaxId > status.id) mMaxId = status.id

                        // 最初のツイートに登場ユーザーをStreaming APIからの取り込み対象にすることでAPI節約!!!
                        mMembers.append(status.user.id, true)
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