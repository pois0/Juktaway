package net.slashOmega.juktaway.model;

import android.os.AsyncTask;
import android.os.Handler;

import de.greenrobot.event.EventBus;
import net.slashOmega.juktaway.JuktawayApplication;
import net.slashOmega.juktaway.R;
import net.slashOmega.juktaway.adapter.MyUserStreamAdapter;
import net.slashOmega.juktaway.event.action.AccountChangeEvent;
import net.slashOmega.juktaway.event.connection.StreamingConnectionEvent;
import net.slashOmega.juktaway.settings.BasicSettings;
import net.slashOmega.juktaway.util.MessageUtil;
import twitter4j.ConnectionLifeCycleListener;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Twitterインスタンス管理
 */
public class TwitterManager {
    private static MyUserStreamAdapter sUserStreamAdapter;
    private static TwitterStream sTwitterStream;
    private static Twitter mTwitter;
    private static boolean sTwitterStreamConnected;

    public static void switchAccessToken(final AccessToken accessToken) {
        AccessTokenManager.setAccessToken(accessToken);
        if (BasicSettings.INSTANCE.getStreamingMode()) {
            MessageUtil.INSTANCE.showToast(R.string.toast_destroy_streaming);
            sUserStreamAdapter.stop();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    sTwitterStream.cleanUp();
                    sTwitterStream.shutdown();
                    return null;
                }

                @Override
                protected void onPostExecute(Void status) {
                    sTwitterStream.setOAuthAccessToken(accessToken);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MessageUtil.INSTANCE.showToast(R.string.toast_create_streaming);
                            sUserStreamAdapter.start();
                            sTwitterStream.user();
                        }
                    }, 5000);
                }
            }.execute();
        }
        EventBus.getDefault().post(new AccountChangeEvent());
    }

    private static String getConsumerKey() {
        return JuktawayApplication.app.getString(R.string.twitter_consumer_key);
    }

    private static String getConsumerSecret() {
        return JuktawayApplication.app.getString(R.string.twitter_consumer_secret);
    }

    public static Twitter getTwitter() {
        if (mTwitter != null) {
            return mTwitter;
        }
        Twitter twitter = getTwitterInstance();

        AccessToken token = AccessTokenManager.getAccessToken();
        if (token != null) {
            twitter.setOAuthAccessToken(token);
            // アクセストークンまである時だけキャッシュしておく
            mTwitter = twitter;
        }
        return twitter;
    }

    public static Twitter getTwitterInstance() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        twitter4j.conf.Configuration conf = configurationBuilder.setOAuthConsumerKey(getConsumerKey())
                .setOAuthConsumerSecret(getConsumerSecret())
                .setTweetModeExtended(true)
                .build();
        TwitterFactory factory = new TwitterFactory(conf);
        Twitter twitter = factory.getInstance();

        return twitter;
    }

    public static TwitterStream getTwitterStream() {
        AccessToken token = AccessTokenManager.getAccessToken();
        if (token == null) {
            return null;
        }
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        twitter4j.conf.Configuration conf = configurationBuilder.setOAuthConsumerKey(getConsumerKey())
                .setOAuthConsumerSecret(getConsumerSecret())
                .setOAuthAccessToken(token.getToken())
                .setOAuthAccessTokenSecret(token.getTokenSecret())
                .setTweetModeExtended(true)
                .build();
        return new TwitterStreamFactory(conf).getInstance();
    }

    public static boolean getTwitterStreamConnected() {
        return sTwitterStreamConnected;
    }

    public static void startStreaming() {
        if (sTwitterStream != null) {
            if (!sTwitterStreamConnected) {
                sUserStreamAdapter.start();
                sTwitterStream.setOAuthAccessToken(AccessTokenManager.getAccessToken());
                sTwitterStream.user();
            }
            return;
        }
        sTwitterStream = getTwitterStream();
        sUserStreamAdapter = new MyUserStreamAdapter();
        sTwitterStream.addListener(sUserStreamAdapter);
        sTwitterStream.addConnectionLifeCycleListener(new MyConnectionLifeCycleListener());
        sTwitterStream.user();
        // BasicSettings.INSTANCE.resetNotification();
    }

    public static void stopStreaming() {
        if (sTwitterStream == null) {
            return;
        }
        BasicSettings.INSTANCE.setStreamingMode(false);
        sUserStreamAdapter.stop();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                sTwitterStream.cleanUp();
                sTwitterStream.shutdown();
                return null;
            }

            @Override
            protected void onPostExecute(Void status) {

            }
        }.execute();
    }

    public static void pauseStreaming() {
        if (sUserStreamAdapter != null) {
            sUserStreamAdapter.pause();
        }
    }

    public static void resumeStreaming() {
        if (sUserStreamAdapter != null) {
            sUserStreamAdapter.resume();
        }
    }

    public static class MyConnectionLifeCycleListener implements ConnectionLifeCycleListener {
        @Override
        public void onConnect() {
            sTwitterStreamConnected = true;
            EventBus.getDefault().post(StreamingConnectionEvent.Companion.onCleanUp());
        }

        @Override
        public void onDisconnect() {
            sTwitterStreamConnected = false;
            EventBus.getDefault().post(StreamingConnectionEvent.Companion.onDisconnect());
        }

        @Override
        public void onCleanUp() {
            sTwitterStreamConnected = false;
            EventBus.getDefault().post(StreamingConnectionEvent.Companion.onCleanUp());
        }
    }
}
