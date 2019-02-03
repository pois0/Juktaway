package net.slash_omega.juktaway

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.core.request.action.JsonObjectApiAction
import jp.nephy.penicillin.endpoints.savedSearches
import jp.nephy.penicillin.endpoints.savedsearches.create
import jp.nephy.penicillin.endpoints.search
import jp.nephy.penicillin.endpoints.search.search
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.cursor.hasNext
import jp.nephy.penicillin.extensions.cursor.next
import jp.nephy.penicillin.models.Search
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.*
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.AlertDialogEvent
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.listener.PressEnterListener
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.model.TabManager
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.KeyboardUtil
import net.slash_omega.juktaway.util.ThemeUtil
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import kotlin.coroutines.CoroutineContext

/**
 * Created on 2018/08/24.
 */
class SearchActivity: DividedFragmentActivity() {
    companion object {
        const val RESULT_CREATE_SAVED_SEARCH = 100
    }

    private lateinit var mAdapter: StatusAdapter
    private var nextAction: JsonObjectApiAction<Search>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this)
        setContentView(R.layout.activity_search)

        actionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        mAdapter = StatusAdapter(this)
        search_list.let { l ->
            l.adapter = mAdapter
            l.onItemClickListener = StatusClickListener(this)
            l.onItemLongClickListener = StatusLongClickListener(this)
        }

        searchWords.setOnKeyListener ( PressEnterListener {
            search()
            true
        })

        intent.getStringExtra("query")?.let {
            searchWords.setText(it)
            search()
        } ?: KeyboardUtil.showKeyboard(searchWords)

        search_list.setOnScrollListener(object: AbsListView.OnScrollListener {
            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {}

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                // 最後までスクロールされたかどうかの判定
                if (totalItemCount == firstVisibleItem + visibleItemCount) additionalReading()

            }
        })

        search_button.setOnClickListener { search() }
        tweet_button.setOnClickListener { v ->
            if (searchWords.text != null) startActivity<PostActivity>("status" to v.toString())
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

    fun onEventMainThread(event: AlertDialogEvent) {
        event.dialogFragment.show(supportFragmentManager, "dialog")
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        launch { mAdapter.removeStatus(event.statusId!!) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save_search ->
                searchWords.text?.let {
                    createSavedSearch(it.toString())
                }
            R.id.search_to_tab -> {
                TabManager.saveTabs(TabManager.loadTabs().apply {
                    searchWords.text.toString().let {
                        add(TabManager.Tab(TabManager.SEARCH_TAB_ID - Math.abs(it.hashCode())).apply {
                            name = it
                        })
                    }
                })
                setResult(Activity.RESULT_OK)
            }
        }
        return true
    }

    private fun additionalReading() {
        nextAction?.let { action ->
            launch {
                guruguru.visibility = View.VISIBLE
                val result = action.await()
                val statuses = result.result.statuses
                if (result.hasNext) nextAction = result.next

                val count = mAdapter.count
                mAdapter.extensionAddAllFromStatuses(statuses)
                mAdapter.notifyDataSetChanged()

                search_list.visibility = View.VISIBLE
                if (count == 0) search_list.setSelection(0)
                guruguru.visibility = View.GONE

                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(searchWords.windowToken, 0)
            }
        }
    }

    private fun search() {
        KeyboardUtil.hideKeyboard(searchWords)
        searchWords.text?.let { text ->
            mAdapter.clear()
            search_list.visibility = View.GONE
            guruguru.visibility = View.VISIBLE
            nextAction = null
            launch {
                val result = runCatching {
                    currentClient.search.search("$text exclude:retweets",
                            count = BasicSettings.pageCount).await()
                }.getOrNull()
                if (result != null) {
                    val statuses = result.result.statuses
                    if (result.hasNext) nextAction = result.next

                    val count = mAdapter.count
                    mAdapter.addAllFromStatusesSuspend(statuses)
                    mAdapter.notifyDataSetChanged()


                    search_list.visibility = View.VISIBLE
                    if (count == 0) search_list.setSelection(0)
                }
                guruguru.visibility = View.GONE

                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(searchWords.windowToken, 0)

            }
        }
    }

    private fun createSavedSearch(word: String) {
        launch {
            val res = runCatching { currentClient.savedSearches.create(word).await() }.isSuccess

            if (res) {
                setResult(RESULT_CREATE_SAVED_SEARCH)
                toast(getString(R.string.toast_save_success))
            }
        }
    }
}