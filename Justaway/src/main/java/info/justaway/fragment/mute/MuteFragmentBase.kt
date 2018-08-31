package info.justaway.fragment.mute

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import info.justaway.R

/**
 * Created on 2018/08/31.
 */
internal abstract class MuteFragmentBase<T>: Fragment() {
    internal lateinit var mAdapter: MuteTargetAdapter<T>
    internal abstract val currentMuteTargets: List<T>
    internal abstract fun getAdapter(): MuteTargetAdapter<T>
    internal abstract val resource: Int
    internal abstract val listId: Int
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mAdapter = getAdapter()
        return inflater.inflate(resource, container, false)?.apply {
            findViewById<ListView>(listId).adapter = mAdapter
            currentMuteTargets.forEach { mAdapter.add(it) }
        }
    }
}