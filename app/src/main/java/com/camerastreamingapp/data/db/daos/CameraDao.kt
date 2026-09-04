package com.camerastreamingapp.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.camerastreamingapp.data.db.entities.CameraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(camera: CameraEntity): Long

    @Update
    suspend fun update(camera: CameraEntity)

    @Delete
    suspend fun delete(camera: CameraEntity)

    @Query("SELECT * FROM cameras WHERE cameraId = :cameraId")
    suspend fun getCameraById(cameraId: Long): CameraEntity?

    @Query("SELECT * FROM cameras ORDER BY createdAt DESC")
    fun getAllCameras(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCameras(): Flow<List<CameraEntity>>

    @Query("DELETE FROM cameras WHERE cameraId = :cameraId")
    suspend fun deleteById(cameraId: Long)
}
