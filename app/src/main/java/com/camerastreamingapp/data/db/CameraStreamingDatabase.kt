package com.camerastreamingapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.camerastreamingapp.data.models.Camera
import com.camerastreamingapp.data.models.Connection
import com.camerastreamingapp.data.models.Recording
import com.camerastreamingapp.data.models.RemoteAccessConfig

/**
 * Room database for camera streaming app
 */
@Database(
    entities = [Camera::class, Connection::class, Recording::class, RemoteAccessConfig::class],
    version = 1,
    exportSchema = false
)
abstract class CameraStreamingDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun recordingDao(): RecordingDao
    abstract fun remoteAccessDao(): RemoteAccessDao

    companion object {
        @Volatile
        private var INSTANCE: CameraStreamingDatabase? = null

        fun getInstance(context: Context): CameraStreamingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CameraStreamingDatabase::class.java,
                    "camera_streaming_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
