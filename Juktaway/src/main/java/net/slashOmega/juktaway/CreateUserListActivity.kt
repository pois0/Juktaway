package net.slashOmega.juktaway

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_create_user_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.TwitterException
import java.lang.ref.WeakReference

class CreateUserListActivity: Activity() {
    companion object {
        private const val ERROR_CODE_NAME_BLANK = 403
    }

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
                val privacy = privacy_radio_group.checkedRadioButtonId == R.id.public_radio
                val description = list_description.text.toString()
                val e = withContext(Dispatchers.Default) {
                    runCatching {
                        TwitterManager.twitter.createUserList(listName, privacy, description)
                    }.exceptionOrNull()
                }
                if (e == null) {
                    MessageUtil.showToast(R.string.toast_create_user_list_success)
                    finish()
                } else if (e is TwitterException) {
                    MessageUtil.showToast(
                            if (e.statusCode == ERROR_CODE_NAME_BLANK)
                                R.string.toast_create_user_list_failure_name_blank
                            else
                                R.string.toast_create_user_list_failure)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?) = item?.run {
            if (itemId == R.id.home) finish()
            true
        } ?: false
}