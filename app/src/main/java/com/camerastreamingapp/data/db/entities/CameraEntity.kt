package com.camerastreamingapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.camerastreamingapp.config.AppConfig

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey(autoGenerate = true)
    val cameraId: Long = 0,
    val name: String,
    val ipAddress: String,
    val port: Int = AppConfig.DEFAULT_RTSP_PORT,
    val username: String?,
    val password: String?,
    val rtspUrl: String,
    val protocolType: String,
    val isActive: Boolean = true,
    val lastConnected: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
