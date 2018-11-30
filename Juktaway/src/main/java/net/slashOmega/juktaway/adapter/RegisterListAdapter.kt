package net.slashOmega.juktaway.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.row_subscribe_user_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserListWithRegistered
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.tryAndTrace

/**
 * Created on 2018/11/13.
 */
class RegisterListAdapter(c: Context, id: Int, userId: Long): ArrayAdapterBase<UserListWithRegistered>(c, id) {
    private val mUserId = longArrayOf(userId)

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { pos, _ ->
            val registered = getItem(pos)!!
            checkbox.apply {
                text = registered.userList?.name ?: ""
                setOnCheckedChangeListener(null)
                isChecked = registered.isRegistered
                tag = registered.userList?.id
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked == registered.isRegistered) return@setOnCheckedChangeListener
                    registered.isRegistered = isChecked
                    MessageUtil.showProgressDialog(context, context.getString(R.string.progress_process))
                    GlobalScope.launch(Dispatchers.Main) {
                        if (isChecked) {
                            GlobalScope.async {
                                tryAndTrace {
                                    TwitterManager.twitter.createUserListMembers(registered.userList!!.id, *mUserId)
                                }
                            }.await().let {
                                MessageUtil.dismissProgressDialog()

                                if (it) {
                                    MessageUtil.showToast(R.string.toast_add_to_list_success)
                                } else {
                                    MessageUtil.showToast(R.string.toast_add_to_list_failure)
                                    registered.isRegistered = false
                                    notifyDataSetChanged()
                                }
                            }
                        } else {
                            GlobalScope.async {
                                tryAndTrace {
                                    TwitterManager.twitter.destroyUserListMembers(registered.userList!!.id, mUserId)
                                }
                            }.await().let {
                                MessageUtil.dismissProgressDialog()

                                if (it) {
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
}