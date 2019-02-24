package net.slash_omega.juktaway.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import net.slash_omega.juktaway.util.SharedPreference

/**
 * Created on 2018/11/18.
 */

object PostStockSettings {
    private var json by SharedPreference("post_settings", "data", "")

    @UseExperimental(ImplicitReflectionSerializer::class)
    private val stocks = json.takeIf { it.isNotBlank() }?.let {
        Json.parse<PostStockSettingsData>(it)
    } ?: PostStockSettingsData()

    //TODO +=などのoperatorを実装したい
    val hashtags
        get() = stocks.hashtags.reversed().toMutableList()
    val drafts
        get() = stocks.drafts.reversed().toMutableList()

    @UseExperimental(ImplicitReflectionSerializer::class)
    private fun savePostStockSettings() {
        GlobalScope.launch(Dispatchers.Default) {
            json = Json.stringify(stocks)
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

    @Serializable
    data class PostStockSettingsData(
            @Optional internal val hashtags: MutableList<String> = mutableListOf(),
            @Optional internal val drafts: MutableList<String> = mutableListOf()
    )
}