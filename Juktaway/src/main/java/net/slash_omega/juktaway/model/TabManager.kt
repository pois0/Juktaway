package net.slash_omega.juktaway.model

import com.google.gson.Gson
import jp.nephy.penicillin.models.TwitterList
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.app
import net.slash_omega.juktaway.twitter.currentIdentifier
import net.slash_omega.juktaway.util.SharedPreference
import java.util.ArrayList

const val HOME_TAB_ID = 0
const val MENTION_TAB_ID = 1
const val FAVORITE_TAB_ID = 2
const val DM_TAB_ID = 3
const val SEARCH_TAB_ID = 4
const val LIST_TAB_ID = 5
const val USER_TAB_ID = 6

object TabManager {
    const val OLD_TIMELINE_TAB_ID = -1L
    const val OLD_INTERACTIONS_TAB_ID = -2L
    const val OLD_DIRECT_MESSAGES_TAB_ID = -3L
    const val OLD_FAVORITES_TAB_ID = -4L
    const val OLD_SEARCH_TAB_ID = -5L

    private const val TABS = "mTabs-"
    private var mTabs = mutableListOf<Tab>()
    private var tabPreference by SharedPreference("settings", TABS + currentIdentifier.userId.toString() + "/v3", "")
    private var oldTabPreference by SharedPreference("settings", TABS + currentIdentifier.userId.toString() + "/v2", "")

    fun loadTabs() = mTabs.also { list ->
        list.clear()
        list.addAll(
                oldTabPreference.takeIf { it.isNotBlank() }?.let { oldJson ->
                    Gson().fromJson(oldJson, OldTabData::class.java)?.tabs?.let { data ->
                        data.removeAll { it.id == OLD_DIRECT_MESSAGES_TAB_ID }
                        translateTab(data)
                    }
                } ?: tabPreference.takeIf { it.isNotBlank() }?.let { json ->
                    Gson().fromJson(json, TabData::class.java)?.tabs
                } ?: generalTabs
        )
    }

    fun reinitialize(tabs: List<Tab>) {
        mTabs.clear()
        mTabs.addAll(tabs.filterNot { it.type == DM_TAB_ID })
        saveTabs()
    }

    fun addTab(tab: Tab) {
        mTabs.add(tab)
        saveTabs()
    }

    fun addSearchTab(searchWord: String) = addTab(Tab(SEARCH_TAB_ID, 0, searchWord, -1))

    private fun saveTabs() {
        tabPreference = Gson().toJson(TabData(ArrayList(mTabs)))
    }

    private fun translateTab(list: List<OldTab>) = list.map {
        when {
            it.id == TabManager.OLD_TIMELINE_TAB_ID -> homeTab
            it.id == TabManager.OLD_INTERACTIONS_TAB_ID -> mentionTab
            it.id == TabManager.OLD_DIRECT_MESSAGES_TAB_ID -> favoriteTab
            it.id == TabManager.OLD_FAVORITES_TAB_ID -> dmTab
            it.id <= TabManager.OLD_SEARCH_TAB_ID ->Tab(SEARCH_TAB_ID, 0, it.name, -1)
            else -> Tab(LIST_TAB_ID, it.id, it.name, -1)
        }
    }

    private val generalTabs
        get() = listOf(homeTab, mentionTab, favoriteTab)

    fun isUserListRegistered(id: Long) = mTabs.any { it.type == LIST_TAB_ID && it.id == id }
}

private data class OldTabData(val tabs: ArrayList<OldTab>)

private data class TabData(val tabs: ArrayList<Tab>)

class OldTab(var id: Long) {
    fun getString(id: Int): String = app.getString(id)

    var name = ""
}

data class Tab(val type: Int, val id: Long, val word: String, val autoReload: Int) {
    override fun equals(other: Any?) = other is Tab && type == other.type && when (type) {
        4 -> word == other.word
        5, 6 -> id == other.id
        else -> true
    }
}

val Tab.icon: Int
    get() = when (type) {
        HOME_TAB_ID -> R.string.fontello_home
        MENTION_TAB_ID -> R.string.fontello_at
        DM_TAB_ID -> R.string.fontello_mail
        FAVORITE_TAB_ID -> R.string.fontello_star
        SEARCH_TAB_ID -> R.string.fontello_search
        LIST_TAB_ID -> R.string.fontello_list
        else -> 0
    }

val Tab.displayString: String
    get() = when (type) {
        HOME_TAB_ID ->app.getString(R.string.title_main)
        MENTION_TAB_ID ->app.getString(R.string.title_interactions)
        DM_TAB_ID ->app.getString(R.string.title_direct_messages)
        FAVORITE_TAB_ID ->app.getString(R.string.title_favorites)
        SEARCH_TAB_ID ->app.getString(R.string.title_search) + ": " + word
        LIST_TAB_ID -> word
        USER_TAB_ID -> app.getString(R.string.title_user) + ": " + word
        else -> ""
    }

val homeTab: Tab
    get() = Tab(HOME_TAB_ID, 0, "", -1)

val mentionTab: Tab
    get() = Tab(MENTION_TAB_ID, 0, "", -1)

val favoriteTab: Tab
    get() = Tab(FAVORITE_TAB_ID, 0, "", -1)

val dmTab: Tab
    get() = Tab(DM_TAB_ID, 0, "", -1)

fun TwitterList.toTab() = Tab(LIST_TAB_ID, id, if (user.id == currentIdentifier.userId) name else fullName, -1)
