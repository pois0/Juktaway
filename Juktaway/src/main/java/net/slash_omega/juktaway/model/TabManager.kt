package net.slash_omega.juktaway.model

import android.content.Context
import com.google.gson.Gson
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.app
import net.slash_omega.juktaway.twitter.currentIdentifier
import java.util.ArrayList

object TabManager {
    const val TIMELINE_TAB_ID = -1L
    const val INTERACTIONS_TAB_ID = -2L
    const val DIRECT_MESSAGES_TAB_ID = -3L
    const val FAVORITES_TAB_ID = -4L
    const val SEARCH_TAB_ID = -5L

    private const val TABS = "tabs-"
    private var sTabs = arrayListOf<Tab>()

    private fun getSharedPreferences() = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun loadTabs(): ArrayList<Tab> {
        sTabs.clear()
        getSharedPreferences().getString(TABS + currentIdentifier.userId.toString() + "/v2", null).let { raw ->
            val gson = Gson()
            val tabData = gson.fromJson(raw, TabData::class.java)
            sTabs = (tabData?.tabs ?: arrayListOf()).apply {
                removeAll { it.id == DIRECT_MESSAGES_TAB_ID }
            }
            for (tab in sTabs) {
                if (tab.id <= SEARCH_TAB_ID) {
                    tab.id = SEARCH_TAB_ID - Math.abs(tab.name.hashCode())
                }
            }
        }
        if (sTabs.size == 0) sTabs = generalTabs
        return sTabs
    }

    fun saveTabs(tabs: ArrayList<Tab>) {
        val tabData = TabData()
        tabData.tabs = tabs
        val json = Gson().toJson(tabData)
        getSharedPreferences().edit().apply {
            remove(TABS + currentIdentifier.userId.toString())
            putString(TABS + currentIdentifier.userId.toString() + "/v2", json)
        }.apply()
        sTabs = tabs
    }

    private val generalTabs
        get() = arrayListOf(Tab(TIMELINE_TAB_ID), Tab(INTERACTIONS_TAB_ID), Tab(FAVORITES_TAB_ID))

    fun hasTabId(findTab: Long) = sTabs.any { it.id == findTab }

    class TabData {
        internal var tabs = arrayListOf<Tab>()
    }

    class Tab(var id: Long) {
        fun getString(id: Int): String = app.getString(id)

        var name = ""
            get() = when {
                id == TIMELINE_TAB_ID ->app.getString(R.string.title_main)
                id == INTERACTIONS_TAB_ID ->app.getString(R.string.title_interactions)
                id == DIRECT_MESSAGES_TAB_ID ->app.getString(R.string.title_direct_messages)
                id == FAVORITES_TAB_ID ->app.getString(R.string.title_favorites)
                id <= SEARCH_TAB_ID ->app.getString(R.string.title_search) + ":" + field
                else -> field
            }

        fun getIcon(): Int {
            return when {
                id == TIMELINE_TAB_ID -> R.string.fontello_home
                id == INTERACTIONS_TAB_ID -> R.string.fontello_at
                id == DIRECT_MESSAGES_TAB_ID -> R.string.fontello_mail
                id == FAVORITES_TAB_ID -> R.string.fontello_star
                id <= SEARCH_TAB_ID -> R.string.fontello_search
                else -> R.string.fontello_list
            }
        }
    }
}