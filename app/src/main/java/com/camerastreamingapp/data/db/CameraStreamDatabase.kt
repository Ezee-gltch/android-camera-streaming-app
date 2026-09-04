package com.camerastreamingapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.camerastreamingapp.data.db.daos.CameraDao
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.data.db.daos.RecordingDao
import com.camerastreamingapp.data.db.entities.CameraEntity
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import com.camerastreamingapp.data.db.entities.RecordingEntity

@Database(
    entities = [CameraEntity::class, ConnectionEntity::class, RecordingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CameraStreamDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: CameraStreamDatabase? = null

        fun getDatabase(context: Context): CameraStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CameraStreamDatabase::class.java,
                    "camera_stream_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
