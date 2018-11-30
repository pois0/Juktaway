package net.slashOmega.juktaway.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import kotlinx.android.synthetic.main.row_auto_complete.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.MainActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.model.TwitterManager
import net.slashOmega.juktaway.util.MessageUtil
import net.slashOmega.juktaway.util.nullToEmpty
import twitter4j.SavedSearch

/**
 * Created on 2018/11/17.
 */
class SearchAdapter(mContext: Context?, mLayout: Int) : ArrayAdapterBase<String>(mContext, mLayout), Filterable {
    private val mStrings = mutableListOf<String>()
    private val mSavedSearches = mutableListOf<SavedSearch>()
    private lateinit var mSearchWord: String
    var savedMode = false
        private set

    init {
        getSavedSearches()
    }

    override fun getItem(position: Int) = mStrings.getOrNull(position) ?: ""

    override fun getCount() = mStrings.size

    override val View.mView: (Int, ViewGroup?) -> Unit
        get() = { position , _ ->
            val item = getItem(position)
            word.text = item
            if (savedMode) {
                val search = mSavedSearches[position]
                trash.visibility = View.VISIBLE
                trash.setOnClickListener {
                    val activity = mContext as MainActivity
                    activity.cancelSearch()
                    AlertDialog.Builder(activity)
                            .setMessage(String.format(mContext.getString(R.string.confirm_destroy_saved_search), item))
                            .setPositiveButton(R.string.button_yes) { _, _ ->
                                GlobalScope.launch(Dispatchers.Main) {
                                    if (async(Dispatchers.Default) {
                                                try {
                                                    TwitterManager.twitter.destroySavedSearch(search.id)
                                                    true
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    false
                                                }
                                            }.await()) MessageUtil.showToast(R.string.toast_destroy_success)
                                }
                            }
                            .setNegativeButton(R.string.button_no) { _, _ -> }
                            .show()
                }
            } else {
                trash.visibility = View.GONE
            }
        }

    override fun getFilter(): Filter = object: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = Filter.FilterResults()
            val mSavedMode = constraint.isNullOrEmpty()
            if (mSavedMode) {
                mStrings.clear()
                for (savedSearch in mSavedSearches) {
                    mStrings.add(savedSearch.query)
                }
            } else {
                mSearchWord = constraint.toString()
                mStrings.apply {
                    clear()
                    add(mSearchWord + mContext?.getString(R.string.label_search_tweet).nullToEmpty())
                    add(mSearchWord + mContext?.getString(R.string.label_search_user).nullToEmpty())
                    add("@" + mSearchWord + mContext?.getString(R.string.label_display_profile).nullToEmpty())
                }
            }
            filterResults.values = mStrings
            filterResults.count = mStrings.size
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?): CharSequence =
            if (savedMode) resultValue as String else mSearchWord
    }

    fun reload() {
        getSavedSearches()
    }

    private fun getSavedSearches() {
        GlobalScope.launch(Dispatchers.Main) {
            async(Dispatchers.Default) {
                try {
                    TwitterManager.twitter.savedSearches
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.await()?.let {
                mSavedSearches.clear()
                for (search in it) {
                    mSavedSearches.add(0, search)
                }
            }
        }
    }
}