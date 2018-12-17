package net.slashOmega.juktaway.fragment.main.tab

import android.os.AsyncTask
import net.slashOmega.juktaway.event.model.StreamingDestroyMessageEvent
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import twitter4j.DirectMessage
import twitter4j.Paging
import twitter4j.ResponseList
import java.lang.ref.WeakReference

class DirectMessagesFragment: BaseFragment() {
    private class DirectMessagesTask(f: DirectMessagesFragment): AsyncTask<Void, Void, ResponseList<DirectMessage>>() {
        val ref = WeakReference(f)

        override fun doInBackground(vararg p0: Void?) = ref.get()?.run {
            try {
                val twitter = TwitterManager.twitter
                val sentMessages = twitter.getSentDirectMessages(Paging().apply {
                    if (mSentDirectMessagesMaxId > 0 && !mReloading) {
                        maxId = mSentDirectMessagesMaxId - 1
                        count = BasicSettings.pageCount / 2
                    } else {
                        count = 10
                    }
                }).apply {
                    lastOrNull { mDirectMessagesMaxId <= 0L || mDirectMessagesMaxId > it.id }?.let { mDirectMessagesMaxId = it.id }
                }
                twitter.getDirectMessages(Paging().apply {
                    if (mDirectMessagesMaxId > 0 && !mReloading) {
                        maxId = mDirectMessagesMaxId - 1
                        count = BasicSettings.pageCount / 2
                    } else {
                        count = 10
                    }
                }).apply {
                    lastOrNull { mDirectMessagesMaxId <= 0L || mDirectMessagesMaxId > it.id }?.let { mDirectMessagesMaxId = it.id }
                    addAll(sentMessages)
                }
            } catch (e: OutOfMemoryError) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override var tabId = TabManager.DIRECT_MESSAGES_TAB_ID

    override fun isSkip(row: Row) = !row.isDirectMessage

    override fun taskExecute() { DirectMessagesTask(this).execute() }

    /**
     * DM削除通知
     * @param event DMのID
     */
    fun onEventMainThread(event: StreamingDestroyMessageEvent) { mAdapter?.removeDirectMessage(event.statusId!!) }
}