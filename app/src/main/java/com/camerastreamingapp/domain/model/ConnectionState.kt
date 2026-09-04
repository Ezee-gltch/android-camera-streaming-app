package com.camerastreamingapp.domain.model

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data object Reconnecting : ConnectionState()
    data class Failed(val error: String, val retryCount: Int) : ConnectionState()
    data object Disconnected : ConnectionState()
}
