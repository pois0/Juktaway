package net.slash_omega.juktaway.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import jp.nephy.jsonkt.stringify
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.extensions.via
import jp.nephy.penicillin.models.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.slash_omega.juktaway.ProfileActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.StatusActivity
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
        private val fav_image = if (preferences.display.tweet.isFavoriteButtonHeartShaped) {
            R.drawable.ic_heart
        } else {
            R.drawable.ic_star
        }

        private val grayColor = Color.parseColor("#666666")
        private val darkGrayColor = Color.parseColor("#444444")
        private val lightGrayColor = Color.parseColor("#999999")

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

    private val greenColor = ContextCompat.getColor(fragmentActivity, R.color.holo_green_light)
    private val orangeColor = ContextCompat.getColor(fragmentActivity, R.color.holo_orange_light)

    private val favColor = if (preferences.display.tweet.isFavoriteButtonHeartShaped) {
        Color.parseColor("#E0245E")
    } else {
        orangeColor
    }

    private val limit = 100
    private var mLimit = limit
    private val mIdSet = Collections.synchronizedSet(mutableSetOf<Long>())

    private val shouldShow get() = { status: Status -> status !in Mute && mIdSet.add(status.id) }
    private val fontSize = preferences.display.general.fontSize.toFloat()

    suspend fun extensionAddAllFromStatuses(statusesParam: List<Status>) = withContext(Dispatchers.Main) {
        val statuses = withContext(Dispatchers.Default) {
            statusesParam.filter(shouldShow)
        }

        super.addAll(statuses)
        mLimit += statuses.size
    }

    override fun add(status: Status) {
        launch { addSuspend(status) }
    }

    suspend fun addSuspend(status: Status) = withContext(Dispatchers.Main) {
        if (withContext(Dispatchers.Default) { shouldShow(status) }) {
            super.add(status)
            limitation()
        }
    }

    suspend fun addAllSuspend(statusesParam: List<Status>) = withContext(Dispatchers.Main) {
        val statuses = withContext(Dispatchers.Default) {
            statusesParam.filter(shouldShow)
        }

        super.addAll(statuses)
    }

    override fun insert(status: Status, index: Int) {
        launch { insertSuspend(status, index) }
    }

    suspend fun insertSuspend(status: Status, index: Int) = withContext(Dispatchers.Main) {
        if (withContext(Dispatchers.Default) { shouldShow(status) }) {
            super.insert(status, index)
        }
        // limitation()
    }

    suspend fun insertAllFromStatus(statusesParam: Collection<Status>, index: Int): Int = withContext(Dispatchers.Main) {
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
        for (i in 0 until count) {
            getItem(i)!!.takeIf { it.id == id || it.retweetedStatus?.id == id }
                    ?.let{remove(it)}
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
        pre
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = parent.context.run {
        val status = getItem(position)!!
        val s = status.retweetedStatus ?: status
        relativeLayout {
            bottomPadding = dip(3)
            leftPadding = dip(6)
            rightPadding = dip(7)
            topPadding = dip(4)
            if (s.isMentionForMe) setBackgroundColor(Color.parseColor("#20ff2020"))

            if (preferences.display.tweet.shouldShowAuthorIcon) {
                imageView {
                    id = R.id.icon
                    contentDescription = resources.getString(R.string.description_icon)
                    setOnClickListener {
                        startActivity(it.context.intentFor<ProfileActivity>("userJson" to s.user.stringify()))
                    }

                    displayUserIcon(s.user)
                }.lparams(width = dip(48), height = dip(48)) {
                    bottomMargin = dip(6)
                    rightMargin = dip(6)
                    topMargin = dip(1)
                }
            } else {
                space {
                    id = R.id.icon
                    topPadding = dip(2)
                }.lparams(width = 0, height = 0) {
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
                rightOf(R.id.icon)
                bottomMargin = dip(6)
            }

            textView {
                id = R.id.screen_name
                textColor = grayColor
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
                    setColorFilter(grayColor)
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
                    setColorFilter(grayColor)
                }.lparams(width = dip(10), height = dip(10)) {
                    endOf(nameEnd)
                    sameBottom(R.id.screen_name)
                    leftMargin = dip(4)
                    bottomMargin = dip(2)
                }
            }

            textView {
                id = R.id.datetime_relative
                textColor = grayColor
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
                setTextFromStatus(s, context)
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
                        textColor = grayColor
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
                        setTextFromStatus(qs, context)
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

                            val play = imageView {
                                id = R.id.quoted_play
                                setImageResource(R.drawable.ic_play_circle)
                                setColorFilter(Color.WHITE)
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
                        startActivity<StatusActivity>("status" to qs.stringify())
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

                    val play = imageView {
                        id = R.id.play
                        setImageResource(R.drawable.ic_play_circle)
                        setColorFilter(Color.WHITE)
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

                imageView(R.drawable.ic_reply) {
                    id = R.id.do_reply
                    padding = dip(6)
                    setColorFilter(grayColor)
                    setOnClickListener {
                        ActionUtil.doReplyAll(s, fragmentActivity)
                    }
                }.lparams(width = dip(28), height = dip(28))

                imageView(R.drawable.ic_retweet) {
                    id = R.id.do_retweet
                    topPadding = dip(6)
                    rightPadding = dip(3)
                    bottomPadding = dip(6)
                    leftPadding = dip(9)
                    setColorFilter(when {
                        s.isRetweeted() -> greenColor
                        s.user.protected && s.user.id != currentIdentifier.userId -> darkGrayColor
                        else -> grayColor
                    })
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
                }.lparams(width = dip(28), height = dip(28)) {
                    rightOf(R.id.do_reply)
                    leftMargin = dip(22)
                }

                textView {
                    id = R.id.retweet_count
                    textSize = 10f
                    if (s.retweetCount > 0) {
                        bottomPadding = dip(6)
                        topPadding = dip(7)
                        textColor = lightGrayColor
                        text = s.retweetCount.omitCount()
                    }
                }.lparams(width = dip(32)) {
                    rightOf(R.id.do_retweet)
                }

                imageView(fav_image) {
                    id = R.id.do_fav
                    topPadding = dip(6)
                    rightPadding = dip(4)
                    leftPadding = dip(8)
                    bottomPadding = dip(6)
                    setColorFilter(if (s.isFavorited()) favColor else grayColor)
                    setOnClickListener {
                        launch {
                            if (s.isFavorited()) s.unfavorite() else s.favorite()
                        }
                    }
                }.lparams(width = dip(28), height = dip(28)) {
                    rightOf(R.id.retweet_count)
                }

                textView {
                    id = R.id.fav_count
                    textSize = 10f
                    if (s.favoriteCount > 0) {
                        bottomPadding = dip(6)
                        topPadding = dip(7)
                        textColor = lightGrayColor
                        text = s.favoriteCount.omitCount()
                    }
                }.lparams {
                    rightOf(R.id.do_fav)
                }

                textView {
                    id = R.id.via
                    bottomPadding = dip(2)
                    textColor = grayColor
                    textSize = 8f //sp
                    text = "via ${s.via.name}"
                }.lparams {
                    alignParentRight()
                }

                textView {
                    id = R.id.datetime
                    textColor = grayColor
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
                        text = "RT by ${status.user.name} @${status.user.screenName}"
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
        }
    }
}
