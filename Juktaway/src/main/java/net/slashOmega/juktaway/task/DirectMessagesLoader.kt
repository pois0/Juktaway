package net.slashOmega.juktaway.task

import android.content.Context

import java.util.Collections
import java.util.Comparator

import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.DirectMessage
import twitter4j.ResponseList
import twitter4j.Twitter
import twitter4j.TwitterException

class DirectMessagesLoader(context: Context) : AbstractAsyncTaskLoader<ResponseList<DirectMessage>>(context) {

    override fun loadInBackground(): ResponseList<DirectMessage>? {
        return try {
            TwitterManager.getTwitter()?.let{ it.directMessages.apply {
                // 送信したDM
                addAll(it.sentDirectMessages)
                sortWith(Comparator { arg0, arg1 -> arg1.createdAt.compareTo(arg0.createdAt) })
            }}
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}
