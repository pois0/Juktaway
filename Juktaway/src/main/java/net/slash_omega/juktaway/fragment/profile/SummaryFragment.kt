package net.slash_omega.juktaway.fragment.profile

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.endpoints.blocks
import jp.nephy.penicillin.endpoints.friendships
import jp.nephy.penicillin.extensions.models.ProfileImageSize
import jp.nephy.penicillin.extensions.models.profileImageUrlWithVariantSize
import jp.nephy.penicillin.models.Relationship
import jp.nephy.penicillin.models.User
import net.slash_omega.juktaway.EditProfileActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.ScaleImageActivity
import net.slash_omega.juktaway.util.ImageUtil
import net.slash_omega.juktaway.util.MessageUtil
import kotlinx.android.synthetic.main.fragment_profile_summary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.twitter.currentIdentifier

class SummaryFragment: Fragment() {
    private var mFollowFlg = false
    private var mBlocking = false
    private var mRuntimeFlg = false
    private var isMyProfile = false
    private lateinit var mUser: User
    private lateinit var relationship: Relationship

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return arguments?.let { arg ->
            mUser = arg.getString("user")?.toJsonObject()?.parse() ?: return null
            relationship = arg.getString("relationship")?.toJsonObject()?.parse() ?: return null
            isMyProfile = mUser.id == currentIdentifier.userId

            mFollowFlg = relationship.source.following
            mBlocking = relationship.source.blocking

            inflater.inflate(R.layout.fragment_profile_summary, container, false)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        icon.setOnClickListener {
            startActivity(Intent(view.context, ScaleImageActivity::class.java).apply {
                putExtra("url", mUser.profileImageUrlWithVariantSize(ProfileImageSize.Original))
            })
        }

        ImageUtil.displayRoundedImage(mUser.profileImageUrlWithVariantSize(ProfileImageSize.Bigger), icon)
        name.text = mUser.name
        screen_name.text = "@" + mUser.screenName
        lock.visibility = if(mUser.protected) View.VISIBLE else View.GONE
        followed_by.text = if(relationship.source.followedBy) getString(R.string.label_followed_by_target) else ""
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
                                GlobalScope.launch(Dispatchers.Main) {
                                    MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                                    val res = runCatching { currentClient.friendships.destroy(mUser.id).await() }.isSuccess
                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        MessageUtil.showToast(R.string.toast_destroy_friendship_success)
                                        follow.setText(R.string.button_follow)
                                        mFollowFlg = false
                                    } else {
                                        MessageUtil.showToast(R.string.toast_destroy_friendship_failure)
                                    }
                                    mRuntimeFlg = false
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                }
                mBlocking -> {
                    AlertDialog.Builder(activity)
                            .setMessage(R.string.confirm_destroy_block)
                            .setPositiveButton(R.string.button_destroy_block) { _, _ ->
                                mRuntimeFlg = true
                                GlobalScope.launch(Dispatchers.Main) {
                                    MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                                    val res = runCatching { currentClient.blocks.destroy(mUser.id).await() }.isSuccess
                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        MessageUtil.showToast(R.string.toast_destroy_block_success)
                                        follow.setText(R.string.button_follow)
                                        mBlocking = false
                                    } else {
                                        MessageUtil.showToast(R.string.toast_destroy_block_failure)
                                    }
                                    mRuntimeFlg = false
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                }
                else -> {
                    AlertDialog.Builder(activity)
                            .setMessage(R.string.confirm_follow)
                            .setPositiveButton(R.string.button_follow) { _, _ ->
                                mRuntimeFlg = true
                                GlobalScope.launch(Dispatchers.Main) {
                                    MessageUtil.showProgressDialog(activity!!, getString(R.string.progress_process))
                                    val res = runCatching { currentClient.friendships.create(mUser.id).await() }.isSuccess
                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        MessageUtil.showToast(R.string.toast_follow_success)
                                        follow.setText(R.string.button_unfollow)
                                        mFollowFlg = true
                                    } else {
                                        MessageUtil.showToast(R.string.toast_follow_failure)
                                    }
                                    mRuntimeFlg = false
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                }
            }
        }

    }
}