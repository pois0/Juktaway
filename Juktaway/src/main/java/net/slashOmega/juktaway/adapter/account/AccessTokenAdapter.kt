package net.slashOmega.juktaway.adapter.account

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.row_account.view.*
import net.slashOmega.juktaway.listener.OnTrashListener
import net.slashOmega.juktaway.model.UserIconManager
import twitter4j.auth.AccessToken
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.ArrayAdapterBase
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.util.ThemeUtil

/**
 * Created on 2018/10/20.
 */
class AccessTokenAdapter(context: Context, mLayout: Int): ArrayAdapterBase<AccessToken>(context, mLayout) {
    var mOnTrashListener: OnTrashListener? = null
    private val mColorBlue by lazy { ThemeUtil.getThemeTextColor(R.attr.holo_blue) }

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { position, _ ->
            val token = getItem(position)
            UserIconManager.displayUserIcon(token.userId, icon)
            screen_name.text = token.screenName

            if (AccessTokenManager.getUserId() == token.userId) {
                screen_name.setTextColor(mColorBlue)
                trash.visibility = View.GONE
            }
            trash.setOnClickListener { _ ->
                mOnTrashListener?.onTrash(position)
            }
        }
}