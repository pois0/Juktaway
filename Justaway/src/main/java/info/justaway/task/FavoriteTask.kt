package info.justaway.task

import android.os.AsyncTask

import de.greenrobot.event.EventBus
import info.justaway.R
import info.justaway.event.action.StatusActionEvent
import info.justaway.model.FavRetweetManager
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil
import twitter4j.TwitterException

class FavoriteTask(private val mStatusId: Long) : AsyncTask<Void, Void, TwitterException>() {

    init {
        /**
         * 先にsetFavしておかないとViewの星が戻ってしまう、
         * 重複エラー以外の理由で失敗し場合（通信エラー等）は戻す
         */
        FavRetweetManager.setFav(mStatusId)
        EventBus.getDefault().post(StatusActionEvent())
    }

    override fun doInBackground(vararg params: Void): TwitterException? {
        return try {
            TwitterManager.getTwitter().createFavorite(mStatusId)
            null
        } catch (e: TwitterException) {
            e
        }
    }

    override fun onPostExecute(e: TwitterException?) {
        when {
            e == null -> MessageUtil.showToast(R.string.toast_favorite_success)
            e.errorCode == 139 -> MessageUtil.showToast(R.string.toast_favorite_already)
            else -> {
                FavRetweetManager.removeFav(mStatusId)
                EventBus.getDefault().post(StatusActionEvent())
                MessageUtil.showToast(R.string.toast_favorite_failure)
            }
        }
    }
}
