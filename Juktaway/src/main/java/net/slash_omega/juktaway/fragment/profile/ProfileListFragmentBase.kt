package net.slash_omega.juktaway.fragment.profile

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.models.User
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.util.parseWithClient

/**
 * Created on 2018/09/01.
 */
internal abstract class ProfileListFragmentBase: Fragment() {
    protected abstract val mAdapter: ArrayAdapter<*>
    protected abstract val layout: Int
    protected val user by lazy { arguments!!.getString("user")!!.toJsonObject().parseWithClient<User>() }
    protected lateinit var mListView: ListView
    protected var mAutoLoader = false
    protected lateinit var mFooter: ProgressBar
    private var isLoading = false

    protected fun finishLoading() {
        mFooter.visibility = View.GONE
        Handler().postDelayed({ isLoading = false }, 200)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)?.apply {
            mListView = findViewById<ListView>(R.id.list_view).apply {
                visibility = View.GONE
                adapter = mAdapter
                setOnScrollListener(object: AbsListView.OnScrollListener {
                    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                        // 最後までスクロールされたかどうかの判定
                        if (totalItemCount == firstVisibleItem + visibleItemCount && totalItemCount > 5 && !isLoading) {
                            additionalReading()
                        }
                    }
                    override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {}
                })
            }
            init()
            mFooter = findViewById(R.id.guruguru)
            showList()
        }
    }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        isLoading = true
        showList()
    }

    protected abstract fun showList()

    protected open fun View.init() {}
}