package com.camerastreamingapp.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.camerastreamingapp.data.db.entities.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE recordingId = :recordingId")
    suspend fun getById(recordingId: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE cameraId = :cameraId ORDER BY startTime DESC")
    fun getByCameraId(cameraId: Long): Flow<List<RecordingEntity>>

    @Query("DELETE FROM recordings WHERE endTime IS NOT NULL AND endTime < :olderThan")
    suspend fun deleteOldRecordings(olderThan: Long)
}
