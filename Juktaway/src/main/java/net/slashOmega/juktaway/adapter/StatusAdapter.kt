package net.slashOmega.juktaway.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.support.v4.util.LongSparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.settings.mute.Mute
import net.slashOmega.juktaway.util.ImageUtil
import net.slashOmega.juktaway.widget.FontelloTextView
import org.jetbrains.anko.*
import twitter4j.Status

/**
 * Created on 2018/11/13.
 */

class StatusAdapter(val mContext: Context, private val mLayout: Int) : ArrayAdapter<Row>(mContext, mLayout) {
    private var mColorBlue = 0
    private val limit = 100
    private var mLimit = limit
    private val mIdSet = mutableSetOf<Long>()

    fun extensionAdd(row: Row) {
        if (Mute.contains(row)) {
            return
        }
        if (exists(row)) {
            return
        }
        super.add(row)
        if (row.isStatus) {
            mIdSet.add(row.status!!.id)
        }
        filter(row)
        mLimit++
    }

    override fun add(row: Row) {
        if (row in Mute || exists(row)) {
            return
        }
        super.add(row)
        if (row.isStatus) mIdSet.add(row.status!!.id)
        filter(row)
        limitation()
    }

    override fun insert(row: Row, index: Int) {
        if (row in Mute || exists(row)) {
            return
        }
        super.insert(row, index)
        if (row.isStatus) mIdSet.add(row.status!!.id)
        filter(row)
        limitation()
    }

    override fun remove(row: Row) {
        super.remove(row)
        if (row.isStatus) mIdSet.add(row.status!!.id)
    }

    private fun exists(row: Row) = row.isStatus && row.status!!.id in mIdSet

    private fun filter(row: Row) {
        row.status?.takeIf { it.isRetweeted }?.let { status ->
            status.retweetedStatus?.takeIf { status.user.id == AccessTokenManager.getUserId() }?.let { retweet ->
                FavRetweetManager.setRtId(retweet.id, status.id)
            }
        }
    }

    fun replaceStatus(status: Status) {
        for (i in 0 until count) {
            val row = getItem(i)
            if (!row!!.isDirectMessage && row.status!!.id == status.id) {
                row.status = status
                notifyDataSetChanged()
                break
            }
        }
    }

    fun removeStatus(id: Long): List<Int> {
        var pos = 0
        val positions = mutableListOf<Int>()
        val rows = mutableListOf<Row>()

        for(i in 0 until count) {
            val row = getItem(i)?.takeUnless { it.isDirectMessage } ?: continue
            val status = row.status
            val retweet = status!!.retweetedStatus
            if (status.id == id || (retweet != null && retweet.id == id)) {
                rows.add(row)
                positions.add(pos)
            }
            pos++
        }
        for (row in rows) {
            remove(row)
        }
        return positions
    }

    fun removeDirectMessage(directMessageId: Long) {
        for (i in 0 until count) {
            val row = getItem(i)?.takeUnless { it.isDirectMessage } ?: continue
            if (row.message!!.id == directMessageId) {
                remove(row)
                break
            }
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
        val row = getItem(position)!!
        val user = row.status?.user ?: row.message!!.sender

        relativeLayout {
            lparams(matchParent, android.R.attr.listPreferredItemHeight)

            relativeLayout {
                bottomPadding = dip(3)
                leftPadding = dip(6)
                rightPadding = dip(7)
                topPadding = dip(4)
                //tools:layout_height = wrap_content //not support attribute

                relativeLayout {
                    id = R.id.action_container
                    fontelloTextView {
                        id = R.id.action_icon
                        gravity = Gravity.RIGHT
                        textSize = 12f //sp
                        //tools:text = �� //not support attribute
                    }.lparams(width = dip(48)) {
                        rightMargin = dip(6)
                        bottomMargin = dip(2)
                    }

                    textView {
                        id = R.id.action_by_display_name
                        textSize = 12f //sp
                        setTypeface(typeface, Typeface.BOLD)
                        //tools:text = Justaway Factory //not support attribute
                    }.lparams {
                        rightOf(R.id.action_icon)
                    }

                    textView {
                        id = R.id.action_by_screen_name
                        textColor = Color.parseColor("#666666")
                        textSize = 10f //sp
                        //tools:ignore = SmallSp //not support attribute
                        //tools:text = \@justawayfactory //not support attribute
                    }.lparams {
                        rightOf(R.id.action_by_display_name)
                        leftMargin = dip(4)
                    }
                }.lparams(width = matchParent)

                imageView {
                    id = R.id.icon
                    topPadding = dip(2)
                    contentDescription = resources.getString(R.string.description_icon)
                    //tools:src = @drawable/ic_launcher //not support attribute
                }.lparams(width = dip(48), height = dip(48)) {
                    below(R.id.action_container)
                    bottomMargin = dip(6)
                    rightMargin = dip(6)
                    topMargin = dip(1)
                }

                textView {
                    id = R.id.display_name
                    textSize = 12f //sp
                    setTypeface(typeface, Typeface.BOLD)
                    //tools:text = Justaway Factory //not support attribute
                }.lparams {
                    below(R.id.action_container)
                    rightOf(R.id.icon)
                    bottomMargin = dip(6)
                }

                textView {
                    id = R.id.screen_name
                    textColor = Color.parseColor("#666666")
                    textSize = 10f //sp
                    //android:lines = 1 //not support attribute
                    //android:ellipsize = end //not support attribute
                    //tools:ignore = SmallSp //not support attribute
                    //tools:text = \@justawayfactory //not support attribute
                }.lparams {
                    leftMargin = dip(4)
                    rightOf(R.id.display_name)
                    baselineOf(R.id.display_name)
                }

                fontelloTextView {
                    id = R.id.lock
                    text = resources.getString(R.string.fontello_lock)
                    textColor = Color.parseColor("#666666")
                    textSize = 10f //sp
                    //tools:ignore = SmallSp //not support attribute
                }.lparams {
                    rightOf(R.id.screen_name)
                    baselineOf(R.id.display_name)
                    leftMargin = dip(4)
                }

                textView {
                    id = R.id.datetime_relative
                    textColor = Color.parseColor("#666666")
                    textSize = 10f //sp
                    //tools:ignore = SmallSp //not support attribute
                    //tools:text = 2H //not support attribute
                }.lparams {
                    alignParentRight()
                    baselineOf(R.id.display_name)
                }

                textView {
                    id = R.id.status
                    textSize = 12f //sp
                    //tools:text = Hello World. //not support attribute
                }.lparams {
                    rightOf(R.id.icon)
                    below(R.id.display_name)
                }

                relativeLayout {
                    id = R.id.quoted_tweet
                    padding = dip(10)
                    backgroundResource = R.drawable.quoted_tweet_frame

                    textView {
                        id = R.id.quoted_display_name
                        textSize = 12f //sp
                        setTypeface(typeface, Typeface.BOLD)
                        //tools:text = Justaway Factory //not support attribute
                    }.lparams {
                        bottomMargin = dip(6)
                    }

                    textView {
                        id = R.id.quoted_screen_name
                        textColor = Color.parseColor("#666666")
                        textSize = 10f //sp
                        //android:lines = 1 //not support attribute
                        //android:ellipsize = end //not support attribute
                        //tools:ignore = SmallSp //not support attribute
                        //tools:text = \@justawayfactory //not support attribute
                    }.lparams {
                        leftMargin = dip(4)
                        rightOf(R.id.quoted_display_name)
                        baselineOf(R.id.quoted_display_name)
                    }

                    textView {
                        id = R.id.quoted_status
                        textSize = 12f //sp
                        //tools:text = Hello World. //not support attribute
                    }.lparams {
                        below(R.id.quoted_display_name)
                    }

                    frameLayout {
                        id = R.id.quoted_images_container_wrapper

                        linearLayout {
                            id = R.id.quoted_images_container
                            orientation = LinearLayout.VERTICAL
                        }

                        fontelloTextView {
                            id = R.id.quoted_play
                            text = resources.getString(R.string.fontello_play)
                            textColor = Color.parseColor("#ffffff")
                            textSize = 24f //sp
                        }.lparams {
                            gravity = Gravity.CENTER
                        }
                    }.lparams {
                        below(R.id.quoted_status)
                        bottomMargin = dip(4)
                        topMargin = dip(10)
                    }
                }.lparams(width = matchParent) {
                    below(R.id.status)
                    rightOf(R.id.icon)
                    topMargin = dip(10)
                    bottomMargin = dip(4)
                }

                frameLayout {
                    id = R.id.images_container_wrapper

                    linearLayout {
                        id = R.id.images_container
                        orientation = LinearLayout.VERTICAL
                    }

                    fontelloTextView {
                        id = R.id.play
                        text = resources.getString(R.string.fontello_play)
                        textColor = Color.parseColor("#ffffff")
                        textSize = 24f //sp
                    }.lparams {
                        gravity = Gravity.CENTER
                    }
                }.lparams {
                    below(R.id.quoted_tweet)
                    rightOf(R.id.icon)
                    bottomMargin = dip(4)
                    topMargin = dip(10)
                }

                relativeLayout {
                    id = R.id.menu_and_via_container

                    fontelloTextView {
                        id = R.id.do_reply
                        padding = dip(6)
                        text = resources.getString(R.string.fontello_reply)
                        textColor = Color.parseColor("#666666")
                        textSize = dip(14).toFloat()
                        //tools:ignore = SpUsage //not support attribute
                    }

                    fontelloTextView {
                        id = R.id.do_retweet
                        topPadding = dip(6)
                        rightPadding = dip(4)
                        bottomPadding = dip(6)
                        leftPadding = dip(6)
                        text = resources.getString(R.string.fontello_retweet)
                        textColor = Color.parseColor("#666666")
                        textSize = dip(14).toFloat()
                        //tools:ignore = SpUsage //not support attribute
                    }.lparams {
                        rightOf(R.id.do_reply)
                        leftMargin = dip(22)
                    }

                    textView {
                        id = R.id.retweet_count
                        bottomPadding = dip(6)
                        topPadding = dip(6)
                        textColor = Color.parseColor("#999999")
                        textSize = dip(10).toFloat()
                        //tools:ignore = SmallSp,SpUsage //not support attribute
                        //tools:text = 12345 //not support attribute
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
                        textColor = Color.parseColor("#666666")
                        textSize = dip(14).toFloat()
                        //tools:ignore = SpUsage //not support attribute
                    }.lparams {
                        rightOf(R.id.retweet_count)
                    }

                    textView {
                        id = R.id.fav_count
                        bottomPadding = dip(6)
                        topPadding = dip(6)
                        textColor = Color.parseColor("#999999")
                        textSize = dip(10).toFloat()
                        //tools:ignore = SpUsage //not support attribute
                        //tools:text = 12345 //not support attribute
                    }.lparams {
                        rightOf(R.id.do_fav)
                    }

                    textView {
                        id = R.id.via
                        bottomPadding = dip(2)
                        textColor = Color.parseColor("#666666")
                        textSize = 8f //sp
                        //tools:ignore = SmallSp //not support attribute
                        //tools:text = via Justaway for Android //not support attribute
                    }.lparams {
                        alignParentRight()
                    }

                    textView {
                        id = R.id.datetime
                        textColor = Color.parseColor("#666666")
                        textSize = 10f //sp
                        //tools:ignore = SmallSp //not support attribute
                        //tools:text = 2014/01/23 15:14:30 //not support attribute
                    }.lparams {
                        below(R.id.via)
                        alignParentRight()
                    }
                }.lparams {
                    below(R.id.images_container_wrapper)
                    rightOf(R.id.icon)
                }

                relativeLayout {
                    id = R.id.retweet_container
                    imageView {
                        id = R.id.retweet_icon
                        contentDescription = resources.getString(R.string.description_icon)
                        //tools:src = @drawable/ic_launcher //not support attribute
                    }.lparams(width = dip(18), height = dip(18)) {
                        rightMargin = dip(4)
                    }
                    
                    textView {
                        id = R.id.retweet_by
                        textSize = 10f //sp
                        //tools:ignore = SmallSp //not support attribute
                        //tools:text = \@su_aska //not support attribute
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