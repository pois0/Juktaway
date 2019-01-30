package net.slash_omega.juktaway.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.util.LongSparseArray
import android.widget.ListView
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.extensions.models.fullText
import jp.nephy.penicillin.extensions.parseModel
import jp.nephy.penicillin.extensions.via
import jp.nephy.penicillin.models.DirectMessage
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.ProfileActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.SearchActivity
import net.slash_omega.juktaway.fragment.AroundFragment
import net.slash_omega.juktaway.fragment.RetweetersFragment
import net.slash_omega.juktaway.fragment.TalkFragment
import net.slash_omega.juktaway.model.FavRetweetManager
import net.slash_omega.juktaway.model.Row
import net.slash_omega.juktaway.settings.mute.SourceMute
import net.slash_omega.juktaway.settings.mute.UserMute
import net.slash_omega.juktaway.settings.mute.WordMute
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.util.*
import org.jetbrains.anko.startActivity

/**
 * Created on 2018/10/27.
 */
class StatusMenuFragment: DialogFragment() {
    companion object {
        fun newInstance(row: Row) = StatusMenuFragment().apply {
            arguments = Bundle().apply {
                if (row.isDirectMessage) {
                    putString("directMessage", row.message!!.toJsonString())
                } else {
                    putString("status", row.status!!.toJsonString())
                }
                if (row.isFavorite) {
                    putString("favoriteSourceUser", row.source!!.toJsonString())
                }
            }
        }
    }

    private lateinit var mActivity: FragmentActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mActivity = activity!!
        ThemeUtil.setTheme(activity!!)
        val adapter = MenuAdapter(mActivity, R.layout.row_menu)
        return AlertDialog.Builder(mActivity)
                .setView(ListView(mActivity).apply {
                    setAdapter(adapter)
                    setOnItemClickListener { _, _, i, _ ->
                        adapter.getItem(i)?.callback?.run()
                    }
                }).let {
                    val message = arguments?.getString("directMessage")?.toJsonObject()?.parseModel<DirectMessage>()
                    if (message != null) onDirectMessage(message, adapter, it)
                    else onStatus(arguments!!.getString("status")!!.toJsonObject().parseModel(), adapter, it)
                }
                .create()
    }



    private fun onDirectMessage(message: DirectMessage, adapter: MenuAdapter, builder: AlertDialog.Builder) = adapter.run {
        builder.setTitle(message.senderScreenName)
        /*
         * 返信(DM)
         */
        add(R.string.context_menu_reply_direct_message) {
            ActionUtil.doReplyDirectMessage(message, mActivity)
            dismiss()
        }

        /*
         * ツイ消し(DM)
         */
        add(R.string.context_menu_destroy_direct_message) {
            GlobalScope.launch(Dispatchers.Main) {
                ActionUtil.destroyDirectMessage(message.id)
                dismiss()
            }
        }

        /*
         * ツイート内のメンション
         */
        for (mention in message.entities.userMentions) {
            add("@" + mention.screenName) {
                val intent = Intent(mActivity, ProfileActivity::class.java)
                intent.putExtra("screenName", mention.screenName)
                mActivity.startActivity(intent)
            }
        }

        /*
         * ツイート内のURL
         */
        val urls = message.entities.urls.map { it.expandedUrl }
        addUrls(adapter, urls)
        builder
    }

    private fun onStatus(status: Status, adapter: MenuAdapter, builder: AlertDialog.Builder) = adapter.run {
        val retweet = status.retweetedStatus
        val source = retweet ?: status
        val mentions = source.entities.userMentions
        val isPublic = !source.user.protected

        builder.setTitle(status.fullText())

        /*
         * リプ
         */
        add(R.string.context_menu_reply) {
            ActionUtil.doReply(source, mActivity)
            dismiss()
        }

        /*
         * 全員にリプ
         */
        if (mentions.size > 1 || mentions.size == 1 && mentions[0].screenName != currentIdentifier.screenName) {
            add(R.string.context_menu_reply_all) {
                ActionUtil.doReplyAll(source, mActivity)
                dismiss()
            }
        }

        /*
         * 引用
         */
        if (isPublic) {
            add(R.string.context_menu_qt) {
                ActionUtil.doQuote(source, mActivity)
                dismiss()
            }
        }

        /*
         * ふぁぼ / あんふぁぼ
         */
        if (FavRetweetManager.isFav(status)) {
            add(R.string.context_menu_destroy_favorite) {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doDestroyFavorite(status.id)
                    dismiss()
                }
            }
        } else {
            add(R.string.context_menu_create_favorite) {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doFavorite(status.id)
                    dismiss()
                }
            }
        }

        /*
         * 自分のツイートまたはRT
         */
        if (status.user.id == currentIdentifier.userId) {

            /*
             * ツイ消し
             */
            add(R.string.context_menu_destroy_status) {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doDestroyStatus(status.id)
                    dismiss()
                }
            }
        }

        /*
         * 自分がRTした事があるツイート
         */
        if (FavRetweetManager.getRtId(status) != null) {

            /*
             * RT解除
             */
            add(R.string.context_menu_destroy_retweet) {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doDestroyRetweet(status)
                    dismiss()
                }
            }
        } else {

            /*
             * 非鍵垢
             */
            if (isPublic) {

                /*
                 * 未ふぁぼ
                 */
                if (!FavRetweetManager.isFav(status)) {

                    /*
                     * ふぁぼ＆RT
                     */
                    add(R.string.context_menu_favorite_and_retweet) {
                        GlobalScope.launch(Dispatchers.Main) {
                            ActionUtil.doFavorite(status.id)
                            ActionUtil.doRetweet(status.id)
                            dismiss()
                        }
                    }
                }

                /*
                 * RT
                 */
                add(R.string.context_menu_retweet) {
                    GlobalScope.launch(Dispatchers.Main) {
                        ActionUtil.doRetweet(status.id)
                        dismiss()
                    }
                }
            }
        }

        /*
         * RTある時
         */
        if (source.retweetCount > 0) {

            /*
             * RTした人を表示
             */
            add(R.string.context_menu_show_retweeters) {
                RetweetersFragment().apply {
                    arguments = Bundle().apply { putLong("statusId", source.id) }
                }.show(mActivity.supportFragmentManager, "dialog")
            }
        }

        /*
         * 会話を表示
         */
        add(R.string.context_menu_talk) {
            TalkFragment().apply {
                arguments = Bundle().apply { putString("status", source.toJsonString()) }
            }.show(mActivity.supportFragmentManager, "dialog")
        }

        /*
         * 前後のツイートを表示
         */
        add(R.string.context_menu_show_around) {
            AroundFragment().apply {
                arguments = Bundle().apply { putString("status", source.toJsonString()) }
            }.show(mActivity.supportFragmentManager, "dialog")
        }

        /*
         * ツイート内のURL
         */
        val urls = source.entities.urls.map { it.expandedUrl }
        addUrls(adapter, urls)

        /*
         * ツイート内のURL(画像)
         */
        val medias = source.entities.media.map { it.expandedUrl }
        addUrls(adapter, medias)

        /*
         * ツイート内のハッシュタグ
         */
        val hashtags = source.entities.hashtags
        for (hashtag in hashtags) {
            add("#" + hashtag.text) {
                mActivity.startActivity<SearchActivity>("query" to "#" + hashtag.text)
            }
        }

        val users = LongSparseArray<Boolean>()

        /*
         * ツイートした人
         */
        users.put(source.user.id, true)
        add("@" + source.user.screenName) {
            mActivity.startActivity<ProfileActivity>("userJson" to source.user.toJsonString())
        }

        /*
         * ふぁぼした人
         */
        val favoriteSourceUser = arguments!!.getString("favoriteSourceUser")?.toJsonObject()?.parseModel<User>()
        if (favoriteSourceUser != null) {
            users.put(favoriteSourceUser.id, true)
            add("@" + favoriteSourceUser.screenName) {
                mActivity.startActivity(Intent(mActivity, ProfileActivity::class.java).apply {
                    putExtra("screenName", favoriteSourceUser.screenName)
                })
            }
        }

        /*
         * RTした人
         */
        if (retweet != null && users.get(status.user.id) == null) {
            users.put(status.user.id, true)
            add("@" + status.user.screenName) {
                mActivity.startActivity<ProfileActivity>("userJson" to status.user.toJsonString())
            }
        }

        /*
         * ツイート内のメンション
         */
        for (mention in mentions) {
            if (users.get(mention.id) != null) continue

            users.put(mention.id, true)
            add("@" + mention.screenName) {
                mActivity.startActivity<ProfileActivity>("screenName" to mention.screenName)
            }
        }

        /*
         * 非鍵垢
         */
        if (isPublic) {

            /*
             * ツイートを共有
             */
            add(R.string.context_menu_share_url) {
                mActivity.startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, source.uri)
                })
            }
        }

        /*
         * ツイートを開く
         */
        add(R.string.context_menu_open_other_apps) {
            mActivity.startActivity(Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/" + source.user.screenName
                            + "/status/" + source.id.toString())
            ))
        }

        /*
         * viaをミュート
         */
        add(String.format(mActivity.getString(R.string.context_menu_mute), StatusUtil.getClientName(source.via.name))) {
            AlertDialog.Builder(activity)
                    .setMessage(String.format(getString(R.string.context_create_mute), StatusUtil.getClientName(source.via.name)))
                    .setPositiveButton(R.string.button_ok) { _, _->
                        SourceMute += StatusUtil.getClientName(source.via.name)
                        MessageUtil.showToast(R.string.toast_create_mute)
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .show()
        }

        /*
         * ハッシュタグをミュート
         */
        for (hashtag in hashtags) {
            add(String.format(mActivity.getString(R.string.context_menu_mute), "#" + hashtag.text)) {
                AlertDialog.Builder(activity)
                        .setMessage(String.format(getString(R.string.context_create_mute), "#" + hashtag.text))
                        .setPositiveButton(R.string.button_ok) { _, _ ->
                            WordMute += "#" + hashtag.text
                            MessageUtil.showToast(R.string.toast_create_mute)
                            dismiss()
                        }
                        .setNegativeButton(R.string.button_cancel) { _, _-> }
                        .show()
            }
        }

        /*
         * ユーザーをミュート
         */
        add(String.format(mActivity.getString(R.string.context_menu_mute), "@" + source.user.screenName)) {
            AlertDialog.Builder(activity)
                    .setMessage(String.format(getString(R.string.context_create_mute), "@" + source.user.screenName))
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                        UserMute += source.user
                        MessageUtil.showToast(R.string.toast_create_mute)
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .show()
        }
        builder
    }

    private fun addUrls(adapter: MenuAdapter, urls: List<String>) {
        for (url in urls) {
            adapter.add(Menu(url, Runnable {
                mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                dismiss()
            }))
        }
    }
}