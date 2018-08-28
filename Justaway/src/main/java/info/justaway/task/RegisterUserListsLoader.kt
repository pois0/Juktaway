package info.justaway.task

import android.content.Context

import java.util.ArrayList

import info.justaway.model.AccessTokenManager
import info.justaway.model.TwitterManager
import twitter4j.ResponseList
import twitter4j.TwitterException
import twitter4j.UserList

class RegisterUserListsLoader(context: Context, private val mUserId: Long) : AbstractAsyncTaskLoader<ArrayList<ResponseList<UserList>>>(context) {

    override fun loadInBackground(): ArrayList<ResponseList<UserList>>? {
        return try {
            arrayListOf(TwitterManager.getTwitter().getUserListsOwnerships(AccessTokenManager.getUserId(), 200, -1),
                    TwitterManager.getTwitter().getUserListMemberships(mUserId, -1, true))
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }
    }
}
