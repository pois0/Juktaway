package net.slashOmega.juktaway.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

/**
 * Created on 2018/10/20.
 */
abstract class ArrayAdapterBase<T>(protected val mContext: Context?, protected val mLayout: Int) : ArrayAdapter<T>(mContext, mLayout) {
    protected val mInflater by lazy { mContext?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater }
    protected abstract val View.mView: (Int, ViewGroup?) -> Unit
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?)
            = (convertView ?: mInflater?.inflate(mLayout, null))?.apply { mView(position, parent) }
}