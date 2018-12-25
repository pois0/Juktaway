package net.slashOmega.juktaway.task

import android.content.Context
import jp.nephy.penicillin.models.User

import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.twitter.currentClient
import twitter4j.TwitterException

class VerifyCredentialsLoader(context: Context) : AbstractAsyncTaskLoader<User>(context) {

    override fun loadInBackground(): User? {
        return try {
            currentClient.account.verifyCredentials()
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}