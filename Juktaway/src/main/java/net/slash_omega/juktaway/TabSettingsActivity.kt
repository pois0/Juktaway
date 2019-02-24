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
import jp.nephy.jsonkt.parseListOrEmpty
import jp.nephy.jsonkt.toJsonArrayOrNull
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.models.TwitterList
import net.slash_omega.juktaway.util.ThemeUtil
import kotlinx.android.synthetic.main.activity_tab_settings.*
import kotlinx.android.synthetic.main.row_tag.view.*
import net.slash_omega.juktaway.model.*
import net.slash_omega.juktaway.util.parseWithClient
import org.jetbrains.anko.startActivityForResult

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
    private lateinit var mDragTab: Tab
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
                    mAdapter.currentTab = null
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
            TabManager.reinitialize(mAdapter.tabs)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onPostResume() {
        super.onPostResume()

    }

    fun startDrag(tab: Tab) {
        mDragTab = tab
        mToPosition = 0
        mSortable = true
        mAdapter.currentTab = mDragTab
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tab_setting, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_add_home_tab)?.isVisible = !mAdapter.hasTabId(TabManager.OLD_TIMELINE_TAB_ID)
        menu.findItem(R.id.menu_add_interactions_tab)?.isVisible = !mAdapter.hasTabId(TabManager.OLD_INTERACTIONS_TAB_ID)
        menu.findItem(R.id.menu_add_direct_messages_tab)?.isVisible = !mAdapter.hasTabId(TabManager.OLD_DIRECT_MESSAGES_TAB_ID)
        menu.findItem(R.id.menu_add_favorites_tab)?.isVisible = !mAdapter.hasTabId(TabManager.OLD_FAVORITES_TAB_ID)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        with(mAdapter) {
            when (item.itemId) {
                android.R.id.home -> finish()
                R.id.menu_add_home_tab -> insert(homeTab, 0)
                R.id.menu_add_interactions_tab -> insert(mentionTab, 0)
                R.id.menu_add_direct_messages_tab -> insert(dmTab, 0)
                R.id.menu_add_favorites_tab -> insert(favoriteTab, 0)
                R.id.menu_user_list_tab -> startActivityForResult<ChooseUserListsActivity>(REQUEST_CHOOSE_USER_LIST)
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHOOSE_USER_LIST -> if (resultCode == Activity.RESULT_OK) {
                val add = data?.getStringArrayExtra("add")
                        ?.takeIf { it.isNotEmpty() }
                        ?.map { it.toJsonObject().parseWithClient<TwitterList>() } ?: emptyList()
                val remove = data?.getStringArrayExtra("remove")
                        ?.takeIf { it.isNotEmpty() }
                        ?.map { it.toJsonObject().parseWithClient<TwitterList>() } ?: emptyList()

                mAdapter.addAll(add.map { it.toTab() })
                mAdapter.removeAll(remove.map { it.toTab() })
                mAdapter.notifyDataSetChanged()
                println(mAdapter.count)
                //mListView.invalidateViews()
                setResult(Activity.RESULT_OK)
            }
        }
    }

    inner class TabAdapter(context: Context, private val mLayout: Int, list: List<Tab>): ArrayAdapter<Tab>(context, mLayout, list) {
        private val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        internal var currentTab: Tab? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        val tabs = list.toMutableList()

        override fun add(tab: Tab) {
            super.add(tab)
            tabs.add(tab)
        }

        override fun addAll(collection: Collection<Tab>) {
            super.addAll(collection)
            tabs.addAll(collection)
        }

        override fun addAll(vararg items: Tab) {
            super.addAll(*items)
            tabs.addAll(items.toList())
        }

        override fun insert(tab: Tab, position: Int) {
            super.insert(tab, position)
            tabs.add(position, tab)
        }

        override fun remove(tab: Tab) {
            super.remove(tab)
            tabs.remove(tab)
        }

        fun removeAll(collection: Collection<Tab>) {
            collection.forEach { super.remove(it) }
            tabs.removeAll(collection)
        }

        override fun clear() {
            super.clear()
            tabs.clear()
        }

        fun hasTabId(tabId: Long?): Boolean = tabs.any { it.id == tabId }

        override fun getView(position: Int, view: View?, parent: ViewGroup?): View? {
            return (view ?: mInflater.inflate(this.mLayout, null))?.apply {
                val tab = tabs[position]
                tab_icon.setText(tab.icon)
                name.text = tab.displayString

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

                setBackgroundColor(if (currentTab == tab) HIGH_LIGHT_COLOR else DEFAULT_COLOR)
            }
        }
    }
}