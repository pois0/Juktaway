package net.slash_omega.juktaway.model

import jp.nephy.penicillin.models.TwitterList

data class UserListWithRegistered(val userList: TwitterList, var isRegistered: Boolean) {
    constructor(userList: TwitterList): this(userList, TabManager.isUserListRegistered(userList.id))
}