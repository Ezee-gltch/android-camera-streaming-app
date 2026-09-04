package com.camerastreamingapp.domain.model

data class CameraModel(
    val cameraId: Long,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val protocolType: String,
    val rtspUrl: String,
    val isActive: Boolean,
    val lastConnected: Long?,
    val username: String? = null,
    val password: String? = null,
    val connectionState: ConnectionState = ConnectionState.Idle
)
