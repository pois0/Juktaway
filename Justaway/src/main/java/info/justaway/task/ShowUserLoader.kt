package info.justaway.task

import android.content.Context

import info.justaway.model.AccessTokenManager
import info.justaway.model.Profile
import info.justaway.model.TwitterManager
import twitter4j.Relationship
import twitter4j.TwitterException
import twitter4j.User

class ShowUserLoader : AbstractAsyncTaskLoader<Profile> {

    private var mScreenName: String? = ""
    private var mUserId: Long = 0

    constructor(context: Context, screenName: String) : super(context) {
        this.mScreenName = screenName
    }

    constructor(context: Context, userId: Long) : super(context) {
        this.mUserId = userId
    }

    override fun loadInBackground(): Profile {
        var args = ""
        return try {
            val twitter = TwitterManager.getTwitter()
            val mUser: User
            val mRelationship: Relationship
            if (mScreenName != null) {
                args = "name:$mScreenName"
                mUser = twitter.showUser(mScreenName)
                mRelationship = twitter.showFriendship(AccessTokenManager.getUserId(), mUser.id)
            } else {
                args = "id:$mUserId"
                mUser = twitter.showUser(mUserId)
                mRelationship = twitter.showFriendship(AccessTokenManager.getUserId(), mUserId)
            }
            Profile().apply {
                relationship = mRelationship
                user = mUser
            }
        } catch (e: TwitterException) {
            e.printStackTrace()
            Profile().apply { error = "(args:" + args + " code:" + e.errorCode + ")" }
        }

    }
}