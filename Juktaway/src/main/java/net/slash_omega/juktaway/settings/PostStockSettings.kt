package net.slash_omega.juktaway.settings

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.util.SharedPreference

/**
 * Created on 2018/11/18.
 */

object PostStockSettings {
    private var json by SharedPreference("post_settings", "data", "")
    private val stocks = json.takeIf { it.isNotBlank() }?.let {
        Gson().fromJson(it, PostStockSettingsData::class.java)
    } ?: PostStockSettingsData()

    //TODO +=などのoperatorを実装したい
    val hashtags
        get() = stocks.hashtags.reversed()
    val drafts
        get() = stocks.drafts.reversed()

    private fun savePostStockSettings() {
        GlobalScope.launch(Dispatchers.Default) {
            json = Gson().toJson(stocks)
        }
    }

    fun addHashtag(tag: String) {
        if (tag in stocks.hashtags) return
        stocks.hashtags.add(tag)
        savePostStockSettings()
    }

    fun removeHashtag(tag: String) {
        stocks.hashtags.remove(tag)
        savePostStockSettings()
    }

    fun addDraft(draft: String) {
        stocks.drafts.add(draft)
        savePostStockSettings()
    }

    fun removeDraft(draft: String) {
        stocks.drafts.remove(draft)
        savePostStockSettings()
    }

    class PostStockSettingsData {
        internal val hashtags: MutableList<String> = mutableListOf()
        internal val drafts: MutableList<String> = mutableListOf()
    }
}