package net.slash_omega.juktaway.adapter.main

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.row_switch_account.view.*
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.ScopedAppCompatActivity
import net.slash_omega.juktaway.adapter.ArrayAdapterBase
import net.slash_omega.juktaway.model.UserIconManager.displayUserIcon
import net.slash_omega.juktaway.twitter.Core
import net.slash_omega.juktaway.twitter.Identifier
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.twitter.identifierList

/**
 * Created on 2018/10/20.
 */
class IdentifierAdapter(private val mActivity: ScopedAppCompatActivity, resourceId: Int, private val highlightColor: Int, private val defaultColor: Int) : ArrayAdapterBase<Identifier>(mActivity, resourceId) {
    init { identifierList.forEach { add(it) } }

    override val View.mView: (Int, ViewGroup?) -> Unit
        @SuppressLint("SetTextI18n")
        get() = { pos, _ ->
            mActivity.launch {
                getItem(pos)?.let { identifier ->
                    icon.displayUserIcon(identifier.userId)
                    screen_name.text = identifier.screenName
                    consumer_name.text = Core.getConsumer(identifier.consumerId)?.name
                    (if (currentIdentifier == identifier) highlightColor else defaultColor).let {
                        screen_name.setTextColor(it)
                        consumer_name.setTextColor(it)
                    }
                }
            }
        }
}
