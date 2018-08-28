package info.justaway.task

import android.os.AsyncTask
import info.justaway.model.TwitterManager

open class DestroyBlockTask: AsyncTask<Long, Void, Boolean>() {
    override fun doInBackground(vararg params: Long?): Boolean {
        return params[0]?.let {
            try {
                TwitterManager.getTwitter().destroyBlock(it)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }
}