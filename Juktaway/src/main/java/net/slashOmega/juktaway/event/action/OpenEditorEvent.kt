package net.slashOmega.juktaway.event.action

import jp.nephy.penicillin.models.Status

class OpenEditorEvent(val text: String, val inReplyToStatus: Status?, val selectionStart: Int?, val selectionStop: Int?)
