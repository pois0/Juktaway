package net.slashOmega.juktaway.task

import android.content.Context

import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.TwitterException

class InteractionsLoader(context: Context) : AbstractAsyncTaskLoader<ResponseList<Status>>(context) {

    override fun loadInBackground(): ResponseList<Status>? {
        return try {
            TwitterManager.twitter.mentionsTimeline
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}
