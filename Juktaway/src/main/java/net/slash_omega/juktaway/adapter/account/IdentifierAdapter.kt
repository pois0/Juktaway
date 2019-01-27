package net.slash_omega.juktaway.adapter.account

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.row_account.view.*
import net.slash_omega.juktaway.listener.OnTrashListener
import net.slash_omega.juktaway.model.UserIconManager.displayUserIcon
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.ArrayAdapterBase
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.util.ThemeUtil

/**
 * Created on 2018/10/20.
 */
class IdentifierAdapter(context: Context, mLayout: Int): ArrayAdapterBase<Identifier>(context, mLayout) {
    var mOnTrashListener: OnTrashListener? = null
    private val mColorBlue by lazy { ThemeUtil.getThemeTextColor(R.attr.holo_blue) }

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { position, _ ->
            getItem(position)?.let { token ->
                icon.displayUserIcon(token.userId)
                screen_name.text = token.screenName

                if (currentIdentifier == token) {
                    screen_name.setTextColor(mColorBlue)
                    trash.visibility = View.GONE
                }
                trash.setOnClickListener { mOnTrashListener?.onTrash(position) }
            }
        }
}