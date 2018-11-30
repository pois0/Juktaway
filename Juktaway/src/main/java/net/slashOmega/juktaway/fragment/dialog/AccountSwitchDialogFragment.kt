package net.slashOmega.juktaway.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.listener.RemoveAccountListener
import twitter4j.auth.AccessToken

/**
 * Created on 2018/10/19.
 */
class AccountSwitchDialogFragment: DialogFragment() {
    companion object {
        fun newInstance(token: AccessToken) = AccountSwitchDialogFragment().apply {
                arguments = Bundle(1).also { it.putSerializable("accessToken", token) }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val token = arguments!!.getSerializable("accessToken") as AccessToken

        return AlertDialog.Builder(activity)
                .setMessage(String.format(getString(R.string.confirm_remove_account), token.screenName))
                .setPositiveButton(R.string.button_yes) { _, _ ->
                    (activity as RemoveAccountListener).removeAccount(token)
                }
                .setNegativeButton(R.string.button_no) { _, _ -> }
                .create()
    }
}