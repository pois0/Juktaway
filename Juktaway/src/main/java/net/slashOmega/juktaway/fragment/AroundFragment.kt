package net.slashOmega.juktaway.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.penicillin.models.Status
import kotlinx.android.synthetic.main.fragment_around.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.model.Row
import net.slashOmega.juktaway.twitter.currentClient
import net.slashOmega.juktaway.util.MessageUtil

class AroundFragment: DialogFragment() {
    private lateinit var mProgressBarTop: ProgressBar
    private lateinit var mProgressBarBottom: ProgressBar
    private val mAdapter by lazy { StatusAdapter(activity!!) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(activity!!).apply {
        window?.requestFeature(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        setContentView(R.layout.fragment_around)

        mProgressBarTop = findViewById<View>(R.id.guruguru_top) as ProgressBar
        mProgressBarBottom = findViewById<View>(R.id.guruguru_bottom) as ProgressBar

        list.apply {
            registerForContextMenu(this)
            adapter = mAdapter
            activity?.let { a ->
                onItemClickListener = StatusClickListener(a)
                onItemLongClickListener = StatusLongClickListener(a)
            }
            arguments?.getString("status")?.toJsonObject()?.parse<Status>()?.let { origin ->
                mAdapter.add(Row.newStatus(origin))
                GlobalScope.launch(Dispatchers.Main) {
                    val beforeList = runCatching {
                        currentClient.timeline.user(origin.user.id, count = 3, maxId = origin.id - 1).await()
                    }.getOrNull() ?: run {
                        MessageUtil.showToast(R.string.toast_load_data_failure)
                        return@launch
                    }
                    mProgressBarBottom.visibility = View.GONE

                    if (beforeList.isEmpty()) return@launch
                    mAdapter.addAll(beforeList.map { Row.newStatus(it) })
                    mAdapter.notifyDataSetChanged()
                    val afterList = runCatching {
                        var lastId = beforeList[0].id - 1
                        for(i in 0 until 5) {
                            val statuses = currentClient.timeline.user(origin.user.id, count = 200, maxId = lastId).await()
                            for ((j, row) in statuses.withIndex()) {
                                if (row.id == origin.id && j > 0) return@runCatching statuses.subList(Math.max(0, j - 4), j - 1)
                            }
                            lastId = statuses.last().id - 1
                        }
                        listOf<Status>()
                    }.getOrNull() ?: run {
                        MessageUtil.showToast(R.string.toast_load_data_failure)
                        return@launch
                    }

                    mProgressBarTop.visibility = View.GONE

                    if (afterList.isEmpty()) return@launch

                    afterList.map { Row.newStatus(it) }.forEachIndexed { i, status ->
                        mAdapter.insert(status, i)
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}