package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.Query
import twitter4j.QueryResult
import java.lang.ref.WeakReference

class SearchFragment: BaseFragment() {
    companion object {
        private class SearchTask(f: SearchFragment): AsyncTask<Void, Void,QueryResult>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg p0: Void?): QueryResult? = ref.get()?.run {
                try {
                    TwitterManager.twitter.search(
                            mQuery?.takeUnless { mReloading }?: Query("$mSearchWord exclude:retweets"))

                } catch (e: OutOfMemoryError) {
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(queryResult: QueryResult?) { ref.get()?.run {
                when {
                    queryResult == null -> {
                        mReloading = false
                        mPullToRefreshLayout.setRefreshComplete()
                        mListView.visibility = View.VISIBLE
                        mQuery = null
                    }
                    mReloading -> {
                        clear()
                        mAdapter?.addAllFromStatuses(queryResult.tweets)
                        mReloading = false
                        if (queryResult.hasNext()) {
                            mQuery = queryResult.nextQuery()
                            mAutoLoader = true
                        } else {
                            mQuery = null
                            mAutoLoader = false
                        }
                        mPullToRefreshLayout.setRefreshComplete()
                    }
                    else -> {
                        mAdapter?.extensionAddAllFromStatuses(queryResult.tweets)
                        mAutoLoader = true
                        mQuery = queryResult.nextQuery()
                        mListView.visibility = View.VISIBLE
                    }
                }
                finishLoad()
            }}
        }
    }

    private var mQuery: Query? = null

    override var tabId = 0L

    override var mSearchWord: String = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (mSearchWord.isEmpty()) mSearchWord = arguments?.getString("searchWord") ?: ""
        tabId = TabManager.SEARCH_TAB_ID - Math.abs(mSearchWord.hashCode())
        super.onActivityCreated(savedInstanceState)
    }

    override fun isSkip(row: Row): Boolean = !row.isStatus || (row.status?.text?.contains(mSearchWord)?.not()?: true)

    override fun taskExecute() { SearchTask(this).execute() }
}