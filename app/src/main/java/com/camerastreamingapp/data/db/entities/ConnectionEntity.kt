package com.camerastreamingapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "connections",
    foreignKeys = [
        ForeignKey(
            entity = CameraEntity::class,
            parentColumns = ["cameraId"],
            childColumns = ["cameraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cameraId"], unique = true)]
)
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true)
    val connectionId: Long = 0,
    val cameraId: Long,
    val connectionType: String,
    val status: String,
    val lastStatusChange: Long = System.currentTimeMillis(),
    val failureCount: Int = 0,
    val nextRetryTime: Long? = null
)
