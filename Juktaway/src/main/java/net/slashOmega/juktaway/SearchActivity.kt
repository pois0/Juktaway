package net.slashOmega.juktaway

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.core.PenicillinJsonObjectAction
import jp.nephy.penicillin.core.hasNext
import jp.nephy.penicillin.core.next
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.*
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.AlertDialogEvent
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.listener.PressEnterListener
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.TabManager
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.ActivityJob
import net.slashOmega.juktaway.util.KeyboardUtil
import net.slashOmega.juktaway.util.ThemeUtil
import org.jetbrains.anko.toast

/**
 * Created on 2018/08/24.
 */
class SearchActivity: FragmentActivity() {
    companion object {
        const val RESULT_CREATE_SAVED_SEARCH = 100
        var job by ActivityJob()
    }

    private lateinit var mAdapter: StatusAdapter
    private var nextAction: PenicillinJsonObjectAction<jp.nephy.penicillin.models.Search>? = null

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
            searchWords.text?.let {
                startActivity(Intent(this, PostActivity::class.java).apply {
                    putExtra("status", " " + v.toString())
                })
            }
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

    override fun onStop() {
        job = null
        super.onStop()
    }

    fun onEventMainThread(event: AlertDialogEvent) {
        event.dialogFragment.show(supportFragmentManager, "dialog")
    }

    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        mAdapter.removeStatus(event.statusId!!)
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
            guruguru.visibility = View.VISIBLE

            job = action.queue {
                val statuses = it.result.statuses
                if (it.hasNext()) nextAction = it.next()

                val count = mAdapter.count
                mAdapter.addAllFromStatuses(statuses)
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
            guruguru.visibility = View.GONE
            nextAction = null

            job = currentClient.search.search(text.toString() + " exclude:retweets").queue {
                val statuses = it.result.statuses
                if (it.hasNext()) nextAction = it.next()

                val count = mAdapter.count
                mAdapter.addAllFromStatuses(statuses)
                mAdapter.notifyDataSetChanged()

                search_list.visibility = View.VISIBLE
                if (count == 0) search_list.setSelection(0)
                guruguru.visibility = View.GONE

                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(searchWords.windowToken, 0)
            }
        }
    }

    private fun createSavedSearch(word: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val res = runCatching { currentClient.savedSearch.create(word) }.isSuccess

            if (res) {
                setResult(RESULT_CREATE_SAVED_SEARCH)
                toast(getString(R.string.toast_save_success))
            }
        }
    }
}