package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.view.MenuItem
import net.slashOmega.juktaway.fragment.profile.UpdateProfileImageFragment
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.task.VerifyCredentialsLoader
import net.slashOmega.juktaway.util.FileUtil
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_edit_profile.*
import twitter4j.User
import java.lang.ref.WeakReference

class EditProfileActivity: FragmentActivity(), LoaderManager.LoaderCallbacks<User> {
    companion object {
        private const val REQ_PICK_PROFILE_IMAGE = 1

        private class UpdateProfileTask(activity: EditProfileActivity) : AsyncTask<Void, Void, User>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg params: Void): User? {
                return ref.get()?.run{
                    try {
                    TwitterManager.getTwitter().updateProfile(
                            name.text.toString(),
                            url.text.toString(),
                            location.text.toString(),
                            description.text.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }

            override fun onPostExecute(user: User?) {
                MessageUtil.dismissProgressDialog()
                if (user != null) {
                    MessageUtil.showToast(R.string.toast_update_profile_success)
                    ref.get()?.finish()
                } else {
                    MessageUtil.showToast(R.string.toast_update_profile_failure)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_edit_profile)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        supportLoaderManager.initLoader<User>(0, null, this)

        icon.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, REQ_PICK_PROFILE_IMAGE)
        }

        save_button.setOnClickListener {
            MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
            UpdateProfileTask(this).execute()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onCreateLoader(id: Int, args: Bundle?): android.support.v4.content.Loader<User> {
        return VerifyCredentialsLoader(this)
    }

    override fun onLoadFinished(loader: android.support.v4.content.Loader<User>, user: User?) {
        if (user == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        } else {
            name.setText(user.name)
            location.setText(user.location)
            url.setText(user.urlEntity.expandedURL)
            description.setText(user.description)
            ImageUtil.displayRoundedImage(user.originalProfileImageURL, icon)
        }
    }

    override fun onLoaderReset(arg0: android.support.v4.content.Loader<User>) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_PICK_PROFILE_IMAGE -> if (resultCode == Activity.RESULT_OK) {
                try {
                    val uri = data?.data ?: return
                    FileUtil.writeToTempFile(cacheDir, contentResolver.openInputStream(uri)!!)?.let {
                        val dialog = UpdateProfileImageFragment.newInstance(it, uri)
                        dialog.show(supportFragmentManager, "dialog")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}