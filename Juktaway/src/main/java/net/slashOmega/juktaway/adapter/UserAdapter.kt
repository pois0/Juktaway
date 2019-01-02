package net.slashOmega.juktaway.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import jp.nephy.penicillin.models.CommonUser
import jp.nephy.penicillin.models.User
import kotlinx.android.synthetic.main.row_user.view.*
import net.slashOmega.juktaway.ProfileActivity
import net.slashOmega.juktaway.util.ImageUtil

/**
 * Created on 2018/10/21.
 */
class UserAdapter(mContext: Context?, mLayout: Int) : ArrayAdapterBase<User>(mContext, mLayout) {
    override val View.mView: (Int, ViewGroup?) -> Unit
        @SuppressLint("SetTextI18n")
        get() = { position, _ ->
            getItem(position)?.let { user ->
                ImageUtil.displayRoundedImage(user.profileImageUrlWithVariantSize(CommonUser.ProfileImageSize.Bigger), icon)
                display_name.text = user.name
                screen_name.text = "@${user.screenName}"
                user.description?.takeIf { it.isNotEmpty() }?.let {
                    var text = it
                    user.entities?.description?.urls?.forEach { url ->
                        text = text.replace(url.url, url.expandedUrl)
                    }
                    description.text = text
                    description.visibility = View.VISIBLE
                } ?: run { description.visibility = View.GONE }

                lock.visibility = if (user.protected) View.VISIBLE else View.INVISIBLE

                setOnClickListener {
                    mContext?.startActivity(Intent(it.context, ProfileActivity::class.java).apply {
                        putExtra("screenName", user.screenName)
                    })
                }
            }
        }
}