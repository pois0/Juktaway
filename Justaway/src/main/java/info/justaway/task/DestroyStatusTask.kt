package info.justaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import info.justaway.R
import info.justaway.event.model.StreamingDestroyStatusEvent
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil

class DestroyStatusTask(private val mStatusId: Long) : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean? {
        return try {
            TwitterManager.getTwitter().destroyStatus(mStatusId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onPostExecute(success: Boolean?) {
        if (success!!) {
            MessageUtil.showToast(R.string.toast_destroy_status_success)
            EventBus.getDefault().post(StreamingDestroyStatusEvent(mStatusId))
        } else {
            MessageUtil.showToast(R.string.toast_destroy_status_failure)
        }
    }
}