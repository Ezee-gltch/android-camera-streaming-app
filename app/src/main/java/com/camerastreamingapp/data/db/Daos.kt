package com.camerastreamingapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.camerastreamingapp.data.models.Camera
import com.camerastreamingapp.data.models.Connection
import com.camerastreamingapp.data.models.Recording
import com.camerastreamingapp.data.models.RemoteAccessConfig
import kotlinx.coroutines.flow.Flow

/**
 * Camera DAO for database operations
 */
@Dao
interface CameraDao {
    @Insert
    suspend fun insert(camera: Camera): Long

    @Update
    suspend fun update(camera: Camera)

    @Delete
    suspend fun delete(camera: Camera)

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: Int): Camera?

    @Query("SELECT * FROM cameras WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCameras(): Flow<List<Camera>>

    @Query("SELECT * FROM cameras ORDER BY name ASC")
    fun getAllCameras(): Flow<List<Camera>>

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteCameraById(id: Int)
}

/**
 * Connection DAO for tracking connection status
 */
@Dao
interface ConnectionDao {
    @Insert
    suspend fun insert(connection: Connection): Long

    @Update
    suspend fun update(connection: Connection)

    @Query("SELECT * FROM connections WHERE cameraId = :cameraId")
    fun getConnectionStatus(cameraId: Int): Flow<Connection?>

    @Query("SELECT * FROM connections WHERE status = 'CONNECTED'")
    fun getConnectedCameras(): Flow<List<Connection>>

    @Query("UPDATE connections SET status = :status, errorMessage = :errorMessage WHERE cameraId = :cameraId")
    suspend fun updateStatus(cameraId: Int, status: String, errorMessage: String = "")

    @Query("UPDATE connections SET failureCount = failureCount + 1 WHERE cameraId = :cameraId")
    suspend fun incrementFailureCount(cameraId: Int)

    @Query("UPDATE connections SET failureCount = 0, connectedAt = :timestamp WHERE cameraId = :cameraId")
    suspend fun resetFailureCount(cameraId: Int, timestamp: Long = System.currentTimeMillis())
}

/**
 * Recording DAO for managing recordings
 */
@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: Recording): Long

    @Update
    suspend fun update(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Int): Recording?

    @Query("SELECT * FROM recordings WHERE cameraId = :cameraId ORDER BY startTime DESC")
    fun getRecordingsByCamera(cameraId: Int): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isComplete = 0")
    fun getIncompleteRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings ORDER BY startTime DESC LIMIT :limit")
    fun getRecentRecordings(limit: Int = 50): Flow<List<Recording>>

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Int)

    @Query("DELETE FROM recordings WHERE startTime < :timestamp")
    suspend fun deleteOldRecordings(timestamp: Long)

    @Query("SELECT SUM(fileSize) FROM recordings WHERE cameraId = :cameraId")
    suspend fun getTotalSizeByCamera(cameraId: Int): Long?
}

/**
 * Remote Access DAO
 */
@Dao
interface RemoteAccessDao {
    @Insert
    suspend fun insert(config: RemoteAccessConfig): Long

    @Update
    suspend fun update(config: RemoteAccessConfig)

    @Query("SELECT * FROM remote_access WHERE cameraId = :cameraId")
    fun getRemoteAccessConfig(cameraId: Int): Flow<RemoteAccessConfig?>

    @Query("SELECT * FROM remote_access WHERE cameraId = :cameraId")
    suspend fun getRemoteAccessConfigOnce(cameraId: Int): RemoteAccessConfig?

    @Query("DELETE FROM remote_access WHERE cameraId = :cameraId")
    suspend fun deleteByCamera(cameraId: Int)
}