package net.slash_omega.juktaway

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.application
import jp.nephy.penicillin.endpoints.account
import jp.nephy.penicillin.endpoints.account.verifyCredentials
import jp.nephy.penicillin.endpoints.oauth
import jp.nephy.penicillin.endpoints.oauth.*
import jp.nephy.penicillin.extensions.await
import net.slash_omega.juktaway.model.UserIconManager
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_signin.*
import kotlinx.coroutines.*
import net.slash_omega.juktaway.twitter.*
import net.slash_omega.juktaway.util.SharedPreference
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast
import kotlin.coroutines.CoroutineContext

/**
 * Created on 2018/08/29.
 */

private const val pinPublishedKey = "published"

private var consumerIdTemp by SharedPreference("twitter", "consumertemp", -1L)
private var rtTemp by SharedPreference("twitter", "rtTemp", "")
private var rtsTemp by SharedPreference("twitter", "rtTemp", "")

class SignInActivity: Activity(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var isPinPublished: Boolean = false
    private var mRequestToken: RequestTokenResponse? = null
    private var consumer: Consumer? = null
    private val addConsumerString by lazy { getString(R.string.add_consumer) }

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

        consumer_spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            addAll(consumerList.map {it.name})
            add(addConsumerString)
        }

        consumer_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val str = (consumer_spinner.getItemAtPosition(position) as String)
                if (str == addConsumerString) {
                    consumer_layout.visibility = View.VISIBLE
                } else if (consumerList.isNotEmpty()) launch {
                    consumer_layout.visibility = View.GONE
                    consumer = Core.getConsumer(str).also {
                        consumerIdTemp = it.id
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        add_consumer_button.onClick {
            launch {
                val consumerName = consumer_name.text.toString()
                if (Core.addConsumer(consumerName, consumer_key.text.toString(), consumer_secret.text.toString())) {
                    consumer = Core.getConsumer(consumerName).also { c ->
                        consumer = c
                        consumerIdTemp = c.id
                        println(c.id)
                    }
                    consumer_spinner.visibility = View.VISIBLE
                    toast(R.string.add_consumer_succeeded)
                    consumer_name.setText("")
                    consumer_key.setText("")
                    consumer_secret.setText("")
                    consumer_layout.visibility = View.GONE
                    (consumer_spinner.adapter as? ArrayAdapter<String>)?.run {
                        insert(consumerName, count - 1)
                        consumer_spinner.setSelection(getPosition(consumerName))
                    }
                } else toast(R.string.add_consumer_failed)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        when {
            isPinPublished -> {
                pin_code.visibility = View.VISIBLE
                consumer_spinner.visibility = View.GONE
                consumer_layout.visibility = View.GONE
            }
            consumerList.isNotEmpty() -> {
                pin_code.visibility = View.GONE
                consumer_spinner.visibility = View.VISIBLE
                consumer_layout.visibility = if (consumer_spinner.selectedItem as? String == addConsumerString) View.VISIBLE else View.GONE
            }
            else -> {
                pin_code.visibility = View.GONE
                consumer_spinner.visibility = View.GONE
                consumer_layout.visibility = View.VISIBLE
            }
        }
    }

    override fun onStop() {
        job.cancelChildren()
        super.onStop()
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
        launch {
            if (consumerIdTemp != -1L) {
                consumer = Core.getConsumer(consumerIdTemp)
            }
        }
    }

    private fun startOAuth() {
        launch {
            clearTemps()
            if (consumer_spinner.selectedItem as? String == addConsumerString) {
                toast(R.string.not_select_consumer)
                return@launch
            }
            MessageUtil.showProgressDialog(this@SignInActivity, getString(R.string.progress_process))
            addUserOAuth()
        }
    }

    private fun verifyOAuth(param: String) {
        launch {
            runCatching {
                PenicillinClient {
                    account {
                        application(consumer!!.ck, consumer!!.cs)
                    }
                }.use { client ->
                    client.oauth.accessToken(rtTemp, rtsTemp, param)
                }
            }.onSuccess { (at, ats, id, sn) ->
                Core.addToken(Identifier(consumer!!.id, at, ats, id, sn))

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

    private fun addUserOAuth() {
        launch {
            runCatching {
                PenicillinClient {
                    account {
                        application(consumer!!.ck, consumer!!.cs)
                    }
                }.use { client ->
                    val (rt, rts) = client.oauth.requestToken()
                    rtTemp = rt
                    rtsTemp = rts
                    client.oauth.authorizeUrl(rt)
                }
            }.onSuccess {
                isPinPublished = true
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.toString())))
            }.onFailure {
                it.printStackTrace()
                toast(R.string.toast_sign_in_failure)
            }
            MessageUtil.dismissProgressDialog()
        }
    }

    private suspend fun clearTemps() {
        withContext(Dispatchers.Default) {
            rtTemp = ""
            rtsTemp = ""
        }
    }
}