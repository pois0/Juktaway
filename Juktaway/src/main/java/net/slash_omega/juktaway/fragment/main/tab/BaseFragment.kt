package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.pull_to_refresh_list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.action.GoToTopEvent
import net.slash_omega.juktaway.event.action.PostAccountChangeEvent
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.settings.BasicSettingsChangeEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.model.Row
import net.slash_omega.juktaway.settings.preferences
import net.slash_omega.juktaway.twitter.currentIdentifier
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis

abstract class BaseFragment: Fragment(), CoroutineScope {
    override val coroutineContext by lazy { (activity as CoroutineScope).coroutineContext }
    protected lateinit var mAdapter: StatusAdapter
    protected var isLoading = false
        set(value) {
            mSwipeRefreshLayout.isRefreshing = value
            field = value
        }
    protected var hasNext = true
    private var mScrolling = false
    private var mMaxId = 0L // 読み込んだ最新のツイートID
    private var mSinceId = 0L
    private val mStackRows = ArrayList<Row>()

    private val statusChannel = Channel<List<Status>>(30)
    private val autoReloadInterval by lazy { arguments?.getLong("reloadInterval") ?: -1L }
    var isAutoReloadEnable = false

    protected lateinit var mListView: ListView
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    /**
     * 1. スクロールが終わった瞬間にストリーミングAPIから受信し溜めておいたツイートがあればそれを表示する
     * 2. スクロールが終わった瞬間に表示位置がトップだったらボタンのハイライトを消すためにイベント発行
     * 3. スクロール中はスクロール中のフラグを立てる
     */
    private val mOnScrollListener = object : AbsListView.OnScrollListener {

        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            when (scrollState) {
                AbsListView.OnScrollListener.SCROLL_STATE_IDLE -> {
                    mScrolling = false
                    if (mStackRows.size > 0) {
                        showStack()
                    } else if (isTop) {
                        EventBus.getDefault().post(GoToTopEvent())
                    }
                }
                AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL, AbsListView.OnScrollListener.SCROLL_STATE_FLING -> mScrolling = true
            }
        }

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            // 最後までスクロールされたかどうかの判定
            if (totalItemCount == firstVisibleItem + visibleItemCount && totalItemCount > 5 && !isLoading) {
                launch { load(LoadStatusesType.ADDITIONAL) }
            }
        }
    }

    private val autoReloadJob by lazy {
        launch(Dispatchers.Default, CoroutineStart.LAZY) {
            var delayed = 0L
            route@ while (isActive) {
                delay(autoReloadInterval - delayed)

                while (!isAutoReloadEnable || isLoading) {
                    if (!isActive) break@route
                    delay(500)
                }

                delayed = measureTimeMillis {
                    getNewStatuses(LoadStatusesType.NEW).takeUnless { it.isNullOrEmpty() }?.let {
                        statusChannel.send(it)
                    }
                }
            }
        }
    }

    private val displayStatusJob by lazy {
        launch(Dispatchers.Main, CoroutineStart.LAZY) {
            for (statuses in statusChannel) {
                mSinceId = statuses.last().id

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (autoReloadInterval > 0) {
            autoReloadJob.start()
            displayStatusJob.start()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.pull_to_refresh_without_guruguru, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = activity ?: return
        mListView = list_view.apply {
            onItemClickListener = StatusClickListener(act)
            onItemLongClickListener = StatusLongClickListener(act)
            setOnScrollListener(mOnScrollListener)
        }

        mSwipeRefreshLayout = sr_layout.apply {
            setOnRefreshListener { launch { load(LoadStatusesType.RELOAD) } }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        /**
         * mMainPagerAdapter.notifyDataSetChanged() された時に
         * onCreateView と onActivityCreated がインスタンスが生きたまま呼ばれる
         * 多重に初期化処理を実行しないように変数チェックを行う
         */
        if (::mAdapter.isInitialized.not()) {
            // Status(ツイート)をViewに描写するアダプター
            mAdapter = StatusAdapter(activity!!)
            mListView.visibility = View.GONE
            launch { load(LoadStatusesType.RELOAD) }
        }

        mListView.adapter = mAdapter
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onDestroy() {
        println("stopped")
        super.onDestroy()
        statusChannel.close()
    }

    fun reload() {
        launch { load(LoadStatusesType.RELOAD) }
    }

    protected fun clear() {
        mAdapter.clear()
    }

    internal fun goToTop(): Boolean {
        mListView.setSelection(0)
        mListView.smoothScrollToPosition(0)
        return if (mStackRows.size > 0) {
            launch { showStack() }
            false
        } else true
    }

    val isTop by lazy { mListView.firstVisiblePosition == 0 }

    /**
     * ツイートの表示処理、画面のスクロール位置によって適切な処理を行う、まだバグがある
     */
    private val mRender = Runnable {
        if (mScrolling) return@Runnable

        // 表示している要素の位置
        val position = mListView.firstVisiblePosition

        // 縦スクロール位置
        val view = mListView.getChildAt(0)
        val y = view?.top ?: 0

        // 要素を上に追加（ addだと下に追加されてしまう ）
        var highlight = false
        mStackRows.forEach { row ->
            mAdapter.insert(row, 0)
            when {
                row.isFavorite -> {
                    // お気に入りしたのが自分じゃない時
                    if (row.source!!.id != currentIdentifier.userId) highlight = true
                }
                row.isStatus -> {
                    // 投稿主が自分じゃない時
                    if (row.status!!.user.id != currentIdentifier.userId) highlight = true
                }
                row.isDirectMessage -> {
                    // 投稿主が自分じゃない時
                    if (row.message!!.senderId != currentIdentifier.userId) highlight = true
                }
            }
        }
        mStackRows.clear()

        val autoScroll = position == 0 && y == 0 && mStackRows.size < 3

        //if (highlight) EventBus.getDefault().post(NewRecordEvent(tabId, mSearchWord, autoScroll))

        if (autoScroll) {
            mListView.setSelection(0)
        } else {
            // 少しでもスクロールさせている時は画面を動かさない様にスクロー位置を復元する
            mListView.setSelectionFromTop(position + mStackRows.size, y)
            // 未読の新規ツイートをチラ見せ
            if (position == 0 && y == 0) {
                mListView.smoothScrollToPositionFromTop(position + mStackRows.size, 120)
            }
        }
    }

    /**
     * 新しいツイートを表示して欲しいというリクエストを一旦待たせ、
     * 250ms以内に同じリクエストが来なかったら表示する。
     * 250ms以内に同じリクエストが来た場合は、更に250ms待つ。
     * 表示を連続で行うと処理が重くなる為この制御を入れている。
     */
    private fun showStack() {
        mListView.removeCallbacks(mRender)
        mListView.postDelayed(mRender, 250)
    }

    /**
     * ストリーミングAPIからツイートやメッセージを受信した時の処理
     * 1. 表示スべき内容かチェックし、不適切な場合はスルーする
     * 2. すぐ表示すると流速が早い時にガクガクするので溜めておく
     *
     * @param row ツイート情報
     */
    fun addStack(row: Row) {
        mStackRows.add(row)
        if (!mScrolling && isTop) {
            showStack()
        } else {
            //EventBus.getDefault().post(NewRecordEvent(tabId, mSearchWord, false))
        }
    }

    /**
     * タブ固有のID、ユーザーリストではリストのIDを、その他はマイナスの固定値を返す
     */


    open var mSearchWord = ""


    private suspend fun load(loadType: LoadStatusesType) {
        if(isLoading || !hasNext) return
        isLoading = true

        val statuses = getNewStatuses(loadType)

        if (statuses.isNullOrEmpty()) {
            if(statuses?.isEmpty() == true) hasNext = false
            mSwipeRefreshLayout.isRefreshing = false
        } else {
            when (loadType) {
                LoadStatusesType.ADDITIONAL -> {
                    mListView.visibility = View.VISIBLE
                    mAdapter.extensionAddAllFromStatuses(statuses)
                }
                LoadStatusesType.RELOAD -> {
                    mAdapter.clear()
                    mAdapter.extensionAddAllFromStatuses(statuses)
                }
                LoadStatusesType.NEW -> {
                    mAdapter.insertAllFromStatus(statuses, 0)
                }
            }

            if (!loadType.limitMin) mMaxId = statuses.first().id
            if (!loadType.limitMax) mSinceId = statuses.last().id
        }

        mListView.visibility = View.VISIBLE
        isLoading = false

        if(loadType == LoadStatusesType.RELOAD) goToTop()
    }

    protected abstract suspend fun getNewStatuses(loadType: LoadStatusesType): List<Status>?

    enum class LoadStatusesType(val limitMax: Boolean, val limitMin: Boolean) {
        RELOAD(false, false), ADDITIONAL(true, false), NEW(false, true)
    }

    protected val LoadStatusesType.requestMaxId: Long?
        get() = mMaxId.takeIf { it > 0 && limitMax }?.minus(1)

    protected val LoadStatusesType.requestSinceId: Long?
        get() = mSinceId.takeIf { it > 0 && limitMin }?.plus(1)

    /**
     *
     * !!! EventBus !!!
     *
     */


    /**
     * 高速スクロールの設定が変わったら切り替える
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: BasicSettingsChangeEvent) { mListView.isFastScrollEnabled = preferences.display.general.isFastScrollEnabled }

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) { mAdapter.notifyDataSetChanged() }

    /**
     * アカウント変更通知を受け、表示中のタブはリロード、表示されていたいタブはクリアを行う
     *
     * @param event アプリが表示しているタブのID
     */
    fun onEventMainThread(event: PostAccountChangeEvent) {
        reload()
    }
}