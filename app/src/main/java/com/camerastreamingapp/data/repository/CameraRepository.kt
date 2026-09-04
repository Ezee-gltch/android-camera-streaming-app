package com.camerastreamingapp.data.repository

import com.camerastreamingapp.data.db.CameraDao
import com.camerastreamingapp.data.db.ConnectionDao
import com.camerastreamingapp.data.db.RecordingDao
import com.camerastreamingapp.data.db.RemoteAccessDao
import com.camerastreamingapp.data.models.Camera
import com.camerastreamingapp.data.models.Connection
import com.camerastreamingapp.data.models.Recording
import com.camerastreamingapp.data.models.RemoteAccessConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern for camera operations
 * Provides single point of access for data layer
 */
class CameraRepository(
    private val cameraDao: CameraDao,
    private val connectionDao: ConnectionDao,
    private val recordingDao: RecordingDao,
    private val remoteAccessDao: RemoteAccessDao
) {
    // Camera operations
    suspend fun addCamera(camera: Camera): Long = cameraDao.insert(camera)

    suspend fun updateCamera(camera: Camera) = cameraDao.update(camera)

    suspend fun deleteCamera(camera: Camera) = cameraDao.delete(camera)

    suspend fun getCameraById(id: Int): Camera? = cameraDao.getCameraById(id)

    fun getAllActiveCameras(): Flow<List<Camera>> = cameraDao.getAllActiveCameras()

    fun getAllCameras(): Flow<List<Camera>> = cameraDao.getAllCameras()

    // Connection operations
    suspend fun addConnection(connection: Connection): Long = connectionDao.insert(connection)

    suspend fun updateConnection(connection: Connection) = connectionDao.update(connection)

    fun getConnectionStatus(cameraId: Int): Flow<Connection?> = connectionDao.getConnectionStatus(cameraId)

    fun getConnectedCameras(): Flow<List<Connection>> = connectionDao.getConnectedCameras()

    suspend fun updateConnectionStatus(cameraId: Int, status: String, errorMessage: String = "") =
        connectionDao.updateStatus(cameraId, status, errorMessage)

    suspend fun incrementFailureCount(cameraId: Int) = connectionDao.incrementFailureCount(cameraId)

    suspend fun resetFailureCount(cameraId: Int, timestamp: Long = System.currentTimeMillis()) =
        connectionDao.resetFailureCount(cameraId, timestamp)

    // Recording operations
    suspend fun addRecording(recording: Recording): Long = recordingDao.insert(recording)

    suspend fun updateRecording(recording: Recording) = recordingDao.update(recording)

    suspend fun deleteRecording(recording: Recording) = recordingDao.delete(recording)

    suspend fun getRecordingById(id: Int): Recording? = recordingDao.getRecordingById(id)

    fun getRecordingsByCamera(cameraId: Int): Flow<List<Recording>> = recordingDao.getRecordingsByCamera(cameraId)

    fun getIncompleteRecordings(): Flow<List<Recording>> = recordingDao.getIncompleteRecordings()

    fun getRecentRecordings(limit: Int = 50): Flow<List<Recording>> = recordingDao.getRecentRecordings(limit)

    suspend fun deleteOldRecordings(timestamp: Long) = recordingDao.deleteOldRecordings(timestamp)

    suspend fun getTotalSizeByCamera(cameraId: Int): Long? = recordingDao.getTotalSizeByCamera(cameraId)

    // Remote access operations
    suspend fun addRemoteAccessConfig(config: RemoteAccessConfig): Long = remoteAccessDao.insert(config)

    suspend fun updateRemoteAccessConfig(config: RemoteAccessConfig) = remoteAccessDao.update(config)

    fun getRemoteAccessConfig(cameraId: Int): Flow<RemoteAccessConfig?> = remoteAccessDao.getRemoteAccessConfig(cameraId)

    suspend fun getRemoteAccessConfigOnce(cameraId: Int): RemoteAccessConfig? = 
        remoteAccessDao.getRemoteAccessConfigOnce(cameraId)

    suspend fun deleteRemoteAccessConfig(cameraId: Int) = remoteAccessDao.deleteByCamera(cameraId)
}
