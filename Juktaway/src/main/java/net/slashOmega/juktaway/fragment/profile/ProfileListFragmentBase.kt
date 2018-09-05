package net.slashOmega.juktaway.fragment.profile

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import net.slashOmega.juktaway.R
import twitter4j.User

/**
 * Created on 2018/09/01.
 */
internal abstract class ProfileListFragmentBase<T1, T2, T3>: Fragment() {
    internal abstract val mAdapter: ArrayAdapter<T1>
    internal abstract val task: AsyncTask<T2, Void, T3>
    internal abstract val taskParam: T2
    internal abstract val layout: Int
    internal val mUser by lazy { arguments!!.getSerializable("user") as User }
    internal lateinit var mListView: ListView
    internal var mAutoLoader = false
    internal lateinit var mFooter: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)?.apply {
            mListView = findViewById(R.id.list_view)
            mListView.adapter = mAdapter
            mListView.setOnScrollListener(object: AbsListView.OnScrollListener {
                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    // 最後までスクロールされたかどうかの判定
                    if (totalItemCount == firstVisibleItem + visibleItemCount) additionalReading()
                }
                override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {}
            })
            task.execute(taskParam)
            mFooter = findViewById(R.id.guruguru)
        }
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        task.execute(taskParam)
    }
}