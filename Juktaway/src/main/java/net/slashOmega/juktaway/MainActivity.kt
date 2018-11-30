package net.slashOmega.juktaway

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.FragmentActivity
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import de.greenrobot.event.EventBus
import kotlinx.android.synthetic.main.action_bar_main.*
import kotlinx.android.synthetic.main.activity_main.*
import net.slashOmega.juktaway.adapter.SearchAdapter
import net.slashOmega.juktaway.adapter.main.AccessTokenAdapter
import net.slashOmega.juktaway.adapter.main.MainPagerAdapter
import net.slashOmega.juktaway.event.AlertDialogEvent
import net.slashOmega.juktaway.event.NewRecordEvent
import net.slashOmega.juktaway.event.action.AccountChangeEvent
import net.slashOmega.juktaway.event.action.OpenEditorEvent
import net.slashOmega.juktaway.event.action.PostAccountChangeEvent
import net.slashOmega.juktaway.event.connection.StreamingConnectionEvent
import net.slashOmega.juktaway.event.settings.BasicSettingsChangeEvent
import net.slashOmega.juktaway.fragment.main.StreamingSwitchDialogFragment
import net.slashOmega.juktaway.fragment.main.tab.*
import net.slashOmega.juktaway.model.AccessTokenManager
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.task.SendDirectMessageTask
import net.slashOmega.juktaway.task.UpdateStatusTask
import net.slashOmega.juktaway.util.KeyboardUtil
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.ThemeUtil
import net.slashOmega.juktaway.util.TwitterUtil
import net.slashOmega.juktaway.widget.FontelloButton
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

        private class UpdateQuickStatusTask(context: MainActivity): UpdateStatusTask(null, arrayListOf()) {
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
    private val mAccessTokenAdapter by lazy {
        AccessTokenAdapter(this,
                R.layout.row_switch_account,
                ThemeUtil.getThemeTextColor(R.attr.holo_blue),
                ThemeUtil.getThemeTextColor(R.attr.text_color))
    }
    private val mMainPagerAdapter by lazy { MainPagerAdapter(this, mViewPager) }
    private val mViewPager by lazy { pager }
    private val mDrawerToggle by lazy { object: ActionBarDrawerToggle(this,
            drawer_layout, Toolbar(this), R.string.open, R.string.close) {
        override fun onDrawerClosed(drawerView: View) {
            invalidateOptionsMenu()
        }

        override fun onDrawerOpened(drawerView: View) {
            invalidateOptionsMenu()
        }
    }}
    private var mFirstBoot = true
    private var mInReplyToStatus: Status? = null

    private var mSwitchAccessToken: AccessToken? = null


    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        mDefaultTextColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color)
        mDisabledTextColor = ThemeUtil.getThemeTextColor(R.attr.menu_text_color_disabled)

        //認証用のアクティビティの起動
        if (AccessTokenManager.getAccessToken() == null) {
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
            StreamingSwitchDialogFragment.newInstance(!BasicSettings.streamingMode).show(supportFragmentManager, "dialog")
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
        account_list.setOnItemClickListener { _, _, position, _ ->
            if (mAccessTokenAdapter.count <= position) {
                startActivityForResult(Intent(this, AccountSettingActivity::class.java), REQUEST_ACCOUNT_SETTING)
                return@setOnItemClickListener
            }
            mAccessTokenAdapter.getItem(position)?.takeIf { AccessTokenManager.getUserId() != it.userId }?.let {
                TwitterManager.switchAccessToken(it)
                mAccessTokenAdapter.notifyDataSetChanged()
            }
            drawer_layout.closeDrawer(left_drawer)
        }

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

        post_button.setOnClickListener {
            startActivity(getIntent(PostActivity::class.java).also { intent ->
                if (quick_tweet_layout.visibility == View.VISIBLE) {
                    with (quick_tweet_edit) {
                        if (!string.isEmpty()) {
                            intent.putExtra("status", string)
                            intent.putExtra("selection", string.length)
                            mInReplyToStatus?.run {
                                intent.putExtra("inReplyToStatus", this)
                            }
                            setText("")
                            clearFocus()
                        }
                    }
                }
            })
        }

        post_button.setOnLongClickListener {
            if (quick_tweet_layout.visibility == View.VISIBLE)
                hideQuickPanel()
            else
                showQuickPanel()
            true
        }

        drawer_layout.addDrawerListener(mDrawerToggle)

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
            if (BasicSettings.keepScreenOn) addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                AccessTokenManager.getAccessTokens().forEach {
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
        // BasicSettings.resetNotification()
        EventBus.getDefault().post(BasicSettingsChangeEvent())

        title = mMainPagerAdapter.getPageTitle(mViewPager.currentItem)
        if (BasicSettings.quickMode) showQuickPanel()
        else hideQuickPanel()


        Handler().postDelayed ({
            try {
                mMainPagerAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 1000)

        if (mSwitchAccessToken != null) {
            TwitterManager.switchAccessToken(mSwitchAccessToken!!)
            mSwitchAccessToken = null
        }
        TwitterManager.resumeStreaming()
        with (action_bar_streaming_button) {
            when {
                TwitterManager.twitterStreamConnected ->
                    ThemeUtil.setThemeTextColor(this, R.attr.holo_green)
                BasicSettings.streamingMode ->
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
            android.R.id.home ->
                cancelSearch()
            R.id.profile ->
                startActivity(getIntent(ProfileActivity::class.java).run {
                    putExtra("screenName", AccessTokenManager.getScreenName())
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
                EventBus.getDefault().post(OpenEditorEvent("#juktaway", null, null, null))
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putInt("signalButtonColor", action_bar_streaming_button.currentHintTextColor)

        tab_menus.run {
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
                it.getIntArray("tabColors")?.let { colors ->
                    for (i in 0 until Math.min(childCount, colors.size)) {
                        (getChildAt(i) as Button?)?.setTextColor(colors[i])
                    }
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
            action_bar_sub_title.text = when (BasicSettings.displayAccountName) {
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
                    setText(tab.getIcon())
                    textSize = 22f
                    setTextColor(outValueTextColor.data)
                    setBackgroundResource(outValueBackground.resourceId)
                    tag = pos++
                    setOnClickListener(mMenuOnClickListener)
                    setOnLongClickListener(mMenuOnLongClickListener)
                })
                when (tab.id) {
                    TabManager.TIMELINE_TAB_ID ->
                        mMainPagerAdapter.addTab(TimelineFragment::class, null, tab.name, tab.id)
                    TabManager.INTERACTIONS_TAB_ID ->
                        mMainPagerAdapter.addTab(InteractionsFragment::class, null, tab.name, tab.id)
                    TabManager.DIRECT_MESSAGES_TAB_ID ->
                        mMainPagerAdapter.addTab(DirectMessagesFragment::class, null, tab.name, tab.id)
                    TabManager.FAVORITES_TAB_ID ->
                        mMainPagerAdapter.addTab(FavoritesFragment::class, null, tab.name, tab.id)
                    in Long.MIN_VALUE..TabManager.SEARCH_TAB_ID ->
                        mMainPagerAdapter.addTab(SearchFragment::class,
                                Bundle().apply { putString("searchWord", tab.name) },
                                tab.name, tab.id, tab.name)
                    else ->
                        mMainPagerAdapter.addTab(UserListFragment::class,
                                Bundle().apply { putLong("userListId", tab.id) },
                                tab.name, tab.id)
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
        mMainPagerAdapter.findFragmentByPosition(pos).run {
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
        mMainPagerAdapter.findFragmentByPosition(it.tag as Int).run {
            reload()
            true
        }
    }

    private fun setup() {
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
        if (BasicSettings.quickMode) showQuickPanel()
        if (BasicSettings.streamingMode) TwitterManager.startStreaming()
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
            if (it.savedMode) {
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
        if (BasicSettings.streamingMode) {
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

    @Suppress("UNUSED_PARAMETER")
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

    private fun <T> getIntent(cls: Class<T>): Intent {
        return Intent(this, cls)
    }

}
