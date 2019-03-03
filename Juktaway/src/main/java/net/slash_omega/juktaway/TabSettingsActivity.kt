package net.slash_omega.juktaway

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_tab_settings.*
import kotlinx.android.synthetic.main.row_tag.view.*

/**
 * Created on 2018/08/29.
 */
class TabSettingsActivity: FragmentActivity() {
    companion object {
        private const val REQUEST_CHOOSE_USER_LIST = 100
        private val HIGH_LIGHT_COLOR = Color.parseColor("#9933b5e5")
        private const val DEFAULT_COLOR = Color.TRANSPARENT
    }

    private lateinit var mAdapter: TabAdapter
    private lateinit var mListView: ListView
    private lateinit var mDragTab: TabManager.Tab
    private var mSortable = false
    private var mToPosition: Int = 0
    private var mRemoveMode = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_tab_settings)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        mListView = findViewById(R.id.list)

        mAdapter = TabAdapter(this, R.layout.row_tag, TabManager.loadTabs())

        mListView.adapter = mAdapter
        mListView.setOnTouchListener { _, e ->
            mSortable && when (e.action) {
                MotionEvent.ACTION_MOVE ->
                    mListView.pointToPosition(e.x.toInt(), e.y.toInt()).takeIf { it >= 0 && it != mToPosition }?.let {
                        mToPosition = it
                        mAdapter.remove(mDragTab)
                        mAdapter.insert(mDragTab, mToPosition)
                        true
                    } ?: false
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    mAdapter.setCurrentTab(null)
                    mSortable = false
                    true
                }
                else -> false
            }
        }

        mode_switch.setOnCheckedChangeListener { _, b ->
            mRemoveMode = b
            mAdapter.notifyDataSetChanged()
        }

        button_cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        button_save.setOnClickListener {
            TabManager.saveTabs(mAdapter.tabs)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    fun startDrag(tab: TabManager.Tab) {
        mDragTab = tab
        mToPosition = 0
        mSortable = true
        mAdapter.setCurrentTab(mDragTab)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tab_setting, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_add_home_tab)?.isVisible = !mAdapter.hasTabId(TabManager.TIMELINE_TAB_ID)
        menu.findItem(R.id.menu_add_interactions_tab)?.isVisible = !mAdapter.hasTabId(TabManager.INTERACTIONS_TAB_ID)
        menu.findItem(R.id.menu_add_direct_messages_tab)?.isVisible = !mAdapter.hasTabId(TabManager.DIRECT_MESSAGES_TAB_ID)
        menu.findItem(R.id.menu_add_favorites_tab)?.isVisible = !mAdapter.hasTabId(TabManager.FAVORITES_TAB_ID)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        with(mAdapter) {
            when (item.itemId) {
                android.R.id.home -> finish()
                R.id.menu_add_home_tab -> insert(TabManager.Tab(TabManager.TIMELINE_TAB_ID), 0)
                R.id.menu_add_interactions_tab -> insert(TabManager.Tab(TabManager.INTERACTIONS_TAB_ID), 0)
                R.id.menu_add_direct_messages_tab -> insert(TabManager.Tab(TabManager.DIRECT_MESSAGES_TAB_ID), 0)
                R.id.menu_add_favorites_tab -> insert(TabManager.Tab(TabManager.FAVORITES_TAB_ID), 0)
                R.id.menu_user_list_tab -> {
                    TabManager.saveTabs(tabs)
                    val intent = Intent(this@TabSettingsActivity, ChooseUserListsActivity::class.java)
                    setResult(Activity.RESULT_OK)
                    startActivityForResult(intent, REQUEST_CHOOSE_USER_LIST)
                }
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHOOSE_USER_LIST -> if (resultCode == Activity.RESULT_OK) {
                mAdapter.clear()
                TabManager.loadTabs().forEach { mAdapter.add(it) }
                mAdapter.notifyDataSetChanged()
                mListView.invalidateViews()
                setResult(Activity.RESULT_OK)
            }
        }
    }

    inner class TabAdapter(context: Context, private val mLayout: Int, list: List<TabManager.Tab>): ArrayAdapter<TabManager.Tab>(context, mLayout, list) {
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        private var mCurrentTab: TabManager.Tab? = null
        val tabs = ArrayList<TabManager.Tab>()


        fun setCurrentTab(tab: TabManager.Tab?) {
            mCurrentTab = tab
            notifyDataSetChanged()
        }


        override fun add(tab: TabManager.Tab?) {
            super.add(tab)
            tab?.let { tabs.add(it) }
        }

        override fun insert(tab: TabManager.Tab?, position: Int) {
            super.insert(tab, position)
            tab?.let { tabs.add(position, it) }
        }

        override fun remove(tab: TabManager.Tab?) {
            super.remove(tab)
            tabs.remove(tab)
        }

        override fun clear() {
            super.clear()
            tabs.clear()
        }

        fun hasTabId(tabId: Long?): Boolean = tabs.any { it.id == tabId }

        override fun getView(position: Int, view: View?, parent: ViewGroup?): View? {
            return (view ?: mInflater.inflate(this.mLayout, null))?.apply {
                val tab = tabs[position]
                tab_icon.setText(tab.getIcon())
                name.text = tab.name

                handle.apply {
                    if (mRemoveMode) {
                        setText(R.string.fontello_trash)
                        setOnTouchListener(null)
                        setOnClickListener { mAdapter.remove(tab) }
                    } else {
                        setText(R.string.fontello_menu)
                        setOnClickListener(null)
                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                this@TabSettingsActivity.startDrag(tab)
                                true
                            } else false
                        }
                    }
                }

                setBackgroundColor(if (mCurrentTab == tab) HIGH_LIGHT_COLOR else DEFAULT_COLOR)
            }
        }
    }
}