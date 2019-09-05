package net.slash_omega.juktaway.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.listener.RemoveAccountListener
import net.slash_omega.juktaway.twitter.Identifier

/**
 * Created on 2018/10/19.
 */
class AccountSwitchDialogFragment: DialogFragment() {
    companion object {
        fun newInstance(identifier: Identifier) = AccountSwitchDialogFragment().apply {
                arguments = Bundle(1).also { it.putSerializable("identifier", identifier) }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val token = arguments!!.getSerializable("identifier") as Identifier

        return AlertDialog.Builder(activity)
                .setMessage(String.format(getString(R.string.confirm_remove_account), token.screenName))
                .setPositiveButton(R.string.button_yes) { _, _ ->
                    (activity as RemoveAccountListener).removeIdentifier(token)
                }
                .setNegativeButton(R.string.button_no) { _, _ -> }
                .create()
    }
}
