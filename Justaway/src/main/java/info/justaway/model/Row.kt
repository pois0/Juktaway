package info.justaway.model

import twitter4j.DirectMessage
import twitter4j.Status
import twitter4j.User

class Row {

    var status: Status? = null
    var message: DirectMessage? = null
    var source: User? = null
    var target: User? = null
    var type: Int = 0

    val isStatus: Boolean
        get() = type == TYPE_STATUS

    val isFavorite: Boolean
        get() = type == TYPE_FAVORITE

    val isDirectMessage: Boolean
        get() = type == TYPE_DM

    companion object {

        private const val TYPE_STATUS = 0
        private const val TYPE_FAVORITE = 1
        private const val TYPE_DM = 2

        fun newStatus(status: Status): Row {
            val row = Row()
            row.status = status
            row.type = TYPE_STATUS
            return row
        }

        fun newFavorite(source: User, target: User, status: Status): Row {
            val row = Row()
            row.status = status
            row.target = target
            row.source = source
            row.type = TYPE_FAVORITE
            return row
        }

        fun newDirectMessage(message: DirectMessage): Row {
            val row = Row()
            row.message = message
            row.type = TYPE_DM
            return row
        }
    }
}
