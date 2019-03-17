package net.slash_omega.juktaway.model

import jp.nephy.penicillin.models.TwitterList
import jp.nephy.penicillin.models.User
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
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
    private const val OLD_TIMELINE_TAB_ID = -1L
    private const val OLD_INTERACTIONS_TAB_ID = -2L
    private const val OLD_DIRECT_MESSAGES_TAB_ID = -3L
    private const val OLD_FAVORITES_TAB_ID = -4L
    private const val OLD_SEARCH_TAB_ID = -5L

    private const val TABS = "mTabs-"
    private var mTabs = mutableListOf<Tab>()
    private var tabPreference by SharedPreference("settings", TABS + currentIdentifier.userId.toString() + "/v3", "")
    private var oldTabPreference by SharedPreference("settings", TABS + currentIdentifier.userId.toString() + "/v2", "")

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun loadTabs(): List<Tab> = mTabs.takeIf { it.isNotEmpty() } ?: run {
                tabPreference.takeIf { it.isNotBlank() }?.let { json ->
                    println(json)
                    Json.parseList<Tab>(json)
                } ?: oldTabPreference.takeIf { it.isNotBlank() }?.let { oldJson ->
                    Json.parse<OldTabData>(oldJson).let { data ->
                        oldTabPreference = ""
                        data.tabs.removeAll { it.id == OLD_DIRECT_MESSAGES_TAB_ID }
                        translateTab(data.tabs)
                    }
                } ?: generalTabs
            }.also { mTabs = it.toMutableList() }

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

    fun addUserTab(user: User) = addTab(Tab(USER_TAB_ID, user.id, user.screenName, -1))

    fun refreshUserTab(user: User) {
        var i = 0
        for (tab in mTabs) {
            if (tab.type == USER_TAB_ID && tab.id == user.id) {
                if (tab.word == user.screenName) return else break
            }
            i++
        }
        mTabs[i] = Tab(USER_TAB_ID, user.id, user.screenName, -1)
        saveTabs()
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    private fun saveTabs() {
        tabPreference = Json.stringify(mTabs)
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

@Serializable
private data class OldTabData(val tabs: ArrayList<OldTab>)

@Serializable
class OldTab(var id: Long, @Optional var name: String = "")

@Serializable
data class Tab(val type: Int, val id: Long, val word: String, val autoReload: Int) {
    override fun equals(other: Any?) = other is Tab && type == other.type && when (type) {
        SEARCH_TAB_ID -> word == other.word
        LIST_TAB_ID, USER_TAB_ID -> id == other.id
        else -> true
    }

    override fun hashCode() = type + 29 * word.hashCode() + id.hashCode()
}

val Tab.icon: Int
    get() = when (type) {
        HOME_TAB_ID -> R.drawable.ic_home
        MENTION_TAB_ID -> R.drawable.ic_atmark
        DM_TAB_ID -> R.drawable.ic_email
        FAVORITE_TAB_ID -> R.drawable.ic_star
        SEARCH_TAB_ID -> R.drawable.ic_search
        LIST_TAB_ID -> R.drawable.ic_list_bulleted
        USER_TAB_ID -> R.drawable.ic_person
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
