package info.justaway.task

import android.os.AsyncTask

import info.justaway.model.AccessTokenManager
import info.justaway.model.TwitterManager
import info.justaway.model.UserListCache
import twitter4j.ResponseList
import twitter4j.UserList

/**
 * アプリケーションのメンバ変数にユーザーリストを読み込む
 */
class LoadUserListsTask : AsyncTask<Void, Void, ResponseList<UserList>>() {
    override fun doInBackground(vararg params: Void): ResponseList<UserList>? {
        return try {
            TwitterManager.getTwitter().getUserLists(AccessTokenManager.getUserId())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(userLists: ResponseList<UserList>?) {
        if (userLists != null) {
            UserListCache.userLists = userLists
        }
    }
}
