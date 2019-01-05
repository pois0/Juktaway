package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.models.special.RequestTokenResponse
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_signin.*
import kotlinx.coroutines.*
import net.slashOmega.juktaway.twitter.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast

/**
 * Created on 2018/08/29.
 */
class SignInActivity: Activity() {
    private val pinPublishedKey = "published"
    private var isPinPublished: Boolean = false
    private var mRequestToken: RequestTokenResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_signin)

        start_oauth_button.setOnClickListener {
            if (pin_code.text.isNotBlank()) {
                MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
                verifyOAuth(pin_code.text.toString())
            } else {
                startOAuth()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPinPublished) {
            pin_code.visibility = View.VISIBLE
            consumer_key.visibility = View.GONE
            consumer_secret.visibility = View.GONE
        } else {
            pin_code.visibility = View.GONE
            consumer_key.visibility = View.VISIBLE
            consumer_secret.visibility = View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        mRequestToken?.let {
            outState.putBoolean(pinPublishedKey, isPinPublished)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        isPinPublished = savedInstanceState.getBoolean(pinPublishedKey)
    }

    private fun startOAuth(addUser: Boolean = false) {
        GlobalScope.launch(Dispatchers.Main) {
            AuthTemp.clearTemps()
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
            runCatching {
                PenicillinClient {
                    account {
                        application(ckTemp, csTemp)
                    }
                }.use { client ->
                    client.oauth.accessToken(rtTemp, rtsTemp, param)
                }
            }.onSuccess { (at, ats, id, sn) ->
                Core.addToken(Identifier(ckTemp, csTemp, at, ats, id, sn))

                MessageUtil.dismissProgressDialog()
                toast(R.string.toast_sign_in_success)
                UserIconManager.addUserIconMap(currentClient.account.verifyCredentials().await().result)
                startActivity(intentFor<MainActivity>().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }.onFailure {
                it.printStackTrace()
                MessageUtil.dismissProgressDialog()
                toast(R.string.toast_sign_in_failure)
            }
        }
    }

    private fun addUserOAuth(ck: String, cs: String) {
        GlobalScope.launch(Dispatchers.Main) {
            csTemp = cs
            ckTemp = ck
            val url = PenicillinClient {
                    account {
                        application(ck, cs)
                    }
                }.use { client ->
                    val (rt, rts) = client.oauth.requestToken()
                    rtTemp = rt
                    rtsTemp = rts
                    client.oauth.authorizeUrl(rt)
                }
            MessageUtil.dismissProgressDialog()

            isPinPublished = true
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}