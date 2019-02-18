package net.slash_omega.juktaway.fragment.profile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.models.User
import kotlinx.android.synthetic.main.fragment_profile_description.view.*
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.util.parseWithClient
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Created on 2018/11/18.
 */

class DescriptionFragment: Fragment() {
    companion object {
        private var mSimpleDateFormat: SimpleDateFormat? = null
    }

    private val user by lazy { arguments!!.getString("user")!!.toJsonObject().parseWithClient<User>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile_description, container, false)?.apply {
            println(user)
            if (!user.description.isNullOrEmpty()) {
                var descStr = user.description
                user.entities?.description?.urls?.forEach {
                    val matcher = Pattern.compile(it.url).matcher(descStr)
                    descStr = matcher.replaceAll(it.expandedUrl)
                }
                description.text = descStr
                description.visibility = View.VISIBLE
            } else {
                description.visibility = View.GONE
            }

            if (!user.location.isNullOrEmpty()) {
                location.text = user.location
                location.visibility = View.VISIBLE
            } else {
                location.visibility = View.GONE
            }

            if (!user.url.isNullOrEmpty()) {
                url.text = user.entities?.url?.urls?.first()?.expandedUrl ?: user.url
                url.visibility = View.VISIBLE
            } else {
                url.visibility = View.GONE
            }

            if (mSimpleDateFormat == null) {
                mSimpleDateFormat = SimpleDateFormat(getString(R.string.format_user_created_at), Locale.ENGLISH)
            }

            start.text = mSimpleDateFormat!!.format(user.createdAt.date)
        }
}