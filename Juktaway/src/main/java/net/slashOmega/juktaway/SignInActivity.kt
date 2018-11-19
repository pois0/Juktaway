package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_signin.*
import twitter4j.TwitterException
import twitter4j.User
import twitter4j.auth.RequestToken
import java.lang.ref.WeakReference

/**
 * Created on 2018/08/29.
 */
class SignInActivity: Activity() {
    companion object {
        private class AddUserOAuthTask(activity: SignInActivity): AsyncTask<Void, Void, RequestToken>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg params: Void): RequestToken? {
                return try {
                    val twitter = TwitterManager.getTwitterInstance()
                    ref.get()?.run {
                        twitter.getOAuthRequestToken(getString(R.string.twitter_callback_url))
                    }
                } catch (e: TwitterException) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(token: RequestToken?) {
                MessageUtil.dismissProgressDialog()
                if (token == null) {
                    MessageUtil.showToast(R.string.toast_connection_failure)
                    return
                }
                val url = token.authorizationURL
                if (url == null) {
                    MessageUtil.showToast(R.string.toast_get_authorization_url_failure)
                    return
                }
                ref.get()?.run {
                    mRequestToken = token
                    start_oauth_button.visibility = View.GONE
                    connect_with_twitter.visibility = View.GONE
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            }
        }

        private class VerifyOAuthTask(activity: SignInActivity): AsyncTask<String, Void, User>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg params: String): User? {
                return ref.get()?.run {
                    try {
                        val twitter = TwitterManager.getTwitterInstance()
                        val accessToken = twitter.getOAuthAccessToken(mRequestToken, params[0])
                        AccessTokenManager.setAccessToken(accessToken)
                        twitter.oAuthAccessToken = accessToken
                        twitter.verifyCredentials()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }

            override fun onPostExecute(user: User?) {
                MessageUtil.dismissProgressDialog()
                user?.let {
                    Log.d("OAuth", "Success Logging in")
                    UserIconManager.addUserIconMap(it)
                    ref.get()?.successOAuth()
                }
            }
        }

    }

    private val STATE_REQUEST_TOKEN = "request_token"
    private var mRequestToken: RequestToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_signin)

        if(intent.getBooleanExtra("add_account", false)) {
            start_oauth_button.visibility = View.GONE
            connect_with_twitter.visibility = View.GONE
            startOAuth()
            return
        }

        savedInstanceState?.let { state ->
            state.get(STATE_REQUEST_TOKEN)?.let { _ -> intent.data?.let { data ->
                data.getQueryParameter("oauth_verifier")?.takeUnless { it.isEmpty() }?.let {
                    start_oauth_button.visibility = View.GONE
                    connect_with_twitter.visibility = View.GONE
                    MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
                    VerifyOAuthTask(this).execute(it)
                }
            }}
        }

        start_oauth_button.setOnClickListener { startOAuth() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        mRequestToken?.let {
            outState.putSerializable(STATE_REQUEST_TOKEN, it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mRequestToken = savedInstanceState.getSerializable(STATE_REQUEST_TOKEN) as RequestToken
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent == null || intent.data == null
                || !intent.data!!.toString().startsWith(getString(R.string.twitter_callback_url))) return

        val oauthVerifier = intent.data!!.getQueryParameter("oauth_verifier")
        if (oauthVerifier.isNullOrEmpty()) return
        MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
        VerifyOAuthTask(this).execute(oauthVerifier)
    }

    private fun successOAuth() {
        MessageUtil.showToast(R.string.toast_sign_in_success)
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun startOAuth() {
        MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
        AddUserOAuthTask(this).execute()
    }
}