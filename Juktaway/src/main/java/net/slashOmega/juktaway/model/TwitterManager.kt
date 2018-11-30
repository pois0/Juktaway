package net.slashOmega.juktaway.model

import android.os.Handler
import de.greenrobot.event.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.MyUserStreamAdapter
import net.slashOmega.juktaway.event.action.AccountChangeEvent
import net.slashOmega.juktaway.event.connection.StreamingConnectionEvent
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.SharedPreference
import twitter4j.*
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

/**
 * Twitterインスタンス管理
 */
object TwitterManager {
    private var sUserStreamAdapter: MyUserStreamAdapter? = null
    private var sTwitterStream: TwitterStream? = null
    private var mTwitter: Twitter? = null
    var twitterStreamConnected: Boolean = false
        private set

    internal var consumerKey by SharedPreference("twitter_csck", "cs", "")

    internal var consumerSecret by SharedPreference("twitter_csck", "ck", "")

    // アクセストークンまである時だけキャッシュしておく
    val twitter: Twitter
        get() {
            if (mTwitter != null) {
                return mTwitter!!
            }
            val twitter = twitterInstance

            val token = AccessTokenManager.getAccessToken()
            if (token != null) {
                twitter.oAuthAccessToken = token
                mTwitter = twitter
            }
            return twitter
        }

    val twitterInstance: Twitter
        get() {
            val configurationBuilder = ConfigurationBuilder()
            val conf = configurationBuilder.setOAuthConsumerKey(consumerKey)
                    .setOAuthConsumerSecret(consumerSecret)
                    .setTweetModeExtended(true)
                    .build()
            val factory = TwitterFactory(conf)

            return factory.instance
        }

    private val twitterStream: TwitterStream
        get() {
            val token = AccessTokenManager.getAccessToken()
            val configurationBuilder = ConfigurationBuilder()
            val conf = configurationBuilder.setOAuthConsumerKey(consumerKey)
                    .setOAuthConsumerSecret(consumerSecret)
                    .setOAuthAccessToken(token!!.token)
                    .setOAuthAccessTokenSecret(token.tokenSecret)
                    .setTweetModeExtended(true)
                    .build()
            return TwitterStreamFactory(conf).instance
        }

    fun switchAccessToken(accessToken: AccessToken) {
        AccessTokenManager.setAccessToken(accessToken)
        if (BasicSettings.streamingMode) {
            MessageUtil.showToast(R.string.toast_destroy_streaming)
            sUserStreamAdapter!!.stop()
            GlobalScope.launch(Dispatchers.Main) {
                async(Dispatchers.Default) {
                    sTwitterStream!!.cleanUp()
                    sTwitterStream!!.shutdown()
                }.await()
                sTwitterStream!!.oAuthAccessToken = accessToken
                Handler().postDelayed({
                    MessageUtil.showToast(R.string.toast_create_streaming)
                    sUserStreamAdapter!!.start()
                    sTwitterStream!!.user()
                }, 5000)
            }
        }
        EventBus.getDefault().post(AccountChangeEvent())
    }

    fun startStreaming() {
        if (sTwitterStream != null) {
            if (!twitterStreamConnected) {
                sUserStreamAdapter!!.start()
                sTwitterStream!!.oAuthAccessToken = AccessTokenManager.getAccessToken()
                sTwitterStream!!.user()
            }
            return
        }
        sTwitterStream = twitterStream
        sUserStreamAdapter = MyUserStreamAdapter()
        sTwitterStream!!.addListener(sUserStreamAdapter)
        sTwitterStream!!.addConnectionLifeCycleListener(MyConnectionLifeCycleListener())
        sTwitterStream!!.user()
        // BasicSettings.INSTANCE.resetNotification();
    }

    fun stopStreaming() {
        if (sTwitterStream == null) {
            return
        }
        BasicSettings.streamingMode = false
        sUserStreamAdapter!!.stop()
        GlobalScope.launch(Dispatchers.Default) {
            sTwitterStream!!.cleanUp()
            sTwitterStream!!.shutdown()
        }
    }

    fun pauseStreaming() {
        if (sUserStreamAdapter != null) {
            sUserStreamAdapter!!.pause()
        }
    }

    fun resumeStreaming() {
        if (sUserStreamAdapter != null) {
            sUserStreamAdapter!!.resume()
        }
    }

    class MyConnectionLifeCycleListener : ConnectionLifeCycleListener {
        override fun onConnect() {
            twitterStreamConnected = true
            EventBus.getDefault().post(StreamingConnectionEvent.onCleanUp())
        }

        override fun onDisconnect() {
            twitterStreamConnected = false
            EventBus.getDefault().post(StreamingConnectionEvent.onDisconnect())
        }

        override fun onCleanUp() {
            twitterStreamConnected = false
            EventBus.getDefault().post(StreamingConnectionEvent.onCleanUp())
        }
    }
}
