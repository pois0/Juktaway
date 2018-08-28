package info.justaway.model

import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.User

class UserListStatusesWithMembers(val statues: ResponseList<Status>, val members: ResponseList<User>)
