package net.slashOmega.juktaway.adapter.main

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.row_switch_account.view.*
import net.slashOmega.juktaway.adapter.ArrayAdapterBase
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.UserIconManager
import twitter4j.auth.AccessToken

/**
 * Created on 2018/10/20.
 */
class AccessTokenAdapter(context: Context?, resourceId: Int, private val highlightColor: Int, private val defaultColor: Int) : ArrayAdapterBase<AccessToken>(context, resourceId) {
    init {
        AccessTokenManager.getAccessTokens()?.forEach { add(it) }
    }

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { pos, _ ->
            getItem(pos)?.let { token ->
                UserIconManager.displayUserIcon(token.userId, icon)
                screen_name.text = token.screenName
                screen_name.setTextColor(if (AccessTokenManager.getUserId() == token.userId) highlightColor else defaultColor)
            }
        }
}