package info.justaway.task

import android.os.AsyncTask
import info.justaway.model.TwitterManager

/**
 * Created on 2018/08/27.
 */
open class FollowTask : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean? {
        return params[0]?.let {
            try {
                TwitterManager.getTwitter().createFriendship(it)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }
}