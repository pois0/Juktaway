package net.slashOmega.juktaway.task

import android.os.AsyncTask
import net.slashOmega.juktaway.model.TwitterManager

/**
 * Created on 2018/08/27.
 */
open class FollowTask : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean? {
        return params[0]?.let {
            try {
                TwitterManager.twitter.createFriendship(it)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }
}