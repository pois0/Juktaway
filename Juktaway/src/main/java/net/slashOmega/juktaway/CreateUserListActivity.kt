package net.slashOmega.juktaway

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_create_user_list.*
import twitter4j.TwitterException
import java.lang.ref.WeakReference

class CreateUserListActivity: Activity() {
    companion object {
        private const val ERROR_CODE_NAME_BLANK = 403

        private class CreateUserListTask(context: CreateUserListActivity, val listName: String, val privacy: Boolean, val listDescription: String)
                : AsyncTask<Void, Void, TwitterException?>() {
            val ref = WeakReference(context)

            override fun doInBackground(vararg p0: Void?): TwitterException? {
                return try {
                    TwitterManager.getTwitter().createUserList(listName, privacy, listDescription)
                    null
                } catch (e: TwitterException) {
                    e.printStackTrace()
                    e
                }
            }

            override fun onPostExecute(e: TwitterException?) {
                MessageUtil.dismissProgressDialog()
                if (e == null) {
                    MessageUtil.showToast(R.string.toast_create_user_list_success)
                    ref.get()?.finish()
                } else {
                    MessageUtil.showToast(
                            if (e.statusCode == ERROR_CODE_NAME_BLANK)
                                R.string.toast_create_user_list_failure_name_blank
                            else
                                R.string.toast_create_user_list_failure)
                }
            }
        }
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
            CreateUserListTask(this,
                    list_name.text.toString(),
                    privacy_radio_group.checkedRadioButtonId == R.id.public_radio,
                    list_description.text.toString()).execute()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return item?.run {
            if (itemId == R.id.home) finish()
            true
        } ?: false
    }
}