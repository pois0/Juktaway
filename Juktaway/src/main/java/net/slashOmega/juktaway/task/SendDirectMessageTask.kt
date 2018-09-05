package net.slashOmega.juktaway.task

import android.os.AsyncTask

import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.auth.AccessToken

open class SendDirectMessageTask(private val mAccessToken: AccessToken?) : AsyncTask<String, Void, TwitterException>() {

    override fun doInBackground(vararg params: String): TwitterException? {
        return try {
            val s = params[0].split(" ".toRegex(), 3).toTypedArray()
            if (mAccessToken == null) {
                TwitterManager.getTwitter().sendDirectMessage(getOrEmpty(s, 1), getOrEmpty(s, 2))
            } else {
                // ツイート画面から来たとき
                TwitterManager.getTwitterInstance().apply { oAuthAccessToken = mAccessToken }
                        .sendDirectMessage(getOrEmpty(s, 1), getOrEmpty(s, 2))
            }
            null
        } catch (e: TwitterException) {
            e
        }
    }

    private fun getOrEmpty(array: Array<String>, index: Int): String = if (index <= 0 || array.size <= index) "" else array[index]

}