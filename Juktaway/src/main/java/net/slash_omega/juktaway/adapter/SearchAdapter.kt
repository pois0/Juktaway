package net.slash_omega.juktaway.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import jp.nephy.penicillin.endpoints.savedSearches
import jp.nephy.penicillin.endpoints.savedsearches.delete
import jp.nephy.penicillin.endpoints.savedsearches.list
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.SavedSearch
import kotlinx.android.synthetic.main.row_auto_complete.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.MainActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.twitter.currentClient
import net.slash_omega.juktaway.util.MessageUtil
import net.slash_omega.juktaway.util.nullToBlank

/**
 * Created on 2018/11/17.
 */
class SearchAdapter(mContext: Context, mLayout: Int) : ArrayAdapterBase<String>(mContext, mLayout), Filterable {
    private val mStrings = mutableListOf<String>()
    private val mSavedSearches = mutableListOf<SavedSearch>()
    private var mSearchWord: String = ""
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
                                    runCatching { currentClient.savedSearches.delete(search.id).await() }.onSuccess {
                                        MessageUtil.showToast(R.string.toast_destroy_success)
                                    }
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
            val filterResults = FilterResults()
            if (constraint.isNullOrEmpty()) {
                mStrings.clear()
                for (savedSearch in mSavedSearches) {
                    mStrings.add(savedSearch.query)
                }
            } else {
                mSearchWord = constraint.toString()
                mStrings.apply {
                    clear()
                    add(mSearchWord + mContext.getString(R.string.label_search_tweet).nullToBlank())
                    add(mSearchWord + mContext.getString(R.string.label_search_user).nullToBlank())
                    add("@" + mSearchWord + mContext.getString(R.string.label_display_profile).nullToBlank())
                }
            }
            return filterResults.apply {
                values = mStrings
                count = mStrings.size
            }
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

    fun reload() { getSavedSearches() }

    private fun getSavedSearches() {
        GlobalScope.launch(Dispatchers.Main) {
            runCatching { currentClient.savedSearches.list().await() }.onSuccess { savedList ->
                mSavedSearches.clear()
                savedList.forEach { mSavedSearches.add(0, it) }
            }
        }
    }
}
