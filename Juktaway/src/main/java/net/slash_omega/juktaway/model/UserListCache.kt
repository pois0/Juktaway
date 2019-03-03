package net.slash_omega.juktaway.model

import jp.nephy.penicillin.models.TwitterList

object UserListCache {
    var userLists = mutableListOf<TwitterList>()

    fun getUserList(id: Long) = userLists.run { first { it.id == id } }
}
