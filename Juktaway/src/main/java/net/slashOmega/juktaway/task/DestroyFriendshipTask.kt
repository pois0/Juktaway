package net.slashOmega.juktaway.task

import android.os.AsyncTask

import net.slashOmega.juktaway.model.TwitterManager

open class DestroyFriendshipTask : AsyncTask<Long, Void, Boolean>() {

    override fun doInBackground(vararg params: Long?): Boolean {
        return params[0]?.let {
            try {
                TwitterManager.getTwitter().destroyFriendship(it)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }
}