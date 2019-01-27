package net.slash_omega.juktaway.adapter

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.ViewGroup
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.models.TwitterList
import kotlinx.android.synthetic.main.row_subscribe_user_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.event.AlertDialogEvent
import net.slash_omega.juktaway.event.model.DestroyUserListEvent
import net.slash_omega.juktaway.model.UserListCache
import net.slash_omega.juktaway.model.UserListWithRegistered
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.twitter.currentIdentifier
import org.jetbrains.anko.support.v4.toast

/**
 * Created on 2018/10/28.
 */
class SubscribeUserListAdapter(c: Context, id: Int): ArrayAdapterBase<UserListWithRegistered>(c, id) {
    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { pos, _ ->
            val list = getItem(pos)
            val userList = list?.userList
            trash.setOnClickListener {
                val dialog = if (currentIdentifier.userId == userList?.user?.id) DestroyUserListDialogFragment()
                else DestroyUserListSubscriptionDialogFragment()
                dialog.arguments = Bundle(1).apply {
                    putString("userList", userList?.toJsonString())
                }
                EventBus.getDefault().post(AlertDialogEvent(dialog))

            }
            checkbox.apply {
                text = if (currentIdentifier.userId == userList?.user?.id) userList.name else userList?.fullName
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
            val userList = arguments?.getString("userList")?.toJsonObject()?.parse(TwitterList::class) ?: return Dialog(activity!!)
            return AlertDialog.Builder(activity)
                    .setTitle(R.string.confirm_destroy_user_list)
                    .setMessage(userList.name)
                    .setPositiveButton(R.string.button_yes) { _, _ ->
                        GlobalScope.launch(Dispatchers.Main) {
                            runCatching { currentClient.lists.destroy(userList.id).await() }
                                    .onSuccess {
                                        toast(R.string.toast_destroy_user_list_success)
                                        EventBus.getDefault().post(DestroyUserListEvent(userList.id))
                                        UserListCache.userLists.remove(userList)
                                    }
                                    .onFailure {
                                        toast(R.string.toast_destroy_user_list_failure)
                                    }
                            dismiss()
                        }
                    }
                    .setNegativeButton(R.string.button_no) { _, _ -> dismiss()}
                    .create()
        }
    }

    class DestroyUserListSubscriptionDialogFragment:DialogFragment() {
        override fun onCreateDialog(savedInstanceState:Bundle?):Dialog {
            val userList = arguments?.getString("userList")?.toJsonObject()?.parse(TwitterList::class) ?: return Dialog(activity!!)
            return AlertDialog.Builder(activity)
                    .setTitle(R.string.confirm_destroy_user_list_subscribe)
                    .setMessage(userList.name)
                    .setPositiveButton(R.string.button_yes) { _, _ ->
                        GlobalScope.launch(Dispatchers.Main) {
                            runCatching { currentClient.lists.unsubscribe(userList.id).await() }
                                    .onSuccess {
                                        toast(R.string.toast_destroy_user_list_subscription_success)
                                        EventBus.getDefault().post(DestroyUserListEvent(userList.id))
                                        UserListCache.userLists.remove(userList)
                                    }
                                    .onFailure {
                                        toast(R.string.toast_destroy_user_list_subscription_failure)
                                    }
                            dismiss()
                        }
                    }
                    .setNegativeButton(R.string.button_no) { _, _ -> dismiss() }
                    .create()
        }
    }
}