package net.slash_omega.juktaway.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.extensions.models.ProfileImageSize
import jp.nephy.penicillin.extensions.models.profileImageUrlWithVariantSize
import jp.nephy.penicillin.models.TwitterList
import kotlinx.android.synthetic.main.row_user_list.view.*
import net.slash_omega.juktaway.ProfileActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.UserListActivity
import net.slash_omega.juktaway.util.ImageUtil

/**
 * Created on 2018/10/29.
 */
class UserListAdapter(c: Context, id: Int): ArrayAdapterBase<TwitterList>(c, id) {
    override val View.mView: (Int, ViewGroup?) -> Unit
        @SuppressLint("SetTextI18n")
        get() = { pos, _ -> getItem(pos)?.let { userList ->
            ImageUtil.displayRoundedImage(userList.user.profileImageUrlWithVariantSize(ProfileImageSize.Bigger), icon)
            list_name.text = userList.name
            screen_name.text = userList.user.screenName
            description.text = userList.description
            member_count.text = (userList.memberCount.toString()) + mContext?.getString(R.string.label_members)
            icon.setOnClickListener {
                mContext?.startActivity(Intent(context, ProfileActivity::class.java).apply {
                    putExtra("userJson", userList.user.toJsonString())
                })
            }
            setOnClickListener {
                mContext?.startActivity(Intent(context, UserListActivity::class.java).apply {
                    putExtra("userList", userList.toJsonString())
                })
            }
        }}
}