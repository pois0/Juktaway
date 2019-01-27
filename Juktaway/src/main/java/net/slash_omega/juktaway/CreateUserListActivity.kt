package net.slash_omega.juktaway

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import io.ktor.http.HttpStatusCode
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.parameters.ListCreationMode
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_create_user_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.currentClient
import org.jetbrains.anko.toast

private val ERROR_CODE_NAME_BLANK = HttpStatusCode.Forbidden

class CreateUserListActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_create_user_list)

        actionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        save.setOnClickListener {
            MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
            GlobalScope.launch(Dispatchers.Main) {
                val listName = list_name.text.toString()
                val privacy = if (privacy_radio_group.checkedRadioButtonId == R.id.public_radio) ListCreationMode.Public
                        else ListCreationMode.Private
                val description = list_description.text.toString()
                runCatching {
                    currentClient.lists.create(listName, privacy, description).await()
                }.onSuccess {
                    toast(R.string.toast_create_user_list_success)
                    finish()
                }.onFailure { e ->
                    if (e is PenicillinException) {
                        toast(if (e.response?.status == ERROR_CODE_NAME_BLANK) R.string.toast_create_user_list_failure_name_blank
                            else R.string.toast_create_user_list_failure)
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?) = item?.run {
            if (itemId == R.id.home) finish()
            true
        } ?: false
}