package net.slashOmega.juktaway.fragment.main

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment

import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.util.MessageUtil

class StreamingSwitchDialogFragment : DialogFragment() {
    companion object {
        fun newInstance(turnOn: Boolean) = StreamingSwitchDialogFragment().apply {
            arguments = Bundle(1).apply { putBoolean("turnOn", turnOn) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val turnOn = arguments!!.getBoolean("turnOn")

        return AlertDialog.Builder(activity)
                .setMessage(if (turnOn) R.string.confirm_create_streaming else R.string.confirm_destroy_streaming)
                .setPositiveButton(getString(R.string.button_ok)) { _, _ ->
                    BasicSettings.streamingMode = turnOn
                    if (turnOn) {
                        TwitterManager.startStreaming()
                        MessageUtil.showToast(R.string.toast_create_streaming)
                    } else {
                        TwitterManager.stopStreaming()
                        MessageUtil.showToast(R.string.toast_destroy_streaming)
                    }
                    dismiss()
                }
                .setNegativeButton(getString(R.string.button_cancel)) { _, _ -> dismiss() }
                .create()
    }
}
