package net.slash_omega.juktaway.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.lists.addMembersByUserIds
import jp.nephy.penicillin.endpoints.lists.removeMembersByUserIds
import jp.nephy.penicillin.extensions.await
import kotlinx.android.synthetic.main.row_subscribe_user_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.model.UserListWithRegistered
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.MessageUtil

/**
 * Created on 2018/11/13.
 */
class RegisterListAdapter(c: Context, id: Int, userId: Long): ArrayAdapterBase<UserListWithRegistered>(c, id) {
    private val mUserId = longArrayOf(userId)

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { pos, _ ->
            val registered = getItem(pos)!!
            checkbox.apply {
                text = registered.userList.name
                setOnCheckedChangeListener(null)
                isChecked = registered.isRegistered
                tag = registered.userList.id
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked == registered.isRegistered) return@setOnCheckedChangeListener
                    registered.isRegistered = isChecked
                    GlobalScope.launch(Dispatchers.Main) {
                        MessageUtil.showProgressDialog(context, context.getString(R.string.progress_process))
                        if (isChecked) {
                            val res = runCatching {
                                currentClient.lists.addMembersByUserIds(registered.userList.id, mUserId.toList()).await()
                            }.isSuccess

                            MessageUtil.dismissProgressDialog()
                            if (res) {
                                MessageUtil.showToast(R.string.toast_add_to_list_success)
                            } else {
                                MessageUtil.showToast(R.string.toast_add_to_list_failure)
                                registered.isRegistered = false
                                notifyDataSetChanged()
                            }
                        } else {
                            val res = runCatching {
                                currentClient.lists.removeMembersByUserIds(registered.userList.id, mUserId.toList()).await()
                            }.isSuccess

                            MessageUtil.dismissProgressDialog()
                            if (res) {
                                MessageUtil.showToast(R.string.toast_remove_from_list_success)
                            } else {
                                MessageUtil.showToast(R.string.toast_remove_from_list_failure)
                                registered.isRegistered = true
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
}