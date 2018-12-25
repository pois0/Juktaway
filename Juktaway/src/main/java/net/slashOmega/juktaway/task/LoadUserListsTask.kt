package net.slashOmega.juktaway.task

import android.os.AsyncTask

import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserListCache
import net.slashOmega.juktaway.twitter.currentIdentifier
import twitter4j.ResponseList
import twitter4j.UserList

/**
 * アプリケーションのメンバ変数にユーザーリストを読み込む
 */
class LoadUserListsTask : AsyncTask<Void, Void, ResponseList<UserList>>() {
    override fun doInBackground(vararg params: Void): ResponseList<UserList>? {
        return try {
            TwitterManager.twitter.getUserLists(currentIdentifier.userId)
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
