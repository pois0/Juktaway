package net.slashOmega.juktaway.fragment.profile

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.slashOmega.juktaway.EditProfileActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.ScaleImageActivity
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.task.DestroyFriendshipTask
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.util.MessageUtil
import kotlinx.android.synthetic.main.fragment_profile_summary.*
import twitter4j.Relationship
import twitter4j.User
import java.lang.ref.WeakReference

class SummaryFragment: Fragment() {
    companion object {
        private class DestroyFriendShipTask(fragment: SummaryFragment): DestroyFriendshipTask() {
            val ref = WeakReference(fragment)

            override fun onPostExecute(result: Boolean?) {
                ref.get()?.run {
                    MessageUtil.dismissProgressDialog()
                    if (result == true) {
                        MessageUtil.showToast(R.string.toast_destroy_friendship_success)
                        follow.setText(R.string.button_follow)
                        mFollowFlg = false
                    } else
                        MessageUtil.showToast(R.string.toast_destroy_friendship_failure)
                    mRuntimeFlg = false
                }
            }
        }

        private class DestroyBlockTask(fragment: SummaryFragment): net.slashOmega.juktaway.task.DestroyBlockTask() {
            val ref = WeakReference(fragment)

            override fun onPostExecute(result: Boolean?) {
                ref.get()?.run {
                    MessageUtil.dismissProgressDialog()
                    if (result == true) {
                        MessageUtil.showToast(R.string.toast_destroy_block_success)
                        follow.setText(R.string.button_follow)
                        mBlocking = false
                    } else
                        MessageUtil.showToast(R.string.toast_destroy_block_failure)
                    mRuntimeFlg = false
                }
            }
        }

        private class FollowTask(fragment: SummaryFragment): net.slashOmega.juktaway.task.FollowTask() {
            val ref = WeakReference(fragment)

            override fun onPostExecute(result: Boolean?) {
                ref.get()?.run {
                    MessageUtil.dismissProgressDialog()
                    if (result == true) {
                        MessageUtil.showToast(R.string.toast_follow_success)
                        follow.setText(R.string.button_unfollow)
                        mFollowFlg = true
                    } else
                        MessageUtil.showToast(R.string.toast_follow_failure)
                    mRuntimeFlg = false
                }
            }
        }
    }

    private var mFollowFlg = false
    private var mBlocking = false
    private var mRuntimeFlg = false
    private var isMyProfile = false
    private lateinit var user: User
    private lateinit var relationship: Relationship

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return arguments?.let { arg ->
            val v = inflater.inflate(R.layout.fragment_profile_summary, container, false) ?: return null
            user = (arg.getSerializable("user") as User?) ?: return null
            relationship = (arg.getSerializable("relationship") as Relationship?) ?: return null
            isMyProfile = user.id == AccessTokenManager.getUserId()

            mFollowFlg = relationship.isSourceFollowingTarget
            mBlocking = relationship.isSourceBlockingTarget

            v
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        icon.setOnClickListener {
            startActivity(Intent(view.context, ScaleImageActivity::class.java).apply {
                putExtra("url", user.originalProfileImageURL)
            })
        }

        ImageUtil.displayRoundedImage(user.biggerProfileImageURL, icon)
        name.text = user.name
        screen_name.text = "@" + user.screenName
        lock.visibility = if(user.isProtected) View.VISIBLE else View.GONE
        followed_by.text = if(relationship.isSourceFollowedByTarget) getString(R.string.label_followed_by_target) else ""
        follow.visibility = View.VISIBLE
        follow.setText( when {
            isMyProfile -> R.string.button_edit_profile
            mFollowFlg -> R.string.button_unfollow
            mBlocking -> R.string.button_blocking
            else -> R.string.button_follow
        })
        follow.setOnClickListener {
            if (mRuntimeFlg) return@setOnClickListener
            when {
                isMyProfile ->
                    startActivity(Intent(activity, EditProfileActivity::class.java))
                mFollowFlg -> {
                    AlertDialog.Builder(activity)
                            .setMessage(R.string.confirm_unfollow)
                            .setPositiveButton(R.string.button_unfollow) { _, _ ->
                                mRuntimeFlg = true
                                MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                                DestroyFriendShipTask(this).execute(user.id)
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                }
                mBlocking -> {
                    AlertDialog.Builder(activity)
                            .setMessage(R.string.confirm_destroy_block)
                            .setPositiveButton(R.string.button_destroy_block) { _, _ ->
                                mRuntimeFlg = true
                                MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                                DestroyBlockTask(this).execute(user.id)
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                }
                else -> {
                    AlertDialog.Builder(activity)
                            .setMessage(R.string.confirm_follow)
                            .setPositiveButton(R.string.button_follow) { _, _ ->
                                mRuntimeFlg = true
                                MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                                FollowTask(this).execute(user.id)
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                }
            }
        }

    }
}