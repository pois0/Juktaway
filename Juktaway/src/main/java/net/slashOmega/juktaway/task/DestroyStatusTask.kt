package net.slashOmega.juktaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil

class DestroyStatusTask(private val mStatusId: Long) : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean? {
        return try {
            TwitterManager.twitter.destroyStatus(mStatusId)
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