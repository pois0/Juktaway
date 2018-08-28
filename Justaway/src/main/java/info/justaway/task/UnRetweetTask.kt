package info.justaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import info.justaway.R
import info.justaway.event.action.StatusActionEvent
import info.justaway.event.model.StreamingDestroyStatusEvent
import info.justaway.model.FavRetweetManager
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil
import twitter4j.TwitterException

class UnRetweetTask(private val mRetweetedStatusId: Long, private val mStatusId: Long) : AsyncTask<Void, Void, TwitterException>() {

    companion object {
        private const val ERROR_CODE_DUPLICATE = 34
    }

    init {
        if (mRetweetedStatusId > 0) {
            FavRetweetManager.setRtId(mRetweetedStatusId, null)
            EventBus.getDefault().post(StatusActionEvent())
        }
    }

    override fun doInBackground(vararg params: Void): TwitterException? {
        return try {
            TwitterManager.getTwitter().destroyStatus(mStatusId)
            null
        } catch (e: TwitterException) {
            e.printStackTrace()
            e
        }

    }

    override fun onPostExecute(e: TwitterException?) {
        when {
            e == null -> {
                MessageUtil.showToast(R.string.toast_destroy_retweet_success)
                EventBus.getDefault().post(StreamingDestroyStatusEvent(mStatusId))
            }
            e.errorCode == ERROR_CODE_DUPLICATE -> MessageUtil.showToast(R.string.toast_destroy_retweet_already)
            else -> {
                if (mRetweetedStatusId > 0) {
                    FavRetweetManager.setRtId(mRetweetedStatusId, mStatusId)
                    EventBus.getDefault().post(StatusActionEvent())
                }
                MessageUtil.showToast(R.string.toast_destroy_retweet_failure)
            }
        }
    }
}