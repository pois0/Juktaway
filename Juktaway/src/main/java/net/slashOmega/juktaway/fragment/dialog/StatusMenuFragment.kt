package net.slashOmega.juktaway.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.util.LongSparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.extensions.models.fullText
import jp.nephy.penicillin.extensions.via
import jp.nephy.penicillin.models.DirectMessage
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.ProfileActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.SearchActivity
import net.slashOmega.juktaway.adapter.ArrayAdapterBase
import net.slashOmega.juktaway.fragment.AroundFragment
import net.slashOmega.juktaway.fragment.RetweetersFragment
import net.slashOmega.juktaway.fragment.TalkFragment
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.settings.mute.SourceMute
import net.slashOmega.juktaway.settings.mute.UserMute
import net.slashOmega.juktaway.settings.mute.WordMute
import net.slashOmega.juktaway.twitter.currentIdentifier
import net.slashOmega.juktaway.util.*
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
                        val message = arguments?.getString("directMessage")?.toJsonObject()?.parse<DirectMessage>()
                        if (message != null) onDirectMessage(message, adapter, it)
                        else onStatus(arguments!!.getString("status")!!.toJsonObject().parse(), adapter, it)
                    }
                .create()
    }



    private fun onDirectMessage(message: DirectMessage, adapter: MenuAdapter, builder: AlertDialog.Builder) = builder.apply {
        setTitle(message.senderScreenName)
        /*
         * 返信(DM)
         */
        adapter.add(Menu(R.string.context_menu_reply_direct_message, Runnable {
            ActionUtil.doReplyDirectMessage(message, mActivity)
            dismiss()
        }))

        /*
         * ツイ消し(DM)
         */
        adapter.add(Menu(R.string.context_menu_destroy_direct_message, Runnable {
            GlobalScope.launch(Dispatchers.Main) {
                ActionUtil.destroyDirectMessage(message.id)
                dismiss()
            }
        }))

        /*
         * ツイート内のメンション
         */
        for (mention in message.entities.userMentions) {
            adapter.add(Menu("@" + mention.screenName, Runnable {
                val intent = Intent(mActivity, ProfileActivity::class.java)
                intent.putExtra("screenName", mention.screenName)
                mActivity.startActivity(intent)
            }))
        }

        /*
         * ツイート内のURL
         */
        val urls = message.entities.urls.map { it.expandedUrl }
        addUrls(adapter, urls)
    }

    private fun onStatus(status: Status, adapter: MenuAdapter, builder: AlertDialog.Builder) = builder.apply {
        val retweet = status.retweetedStatus
        val source = retweet ?: status
        val mentions = source.entities.userMentions
        val isPublic = !source.user.protected

        setTitle(status.fullText())

        /*
         * リプ
         */
        adapter.add(Menu(R.string.context_menu_reply, Runnable {
            ActionUtil.doReply(source, mActivity)
            dismiss()
        }))

        /*
         * 全員にリプ
         */
        if (mentions.size > 1 || mentions.size == 1 && mentions[0].screenName != currentIdentifier.screenName) {
            adapter.add(Menu(R.string.context_menu_reply_all, Runnable {
                ActionUtil.doReplyAll(source, mActivity)
                dismiss()
            }))
        }

        /*
         * 引用
         */
        if (isPublic) {
            adapter.add(Menu(R.string.context_menu_qt, Runnable {
                ActionUtil.doQuote(source, mActivity)
                dismiss()
            }))
        }

        /*
         * ふぁぼ / あんふぁぼ
         */
        if (FavRetweetManager.isFav(status)) {
            adapter.add(Menu(R.string.context_menu_destroy_favorite, Runnable {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doDestroyFavorite(status.id)
                    dismiss()
                }
            }))
        } else {
            adapter.add(Menu(R.string.context_menu_create_favorite, Runnable {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doFavorite(status.id)
                    dismiss()
                }
            }))
        }

        /*
         * 自分のツイートまたはRT
         */
        if (status.user.id == currentIdentifier.userId) {

            /*
             * ツイ消し
             */
            adapter.add(Menu(R.string.context_menu_destroy_status, Runnable {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doDestroyStatus(status.id)
                    dismiss()
                }
            }))
        }

        /*
         * 自分がRTした事があるツイート
         */
        if (FavRetweetManager.getRtId(status) != null) {

            /*
             * RT解除
             */
            adapter.add(Menu(R.string.context_menu_destroy_retweet, Runnable {
                GlobalScope.launch(Dispatchers.Main) {
                    ActionUtil.doDestroyRetweet(status)
                    dismiss()
                }
            }))
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
                    adapter.add(Menu(R.string.context_menu_favorite_and_retweet, Runnable {
                        GlobalScope.launch(Dispatchers.Main) {
                            ActionUtil.doFavorite(status.id)
                            ActionUtil.doRetweet(status.id)
                            dismiss()
                        }
                    }))
                }

                /*
                 * RT
                 */
                adapter.add(Menu(R.string.context_menu_retweet, Runnable {
                    GlobalScope.launch(Dispatchers.Main) {
                        ActionUtil.doRetweet(status.id)
                        dismiss()
                    }
                }))
            }
        }

        /*
         * RTある時
         */
        if (source.retweetCount > 0) {

            /*
             * RTした人を表示
             */
            adapter.add(Menu(R.string.context_menu_show_retweeters, Runnable {
                RetweetersFragment().apply {
                    arguments = Bundle().apply { putLong("statusId", source.id) }
                }.show(mActivity.supportFragmentManager, "dialog")
            }))
        }

        /*
         * 会話を表示
         */
        adapter.add(Menu(R.string.context_menu_talk, Runnable {
            TalkFragment().apply {
                arguments = Bundle().apply { putString("status", source.toJsonString()) }
            }.show(mActivity.supportFragmentManager, "dialog")
        }))

        /*
         * 前後のツイートを表示
         */
        adapter.add(Menu(R.string.context_menu_show_around, Runnable {
            AroundFragment().apply {
                arguments = Bundle().apply { putString("status", source.toJsonString()) }
            }.show(mActivity.supportFragmentManager, "dialog")
        }))

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
            adapter.add(Menu("#" + hashtag.text, Runnable {
                mActivity.startActivity<SearchActivity>("query" to "#" + hashtag.text)
            }))
        }

        val users = LongSparseArray<Boolean>()

        /*
         * ツイートした人
         */
        users.put(source.user.id, true)
        adapter.add(Menu("@" + source.user.screenName, Runnable {
            mActivity.startActivity(Intent(mActivity, ProfileActivity::class.java).apply {
                putExtra("screenName", source.user.screenName)
            })
        }))

        /*
         * ふぁぼした人
         */
        val favoriteSourceUser = arguments!!.getString("favoriteSourceUser")?.toJsonObject()?.parse<User>()
        if (favoriteSourceUser != null) {
            users.put(favoriteSourceUser.id, true)
            adapter.add(Menu("@" + favoriteSourceUser.screenName, Runnable {
                mActivity.startActivity(Intent(mActivity, ProfileActivity::class.java).apply {
                    putExtra("screenName", favoriteSourceUser.screenName)
                })
            }))
        }

        /*
         * RTした人
         */
        if (retweet != null && users.get(status.user.id) == null) {
            users.put(status.user.id, true)
            adapter.add(Menu("@" + status.user.screenName, Runnable {
                mActivity.startActivity(Intent(mActivity, ProfileActivity::class.java).apply {
                    putExtra("screenName", status.user.screenName)
                })
            }))
        }

        /*
         * ツイート内のメンション
         */
        for (mention in mentions) {
            if (users.get(mention.id) != null) continue

            users.put(mention.id, true)
            adapter.add(Menu("@" + mention.screenName, Runnable {
                mActivity.startActivity(Intent(mActivity, ProfileActivity::class.java).apply {
                    putExtra("screenName", mention.screenName)
                })
            }))
        }

        /*
         * 非鍵垢
         */
        if (isPublic) {

            /*
             * ツイートを共有
             */
            adapter.add(Menu(R.string.context_menu_share_url, Runnable {
                mActivity.startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, source.uri)
                })
            }))
        }

        /*
         * ツイートを開く
         */
        adapter.add(Menu(R.string.context_menu_open_other_apps, Runnable {
            mActivity.startActivity(Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/" + source.user.screenName
                            + "/status/" + source.id.toString())
            ))
        }))

        /*
         * viaをミュート
         */
        adapter.add(Menu(String.format(mActivity.getString(R.string.context_menu_mute), StatusUtil.getClientName(source.via.name)), Runnable {
            AlertDialog.Builder(activity)
                    .setMessage(String.format(getString(R.string.context_create_mute), StatusUtil.getClientName(source.via.name)))
                    .setPositiveButton(R.string.button_ok) { _, _->
                        SourceMute += StatusUtil.getClientName(source.via.name)
                        MessageUtil.showToast(R.string.toast_create_mute)
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .show()
        }))

        /*
         * ハッシュタグをミュート
         */
        for (hashtag in hashtags) {
            adapter.add(Menu(String.format(mActivity.getString(R.string.context_menu_mute), "#" + hashtag.text), Runnable {
                AlertDialog.Builder(activity)
                        .setMessage(String.format(getString(R.string.context_create_mute), "#" + hashtag.text))
                        .setPositiveButton(R.string.button_ok) { _, _ ->
                            WordMute += "#" + hashtag.text
                            MessageUtil.showToast(R.string.toast_create_mute)
                            dismiss()
                        }
                        .setNegativeButton(R.string.button_cancel) { _, _-> }
                        .show()
            }))
        }

        /*
         * ユーザーをミュート
         */
        adapter.add(Menu(String.format(mActivity.getString(R.string.context_menu_mute), "@" + source.user.screenName), Runnable {
            AlertDialog.Builder(activity)
                    .setMessage(String.format(getString(R.string.context_create_mute), "@" + source.user.screenName))
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                        UserMute += source.user
                        MessageUtil.showToast(R.string.toast_create_mute)
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .show()
        }))
    }

    private fun addUrls(adapter: MenuAdapter, urls: List<String>) {
        for (url in urls) {
            adapter.add(Menu(url, Runnable {
                mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                dismiss()
            }))
        }
    }

    inner class Menu(val label: String, val callback: Runnable) {
        constructor(resId: Int, callback: Runnable): this(getString(resId), callback)
    }

    class MenuAdapter(c: Context, i: Int): ArrayAdapterBase<Menu>(c, i) {
        private val mMenuList = mutableListOf<Menu>()

        override fun add(menu: Menu?) { menu?.let {
            super.add(menu)
            mMenuList.add(menu)
        }}

        override val View.mView: (Int, ViewGroup?) -> Unit
            get() = { pos, _ -> (this as TextView).text = mMenuList[pos].label }
    }
}