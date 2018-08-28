package info.justaway.task

import android.content.Context

import info.justaway.model.AccessTokenManager
import info.justaway.model.TwitterManager
import twitter4j.ResponseList
import twitter4j.TwitterException
import twitter4j.UserList

class UserListsLoader(context: Context) : AbstractAsyncTaskLoader<ResponseList<UserList>>(context) {

    override fun loadInBackground(): ResponseList<UserList>? {
        return try {
            TwitterManager.getTwitter().getUserLists(AccessTokenManager.getUserId())
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}
