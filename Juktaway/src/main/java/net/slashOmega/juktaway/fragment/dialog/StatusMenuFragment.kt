package net.slashOmega.juktaway.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.util.LongSparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import net.slashOmega.juktaway.ProfileActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.SearchActivity
import net.slashOmega.juktaway.adapter.ArrayAdapterBase
import net.slashOmega.juktaway.fragment.AroundFragment
import net.slashOmega.juktaway.fragment.RetweetersFragment
import net.slashOmega.juktaway.fragment.TalkFragment
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.FavRetweetManager
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.plugin.TwiccaPlugin
import net.slashOmega.juktaway.settings.MuteSettings
import net.slashOmega.juktaway.util.ActionUtil
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.StatusUtil
import net.slashOmega.juktaway.util.ThemeUtil
import twitter4j.DirectMessage
import twitter4j.Status
import twitter4j.URLEntity
import twitter4j.User

/**
 * Created on 2018/10/27.
 */
class StatusMenuFragment: DialogFragment() {
    companion object {
        fun newInstance(row: Row) = StatusMenuFragment().apply {
            arguments = Bundle().apply {
                if (row.isDirectMessage) {
                    putSerializable("directMessage", row.message)
                } else {
                    putSerializable("status", row.status)
                }
                if (row.isFavorite) {
                    putSerializable("favoriteSourceUser", row.source)
                }
            }
        }
    }

    private lateinit var mActivity: FragmentActivity

    private var mTwiccaPlugins: List<ResolveInfo> = mutableListOf()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {
            mActivity = it
            ThemeUtil.setTheme(it)
        } ?: return Dialog(null)
        val adapter = MenuAdapter(mActivity, R.layout.row_menu)
        return AlertDialog.Builder(mActivity)
                .setView(ListView(mActivity).apply {
                    setAdapter(adapter)
                    setOnItemClickListener { _, _, i, _ ->
                        adapter.getItem(i)?.callback?.run()
                    }
                }).let {
                    val message = arguments?.getSerializable("directMessage") as? DirectMessage
                    if (message != null) onDirectMessage(message, adapter, it)
                    else onStatus(arguments!!.getSerializable("status") as Status, adapter, it)
                }.create()
    }



    private fun onDirectMessage(message: DirectMessage, adapter: MenuAdapter, builder: AlertDialog.Builder) = builder.also {
        it.setTitle(message.senderScreenName)
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
            ActionUtil.doDestroyDirectMessage(message.id)
            dismiss()
        }))

        /*
         * ツイート内のメンション
         */
        for (mention in message.userMentionEntities) {
            adapter.add(Menu("@" + mention.screenName, Runnable {
                val intent = Intent(mActivity, ProfileActivity::class.java)
                intent.putExtra("screenName", mention.screenName)
                mActivity.startActivity(intent)
            }))
        }

        /*
         * ツイート内のURL
         */
        val urls = message.urlEntities
        addUrls(adapter, urls)
    }

    private fun onStatus(status: Status, adapter: MenuAdapter, builder: AlertDialog.Builder) = builder.also {
        val retweet = status.retweetedStatus
        val source = retweet ?: status
        val mentions = source.userMentionEntities
        val isPublic = !source.user.isProtected

        builder.setTitle(status.text)

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
        if (mentions.size > 1 || mentions.size == 1 && mentions[0].screenName != AccessTokenManager.getScreenName()) {
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
                ActionUtil.doDestroyFavorite(status.id)
                dismiss()
            }))
        } else {
            adapter.add(Menu(R.string.context_menu_create_favorite, Runnable {
                ActionUtil.doFavorite(status.id)
                dismiss()
            }))
        }

        /*
         * 自分のツイートまたはRT
         */
        if (status.user.id == AccessTokenManager.getUserId()) {

            /*
             * ツイ消し
             */
            adapter.add(Menu(R.string.context_menu_destroy_status, Runnable {
                ActionUtil.doDestroyStatus(status.id)
                dismiss()
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
                ActionUtil.doDestroyRetweet(status)
                dismiss()
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
                        ActionUtil.doFavorite(status.id)
                        ActionUtil.doRetweet(status.id)
                        dismiss()
                    }))
                }

                /*
                 * RT
                 */
                adapter.add(Menu(R.string.context_menu_retweet, Runnable {
                    ActionUtil.doRetweet(status.id)
                    dismiss()
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
                arguments = Bundle().apply { putSerializable("status", source) }
            }.show(mActivity.supportFragmentManager, "dialog")
        }))

        /*
         * 前後のツイートを表示
         */
        adapter.add(Menu(R.string.context_menu_show_around, Runnable {
            AroundFragment().apply {
                arguments = Bundle().apply { putSerializable("status", source) }
            }.show(mActivity.supportFragmentManager, "dialog")
        }))

        /*
         * ツイート内のURL
         */
        val urls = source.urlEntities
        addUrls(adapter, urls)

        /*
         * ツイート内のURL(画像)
         */
        val medias = source.mediaEntities
        addUrls(adapter, medias)

        /*
         * ツイート内のハッシュタグ
         */
        val hashtags = source.hashtagEntities
        for (hashtag in hashtags) {
            adapter.add(Menu("#" + hashtag.text, Runnable {
                mActivity.startActivity(Intent(mActivity, SearchActivity::class.java).apply {
                    putExtra("query", "#" + hashtag.text)
                })
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
        val favoriteSourceUser = arguments!!.getSerializable("favoriteSourceUser") as? User
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
             * TwiccaPlugin
             */
            if (mTwiccaPlugins.isEmpty()) {
                mTwiccaPlugins = TwiccaPlugin.getResolveInfo(mActivity.packageManager,
                        TwiccaPlugin.TWICCA_ACTION_SHOW_TWEET)
            }
            if (!mTwiccaPlugins.isEmpty()) {
                val pm = mActivity.packageManager
                for (resolveInfo in mTwiccaPlugins) {
                    if (pm == null || resolveInfo.activityInfo == null) continue

                    val label = resolveInfo.activityInfo.loadLabel(pm) as? String ?: continue
                    adapter.add(Menu(label, Runnable {
                        mActivity.startActivity(TwiccaPlugin.createIntentShowTweet(status,
                                resolveInfo.activityInfo.packageName,
                                resolveInfo.activityInfo.name))
                    }))
                }
            }

            /*
             * ツイートを共有
             */
            adapter.add(Menu(R.string.context_menu_share_url, Runnable {
                mActivity.startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://twitter.com/" + source.user.screenName
                            + "/status/" + source.id.toString())
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
        adapter.add(Menu(String.format(mActivity.getString(R.string.context_menu_mute), StatusUtil.getClientName(source.source)), Runnable {
            AlertDialog.Builder(activity)
                    .setMessage(String.format(getString(R.string.context_create_mute), StatusUtil.getClientName(source.source)))
                    .setPositiveButton(R.string.button_ok) { _, _->
                        MuteSettings.addSource(StatusUtil.getClientName(source.source))
                        MuteSettings.saveMuteSettings()
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
                            MuteSettings.addWord("#" + hashtag.text)
                            MuteSettings.saveMuteSettings()
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
                        MuteSettings.addUser(source.user.id, source.user.screenName)
                        MuteSettings.saveMuteSettings()
                        MessageUtil.showToast(R.string.toast_create_mute)
                        dismiss()
                    }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .show()
        }))
    }

    private fun addUrls(adapter: MenuAdapter, urls: Array<out URLEntity>) {
        for (url in urls) {
            adapter.add(Menu(url.expandedURL, Runnable {
                mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.expandedURL)))
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