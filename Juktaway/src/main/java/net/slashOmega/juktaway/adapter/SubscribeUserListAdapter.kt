package net.slashOmega.juktaway.adapter

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.ViewGroup
import de.greenrobot.event.EventBus
import kotlinx.android.synthetic.main.row_subscribe_user_list.view.*
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.event.AlertDialogEvent
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.UserListWithRegistered
import net.slashOmega.juktaway.task.DestroyUserListSubscriptionTask
import net.slashOmega.juktaway.task.DestroyUserListTask
import twitter4j.UserList

/**
 * Created on 2018/10/28.
 */
class SubscribeUserListAdapter(c: Context, id: Int): ArrayAdapterBase<UserListWithRegistered>(c, id) {
    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { pos, _ ->
            val list = getItem(pos)
            val userList = list?.userList
            trash.setOnClickListener {
                val dialog = if (AccessTokenManager.getUserId() == userList?.user?.id) DestroyUserListDialogFragment()
                else DestroyUserListSubscriptionDialogFragment()
                dialog.arguments = Bundle(1).apply {
                    putSerializable("userList", userList)
                }
                EventBus.getDefault().post(AlertDialogEvent(dialog))

            }
            checkbox.apply {
                text = if (AccessTokenManager.getUserId() == userList?.user?.id) userList.name else userList?.fullName
                setOnCheckedChangeListener(null)
                isChecked = list?.isRegistered ?: false
                setOnCheckedChangeListener { _, b -> list?.isRegistered = b }
            }
        }

    fun findByUserListId(userListId: Long?): UserListWithRegistered? {
        for (i in 0 until count) {
            val userListWithRegistered = getItem(i)
            if (userListWithRegistered?.userList?.id == userListId) return userListWithRegistered
        }
        return null
    }

    class DestroyUserListDialogFragment:DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val userList = arguments?.getSerializable("userList") as? UserList ?: return Dialog(activity)
            return AlertDialog.Builder(activity)
                    .setTitle(R.string.confirm_destroy_user_list)
                    .setMessage(userList.name)
                    .setPositiveButton(R.string.button_yes) { _, _ ->
                        DestroyUserListTask(userList).execute()
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_no) { _, _ -> dismiss()}
                    .create()
        }
    }

    class DestroyUserListSubscriptionDialogFragment:DialogFragment() {
        override fun onCreateDialog(savedInstanceState:Bundle?):Dialog {
            val userList = arguments?.getSerializable("userList") as? UserList ?: return Dialog(activity)
            return AlertDialog.Builder(activity)
                    .setTitle(R.string.confirm_destroy_user_list_subscribe)
                    .setMessage(userList.name)
                    .setPositiveButton(R.string.button_yes) { _, _ ->
                        DestroyUserListSubscriptionTask(userList).execute()
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_no) { _, _ -> dismiss() }
                    .create()
        }
    }
}