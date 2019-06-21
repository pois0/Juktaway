package net.slash_omega.juktaway.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.extensions.via
import jp.nephy.penicillin.models.Status
import kotlinx.coroutines.*
import net.slash_omega.juktaway.ProfileActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.StatusActivity
import net.slash_omega.juktaway.layouts.fontelloTextView
import net.slash_omega.juktaway.model.displayUserIcon
import net.slash_omega.juktaway.model.isFavorited
import net.slash_omega.juktaway.model.isRetweeted
import net.slash_omega.juktaway.settings.mute.Mute
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.util.*
import net.slash_omega.juktaway.util.TwitterUtil.omitCount
import org.jetbrains.anko.*
import java.util.*

/**
 * Created on 2018/11/13.
 */

class StatusAdapter(private val fragmentActivity: FragmentActivity): ArrayAdapter<Status>(fragmentActivity, 0), CoroutineScope by fragmentActivity.scope {
    companion object {
        class DestroyRetweetDialogFragment: DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?)
                    = arguments?.getString("status")?.toJsonObject()?.parseWithClient<Status>()?.let {
                AlertDialog.Builder(activity)
                        .setTitle(R.string.confirm_destroy_retweet)
                        .setMessage(it.text)
                        .setPositiveButton(getString(R.string.button_destroy_retweet)) { _, _  ->
                            activity!!.scope.launch {
                                it.destroyRetweet()
                                dismiss()
                            }
                        }
                        .setNegativeButton(getString(R.string.button_cancel)) { _, _ -> dismiss() }
                        .create()
            } ?: throw IllegalStateException()
        }

        class RetweetDialogFragment: DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?)
                    = arguments?.getString("status")?.toJsonObject()?.parseWithClient<Status>()?.let {
                AlertDialog.Builder(activity)
                        .setTitle(R.string.confirm_retweet)
                        .setMessage(it.text)
                        .setNeutralButton(R.string.button_quote) { _, _ ->
                            it.quote(activity!!)
                            dismiss()
                        }
                        .setPositiveButton(R.string.button_retweet) { _, _ ->
                            activity!!.scope.launch {
                                it.retweet()
                                dismiss()
                            }
                        }
                        .setNegativeButton(R.string.button_cancel) { _, _ -> dismiss() }
                        .create()
            } ?: throw IllegalStateException()
        }
    }

    private val limit = 100
    private var mLimit = limit
    private val mIdSet = Collections.synchronizedSet(mutableSetOf<Long>())

    private val shouldShow get() = { status: Status -> status !in Mute && mIdSet.add(status.id) }

    suspend fun extensionAddAllFromStatuses(statusesParam: List<Status>) {
        val statuses = withContext(Dispatchers.Default) {
            statusesParam.filter(shouldShow)
        }

        super.addAll(statuses)
        mLimit += statuses.size
    }

    override fun add(status: Status) {
        launch { addSuspend(status) }
    }

    suspend fun addSuspend(status: Status) {
        if (withContext(Dispatchers.Default) { !shouldShow(status) }) return
        super.add(status)
        limitation()
    }

    suspend fun addAllSuspend(statusesParam: List<Status>) {
        val statuses = withContext(Dispatchers.Default) {
            statusesParam.filter(shouldShow)
        }

        super.addAll(statuses)

        // limitation()
    }

    override fun insert(status: Status, index: Int) {
        launch { insertSuspend(status, index) }
    }

    suspend fun insertSuspend(status: Status, index: Int) {
        if (withContext(Dispatchers.Default) { !shouldShow(status) }) return
        super.insert(status, index)
        // limitation()
    }

    suspend fun insertAllFromStatus(statusesParam: Collection<Status>, index: Int): Int = coroutineScope {
        setNotifyOnChange(false)

        var position = index
        val statuses = withContext(Dispatchers.Default) {
            statusesParam.filter(shouldShow)
        }

        statuses.forEach {
            super.insert(it, position++)
        }

        notifyDataSetChanged()

        statuses.size
    }

    override fun remove(status: Status) {
        super.remove(status)
        mIdSet.add(status.id)
    }

    fun removeStatus(id: Long) {
        (0 until count).mapNotNull { i ->
            getItem(i)!!.takeIf { it.id == id || it.retweetedStatus?.id == id }
        }.forEach {
            remove(it)
        }
    }

    private fun limitation() {
        if (count > mLimit) {
            val count = count - mLimit
            for (i in 0 until count) {
                super.remove(getItem(count - i - 1))
            }
        }
    }

    override fun clear() {
        super.clear()
        mIdSet.clear()
        mLimit = limit
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = parent.context.run {
        val status = getItem(position)!!
        val s = status.retweetedStatus ?: status
        val fontSize = preferences.display.general.fontSize.toFloat()
        relativeLayout {
            bottomPadding = dip(3)
            leftPadding = dip(6)
            rightPadding = dip(7)
            topPadding = dip(4)


            val data = s.retweetedStatus?.run {
                RowData(R.string.fontello_retweet, ContextCompat.getColor(fragmentActivity, R.color.holo_green_light),
                        user.name, user.screenName)

            } ?: RowData(R.string.fontello_at, ContextCompat.getColor(fragmentActivity, R.color.holo_red_light),
                    s.user.name, s.user.screenName)

            val actionContainer = relativeLayout {
                id = R.id.action_container

                fontelloTextView {
                    id = R.id.action_icon
                    gravity = Gravity.END
                    textSize = 12f //sp
                    setText(data.textId)
                    textColor = data.textColor
                }.lparams(width = dip(48), height = wrapContent) {
                    rightMargin = dip(6)
                    bottomMargin = dip(2)
                }

                textView {
                    id = R.id.action_by_display_name
                    textSize = 12f //sp
                    setTypeface(typeface, Typeface.BOLD)
                    text = data.displayName
                }.lparams(width = wrapContent, height = wrapContent) {
                    rightOf(R.id.action_icon)
                }

                textView {
                    id = R.id.action_by_screen_name
                    textColor = Color.parseColor("#666666")
                    textSize = 10f //sp
                    text = data.screenName
                }.lparams(width = wrapContent, height = wrapContent) {
                    rightOf(R.id.action_by_display_name)
                    leftMargin = dip(4)
                }
            }.lparams(width = matchParent)

            imageView {
                id = R.id.icon
                topPadding = dip(2)
                contentDescription = resources.getString(R.string.description_icon)
                setOnClickListener {
                    startActivity(it.context.intentFor<ProfileActivity>("userJson" to s.user.toJsonString()))
                }

                displayUserIcon(s.user)
            }.run {
                val iconSize = if (preferences.display.tweet.shouldShowAuthorIcon) 48 else 0
                lparams(width = dip(iconSize), height = dip(iconSize)) {
                    below(R.id.action_container)
                    bottomMargin = dip(6)
                    rightMargin = dip(6)
                    topMargin = dip(1)
                }
            }

            textView {
                id = R.id.display_name
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
                setTypeface(typeface, Typeface.BOLD)
                text = s.user.name
            }.lparams {
                below(R.id.action_container)
                rightOf(R.id.icon)
                bottomMargin = dip(6)
            }

            textView {
                id = R.id.screen_name
                textColor = Color.parseColor("#666666")
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize - 2)
                text = "@" + s.user.screenName
                lines = 1
                ellipsize = TextUtils.TruncateAt.END
            }.lparams {
                leftMargin = dip(4)
                rightOf(R.id.display_name)
                baselineOf(R.id.display_name)
            }
            var nameEnd = R.id.screen_name

            if (s.user.protected) {
                imageView {
                    id = R.id.lock
                    setImageResource(R.drawable.ic_lock)
                    setColorFilter(Color.parseColor("#666666"))
                }.lparams(width = dip(10), height = dip(10)) {
                    endOf(R.id.screen_name)
                    sameBottom(R.id.screen_name)
                    leftMargin = dip(4)
                    bottomMargin = dip(2)
                }
                nameEnd = R.id.verified
            }

            if (s.user.verified) {
                imageView {
                    id = R.id.verified
                    setImageResource(R.drawable.ic_verified)
                    setColorFilter(Color.parseColor("#666666"))
                }.lparams(width = dip(10), height = dip(10)) {
                    endOf(nameEnd)
                    sameBottom(R.id.screen_name)
                    leftMargin = dip(4)
                    bottomMargin = dip(2)
                }
            }

            textView {
                id = R.id.datetime_relative
                textColor = Color.parseColor("#666666")
                setTextSize(TypedValue.COMPLEX_UNIT_SP,fontSize - 2)
                text = TimeUtil.getRelativeTime(s.createdAt.date)
            }.lparams {
                alignParentRight()
                baselineOf(R.id.display_name)
            }

            textView {
                id = R.id.status
                tag = fontSize
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
                text = StatusUtil.generateUnderline(StatusUtil.getExpandedText(s))

            }.lparams {
                rightOf(R.id.icon)
                below(R.id.display_name)
            }

            var bottom = R.id.status

            s.quotedStatus?.let { qs ->
                relativeLayout {
                    id = R.id.quoted_tweet
                    padding = dip(10)
                    backgroundResource = R.drawable.quoted_tweet_frame

                    textView {
                        id = R.id.quoted_display_name
                        textSize = 12f //sp
                        setTypeface(typeface, Typeface.BOLD)
                        text = qs.user.name
                    }.lparams {
                        bottomMargin = dip(6)
                    }

                    textView {
                        id = R.id.quoted_screen_name
                        textColor = Color.parseColor("#666666")
                        textSize = 10f //sp
                        text = "@" + qs.user.screenName
                        lines = 1
                        ellipsize = TextUtils.TruncateAt.END
                    }.lparams {
                        leftMargin = dip(4)
                        rightOf(R.id.quoted_display_name)
                        baselineOf(R.id.quoted_display_name)
                    }

                    textView {
                        id = R.id.quoted_status
                        textSize = 12f //sp
                        text = qs.text
                    }.lparams {
                        below(R.id.quoted_display_name)
                    }

                    if (preferences.display.tweet.shouldShowThumbnail) {
                        frameLayout {
                            id = R.id.quoted_images_container_wrapper

                            val container = linearLayout {
                                id = R.id.quoted_images_container
                                orientation = LinearLayout.VERTICAL
                            }

                            val play = fontelloTextView {
                                id = R.id.quoted_play
                                text = resources.getString(R.string.fontello_play)
                                textColor = Color.parseColor("#ffffff")
                                textSize = 24f //sp
                            }.lparams {
                                gravity = Gravity.CENTER
                            }

                            qs.let {
                                ImageUtil.displayThumbnailImages(fragmentActivity, container, this, play, it)
                            }
                        }.lparams {
                            below(R.id.quoted_status)
                            bottomMargin = dip(4)
                            topMargin = dip(10)
                        }
                    }
                    setOnClickListener {
                        startActivity<StatusActivity>("status" to qs.toJsonString())
                    }
                }.lparams(width = matchParent) {
                    below(R.id.status)
                    rightOf(R.id.icon)
                    if (s.quotedStatus != null) {
                        topMargin = dip(10)
                        bottomMargin = dip(4)
                    }
                }
                bottom = R.id.quoted_tweet
            }

            if (preferences.display.tweet.shouldShowThumbnail) {
                frameLayout {
                    id = R.id.images_container_wrapper

                    val container = linearLayout {
                        id = R.id.images_container
                        orientation = LinearLayout.VERTICAL
                    }

                    val play = fontelloTextView {
                        id = R.id.play
                        text = resources.getString(R.string.fontello_play)
                        textColor = Color.parseColor("#ffffff")
                        textSize = 24f //sp
                    }.lparams {
                        gravity = Gravity.CENTER
                    }

                    ImageUtil.displayThumbnailImages(fragmentActivity, container, this, play, s)
                }.lparams {
                    below(bottom)
                    rightOf(R.id.icon)
                    bottomMargin = dip(4)
                    topMargin = dip(10)
                }
                bottom = R.id.images_container_wrapper
            }

            relativeLayout {
                id = R.id.menu_and_via_container

                fontelloTextView {
                    id = R.id.do_reply
                    padding = dip(6)
                    text = resources.getString(R.string.fontello_reply)
                    textColor = Color.parseColor("#666666")
                    textSize = 14f
                    setOnClickListener {
                        ActionUtil.doReplyAll(s, fragmentActivity)
                    }
                }

                fontelloTextView {
                    id = R.id.do_retweet
                    topPadding = dip(6)
                    rightPadding = dip(4)
                    bottomPadding = dip(6)
                    leftPadding = dip(6)
                    text = resources.getString(R.string.fontello_retweet)
                    textColor = if (s.isRetweeted()) ContextCompat.getColor(fragmentActivity, R.color.holo_green_light)
                            else Color.parseColor("#666666")
                    textSize = 14f
                    setOnClickListener {
                        if (s.user.protected && s.user.id != currentIdentifier.userId) {
                            MessageUtil.showToast(R.string.toast_protected_tweet_can_not_share)
                            return@setOnClickListener
                        }

                        (if (s.isRetweeted()) {
                            DestroyRetweetDialogFragment().apply {
                                arguments = s.generateJsonBundle()
                            }
                        } else {
                            RetweetDialogFragment().apply {
                                arguments = s.generateJsonBundle()
                            }
                        }).show(fragmentActivity.supportFragmentManager, "dialog")
                    }
                }.lparams {
                    rightOf(R.id.do_reply)
                    leftMargin = dip(22)
                }

                textView {
                    id = R.id.retweet_count
                    textSize = 10f
                    if (s.retweetCount > 0) {
                        bottomPadding = dip(6)
                        topPadding = dip(6)
                        textColor = Color.parseColor("#999999")
                        text = s.retweetCount.omitCount()
                    }
                }.lparams(width = dip(32)) {
                    rightOf(R.id.do_retweet)
                }

                fontelloTextView {
                    id = R.id.do_fav
                    topPadding = dip(6)
                    rightPadding = dip(4)
                    bottomPadding = dip(6)
                    leftPadding = dip(2)
                    text = resources.getString(R.string.fontello_star)
                    textColor = if (s.isFavorited()) {
                        ContextCompat.getColor(fragmentActivity, R.color.holo_orange_light)
                    } else {
                        Color.parseColor("#666666")
                    }
                    textSize = 14f
                    setOnClickListener {
                        launch {
                            if (s.isFavorited()) {
                                s.unfavorite()
                            } else {
                                s.favorite()
                            }
                        }
                    }
                }.lparams {
                    rightOf(R.id.retweet_count)
                }

                textView {
                    id = R.id.fav_count
                    textSize = 10f
                    if (s.favoriteCount > 0) {
                        bottomPadding = dip(6)
                        topPadding = dip(6)
                        textColor = Color.parseColor("#999999")
                        text = s.favoriteCount.omitCount()
                    }
                }.lparams {
                    rightOf(R.id.do_fav)
                }

                textView {
                    id = R.id.via
                    bottomPadding = dip(2)
                    textColor = Color.parseColor("#666666")
                    textSize = 8f //sp
                    text = "via ${s.via.name}"
                }.lparams {
                    alignParentRight()
                }

                textView {
                    id = R.id.datetime
                    textColor = Color.parseColor("#666666")
                    textSize = 10f //sp
                    text = s.createdAtString
                }.lparams {
                    below(R.id.via)
                    alignParentRight()
                }
            }.lparams {
                below(bottom)
                rightOf(R.id.icon)
            }

            if (status.retweetedStatus != null) {
                relativeLayout {
                    id = R.id.retweet_container

                    imageView {
                        id = R.id.retweet_icon
                        contentDescription = resources.getString(R.string.description_icon)
                        displayUserIcon(status.user)
                    }.lparams(width = dip(18), height = dip(18)) {
                        rightMargin = dip(4)
                    }

                    textView {
                        id = R.id.retweet_by
                        textSize = 10f //sp
                        text = "RT by ${status.user.name} @ ${status.user.screenName}"
                    }.lparams {
                        rightOf(R.id.retweet_icon)
                        topMargin = dip(2)
                    }

                }.lparams {
                    rightOf(R.id.icon)
                    below(R.id.menu_and_via_container)
                    bottomMargin = dip(2)
                }
            }

            if (StatusUtil.isMentionForMe(s).not() && s.retweetedStatus == null) actionContainer.visibility = View.GONE
        }
    }

    private inner class RowData(val textId: Int, val textColor: Int, val displayName: String, screenNameParam: String) {
        val screenName: String = "@$screenNameParam"
    }
}