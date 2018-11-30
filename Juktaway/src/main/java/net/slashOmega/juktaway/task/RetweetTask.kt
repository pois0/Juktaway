package net.slashOmega.juktaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import twitter4j.TwitterException

class RetweetTask(private val mStatusId: Long) : AsyncTask<Void, Void, TwitterException>() {

    companion object {
        private const val ERROR_CODE_DUPLICATE = 37
    }

    init {
        FavRetweetManager.setRtId(mStatusId, 0.toLong())
        EventBus.getDefault().post(StatusActionEvent())
    }

    override fun doInBackground(vararg params: Void): TwitterException? {
        return try {
            val status = TwitterManager.twitter.retweetStatus(mStatusId)
            FavRetweetManager.setRtId(status.retweetedStatus.id, status.id)
            null
        } catch (e: TwitterException) {
            FavRetweetManager.setRtId(mStatusId, null)
            e.printStackTrace()
            e
        }

    }

    override fun onPostExecute(e: TwitterException?) {
        when {
            e == null -> MessageUtil.showToast(R.string.toast_retweet_success)
            e.errorCode == ERROR_CODE_DUPLICATE -> MessageUtil.showToast(R.string.toast_retweet_already)
            else -> {
                EventBus.getDefault().post(StatusActionEvent())
                MessageUtil.showToast(R.string.toast_retweet_failure)
            }
        }
    }
}