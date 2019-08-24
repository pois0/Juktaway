package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.pull_to_refresh_list.*
import kotlinx.coroutines.*
import net.slash_omega.juktaway.MainActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.settings.BasicSettingsChangeEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.util.regenerableLaunch
import kotlin.system.measureTimeMillis

abstract class BaseFragment: Fragment(), CoroutineScope {
    override val coroutineContext by lazy { mainActivity.coroutineContext }
    private val mainActivity by lazy { activity as MainActivity }

    protected lateinit var mAdapter: StatusAdapter
    private var isLoading = false
        set(value) {
            field = value
            swipe_refresh_layout.isRefreshing = value
        }
    @Volatile protected var hasNext = true
    @Volatile private var statusIdMax = 0L // 読み込んだ最新のツイートID
    @Volatile private var statusIdMin = 0L
    private val position by lazy { arguments?.getInt("position", -1) ?: -1 }

    private val autoReloadInterval by lazy { arguments?.getLong("reloadInterval") ?: -1L }

    protected lateinit var mListView: ListView

    private val autoReloadJob by lazy {
        if (autoReloadInterval > 0) regenerableLaunch(Dispatchers.Default) {
            var delayed = autoReloadInterval - 1000
            route@ while (isActive) {
                delay(autoReloadInterval - delayed)

                while (position != mainActivity.currentTabPosition || isLoading) {
                    if (!isActive) break@route
                    delay(500)
                }

                delayed = measureTimeMillis { load(LoadStatusesType.NEW_ARRIVAL) }
            }
        } else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.pull_to_refresh_without_guruguru, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = activity ?: return
        mListView = list_view.apply {
            onItemClickListener = StatusClickListener(act)
            onItemLongClickListener = StatusLongClickListener(act)
            setOnScrollListener(object: AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    // 最後までスクロールされたかどうかの判定
                    if (totalItemCount == firstVisibleItem + visibleItemCount && totalItemCount > 5 && !isLoading) {
                        launch { load(LoadStatusesType.ADDITIONAL) }
                    }
                }
            })
        }

        swipe_refresh_layout.setOnRefreshListener {
            launch { load(LoadStatusesType.RELOAD) }
        }

        mAdapter = StatusAdapter(activity!!)
        mListView.visibility = View.GONE
        launch { load(LoadStatusesType.RELOAD) }

        mListView.adapter = mAdapter
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        autoReloadJob?.restart()
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        autoReloadJob?.cancel()
        super.onPause()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        super.onDestroy()
    }

    internal fun reload() {
        launch { load(LoadStatusesType.RELOAD) }
    }

    internal fun goToTop(): Boolean {
        mListView.setSelection(0)
        mListView.smoothScrollToPosition(0)
        return true
    }

    val isTop
        get() = mListView.firstVisiblePosition == 0

    private suspend fun load(loadType: LoadStatusesType) = withContext(Dispatchers.Main) {
        if (isLoading || (loadType == LoadStatusesType.ADDITIONAL && !hasNext)) return@withContext
        if (loadType != LoadStatusesType.NEW_ARRIVAL) isLoading = true

        val statuses = getNewStatuses(loadType)

        if (loadType == LoadStatusesType.NEW_ARRIVAL && isLoading) return@withContext

        if (statuses?.isNotEmpty() == true)  {
            when (loadType) {
                LoadStatusesType.ADDITIONAL -> {
                    mAdapter.addAllSuspend(statuses)
                }
                LoadStatusesType.RELOAD -> {
                    mAdapter.clear()
                    mAdapter.extensionAddAllFromStatuses(statuses)
                }
                LoadStatusesType.NEW_ARRIVAL -> {
                    val position = mListView.firstVisiblePosition
                    val y = mListView.getChildAt(0)?.top ?: 0

                    val size = mAdapter.insertAllFromStatus(statuses, 0)

                    if (position == 0 && y == 0) {
                        mListView.setSelection(0)
                    } else {
                        mListView.setSelectionFromTop(position + size, y)
                    }
                }
            }

            if (!loadType.limitMin) statusIdMax = statuses.first().id
            if (!loadType.limitMax) statusIdMin = statuses.last().id
        }

        mListView.visibility = View.VISIBLE

        if (loadType != LoadStatusesType.NEW_ARRIVAL) isLoading = false
        if (loadType == LoadStatusesType.RELOAD) goToTop()
    }

    protected abstract suspend fun getNewStatuses(loadType: LoadStatusesType): List<Status>?

    protected enum class LoadStatusesType(val limitMax: Boolean, val limitMin: Boolean) {
        RELOAD(false, false), ADDITIONAL(true, false), NEW_ARRIVAL(false, true)
    }

    protected val LoadStatusesType.requestMaxId: Long?
        get() = statusIdMin.takeIf { it > 0 && limitMax }?.minus(1)

    protected val LoadStatusesType.requestSinceId: Long?
        get() = statusIdMax.takeIf { it > 0 && limitMin }?.plus(1)

    /*
     * !!! EventBus !!!
     *
     * 高速スクロールの設定が変わったら切り替える
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: BasicSettingsChangeEvent) { mListView.isFastScrollEnabled = preferences.display.general.isFastScrollEnabled }

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) { mAdapter.notifyDataSetChanged() }
}
