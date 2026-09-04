package com.camerastreamingapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = CameraEntity::class,
            parentColumns = ["cameraId"],
            childColumns = ["cameraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cameraId")]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val recordingId: Long = 0,
    val cameraId: Long,
    val fileName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long,
    val fileSize: Long,
    val isComplete: Boolean
)
