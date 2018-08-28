package info.justaway.model

import twitter4j.Relationship
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.User

class Profile {
    var user: User? = null
    var relationship: Relationship? = null
    var statuses: ResponseList<Status>? = null
    var error: String? = null
}
