package info.justaway.event.action

import twitter4j.Status

class OpenEditorEvent(val text: String, val inReplyToStatus: Status?, val selectionStart: Int?, val selectionStop: Int?)
