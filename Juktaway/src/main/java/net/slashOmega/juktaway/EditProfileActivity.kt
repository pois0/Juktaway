package net.slashOmega.juktaway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.MenuItem
import jp.nephy.penicillin.endpoints.account
import jp.nephy.penicillin.endpoints.account.updateProfile
import jp.nephy.penicillin.endpoints.account.verifyCredentials
import jp.nephy.penicillin.extensions.models.ProfileImageSize
import jp.nephy.penicillin.extensions.models.profileImageUrlWithVariantSize
import net.slashOmega.juktaway.fragment.profile.UpdateProfileImageFragment
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.coroutines.*
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.*
import org.jetbrains.anko.toast

private const val REQ_PICK_PROFILE_IMAGE = 1

class EditProfileActivity: FragmentActivity(){
    companion object {
        private var job: Job? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        job = GlobalScope.launch(Dispatchers.Main) {
            val user = currentClient.account.verifyCredentials().await().result
            name.setText(user.name)
            location.setText(user.location)
            url.setText(user.url)
            description.setText(user.description)
            icon.displayRoundedImage(user.profileImageUrlWithVariantSize(ProfileImageSize.Original))
        }

        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_edit_profile)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        icon.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, REQ_PICK_PROFILE_IMAGE)
        }

        save_button.setOnClickListener {
            MessageUtil.showProgressDialog(this, getString(R.string.progress_process))
            GlobalScope.launch(Dispatchers.Main) {
                runCatching {
                    currentClient.account.updateProfile(
                            name.text.toString(),
                            url.text.toString(),
                            location.text.toString(),
                            description.text.toString()
                    ).await()
                }.getOrNull() ?: toast(R.string.toast_update_profile_failure)

                toast(R.string.toast_update_profile_success)
                finish()
            }
        }
    }

    override fun onStop() {
        job?.cancel()
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

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