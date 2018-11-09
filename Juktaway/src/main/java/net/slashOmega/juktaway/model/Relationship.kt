package net.slashOmega.juktaway.model

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterException

object Relationship {
    private val blockList = mutableListOf<Long>()
    private val officialMuteList = mutableListOf<Long>()
    private val noRetweetList = mutableListOf<Long>()
    private val myIdList = mutableListOf<Long>()

    fun init() {
        val accessTokens = AccessTokenManager.getAccessTokens()
        if (accessTokens.isEmpty()) return
        for (accessToken in accessTokens) {
            val twitter = TwitterManager.getTwitterInstance()
            twitter.oAuthAccessToken = accessToken
            myIdList.add(accessToken.userId)
            loadBlock(twitter)
            loadOfficialMute(twitter)
            loadNoRetweet(twitter)
        }
    }

    fun isMe(userId: Long): Boolean = userId in myIdList

    fun isBlock(userId: Long): Boolean = userId in blockList

    fun isOfficialMute(userId: Long): Boolean = userId in officialMuteList

    fun isNoRetweet(userId: Long): Boolean = userId in noRetweetList


    fun setBlock(userId: Long) { blockList.add(userId) }

    fun setOfficialMute(userId: Long) { officialMuteList.add(userId) }

    fun setNoRetweet(userId: Long) { noRetweetList.add(userId) }

    fun removeBlock(userId: Long) { blockList.remove(userId) }

    fun removeOfficialMute(userId: Long) { officialMuteList.remove(userId) }

    fun removeNoRetweet(userId: Long) { noRetweetList.remove(userId) }

    fun loadBlock(twitter: Twitter) {
        GlobalScope.launch {
            try {
                twitter.blocksIDs?.iDs?.let {
                    blockList.addAll(it.toList())
                }
            } catch (e: TwitterException) {
                e.printStackTrace()
            }
        }
    }

    fun loadOfficialMute(twitter: Twitter) {
        GlobalScope.launch {
            try {
                twitter.getMutesIDs(-1L)?.iDs?.let {
                    officialMuteList.addAll(it.toList())
                }
            } catch (e: TwitterException) {
                e.printStackTrace()
            }
        }
    }

    fun loadNoRetweet(twitter: Twitter) {
        GlobalScope.launch {
            try {
                twitter.noRetweetsFriendships?.iDs?.let {
                    noRetweetList.addAll(it.toList())
                }
            } catch (e: TwitterException) {
                e.printStackTrace()
            }
        }
    }
    private fun isThatStatusVisible(id: Long): Boolean = isMe(id) || !(isBlock(id) || isOfficialMute(id))

    fun isVisible(status: Status) = isThatStatusVisible(status.user.id)
            && status.retweetedStatus?.let { isThatStatusVisible(status.user.id) && !isNoRetweet(status.user.id) } ?: true
            && status.quotedStatus?.let { isThatStatusVisible(status.user.id) } ?: true
}
