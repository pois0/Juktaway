package net.slashOmega.juktaway.task

import android.content.Context

import java.util.ArrayList

import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.twitter.currentIdentifier
import twitter4j.ResponseList
import twitter4j.TwitterException
import twitter4j.UserList

class RegisterUserListsLoader(context: Context, private val mUserId: Long) : AbstractAsyncTaskLoader<ArrayList<ResponseList<UserList>>>(context) {

    override fun loadInBackground(): ArrayList<ResponseList<UserList>>? {
        return try {
            arrayListOf(TwitterManager.twitter.getUserListsOwnerships(currentIdentifier.userId, 200, -1),
                    TwitterManager.twitter.getUserListMemberships(mUserId, -1, true))
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }
    }
}
