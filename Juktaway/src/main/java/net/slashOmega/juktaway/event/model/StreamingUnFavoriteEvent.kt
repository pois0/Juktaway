package net.slashOmega.juktaway.event.model

import twitter4j.Status
import twitter4j.User

class StreamingUnFavoriteEvent(val user: User, val status: Status)
