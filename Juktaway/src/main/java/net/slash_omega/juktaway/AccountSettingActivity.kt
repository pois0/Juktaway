package net.slash_omega.juktaway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import net.slash_omega.juktaway.adapter.account.IdentifierAdapter
import net.slash_omega.juktaway.fragment.dialog.AccountSwitchDialogFragment
import net.slash_omega.juktaway.listener.RemoveAccountListener
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_account_setting.*
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.Core
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.twitter.identifierList
import org.jetbrains.anko.startActivity

/**
 * Created on 2018/08/23.
 */
class AccountSettingActivity: ScopedAppCompatActivity(), RemoveAccountListener {
    private lateinit var mAccountAdapter: IdentifierAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_account_setting)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        mAccountAdapter = IdentifierAdapter(this, R.layout.row_account) { identifier ->
            AccountSwitchDialogFragment.newInstance(identifier).show(supportFragmentManager, "dialog")
        }.apply {
            identifierList.forEach { add(it) }
        }

        with(list_view) {
            adapter = mAccountAdapter
            setOnItemClickListener { _, _, i, _ ->
                mAccountAdapter.getItem(i).also {
                    if (it?.userId != currentIdentifier.userId) {
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("identifier", it)
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
            R.id.add_account -> startActivity<SignInActivity>("add_account" to true)
            android.R.id.home -> finish()
        }
        return true
    }

    override fun removeIdentifier(identifier: Identifier) {
        mAccountAdapter.remove(identifier)
        launch { Core.removeIdentifier(identifier) }
    }
}
