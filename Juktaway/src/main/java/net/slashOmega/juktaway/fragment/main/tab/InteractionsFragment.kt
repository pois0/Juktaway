package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import android.view.View
import net.slashOmega.juktaway.event.model.StreamingCreateFavoriteEvent
import net.slashOmega.juktaway.event.model.StreamingUnFavoriteEvent
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentIdentifier
import net.slashOmega.juktaway.util.StatusUtil
import twitter4j.Paging
import twitter4j.ResponseList
import twitter4j.Status
import java.lang.ref.WeakReference

class InteractionsFragment: BaseFragment() {
    companion object {
        private class MentionsTimelineTask(f: InteractionsFragment) : AsyncTask<Void, Void, ResponseList<Status>>() {
            val ref = WeakReference(f)

            override fun doInBackground(vararg params: Void): ResponseList<twitter4j.Status>? = ref.get()?.run {
                try {
                    TwitterManager.twitter.getMentionsTimeline(Paging().also {
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
                statuses.forEach { if (mMaxId <= 0L || mMaxId > it.id) mMaxId = it.id }
                if (mReloading) {
                    clear()
                    mAdapter?.addAllFromStatuses(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                } else {
                    mAdapter?.extensionAddAllFromStatuses(statuses)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
                finishLoad()
            }}
        }
    }

    override var tabId = TabManager.INTERACTIONS_TAB_ID

    override fun isSkip(row: Row): Boolean = when {
        row.isFavorite -> row.source?.id == currentIdentifier.userId
        row.isStatus -> {
            val status = row.status
            val retweet = status!!.retweetedStatus

            /**
             * 自分のツイートがRTされた時
             */
            if (retweet != null && retweet.user.id == currentIdentifier.userId) false

            /**
             * 自分宛のメンション（但し「自分をメンションに含むツイートがRTされた時」はうざいので除く）
             */
            else !(retweet == null && StatusUtil.isMentionForMe(status))
        }
        else -> true
    }

    override fun taskExecute() { MentionsTimelineTask(this).execute() }

    /**
     * ストリーミングAPIからふぁぼを受け取った時のイベント
     * @param event ふぁぼイベント
     */
    fun onEventMainThread(event: StreamingCreateFavoriteEvent) { addStack(event.row) }

    /**
     * ストリーミングAPIからあんふぁぼイベントを受信
     * @param event ツイート
     */
    fun onEventMainThread(event: StreamingUnFavoriteEvent) {
        val removePositions = mAdapter?.removeStatus(event.status.id) ?: return
        for (removePosition in removePositions) {
            if (removePosition >= 0) {
                val visiblePosition = mListView.firstVisiblePosition
                if (visiblePosition > removePosition) {
                    val view = mListView.getChildAt(0)
                    val y = view?.top ?: 0
                    mListView.setSelectionFromTop(visiblePosition - 1, y)
                    break
                }
            }
        }
    }
}