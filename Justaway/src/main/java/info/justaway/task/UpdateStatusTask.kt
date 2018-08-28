package info.justaway.task

import android.os.AsyncTask
import info.justaway.model.ImageResizer
import info.justaway.model.TwitterManager
import info.justaway.settings.PostStockSettings
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import java.io.File

/**
 * Created on 2018/08/28.
 */
open class UpdateStatusTask(private val mAccessToken: AccessToken?, private val mImagePathList: ArrayList<File>): AsyncTask<StatusUpdate, Void, TwitterException>() {
    companion object {
        private const val maxFileSize: Long = 3145728
    }

    override fun doInBackground(vararg params: StatusUpdate?): TwitterException? {
        return params[0]?.let { sUpdate ->
            try {
                val status = mAccessToken?.let { token ->
                    val twitter = TwitterManager.getTwitterInstance().apply { oAuthAccessToken = token }
                    if (mImagePathList.isNotEmpty()) {
                        sUpdate.setMediaIds(*mImagePathList.map {
                            twitter.uploadMedia(ImageResizer.compress(it, maxFileSize)).mediaId
                        }.toLongArray())
                    }
                    twitter.updateStatus(sUpdate)
                } ?: TwitterManager.getTwitter().updateStatus(sUpdate)
                PostStockSettings().apply {
                    status.hashtagEntities.forEach { addHashtag("#${it.text}") }
                }
                null
            } catch (e: TwitterException) {
                e.printStackTrace()
                e
            }
        }
    }
}