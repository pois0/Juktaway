package info.justaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import info.justaway.R
import info.justaway.event.model.StreamingDestroyMessageEvent
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil
import twitter4j.DirectMessage

class DestroyDirectMessageTask : AsyncTask<Long, Void, DirectMessage>() {

    override fun doInBackground(vararg params: Long?): DirectMessage? {
        return params[0]?.let {
            try {
                TwitterManager.getTwitter().destroyDirectMessage(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override fun onPostExecute(directMessage: DirectMessage?) {
        if (directMessage != null) {
            MessageUtil.showToast(R.string.toast_destroy_direct_message_success)
            EventBus.getDefault().post(StreamingDestroyMessageEvent(directMessage.id))
        } else {
            MessageUtil.showToast(R.string.toast_destroy_direct_message_failure)
        }
    }
}
