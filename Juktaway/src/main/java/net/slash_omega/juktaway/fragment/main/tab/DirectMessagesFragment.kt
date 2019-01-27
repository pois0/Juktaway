package net.slash_omega.juktaway.fragment.main.tab

import jp.nephy.penicillin.endpoints.directMessages
import net.slash_omega.juktaway.event.model.StreamingDestroyMessageEvent
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient

class DirectMessagesFragment: BaseFragment() {
    override var tabId = TabManager.DIRECT_MESSAGES_TAB_ID

    override suspend fun taskExecute() {
        val sentMessages = currentClient.directMessages.run {
            if (mSentDirectMessagesMaxId > 0 && !mReloading) sentMessages(maxId = mSentDirectMessagesMaxId - 1,
                    count = BasicSettings.pageCount / 2)
            else sentMessages(count = 10)
        }.await().apply {
            lastOrNull { mDirectMessagesMaxId <= 0L || mDirectMessagesMaxId > it.id }?.let { mDirectMessagesMaxId = it.id }
        }
        currentClient.directMessages.run {
            if (mDirectMessagesMaxId > 0 && !mReloading) list( maxId = mDirectMessagesMaxId - 1,
                    count = BasicSettings.pageCount / 2)
            else list(count = 10)
        }.await().toMutableList().apply {
            lastOrNull { mDirectMessagesMaxId <= 0L || mDirectMessagesMaxId > it.id }?.let { mDirectMessagesMaxId = it.id }
            addAll(sentMessages)
        }
    }

    /**
     * DM削除通知
     * @param event DMのID
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StreamingDestroyMessageEvent) { mAdapter?.removeDirectMessage(event.statusId!!) }
}