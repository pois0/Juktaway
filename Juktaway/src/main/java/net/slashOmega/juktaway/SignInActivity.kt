package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_signin.*
import kotlinx.coroutines.*
import net.slashOmega.juktaway.util.takeNotEmpty
import net.slashOmega.juktaway.util.tryAndTraceGet
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import twitter4j.auth.RequestToken

/**
 * Created on 2018/08/29.
 */
class SignInActivity: Activity() {
    private val stateRequestToken = "request_token"
    private var mRequestToken: RequestToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_signin)

        pin_code.visibility = View.GONE

        if (intent.getBooleanExtra("add_account", false)) {
            consumer_key.setText(TwitterManager.consumerKey)
            consumer_key.setText(TwitterManager.consumerSecret)
            consumer_key.visibility = View.GONE
            consumer_secret.visibility = View.GONE
            startOAuth(true)
            return
        }

        start_oauth_button.setOnClickListener {
            if (pin_code.text.isNotBlank()) {
                MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
                verifyOAuth(pin_code.text.toString())
            } else {
                startOAuth()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        mRequestToken?.let {
            outState.putSerializable(stateRequestToken, it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mRequestToken = savedInstanceState.getSerializable(stateRequestToken) as RequestToken
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent == null || intent.data == null
                || !intent.data!!.toString().startsWith(getString(R.string.twitter_callback_url))) return

        val oauthVerifier = intent.data!!.getQueryParameter("oauth_verifier")
        if (oauthVerifier.isNullOrEmpty()) return
        MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
        verifyOAuth(oauthVerifier)
    }

    private fun successOAuth() {
        MessageUtil.showToast(R.string.toast_sign_in_success)
        startActivity(intentFor<MainActivity>().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun startOAuth(addUser: Boolean = false) {
        if (!addUser) {
            if (consumer_key.text.isBlank() || consumer_secret.text.isBlank()) {
                toast(R.string.signin_csck_blank)
                return
            }
            TwitterManager.consumerKey = consumer_key.text.toString()
            TwitterManager.consumerSecret = consumer_secret.text.toString()
        }
        MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
        addUserOAuth()
    }

    private fun verifyOAuth(param: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val user = withContext(Dispatchers.Default) {
                tryAndTraceGet {
                    TwitterManager.twitterInstance.apply {
                        val token = getOAuthAccessToken(mRequestToken, param)
                        AccessTokenManager.setAccessToken(token)
                        oAuthAccessToken = token
                    }.verifyCredentials()
                }
            }

            MessageUtil.dismissProgressDialog()
            user?.let {
                UserIconManager.addUserIconMap(it)
                successOAuth()
            }
        }
    }

    private fun addUserOAuth() {
        GlobalScope.launch(Dispatchers.Main) {
            val token = withContext(Dispatchers.Default) {
                tryAndTraceGet {
                    TwitterManager.twitterInstance.getOAuthRequestToken("oob")
                }
            }
            MessageUtil.dismissProgressDialog()
            if (token == null) {
                MessageUtil.showToast(R.string.toast_connection_failure)
                return@launch
            }
            val url = token.authorizationURL ?: run {
                MessageUtil.showToast(R.string.toast_get_authorization_url_failure)
                return@launch
            }

            mRequestToken = token
            consumer_key.visibility = View.GONE
            consumer_secret.visibility = View.GONE
            pin_code.visibility = View.VISIBLE
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}