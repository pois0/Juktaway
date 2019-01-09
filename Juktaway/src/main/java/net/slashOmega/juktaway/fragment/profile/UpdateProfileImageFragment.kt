package net.slashOmega.juktaway.fragment.profile

import android.app.AlertDialog.Builder
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import kotlinx.coroutines.*
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.displayImage
import net.slashOmega.juktaway.util.mediaType
import org.jetbrains.anko.support.v4.toast
import java.io.File

private const val imgPathArg = "imgPath"
private const val uriArg = "uri"

class UpdateProfileImageFragment: DialogFragment() {
    companion object {
        fun newInstance(imgPath: File, uri: Uri) = UpdateProfileImageFragment().apply {
            arguments = Bundle(2).apply {
                putSerializable(imgPathArg, imgPath)
                putParcelable(uriArg, uri)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
        = Builder(activity!!)
            .setMessage(R.string.confirm_update_profile_image)
            .setView(LinearLayout(activity!!).also { ll ->
                ll.addView(ImageView(activity!!).apply {
                    scaleType = ScaleType.CENTER_CROP
                    layoutParams = LayoutParams(340, 340)
                    displayImage(arguments!!.get("uri") as Uri)
                })
                ll.gravity = Gravity.CENTER
            })
            .setPositiveButton(R.string.button_apply) { _, _ ->
                MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                GlobalScope.launch(Dispatchers.Main) {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            val file = arguments!!.get(uriArg) as File
                            currentClient.account.updateProfileImage(file.readBytes(), file.mediaType())
                        }
                    }.run {
                        MessageUtil.dismissProgressDialog()
                        toast(if (isSuccess) R.string.toast_update_profile_image_success else R.string.toast_update_profile_image_failure)
                        dismiss()
                    }
                }
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> dismiss() }
            .create()
}