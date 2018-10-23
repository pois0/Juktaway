//package net.slashOmega.juktaway.adapter
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.view.View
//import android.view.ViewGroup
//import kotlinx.android.synthetic.main.row_user.view.*
//import net.slashOmega.juktaway.ProfileActivity
//import net.slashOmega.juktaway.util.ImageUtil
//import twitter4j.User
//
///**
// * Created on 2018/10/21.
// */
//class UserAdapter(mContext: Context?, mLayout: Int) : ArrayAdapterBase<User>(mContext, mLayout) {
//    override val View.mView: (Int, ViewGroup) -> Unit
//        @SuppressLint("SetTextI18n")
//        get() = { position, _ ->
//            val user = getItem(position)
//            ImageUtil.displayRoundedImage(user.biggerProfileImageURL, icon)
//            display_name.text = user.name
//            screen_name.text = "@${user.screenName}"
//            user.description?.takeIf { it.isEmpty() }?.let {
//                var text = it
//                user.descriptionURLEntities.forEach { e ->
//                    text = text.replace(e.url, e.expandedURL)
//                }
//                description.text = text
//                description.visibility = View.VISIBLE
//            } ?: run { description.visibility = View.GONE }
//
//            lock.visibility = if (user.isProtected) View.VISIBLE else View.INVISIBLE
//
//            setOnClickListener {
//                mContext?.startActivity(Intent(it.context, ProfileActivity::class.java).apply {
//                    putExtra("screenName", user.screenName)
//                })
//            }
//        }
//}