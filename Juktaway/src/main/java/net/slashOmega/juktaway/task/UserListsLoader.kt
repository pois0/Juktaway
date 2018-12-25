package net.slashOmega.juktaway.task

import android.content.Context

import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.twitter.currentIdentifier
import twitter4j.ResponseList
import twitter4j.TwitterException
import twitter4j.UserList

class UserListsLoader(context: Context) : AbstractAsyncTaskLoader<ResponseList<UserList>>(context) {

    override fun loadInBackground(): ResponseList<UserList>? {
        return try {
            TwitterManager.twitter.getUserLists(currentIdentifier.userId)
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}
