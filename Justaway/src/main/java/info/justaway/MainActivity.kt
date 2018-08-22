package info.justaway

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.action_bar_main.*

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActionBarDrawerToggle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import de.greenrobot.event.EventBus
import info.justaway.adapter.SearchAdapter
import info.justaway.adapter.main.AccessTokenAdapter
import info.justaway.adapter.main.MainPagerAdapter
import info.justaway.event.AlertDialogEvent
import info.justaway.event.NewRecordEvent
import info.justaway.event.action.AccountChangeEvent
import info.justaway.event.action.OpenEditorEvent
import info.justaway.event.action.PostAccountChangeEvent
import info.justaway.event.connection.StreamingConnectionEvent
import info.justaway.event.settings.BasicSettingsChangeEvent
import info.justaway.fragment.main.StreamingSwitchDialogFragment
import info.justaway.fragment.main.tab.*
import info.justaway.model.AccessTokenManager
import info.justaway.model.TabManager
import info.justaway.model.TwitterManager
import info.justaway.model.UserIconManager
import info.justaway.settings.BasicSettings
import info.justaway.task.SendDirectMessageTask
import info.justaway.task.UpdateStatusTask
import info.justaway.util.KeyboardUtil
import info.justaway.util.MessageUtil
import info.justaway.util.ThemeUtil
import info.justaway.util.TwitterUtil
import info.justaway.widget.FontelloButton
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import java.lang.ref.WeakReference
import java.util.regex.Matcher
import java.util.regex.Pattern

class MainActivity: FragmentActivity() {
    companion object {
        private const val REQUEST_ACCOUNT_SETTING = 200
        private const val REQUEST_SETTINGS = 300
        private const val REQUEST_TAB_SETTINGS = 400
        private const val REQUEST_SEARCH = 500
        private const val ERROR_CODE_DUPLICATE_STATUS = 187
        private val USER_LIST_PATTERN = Pattern.compile("^(@[a-zA-Z0-9_]+)/(.*)$")

        private class SendQuickDMTask(context: MainActivity): SendDirectMessageTask(null) {
            private val ref = WeakReference(context)

            override fun onPostExecute(res: TwitterException?) {
                MessageUtil.dismissProgressDialog()
                res?.run { MessageUtil.showToast(R.string.toast_update_status_failure) }
                        ?: ref.get()?.quick_tweet_edit?.setText("")
            }
        }

        private class UpdateQuickStatusTask(context: MainActivity): UpdateStatusTask(null, null) {
            private val ref = WeakReference(context)

            override fun onPostExecute(res: TwitterException?) {
                MessageUtil.dismissProgressDialog()
                res?.run { MessageUtil.showToast(
                        if (res.errorCode == ERROR_CODE_DUPLICATE_STATUS) R.string.toast_update_status_already
                        else R.string.toast_update_status_failure
                )} ?: ref.get()?.quick_tweet_edit?.setText("")
            }
        }
    }

    private var mDefaultTextColor: Int = 0
    private var mDisabledTextColor: Int = 0
    private var mSearchAdapter: SearchAdapter? = null
    private lateinit var mAccessTokenAdapter: AccessTokenAdapter
    private lateinit var mMainPagerAdapter: MainPagerAdapter
    private lateinit var mViewPager: ViewPager
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private var mFirstBoot = true
    private var mInReplyToStatus: Status? = null

    private var mSwitchAccessToken: AccessToken? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        JustawayApplication.app
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        mDefaultTextColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color)
        mDisabledTextColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color_disabled)
        mAccessTokenAdapter = AccessTokenAdapter(this,
                R.layout.row_switch_account,
                ThemeUtil.getThemeTextColor(R.attr.holo_blue),
                ThemeUtil.getThemeTextColor(R.attr.text_color))

        //認証用のアクティビティの起動
        if (!AccessTokenManager.hasAccessToken()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            if ((displayOptions and ActionBar.DISPLAY_SHOW_CUSTOM) == ActionBar.DISPLAY_SHOW_CUSTOM) {
                setDisplayShowCustomEnabled(false)
            } else {
                setDisplayShowCustomEnabled(true)
                if (customView == null) {
                    setCustomView(R.layout.action_bar_main)
                    mSearchAdapter = SearchAdapter(this@MainActivity, R.layout.row_auto_complete)
                    with (action_bar_search_text) {
                        threshold = 0
                        setAdapter(mSearchAdapter)
                        onItemClickListener = actionBarAutoCompleteOnClickListener
                        setOnKeyListener(onKeyListener)
                    }
                }
            }
        }
        action_bar_streaming_button.setOnClickListener {
            StreamingSwitchDialogFragment.newInstance(!BasicSettings.getStreamingMode()).show(supportFragmentManager, "dialog")
        }
        action_bar_search_button.setOnClickListener {
            startSearch()
        }
        action_bar_search_cancel.setOnClickListener {
            cancelSearch()
        }

        setContentView(R.layout.activity_main)


        /*
         * 起動と同時にキーボードが出現するのを抑止、クイックモード時に起きる
         */
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        
        drawer_layout.requestFocus()

        /*
         * ナビゲーションドロワーの初期化処理
         */
        account_list.addFooterView(
                layoutInflater.inflate(R.layout.drawer_menu, null, false),
                null, true)
        account_list.adapter = mAccessTokenAdapter

        send_button.setOnClickListener {
            val msg = quick_tweet_edit.string
            if (!msg.isEmpty()) {
                MessageUtil.showProgressDialog(this, getString(R.string.progress_sending))
                if (msg.startsWith("D ")) {
                    SendQuickDMTask(this).execute(msg)
                } else {
                    UpdateQuickStatusTask(this).execute(StatusUpdate(msg).apply {
                        if (mInReplyToStatus != null) {
                            inReplyToStatusId = mInReplyToStatus!!.id
                            mInReplyToStatus = null
                        }
                    })
                }
            }
        }

        post_button.setOnClickListener { _ ->
            startActivity(getIntent(PostActivity::class.java).also {
                if (quick_tweet_layout.visibility == View.VISIBLE) {
                    with (quick_tweet_edit) {
                        if (!string.isEmpty()) {
                            it.putExtra("status", string)
                            it.putExtra("selection", string.length)
                            mInReplyToStatus?.run {
                                it.putExtra("inReplyToStatus", this)
                            }
                            setText("")
                            clearFocus()
                        }
                    }
                }
            })
        }

        post_button.setOnLongClickListener { _ ->
            if (quick_tweet_layout.visibility == View.VISIBLE)
                hideQuickPanel()
            else
                showQuickPanel()
            true
        }

        mDrawerToggle = object: ActionBarDrawerToggle(this,
                drawer_layout, R.drawable.ic_dark_drawer, R.string.open, R.string.close) {
            override fun onDrawerClosed(drawerView: View) {
                invalidateOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                invalidateOptionsMenu()
            }
        }
        drawer_layout.setDrawerListener(mDrawerToggle)

        setup()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onStart() {
        super.onStart()

        MyUncaughtExceptionHandler.showBugReportDialogIfExist(this)

        with (window) {
            if (BasicSettings.getKeepScreenOn()) addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_TAB_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    setupTab()
                }
            }
            REQUEST_ACCOUNT_SETTING -> {
                if (resultCode == Activity.RESULT_OK)
                    mSwitchAccessToken = data?.getSerializableExtra("accessToken") as AccessToken
                mAccessTokenAdapter.clear()
                AccessTokenManager.getAccessTokens()?.forEach {
                    mAccessTokenAdapter.add(it)
                }
            }
            REQUEST_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    BasicSettings.init()
                    finish()
                    startActivity(getIntent(this::class.java))
                }
            }
            REQUEST_SEARCH -> {
                if (resultCode == Activity.RESULT_OK) {
                    setupTab()
                } else if (resultCode == SearchActivity.RESULT_CREATE_SAVED_SEARCH) {
                    mSearchAdapter?.reload()
                }
                cancelSearch()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPostResume() {
        super.onPostResume()

        if (mFirstBoot) {
            mFirstBoot = false
            return
        }

        BasicSettings.init()
        BasicSettings.resetNotification()
        EventBus.getDefault().post(BasicSettingsChangeEvent())

        title = mMainPagerAdapter.getPageTitle(mViewPager.currentItem)
        if (BasicSettings.getQuickMode()) showQuickPanel()
        else hideQuickPanel()


        Handler().postDelayed ({
            try {
                mMainPagerAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 1000)

        if (mSwitchAccessToken != null) {
            TwitterManager.switchAccessToken(mSwitchAccessToken)
            mSwitchAccessToken = null
        }
        TwitterManager.resumeStreaming()
        with (action_bar_streaming_button) {
            when {
                TwitterManager.getTwitterStreamConnected() ->
                    ThemeUtil.setThemeTextColor(this, R.attr.holo_green)
                BasicSettings.getStreamingMode() ->
                    ThemeUtil.setThemeTextColor(this, R.attr.holo_red)
                else -> setTextColor(Color.WHITE)
            }
        }
    }

    override fun onPause() {
        TwitterManager.pauseStreaming()
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onBackPressed() {
        if (quick_tweet_edit.run { text != null && text.isNotEmpty() }) {
            quick_tweet_edit.setText("")
            mInReplyToStatus = null
        } else with (drawer_layout) {
            if (isDrawerVisible(GravityCompat.START))
                closeDrawers()
            else
                finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mDrawerToggle.onOptionsItemSelected(item)) return true
        when (item?.itemId) {
            R.id.home ->
                cancelSearch()
            R.id.profile ->
                startActivity(getIntent(ProfileActivity::class.java).run {
                    putExtra("userId", AccessTokenManager.getUserId())
                })
            R.id.tab_settings -> {
                startActivityForResult(getIntent(TabSettingsActivity::class.java), REQUEST_TAB_SETTINGS)
            }
            R.id.action_bar_search_button ->
                startActivity(getIntent(SearchActivity::class.java))
            R.id.settings ->
                startActivityForResult(getIntent(SettingsActivity::class.java), REQUEST_SETTINGS)
            R.id.official_website ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.official_website))))
            R.id.feedback ->
                EventBus.getDefault().post(OpenEditorEvent("#justaway", null, null, null))
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putInt("signalButtonColor", action_bar_streaming_button.currentHintTextColor)

        with (tab_menus) {
            val tabColors = IntArray(childCount)
            for (i in 0 until childCount) {
                (getChildAt(i) as Button?)?.run {
                    tabColors[i] = currentTextColor
                }
            }
            outState?.putIntArray("tabColors", tabColors)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState?.let {
            action_bar_streaming_button.setTextColor(it.getInt("signalButtonColor"))
            with (tab_menus) {
                val tabColors = it.getIntArray("tabColors")
                for (i in 0 until Math.min(childCount, tabColors.size)) {
                    (getChildAt(i) as Button?)?.setTextColor(tabColors[i])
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun setTitle(title: CharSequence?) {
        val matcher: Matcher = USER_LIST_PATTERN.matcher(title)
        if (matcher.find()) {
            action_bar_title.text = matcher.group(2)
            action_bar_sub_title.text = matcher.group(1)
        } else {
            action_bar_title.text = title
            action_bar_sub_title.text = when (BasicSettings.getDisplayAccountName()) {
                BasicSettings.DisplayAccountName.SCREEN_NAME ->
                     "@" + AccessTokenManager.getScreenName()
                BasicSettings.DisplayAccountName.DISPLAY_NAME ->
                    UserIconManager.getName(AccessTokenManager.getUserId())
                else ->
                    ""
            }

        }
    }

    override fun setTitle(titleId: Int) {
        title = getString(titleId)
    }

    private fun showQuickPanel() {
        quick_tweet_layout.visibility = View.VISIBLE
        with (quick_tweet_edit) {
            isFocusable = true
            isFocusableInTouchMode = true
            isEnabled = true
        }
        BasicSettings.setQuickMod(true)
    }

    private fun hideQuickPanel() {
        with (quick_tweet_edit) {
            isFocusable = false
            isFocusableInTouchMode = false
            isEnabled = false
            clearFocus()
        }
        quick_tweet_layout.visibility = View.GONE
        BasicSettings.setQuickMod(false)
    }

    private fun setupTab() {
        val tabs = TabManager.loadTabs()
        if (!tabs.isEmpty()) {
            val outValueTextColor = TypedValue()
            val outValueBackground = TypedValue()
            theme?.resolveAttribute(R.attr.menu_text_color, outValueTextColor, true)
            theme?.resolveAttribute(R.attr.button_stateful, outValueBackground, true)

            tab_menus.removeAllViews()
            mMainPagerAdapter.clearTab()

            var pos = 0
            for (tab: TabManager.Tab in tabs) {
                tab_menus.addView(FontelloButton(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                            (60 * resources.displayMetrics.density + 0.5f).toInt(),
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setText(tab.icon)
                    textSize = 22f
                    setTextColor(outValueTextColor.data)
                    setBackgroundResource(outValueBackground.resourceId)
                    tag = pos++
                    setOnClickListener(mMenuOnClickListener)
                    setOnLongClickListener(mMenuOnLongClickListener)
                })
                when (tab.id) {
                    TabManager.TIMELINE_TAB_ID ->
                        mMainPagerAdapter.addTab(TimelineFragment::class.java, null, tab.getName(), tab.id)
                    TabManager.INTERACTIONS_TAB_ID ->
                        mMainPagerAdapter.addTab(InteractionsFragment::class.java, null, tab.getName(), tab.id)
                    TabManager.DIRECT_MESSAGES_TAB_ID ->
                        mMainPagerAdapter.addTab(DirectMessagesFragment::class.java, null, tab.getName(), tab.id)
                    TabManager.FAVORITES_TAB_ID ->
                        mMainPagerAdapter.addTab(FavoritesFragment::class.java, null, tab.getName(), tab.id)
                    in Long.MIN_VALUE..TabManager.SEARCH_TAB_ID ->
                        mMainPagerAdapter.addTab(SearchFragment::class.java,
                                Bundle().apply { putString("searchWord", tab.name) },
                                tab.getName(), tab.id, tab.name)
                    else ->
                        mMainPagerAdapter.addTab(UserListFragment::class.java,
                                Bundle().apply { putLong("userListId", tab.id) },
                                tab.getName(), tab.id)
                }
                mMainPagerAdapter.notifyDataSetChanged()

                val currentPos = mViewPager.currentItem
                val tmp = tab_menus.getChildAt(currentPos)
                if (tmp != null) (tmp as Button).isSelected = true
                title = mMainPagerAdapter.getPageTitle(currentPos)
            }
        }
    }

    private val mMenuOnClickListener = View.OnClickListener {
        val pos = it.tag as Int
        mMainPagerAdapter.findFragmentByPosition(pos)?.run {
            if (mViewPager.currentItem == pos) {
                if (goToTop()) {
                    showTopView()
                }
            } else {
                mViewPager.currentItem = pos
                if (isTop) {
                    showTopView()
                }
            }
        }
    }

    private val mMenuOnLongClickListener = View.OnLongClickListener {
        mMainPagerAdapter.findFragmentByPosition(it.tag as Int)?.run {
            reload()
            true
        } ?: false
    }

    private fun setup() {
        mViewPager = pager
        mMainPagerAdapter = MainPagerAdapter(this, mViewPager)

        setupTab()

        footer.visibility = View.VISIBLE

        mViewPager.offscreenPageLimit = 10

        mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (mMainPagerAdapter.findFragmentByPosition(position).isTop) {
                    showTopView()
                }
                with (tab_menus) {
                    for (i in 0 until childCount) {
                        (getChildAt(i) as Button?)?.isSelected = i == position
                    }
                }
                title = mMainPagerAdapter.getPageTitle(position)
            }
        })

        quick_tweet_edit.addTextChangedListener(mQuickTweetTextWatcher)
        if (BasicSettings.getQuickMode()) showQuickPanel()
        if (BasicSettings.getStreamingMode()) TwitterManager.startStreaming()
    }

    private fun showTopView() {
        (tab_menus.getChildAt(mViewPager.currentItem) as Button?)?.let {
            ThemeUtil.setThemeTextColor(it, R.attr.menu_text_color)
        }
    }

    private fun startSearch() {
        mDrawerToggle.isDrawerIndicatorEnabled = false
        action_bar_normal_layout.visibility = View.GONE
        action_bar_search_layout.visibility = View.VISIBLE
        action_bar_search_text.showDropDown()
        action_bar_search_text.setText("")
        KeyboardUtil.showKeyboard(action_bar_search_text)
    }

    fun cancelSearch() {
        action_bar_search_text.setText("")
        KeyboardUtil.hideKeyboard(action_bar_search_text)
        action_bar_search_layout.visibility = View.GONE
        action_bar_normal_layout.visibility = View.VISIBLE
        mDrawerToggle.isDrawerIndicatorEnabled = true
    }

    private val mQuickTweetTextWatcher: TextWatcher = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(cs: CharSequence?, p1: Int, p2: Int, p3: Int) {
            val length = TwitterUtil.count(cs.toString())
            with (count) {
                setTextColor(when {
                    length < 0 -> Color.RED
                    cs.toString().isEmpty() -> mDisabledTextColor
                    else -> mDefaultTextColor
                })
                text = length.toString()
            }
            send_button.isEnabled = !(length < 0 || cs.toString().isEmpty())
        }

        override fun afterTextChanged(p0: Editable?) {}
    }


    private val actionBarAutoCompleteOnClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
        if (action_bar_search_text.text == null) return@OnItemClickListener
        val searchWord = action_bar_search_text.string
        KeyboardUtil.hideKeyboard(action_bar_search_text)
        mSearchAdapter?.let {
            if (it.isSavedMode) {
                startActivityForResult(getIntent(SearchActivity::class.java).apply{
                    putExtra("query", searchWord)
                }, REQUEST_SEARCH)
                return@OnItemClickListener
            }
        }
        when (i) {
            0 ->
                startActivityForResult(getIntent(SearchActivity::class.java).apply {
                    putExtra("query", searchWord)
                }, REQUEST_SEARCH)
            1 ->
                startActivityForResult(getIntent(UserSearchActivity::class.java).apply {
                    putExtra("query", searchWord)
                }, REQUEST_SEARCH)
            2 ->
                startActivity(getIntent(ProfileActivity::class.java).apply {
                    putExtra("screenName", searchWord)
                })
        }
    }

    private val onKeyListener = View.OnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            if (action_bar_search_text.text == null) return@OnKeyListener false
            KeyboardUtil.hideKeyboard(action_bar_search_text)
            startActivityForResult(getIntent(SearchActivity::class.java).apply {
                putExtra("query", action_bar_search_text.string)
            }, REQUEST_SEARCH)
            return@OnKeyListener true
        }
        false
    }

    fun onEventMainThread(e: AlertDialogEvent) {
        e.dialogFragment.show(supportFragmentManager, "dialog")
    }

    fun onEventMainThread() {
        showTopView()
    }

    fun onEventMainThread(e: OpenEditorEvent) {
        if (with (quick_tweet_layout) { this != null && visibility == View.VISIBLE }) {
            with (quick_tweet_edit) {
                setText(e.text)
                if (e.selectionStart != null) {
                    if (e.selectionStop != null)
                        setSelection(e.selectionStart, e.selectionStop)
                    else
                        setSelection(e.selectionStart)
                }
                mInReplyToStatus = e.inReplyToStatus
                KeyboardUtil.showKeyboard(this)
            }
        } else {
            startActivity(getIntent(PostActivity::class.java).apply {
                putExtra("status", e.text)
                e.selectionStart?.let { putExtra("selection", it) }
                e.selectionStop?.let { putExtra("selection_stop", it) }
                e.inReplyToStatus?.let { putExtra("inReplytoStatus", it) }
            })
        }
    }

    fun onEventMainThread(e: StreamingConnectionEvent) {
        if (BasicSettings. getStreamingMode()) {
            ThemeUtil.setThemeTextColor(action_bar_streaming_button, when (e.status) {
                StreamingConnectionEvent.Status.STREAMING_CONNECT ->
                    R.attr.holo_green
                StreamingConnectionEvent.Status.STREAMING_CLEANUP ->
                    R.attr.holo_orange
                else ->
                    R.attr.holo_red
            })
        } else {
            action_bar_streaming_button.setTextColor(Color.WHITE)
        }
    }

    fun onEventMainThread(e: AccountChangeEvent) {
        mAccessTokenAdapter.notifyDataSetInvalidated()
        setupTab()
        EventBus.getDefault().post(PostAccountChangeEvent(mMainPagerAdapter.getItemId(mViewPager.currentItem)))
    }

    fun onEventMainThread(e: NewRecordEvent) {
        val pos = if (e.tabId > TabManager.SEARCH_TAB_ID) mMainPagerAdapter.findPositionById(e.tabId)
                else mMainPagerAdapter.findPositionBySearchWord(e.searchWord)
        if (pos < 0) return
        (tab_menus.getChildAt(pos) as Button?)?.let {
            ThemeUtil.setThemeTextColor(it, if (mViewPager.currentItem == pos && e.autoScroll) R.attr.menu_text_color else R.attr.holo_blue)
        }
    }

    fun <T> getIntent(cls: Class<T>): Intent {
        return Intent(this, cls)
    }

}
