package net.slash_omega.juktaway

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.util.TypedValue
import android.view.*
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import androidx.core.os.bundleOf
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.models.TwitterList
import kotlinx.android.synthetic.main.activity_tab_settings.*
import kotlinx.android.synthetic.main.dialog_tab_interval.view.*
import kotlinx.android.synthetic.main.row_tag.view.*
import net.slash_omega.juktaway.model.*
import net.slash_omega.juktaway.util.ThemeUtil
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
        menu.findItem(R.id.menu_add_home_tab)?.isVisible = !mAdapter.hasTab(HOME_TAB_ID)
        menu.findItem(R.id.menu_add_interactions_tab)?.isVisible = !mAdapter.hasTab(MENTION_TAB_ID)
        menu.findItem(R.id.menu_add_direct_messages_tab)?.isVisible = false //mAdapter.hasTab(DM_TAB_ID)
        menu.findItem(R.id.menu_add_favorites_tab)?.isVisible = !mAdapter.hasTab(FAVORITE_TAB_ID)
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
                //mListView.invalidateViews()
                setResult(Activity.RESULT_OK)
            }
        }
    }

    internal fun updateTab(pos: Int, interval: Long) {
        val current = mAdapter.tabs[pos]
        mAdapter.tabs[pos] = Tab(current.type, current.id, current.word, interval)
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

        fun hasTab(type: Int): Boolean = tabs.any { it.type == type }

        override fun getView(position: Int, view: View?, parent: ViewGroup?): View? {
            return (view ?: mInflater.inflate(this.mLayout, null))?.apply {
                val tab = tabs[position]
                val color = TypedValue().also {
                    theme?.resolveAttribute(R.attr.menu_text_color, it, true)
                }.data

                tab_icon.setImageResource(tab.icon)
                tab_icon.setColorFilter(color)
                timer.setColorFilter(color)
                name.text = tab.displayString

                handle.apply {
                    if (mRemoveMode) {
                        setImageResource(R.drawable.ic_delete)
                        setColorFilter(color)
                        setOnTouchListener(null)
                        setOnClickListener { mAdapter.remove(tab) }
                    } else {
                        setImageResource(R.drawable.ic_reorder)
                        setColorFilter(color)
                        setOnClickListener(null)
                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                this@TabSettingsActivity.startDrag(tab)
                                true
                            } else false
                        }
                    }
                }

                timer.setOnClickListener {
                    AutoLoadDialog().apply {
                        arguments = bundleOf("position" to position, "interval" to tab.autoReload)
                    }.show(supportFragmentManager, "auto-load-dialog")
                }

                setBackgroundColor(if (currentTab == tab) HIGH_LIGHT_COLOR else DEFAULT_COLOR)
            }
        }
    }
}

class AutoLoadDialog: DialogFragment() {
    private var intervalValue
        get() = intervalView.text.toString().toLongOrNull() ?: -1
        set(v) { intervalView.setText(v.takeIf { it > 0 }?.toString() ?: "") }

    private var isChecked
        get() = enableView.isChecked
        set(v) { enableView.isChecked = v }

    private lateinit var intervalView: EditText
    private lateinit var enableView: CheckBox

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setView(activity!!.layoutInflater.inflate(R.layout.dialog_tab_interval, null).apply {
                    intervalView = interval
                    enableView = enable
                })
                .setTitle(R.string.dialog_title_tab_interval)
                .setPositiveButton(R.string.button_save, onPositiveClicked)
                .setNegativeButton(R.string.button_cancel) { _, _ -> }
                .create()
    }

    override fun onResume() {
        super.onResume()
        val intervalArg = arguments?.getLong("interval")

        intervalValue = intervalArg ?: -1
        isChecked = intervalArg?.let { it > 0 } ?: false

        intervalView.also {
            it.isClickable = isChecked
            it.isFocusable = isChecked
            it.isFocusableInTouchMode = isChecked
        }

        enableView.setOnCheckedChangeListener { _, isChecked ->
            intervalValue = -1
            intervalView.also {
                it.isClickable = isChecked
                it.isFocusable = isChecked
                it.isFocusableInTouchMode = isChecked
            }
        }
    }

    private val onPositiveClicked = DialogInterface.OnClickListener { _, _ ->
        (activity as TabSettingsActivity).updateTab(arguments!!.getInt("position"), intervalValue)
        dismiss()
    }
}
