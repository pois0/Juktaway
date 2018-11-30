package net.slashOmega.juktaway.task

import android.os.AsyncTask
import net.slashOmega.juktaway.model.ImageResizer
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.PostStockSettings
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
                    val twitter = TwitterManager.twitterInstance.apply { oAuthAccessToken = token }
                    if (mImagePathList.isNotEmpty()) {
                        sUpdate.setMediaIds(*mImagePathList.map {
                            twitter.uploadMedia(ImageResizer.compress(it, maxFileSize)).mediaId
                        }.toLongArray())
                    }
                    twitter.updateStatus(sUpdate)
                } ?: TwitterManager.twitter.updateStatus(sUpdate)

                status.hashtagEntities.forEach { PostStockSettings.addHashtag("#${it.text}") }
                null
            } catch (e: TwitterException) {
                e.printStackTrace()
                e
            }
        }
    }
}