package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.Menu
import android.view.MenuItem
import net.slashOmega.juktaway.adapter.account.AccessTokenAdapter
import net.slashOmega.juktaway.fragment.dialog.AccountSwitchDialogFragment
import net.slashOmega.juktaway.listener.OnTrashListener
import net.slashOmega.juktaway.listener.RemoveAccountListener
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_account_setting.*
import twitter4j.auth.AccessToken

/**
 * Created on 2018/08/23.
 */
class AccountSettingActivity: FragmentActivity(), RemoveAccountListener {
    private lateinit var mAccountAdapter: AccessTokenAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_account_setting)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        mAccountAdapter = AccessTokenAdapter(this, R.layout.row_account) .apply {
            AccessTokenManager.getAccessTokens()?.forEach { add(it) }
            setOnTrashListener(object: OnTrashListener {
                override fun onTrash(position: Int) {
                    AccountSwitchDialogFragment.newInstance(getItem(position)).show(supportFragmentManager, "dialog")
                }
            })
        }

        with (list_view) {
            adapter = mAccountAdapter
            setOnItemClickListener { _, _, i, _ ->
                mAccountAdapter.getItem(i).also {
                    if (it.userId != AccessTokenManager.getUserId()) {
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("accessToken", it)
                        })
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.account_setting, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.add_account ->
                startActivity(Intent(this, SignInActivity::class.java).apply {
                    putExtra("add_account", true)
                })
            R.id.home ->
                finish()
        }
        return true
    }

    override fun removeAccount(accessToken: AccessToken) {
        mAccountAdapter.remove(accessToken)
        AccessTokenManager.removeAccessToken(accessToken)
    }
}