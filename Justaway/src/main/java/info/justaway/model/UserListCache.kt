package info.justaway.model

import twitter4j.ResponseList
import twitter4j.UserList

object UserListCache {
    var userLists: ResponseList<UserList>? = null

    fun getUserList(id: Long) = userLists?.run { first { it.id == id } }
}
