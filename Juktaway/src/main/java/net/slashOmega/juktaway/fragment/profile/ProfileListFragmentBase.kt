package net.slashOmega.juktaway.fragment.profile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.slashOmega.juktaway.R
import twitter4j.User

/**
 * Created on 2018/09/01.
 */
internal abstract class ProfileListFragmentBase: Fragment() {
    protected abstract val mAdapter: ArrayAdapter<*>
    protected abstract val layout: Int
    protected val user by lazy { arguments!!.getSerializable("user") as User }
    protected lateinit var mListView: ListView
    protected var mAutoLoader = false
    protected lateinit var mFooter: ProgressBar
    protected var cursor = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)?.apply {
            mListView = findViewById<ListView>(R.id.list_view).apply {
                visibility = View.GONE
                adapter = mAdapter
                setOnScrollListener(object: AbsListView.OnScrollListener {
                    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                        // 最後までスクロールされたかどうかの判定
                        if (totalItemCount == firstVisibleItem + visibleItemCount) additionalReading()
                    }
                    override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {}
                })
            }
            showList()
            mFooter = findViewById(R.id.guruguru)
        }
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        showList()
    }

    protected abstract fun showList()
}