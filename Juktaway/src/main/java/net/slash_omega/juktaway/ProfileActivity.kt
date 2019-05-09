package net.slash_omega.juktaway

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import de.greenrobot.event.EventBus
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.endpoints.blocks
import jp.nephy.penicillin.endpoints.blocks.createByUserId
import jp.nephy.penicillin.endpoints.blocks.destroyByUserId
import jp.nephy.penicillin.endpoints.friendships
import jp.nephy.penicillin.endpoints.friendships.showByScreenName
import jp.nephy.penicillin.endpoints.friendships.showByUserId
import jp.nephy.penicillin.endpoints.friendships.updateByUserId
import jp.nephy.penicillin.endpoints.mutes
import jp.nephy.penicillin.endpoints.mutes.createByUserId
import jp.nephy.penicillin.endpoints.mutes.destroyByUserId
import jp.nephy.penicillin.endpoints.users
import jp.nephy.penicillin.endpoints.users.reportSpamByUserId
import jp.nephy.penicillin.endpoints.users.showByScreenName
import jp.nephy.penicillin.endpoints.users.showByUserId
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.models.ProfileBannerSize
import jp.nephy.penicillin.extensions.models.profileBannerUrlWithVariantSize
import jp.nephy.penicillin.models.Relationship
import jp.nephy.penicillin.models.User
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.*
import net.slash_omega.juktaway.adapter.SimplePagerAdapter
import net.slash_omega.juktaway.event.AlertDialogEvent
import net.slash_omega.juktaway.fragment.profile.*
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.util.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class ProfileActivity: ScopedFragmentActivity() {
    companion object {
        private const val OPTION_MENU_DESTROY_BLOCK = 4
        private const val OPTION_MENU_GROUP_RELATION = 1
        private const val OPTION_MENU_CREATE_BLOCK = 1
        private const val OPTION_MENU_CREATE_OFFICIAL_MUTE = 2
        private const val OPTION_MENU_CREATE_NO_RETWEET = 3
        private const val OPTION_MENU_DESTROY_OFFICIAL_MUTE = 5
        private const val OPTION_MENU_DESTROY_NO_RETWEET = 6
    }

    private lateinit var mUser: User
    private lateinit var mRelationship: Relationship
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_profile)

        actionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        collapse_label.setOnClickListener {
            with (frame) {
                if (visibility == View.VISIBLE) {
                    visibility = View.GONE
                    collapse_label.setText(R.string.fontello_down)
                } else {
                    visibility = View.VISIBLE
                    collapse_label.setText(R.string.fontello_up)
                }
            }
        }

        launch {
            MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_loading))

            runCatching {
                intent.getStringExtra("userJson")?.takeIf { it.isNotEmpty() }?.let {
                    return@runCatching withContext(Dispatchers.Default) {
                        val user = it.toJsonObject().parseWithClient<User>()
                        user to currentClient.friendships.showByUserId(currentIdentifier.userId, user.id).await().result.relationship
                    }
                }

                val screenName = intent.run {
                    if (Intent.ACTION_VIEW == action && data != null && data?.lastPathSegment.isNullOrEmpty().not()) {
                        data!!.lastPathSegment
                    } else {
                        getStringExtra("screenName")
                    }
                }
                val userId = intent.getLongExtra("userId", -1L)

                withContext(Dispatchers.Default) {
                    screenName?.let {
                        currentClient.run {
                            val userJob = users.showByScreenName(it)
                            val relJob = friendships.showByScreenName(currentIdentifier.screenName, it)
                            userJob.await().result to relJob.await().result.relationship
                        }
                    } ?: userId.takeUnless { it == -1L }?.let {
                        currentClient.run {
                            val userJob = users.showByUserId(it)
                            val relJob = friendships.showByUserId(currentIdentifier.userId, it)
                            userJob.await().result to relJob.await().result.relationship
                        }
                    } ?: throw IllegalArgumentException("Both of screen word and userId are null.")
                }
            }.onSuccess {
                mUser = it.first
                mRelationship = it.second

                onLoadFinished()
            }.onFailure {
                it.printStackTrace()
                toast(R.string.toast_load_data_failure)
            }
            MessageUtil.dismissProgressDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile, menu)
        menu?.let { this.menu = it }
        return true
    }

    fun onEventMainThread(event: AlertDialogEvent) {
        event.dialogFragment.show(supportFragmentManager, "dialog")
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item == null) return false
        if (item.groupId == OPTION_MENU_GROUP_RELATION) {
            when (item.itemId) {
                OPTION_MENU_CREATE_BLOCK ->
                    AlertDialog.Builder(this)
                            .setMessage(R.string.confirm_create_block)
                            .setPositiveButton(R.string.button_create_block) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))
                                    val res = runCatching {
                                        currentClient.blocks.createByUserId(mUser.id).await()
                                    }.isSuccess

                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.setBlock(mUser.id)
                                        toast(R.string.toast_create_block_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_create_block_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                OPTION_MENU_CREATE_OFFICIAL_MUTE ->
                    AlertDialog.Builder(this)
                            .setMessage(R.string.confirm_create_official_mute)
                            .setPositiveButton(R.string.button_create_official_mute) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))
                                    val res = runCatching {
                                        currentClient.mutes.createByUserId(mUser.id).await()
                                    }.isSuccess

                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.setOfficialMute(mUser.id)
                                        toast(R.string.toast_create_official_mute_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_create_official_mute_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                OPTION_MENU_CREATE_NO_RETWEET ->
                    AlertDialog.Builder(this)
                            .setMessage(R.string.confirm_create_no_retweet)
                            .setPositiveButton(R.string.button_create_no_retweet) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))

                                    val res = runCatching {
                                        currentClient.friendships.updateByUserId(mUser.id, retweets = false).await()
                                    }.isSuccess

                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.setNoRetweet(mUser.id)
                                        toast(R.string.toast_create_no_retweet_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_create_no_retweet_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                OPTION_MENU_DESTROY_BLOCK ->
                    AlertDialog.Builder(this)
                            .setMessage(R.string.confirm_create_block)
                            .setPositiveButton(R.string.button_destroy_block) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))
                                    val res = runCatching {
                                        currentClient.blocks.destroyByUserId(mUser.id).await()
                                    }.isSuccess

                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.removeBlock(mUser.id)
                                        toast(R.string.toast_destroy_block_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_destroy_block_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                OPTION_MENU_DESTROY_OFFICIAL_MUTE ->
                    AlertDialog.Builder(this)
                            .setMessage(R.string.confirm_destroy_official_mute)
                            .setPositiveButton(R.string.button_destroy_official_mute) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))
                                    val res = runCatching {
                                        currentClient.mutes.destroyByUserId(mUser.id).await()
                                    }.isSuccess


                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.removeOfficialMute(mUser.id)
                                        toast(R.string.toast_destroy_official_mute_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_destroy_official_mute_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
                OPTION_MENU_DESTROY_NO_RETWEET ->
                    AlertDialog.Builder(this@ProfileActivity)
                            .setMessage(R.string.confirm_destroy_no_retweet)
                            .setPositiveButton(R.string.button_destroy_no_retweet) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))

                                    val res = runCatching {
                                        currentClient.friendships.updateByUserId(mUser.id, retweets = true).await()
                                    }.isSuccess

                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.removeNoRetweet(mUser.id)
                                        toast(R.string.toast_destroy_no_retweet_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_destroy_no_retweet_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
            }
        } else {
            when (item.itemId) {
                android.R.id.home ->
                    finish()
                R.id.send_reply -> {
                    val text = "@" + mUser.screenName
                    startActivity<PostActivity>("status" to text, "selection" to text.length)
                }
                R.id.send_direct_messages -> {
                    val text = "D " + mUser.screenName
                    startActivity<PostActivity>("status" to text, "selection" to text.length)
                }
                R.id.send_kusoripu -> {
                    launch {
                        val content = KusoripuUtil.getKusoripu(mUser.screenName)
                        startActivity<PostActivity>("status" to content, "selection" to content.length)
                    }
                }
                R.id.add_to_list ->
                    startActivity<RegisterUserListActivity>("userId" to mUser.id)
                R.id.open_twitter ->
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://twitter.com/" + mUser.screenName)))
                R.id.open_twilog ->
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://twilog.org/" + mUser.screenName)))
                R.id.user_to_tab -> TabManager.addUserTab(mUser)
                R.id.report_spam ->
                    AlertDialog.Builder(this@ProfileActivity)
                            .setMessage(R.string.confirm_report_spam)
                            .setPositiveButton(R.string.button_report_spam) { _, _ ->
                                launch {
                                    MessageUtil.showProgressDialog(this@ProfileActivity, getString(R.string.progress_process))
                                    val res = runCatching {
                                        currentClient.users.reportSpamByUserId(mUser.id)
                                    }.isSuccess

                                    MessageUtil.dismissProgressDialog()
                                    if (res) {
                                        net.slash_omega.juktaway.model.Relationship.setBlock(mUser.id)
                                        toast(R.string.toast_report_spam_success)
                                        restart()
                                    } else {
                                        toast(R.string.toast_report_spam_failure)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .show()
            }
        }
        return true
    }

    private fun onLoadFinished() {
        favourites_count.text = getString(R.string.label_favourites, String.format("%1$,3d", mUser.favouritesCount))
        statuses_count.text = getString(R.string.label_tweets, String.format("%1$,3d", mUser.statusesCount))
        friends_count.text = getString(R.string.label_following, String.format("%1$,3d", mUser.friendsCount))
        followers_count.text = getString(R.string.label_followers, String.format("%1$,3d", mUser.followersCount))
        listed_count.text = getString(R.string.label_listed, String.format("%1$,3d", mUser.listedCount))

        mUser.profileBannerUrlWithVariantSize(ProfileBannerSize.MobileRetina)?.let { banner.displayImage(it) }

        with (menu) {
            if (mRelationship.source.blocking) {
                add(OPTION_MENU_GROUP_RELATION, OPTION_MENU_DESTROY_BLOCK, 100, R.string.menu_destroy_block)
            } else {
                add(OPTION_MENU_GROUP_RELATION, OPTION_MENU_CREATE_BLOCK, 100, R.string.menu_create_block)
            }
            if (mRelationship.source.following) {
                if (mRelationship.source.muting) {
                    add(OPTION_MENU_GROUP_RELATION, OPTION_MENU_DESTROY_OFFICIAL_MUTE, 100, R.string.menu_destory_official_mute)
                } else {
                    add(OPTION_MENU_GROUP_RELATION, OPTION_MENU_CREATE_OFFICIAL_MUTE, 100, R.string.menu_create_official_mute)
                }
                if (mRelationship.source.wantRetweets) {
                    add(OPTION_MENU_GROUP_RELATION, OPTION_MENU_CREATE_NO_RETWEET, 100, R.string.menu_create_no_retweet)
                } else {
                    add(OPTION_MENU_GROUP_RELATION, OPTION_MENU_DESTROY_NO_RETWEET, 100, R.string.menu_destory_no_retweet)
                }
            }
        }

        val args = Bundle().apply {
            putString("user", mUser.toJsonString())
            putString("relationship", mRelationship.toJsonString())
        }
        SimplePagerAdapter(this, pager).apply {
            addTab(SummaryFragment::class, args)
            addTab(DescriptionFragment::class, args)
        }.notifyDataSetChanged()
        symbol.setViewPager(pager)

        /*
         * スワイプの度合いに応じて背景色を暗くする
         * これは透明度＆背景色黒で実現している、背景色黒だけだと背景画像が見えないが、
         * 透明度を指定することで背景画像の表示と白色のテキストの視認性を両立している
         */
        pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                // OnPageChangeListenerは1つしかセットできないのでCirclePageIndicatorの奴も呼んであげる
                symbol.onPageScrollStateChanged(state)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                /*
                 * 背景色の透過度の範囲は00〜99とする（FFは真っ黒で背景画像が見えない）
                 * 99は10進数で153
                 * positionは0が1ページ目（スワイプ中含む）で1だと完全に2ページ目に遷移した状態
                 * positionOffsetには0.0〜1.0のスクロール率がかえってくる、真ん中だと0.5
                 * hexにはpositionOffsetに応じて00〜99（153）の値が入るように演算を行う
                 * 例えばpositionOffsetが0.5の場合はhexは4dになる
                 * positionが1の場合は最大値（99）を無条件で設定している
                 */

                val maxHex = 153
                val hex = if (position == 1) "99" else String.format("%02X", (maxHex * positionOffset).toInt())
                pager.setBackgroundColor(Color.parseColor("#" + hex + "000000"))
                // OnPageChangeListenerは1つしかセットできないのでCirclePageIndicatorの奴も呼んであげる
                symbol.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                // OnPageChangeListenerは1つしかセットできないのでCirclePageIndicatorの奴も呼んであげる
                symbol.onPageSelected(position)
            }
        })

        val listArgs = Bundle().apply {
            putString("user", mUser.toJsonString())
        }

        SimplePagerAdapter(this, list_pager).apply {
            addTab(UserTimelineFragment::class, listArgs)
            addTab(FollowingListFragment::class, listArgs)
            addTab(FollowersListFragment::class, listArgs)
            addTab(UserListMembershipsFragment::class, listArgs)
            addTab(FavoritesListFragment::class, listArgs)
        }.notifyDataSetChanged()
        list_pager.offscreenPageLimit = 5

        val tabs = listOf<TextView>(statuses_count, friends_count, followers_count, listed_count, favourites_count)

        val colorBlue = ThemeUtil.getThemeTextColor(R.attr.holo_blue)
        val colorWhite = ThemeUtil.getThemeTextColor(R.attr.text_color)

        tabs.forEach { it.setTextColor(colorWhite) }
        tabs[0].setTextColor(colorBlue)

        list_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                /**
                 * タブのindexと選択されたpositionを比較して色を設定
                 */
                for (i in tabs.indices) {
                    tabs[i].setTextColor(if (i == position) colorBlue else colorWhite)
                }
            }
        })

        for (i in tabs.indices) {
            tabs[i].setOnClickListener { list_pager.currentItem = i }
        }
    }

    private fun restart() {
        runCatching {
            startActivity<ProfileActivity>("userJson" to mUser.toJsonString())
        }.onFailure {
            startActivity<ProfileActivity>("userId" to mUser.id)
        }

        finish()
    }
}