package net.slashOmega.juktaway.fragment.main.tab

import android.os.Bundle
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.Query

class SearchFragment: BaseFragment() {
    private var mQuery: Query? = null

    override var tabId = 0L

    override var mSearchWord: String = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (mSearchWord.isEmpty()) mSearchWord = arguments?.getString("searchWord") ?: ""
        tabId = TabManager.SEARCH_TAB_ID - Math.abs(mSearchWord.hashCode())
        super.onActivityCreated(savedInstanceState)
    }

    override fun isSkip(row: Row): Boolean = !row.isStatus || (row.status?.text?.contains(mSearchWord)?.not()?: true)

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
            val qr = try {
                TwitterManager.twitter.search(
                        mQuery?.takeUnless { mReloading }?: Query("$mSearchWord exclude:retweets"))

            } catch (e: OutOfMemoryError) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            when {
                qr == null -> {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                    mQuery = null
                }
                mReloading -> {
                    clear()
                    mAdapter?.addAllFromStatusesSuspend(qr.tweets)
                    mReloading = false
                    if (qr.hasNext()) {
                        mQuery = qr.nextQuery()
                        mAutoLoader = true
                    } else {
                        mQuery = null
                        mAutoLoader = false
                    }
                    mPullToRefreshLayout.setRefreshComplete()
                }
                else -> {
                    mAdapter?.extensionAddAllFromStatusesSuspend(qr.tweets)
                    mAutoLoader = true
                    mQuery = qr.nextQuery()
                    mListView.visibility = View.VISIBLE
                }
            }
            finishLoad()
        }
    }
}