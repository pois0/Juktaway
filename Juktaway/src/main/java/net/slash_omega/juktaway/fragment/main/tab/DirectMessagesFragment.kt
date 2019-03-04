package net.slash_omega.juktaway.fragment.main.tab

import jp.nephy.penicillin.endpoints.directMessages
import jp.nephy.penicillin.endpoints.directmessages.list
import jp.nephy.penicillin.endpoints.directmessages.sentMessages
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.DirectMessage
import net.slash_omega.juktaway.event.model.StreamingDestroyMessageEvent
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient

class DirectMessagesFragment: BaseFragment() {
    override suspend fun taskExecute() {
        val sentMessages = currentClient.directMessages.runCatching {
            if (mSentDirectMessagesMaxId > 0 && !mReloading) sentMessages(maxId = mSentDirectMessagesMaxId - 1,
                    count = BasicSettings.pageCount / 2).await()
            else sentMessages(count = 10).await()
        }.getOrNull()?.apply {
            lastOrNull { mDirectMessagesMaxId <= 0L || mDirectMessagesMaxId > it.id }?.let { mDirectMessagesMaxId = it.id }
        } ?: emptyList<DirectMessage>()
        currentClient.directMessages.runCatching {
            if (mDirectMessagesMaxId > 0 && !mReloading) list( maxId = mDirectMessagesMaxId - 1,
                    count = BasicSettings.pageCount / 2).await()
            else list(count = 10).await()
        }.getOrNull()?.toMutableList()?.apply {
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