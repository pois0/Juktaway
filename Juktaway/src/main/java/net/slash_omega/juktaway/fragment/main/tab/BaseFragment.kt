package net.slash_omega.juktaway.fragment.main.tab

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.pull_to_refresh_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.action.GoToTopEvent
import net.slash_omega.juktaway.event.action.PostAccountChangeEvent
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingCreateStatusEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.event.settings.BasicSettingsChangeEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.model.Row
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentIdentifier
import kotlin.collections.ArrayList

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
    private val mStackRows = ArrayList<Row>()

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
                launch { load(true) }
            }
        }
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
            setOnScrollListener(mOnScrollListener)
        }

        mSwipeRefreshLayout = sr_layout.apply {
            setOnRefreshListener { launch { load(false) } }
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
            launch { load(false) }
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

    fun reload() {
        launch { load(false) }
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


    private suspend fun load(additional: Boolean) {
        if(isLoading || !hasNext) return
        isLoading = true

        val statuses = getNewStatuses(additional)

        when {
            statuses.isNullOrEmpty() -> {
                if(statuses?.isEmpty() == true) hasNext = false
                mSwipeRefreshLayout.isRefreshing = false
                mListView.visibility = View.VISIBLE
            }
            additional -> {
                mMaxId = statuses.last().id
                mAdapter.extensionAddAllFromStatuses(statuses)
                mListView.visibility = View.VISIBLE
            }
            else -> {
                mAdapter.clear()
                mMaxId = statuses.last().id
                mAdapter.extensionAddAllFromStatuses(statuses)
            }
        }

        mListView.visibility = View.VISIBLE
        isLoading = false

        if(!additional) goToTop()
    }

    protected abstract suspend fun getNewStatuses(additional: Boolean): List<Status>?

    protected fun getRequestMaxId(additional: Boolean): Long?
        = mMaxId.takeIf { it > 0 && additional }?.minus(1)

    /**
     *
     * !!! EventBus !!!
     *
     */


    /**
     * 高速スクロールの設定が変わったら切り替える
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: BasicSettingsChangeEvent) { mListView.isFastScrollEnabled = BasicSettings.fastScrollOn }

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) { mAdapter.notifyDataSetChanged() }

    /**
     * ストリーミングAPIからツイ消しイベントを受信
     *
     * @param event ツイート
     */
    open fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        launch {
            val removePositions = mAdapter.removeStatus(event.statusId!!)
            for (removePosition in removePositions) {
                if (removePosition >= 0) {
                    val visiblePosition = mListView.firstVisiblePosition
                    if (visiblePosition > removePosition) {
                        val view = mListView.getChildAt(0)
                        val y = view?.top ?: 0
                        mListView.setSelectionFromTop(visiblePosition - 1, y)
                        break
                    }
                }
            }
        }
    }

    /**
     * ストリーミングAPIからツイートイベントを受信
     *
     * @param event ツイート
     */
    open fun onEventMainThread(event: StreamingCreateStatusEvent) {
        addStack(event.row)
    }

    /**
     * アカウント変更通知を受け、表示中のタブはリロード、表示されていたいタブはクリアを行う
     *
     * @param event アプリが表示しているタブのID
     */
    fun onEventMainThread(event: PostAccountChangeEvent) {
        reload()
    }
}