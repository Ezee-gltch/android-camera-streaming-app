package com.camerastreamingapp.domain.connection

sealed class ConnectionState(
    open val retryCount: Int,
    open val timestamp: Long,
    open val errorMessage: String? = null
) {
    data class Idle(override val timestamp: Long = System.currentTimeMillis()) : ConnectionState(0, timestamp)
    data class Connecting(override val timestamp: Long = System.currentTimeMillis()) : ConnectionState(0, timestamp)
    data class Connected(override val timestamp: Long = System.currentTimeMillis()) : ConnectionState(0, timestamp)
    data class Reconnecting(
        override val retryCount: Int,
        val nextRetryAt: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ConnectionState(retryCount, timestamp)

    data class Failed(
        override val errorMessage: String,
        override val retryCount: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ConnectionState(retryCount, timestamp, errorMessage)

    data class Disconnected(override val timestamp: Long = System.currentTimeMillis()) : ConnectionState(0, timestamp)
}
