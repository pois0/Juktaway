package info.justaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import info.justaway.R
import info.justaway.event.action.StatusActionEvent
import info.justaway.model.FavRetweetManager
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil
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
            val status = TwitterManager.getTwitter().retweetStatus(mStatusId)
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