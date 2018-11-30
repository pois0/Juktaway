package net.slashOmega.juktaway.fragment.profile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_profile_description.view.*
import net.slashOmega.juktaway.R
import twitter4j.User
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

    private val user by lazy { arguments!!.getSerializable("user") as User }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile_description, container, false)?.apply {
            if (!user.description.isNullOrEmpty()) {
                var descStr = user.description
                user.descriptionURLEntities?.forEach {
                    val matcher = Pattern.compile(it.url).matcher(descStr)
                    descStr = matcher.replaceAll(it.expandedURL)
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
                url.text = user.urlEntity?.expandedURL ?: user.url
                url.visibility = View.VISIBLE
            } else {
                url.visibility = View.GONE
            }

            if (mSimpleDateFormat == null) {
                mSimpleDateFormat = SimpleDateFormat(getString(R.string.format_user_created_at), Locale.ENGLISH)
            }

            start.text = mSimpleDateFormat!!.format(user.createdAt)
        }
}