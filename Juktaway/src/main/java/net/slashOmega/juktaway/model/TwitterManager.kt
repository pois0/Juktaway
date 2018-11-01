//package net.slashOmega.juktaway.model
//
//import android.os.Handler
//import de.greenrobot.event.EventBus
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import net.slashOmega.juktaway.JuktawayApplication
//import net.slashOmega.juktaway.R
//import net.slashOmega.juktaway.adapter.MyUserStreamAdapter
//import net.slashOmega.juktaway.event.action.AccountChangeEvent
//import net.slashOmega.juktaway.event.connection.StreamingConnectionEvent
//import net.slashOmega.juktaway.settings.BasicSettings
//import net.slashOmega.juktaway.util.MessageUtil
//import twitter4j.*
//import twitter4j.auth.AccessToken
//import twitter4j.conf.ConfigurationBuilder
//
//object TwitterManager {
//    private val consumerKey: String by lazy { JuktawayApplication.app.getString(R.string.twitter_consumer_secret) }
//    private val consumerSecret: String by lazy { JuktawayApplication.app.getString(R.string.twitter_consumer_key) }
//
//    private var twitter: Twitter? = null
//    private var sTwitterStream: TwitterStream? = null
//    private var sUserStreamAdapter: MyUserStreamAdapter? = null
//    var twitterStreamConnected: Boolean = false
//
//    fun switchAccessToken(token: AccessToken) {
//        AccessTokenManager.setAccessToken(token)
//        if (BasicSettings.streamingMode) {
//            MessageUtil.showToast(R.string.toast_destroy_streaming)
//            sUserStreamAdapter?.stop()
//            GlobalScope.launch {
//                sTwitterStream?.cleanUp()
//                sTwitterStream?.shutdown()
//            }
//            sTwitterStream?.oAuthAccessToken = token
//            Handler().postDelayed({
//                MessageUtil.showToast(R.string.toast_create_streaming)
//                sUserStreamAdapter?.start()
//                sTwitterStream?.user()
//            }, 5000)
//        }
//        EventBus.getDefault().post(AccountChangeEvent())
//
//    }
//
//    fun getTwitterInstance(): Twitter {
//        val conf = ConfigurationBuilder().setOAuthConsumerKey(consumerKey)
//                .setOAuthConsumerSecret(consumerSecret)
//                .setTweetModeExtended(true)
//                .build()
//        return TwitterFactory(conf).instance
//    }
//
//    fun getTwitter(): Twitter = twitter ?: AccessTokenManager.getAccessToken()?.let {
//        twitter = getTwitterInstance().apply {
//            oAuthAccessToken = it
//        }
//        twitter
//    }!!
//
//    fun getTwitterStream(): TwitterStream {
//        val token = AccessTokenManager.getAccessToken()!!
//        val conf = ConfigurationBuilder().setOAuthConsumerKey(consumerKey)
//                .setOAuthConsumerSecret(consumerSecret)
//                .setOAuthAccessToken(token.token)
//                .setOAuthAccessTokenSecret(token.tokenSecret)
//                .setTweetModeExtended(true)
//                .build()
//        return TwitterStreamFactory(conf).instance
//    }
//
//    fun startStreaming() {
//        if (sTwitterStream != null) {
//            if (!twitterStreamConnected) {
//                sUserStreamAdapter?.start()
//                sTwitterStream?.oAuthAccessToken = AccessTokenManager.getAccessToken()
//                sTwitterStream?.user()
//            }
//            return
//        }
//        sTwitterStream = getTwitterStream()
//        sUserStreamAdapter = MyUserStreamAdapter()
//        sTwitterStream?.addListener(sUserStreamAdapter)
//        sTwitterStream?.addConnectionLifeCycleListener(MyConnectionLifeCycleListener())
//        sTwitterStream?.user()
//        // BasicSettings.INSTANCE.resetNotification();
//    }
//
//    fun stopStreaming() {
//        BasicSettings.streamingMode = false
//        sUserStreamAdapter?.stop()
//        GlobalScope.launch {
//            sTwitterStream?.cleanUp()
//            sTwitterStream?.shutdown()
//        }
//    }
//
//    fun pauseStreaming() { sUserStreamAdapter?.pause() }
//
//    fun resumeStreaming() { sUserStreamAdapter?.resume() }
//
//    class MyConnectionLifeCycleListener : ConnectionLifeCycleListener {
//        override fun onConnect() {
//            twitterStreamConnected = true
//            EventBus.getDefault().post(StreamingConnectionEvent.onCleanUp())
//        }
//
//        override fun onDisconnect() {
//            twitterStreamConnected = false
//            EventBus.getDefault().post(StreamingConnectionEvent.onDisconnect())
//        }
//
//        override fun onCleanUp() {
//            twitterStreamConnected = false
//            EventBus.getDefault().post(StreamingConnectionEvent.onCleanUp())
//        }
//    }
//}