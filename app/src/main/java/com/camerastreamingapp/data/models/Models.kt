package com.camerastreamingapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Camera configuration data model
 */
@Entity(tableName = "cameras")
data class Camera(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ipAddress: String,
    val port: Int = 554, // Default RTSP port
    val username: String = "",
    val password: String = "",
    val rtspPath: String = "/stream1", // Camera-specific path
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable {
    val rtspUrl: String
        get() {
            val auth = if (username.isNotEmpty() && password.isNotEmpty()) {
                "$username:$password@"
            } else {
                ""
            }
            return "rtsp://$auth$ipAddress:$port$rtspPath"
        }
}

/**
 * Connection status tracking
 */
@Entity(tableName = "connections")
data class Connection(
    @PrimaryKey
    val cameraId: Int,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastConnectAttempt: Long = 0L,
    val failureCount: Int = 0,
    val errorMessage: String = "",
    val connectedAt: Long? = null,
    val bandwidth: Float = 0f // kbps
) : Serializable

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR,
    RECONNECTING
}

/**
 * Recording metadata
 */
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cameraId: Int,
    val cameraName: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long = 0L, // Bytes
    val duration: Long = 0L, // Milliseconds
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val isComplete: Boolean = false,
    val thumbnailPath: String? = null
) : Serializable

/**
 * Remote access configuration
 */
@Entity(tableName = "remote_access")
data class RemoteAccessConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cameraId: Int,
    val accessMethod: AccessMethod = AccessMethod.LOCAL_NETWORK,
    val vpnEnabled: Boolean = false,
    val vpnProfile: String? = null, // VPN config file path
    val cloudRelayUrl: String? = null, // Cloud relay server URL
    val cloudRelayToken: String? = null, // Auth token for cloud relay
    val portForwardingEnabled: Boolean = false,
    val externalPort: Int? = null,
    val natTraversalEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

enum class AccessMethod {
    LOCAL_NETWORK,      // Direct connection on home WiFi
    VPN,               // Through VPN tunnel
    CLOUD_RELAY,       // Through cloud relay server
    PORT_FORWARDING    // Direct port forwarding
}