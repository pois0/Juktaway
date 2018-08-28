package info.justaway.event.connection

class StreamingConnectionEvent(val status: Status) {

    /**
     * 接続、クリーンナップ、切断
     */
    enum class Status {
        STREAMING_CONNECT,
        STREAMING_CLEANUP,
        STREAMING_DISCONNECT
    }

    companion object {
        fun onConnect(): StreamingConnectionEvent {
            return StreamingConnectionEvent(Status.STREAMING_CONNECT)
        }

        fun onCleanUp(): StreamingConnectionEvent {
            return StreamingConnectionEvent(Status.STREAMING_CLEANUP)
        }

        fun onDisconnect(): StreamingConnectionEvent {
            return StreamingConnectionEvent(Status.STREAMING_DISCONNECT)
        }
    }
}
