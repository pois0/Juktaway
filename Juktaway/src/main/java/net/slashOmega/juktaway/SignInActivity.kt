package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.PenicillinClient
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_signin.*
import kotlinx.coroutines.*
import net.slashOmega.juktaway.twitter.*
import net.slashOmega.juktaway.util.takeNotEmpty
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

        if(intent.getBooleanExtra("add_account", false)) {
            consumer_key.visibility = View.GONE
            consumer_secret.visibility = View.GONE
            start_oauth_button.visibility = View.GONE
            connect_with_twitter.visibility = View.GONE
            startOAuth(true)
            return
        }

        if (savedInstanceState?.get(stateRequestToken) != null) {
            intent.data?.getQueryParameter("oauth_verifier").takeNotEmpty()?.let {
                start_oauth_button.visibility = View.GONE
                connect_with_twitter.visibility = View.GONE
                MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
                verifyOAuth(it)
            }
        }

        start_oauth_button.setOnClickListener { startOAuth() }
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
                || !intent.data!!.toString().startsWith(getString(R.string.twitter_callback_url))) {
            finish()
            return
        }
        val oauthVerifier = intent.data!!.getQueryParameter("oauth_verifier")
        if (oauthVerifier.isNullOrEmpty()) return
        MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
        verifyOAuth(oauthVerifier)
    }

    private fun startOAuth(addUser: Boolean = false) {
        GlobalScope.launch(Dispatchers.Main) {
            launch(Dispatchers.Default) { AuthTemp.clearTemps() }
            if (!addUser) {
                if (consumer_key.text.isBlank() || consumer_secret.text.isBlank()) {
                    toast(R.string.signin_csck_blank)
                    return@launch
                }
            }
            MessageUtil.showProgressDialog(this@SignInActivity, getString(R.string.progress_process))
            addUserOAuth(consumer_key.text.toString(), consumer_secret.text.toString())
        }
    }

    private fun verifyOAuth(param: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val (at, ats, id, sn) = runCatching {
                withContext(Dispatchers.Default) {
                    PenicillinClient {
                        account {
                            application(ckTemp, csTemp)
                        }
                    }.use { client ->
                        client.oauth.accessToken(rtTemp, rtsTemp, param)
                    }
                }
            }.getOrNull() ?: return@launch
            Core.addToken(Core.Identifier(ckTemp, csTemp, at, ats, id, sn))

            MessageUtil.dismissProgressDialog()
            toast(R.string.toast_sign_in_success)
            withClient { UserIconManager.addUserIconMap(account.verifyCredentials().await().result) }
            startActivity(intentFor<MainActivity>().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun addUserOAuth(cs: String, ck: String) {
        GlobalScope.launch(Dispatchers.Main) {
            csTemp = cs
            ckTemp = ck
            val url = withContext(Dispatchers.Default) {
                PenicillinClient {
                    account {
                        application(cs, ck)
                    }
                }.use { client ->
                    val (rt, rts) = client.oauth.requestToken(getString(R.string.twitter_callback_url))
                    rtTemp = rt
                    rtsTemp = rts
                    client.oauth.authorizeUrl(rt)
                }
            }
            MessageUtil.dismissProgressDialog()

            consumer_key.visibility = View.GONE
            consumer_secret.visibility = View.GONE
            start_oauth_button.visibility = View.GONE
            connect_with_twitter.visibility = View.GONE
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}