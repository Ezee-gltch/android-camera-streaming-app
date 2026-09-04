package com.camerastreamingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Data model representing an IP camera device
 */
@Entity(tableName = "cameras")
data class Camera(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ipAddress: String,
    val port: Int = 554,  // Default RTSP port
    val protocol: String = "RTSP",  // RTSP, HTTP, ONVIF
    val username: String = "",
    val password: String = "",
    val rtspUrl: String = "",
    val onvifUrl: String = "",
    val isActive: Boolean = true,
    val isRemoteAccessEnabled: Boolean = false,
    val remoteAccessType: String = "NONE",  // NONE, VPN, CLOUD_RELAY, PORT_FORWARD
    val remoteAccessId: String = "",  // VPN config ID, Cloud relay ID, or Port forward ID
    val lastConnected: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Data model representing a camera connection state
 */
@Entity(tableName = "camera_connections")
data class CameraConnection(
    @PrimaryKey
    val cameraId: Long,
    val isConnected: Boolean = false,
    val connectionType: String = "LOCAL",  // LOCAL, REMOTE_VPN, REMOTE_CLOUD
    val lastConnectionAttempt: LocalDateTime? = null,
    val lastSuccessfulConnection: LocalDateTime? = null,
    val failureCount: Int = 0,
    val errorMessage: String = "",
    val streamBitrate: Int = 0,  // in kbps
    val latency: Long = 0,  // in ms
    val signalStrength: Int = 0  // 0-100
)

/**
 * Data model representing a recorded video file
 */
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cameraId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,  // in bytes
    val duration: Long,  // in milliseconds
    val format: String = "MP4",
    val codec: String = "H264",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isEncrypted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Data model representing remote access configuration
 */
@Entity(tableName = "remote_access_configs")
data class RemoteAccessConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cameraId: Long,
    val configType: String,  // VPN, CLOUD_RELAY, PORT_FORWARD
    val isEnabled: Boolean = true,
    
    // VPN specific
    val vpnProvider: String = "",  // OpenVPN, WireGuard, etc
    val vpnConfigPath: String = "",
    val vpnUsername: String = "",
    val vpnPassword: String = "",
    
    // Cloud Relay specific
    val cloudRelayUrl: String = "",
    val cloudRelayToken: String = "",
    val cloudCameraId: String = "",
    
    // Port Forward specific
    val externalPort: Int = 0,
    val internalPort: Int = 0,
    val routerAddress: String = "",
    val routerUsername: String = "",
    val routerPassword: String = "",
    
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Data model for connection retry policy
 */
data class RetryPolicy(
    val maxRetries: Int = 5,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffMultiplier: Float = 2f,
    val retryOnNetworkChange: Boolean = true,
    val retryOnTimeout: Boolean = true
)

/**
 * Data model representing connection state
 */
data class ConnectionState(
    val cameraId: Long,
    val isConnected: Boolean,
    val connectionType: String,
    val errorMessage: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
