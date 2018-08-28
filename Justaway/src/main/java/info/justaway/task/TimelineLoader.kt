package info.justaway.task

import android.content.Context

import info.justaway.model.TwitterManager
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.TwitterException

class TimelineLoader(context: Context) : AbstractAsyncTaskLoader<ResponseList<Status>>(context) {

    override fun loadInBackground(): ResponseList<Status>? {
        return try {
            TwitterManager.getTwitter().homeTimeline
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}
