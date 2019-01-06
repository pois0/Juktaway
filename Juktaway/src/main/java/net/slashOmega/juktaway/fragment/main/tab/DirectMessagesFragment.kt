package net.slashOmega.juktaway.fragment.main.tab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.event.model.StreamingDestroyMessageEvent
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class DirectMessagesFragment: BaseFragment() {
    override var tabId = TabManager.DIRECT_MESSAGES_TAB_ID

    override fun taskExecute() {
        GlobalScope.launch(Dispatchers.Main) {
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
    }

    /**
     * DM削除通知
     * @param event DMのID
     */
    fun onEventMainThread(event: StreamingDestroyMessageEvent) { mAdapter?.removeDirectMessage(event.statusId!!) }
}