package com.camerastreamingapp.domain.repository

import com.camerastreamingapp.data.db.daos.CameraDao
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.data.db.entities.CameraEntity
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import com.camerastreamingapp.domain.model.CameraModel
import com.camerastreamingapp.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CameraRepository(
    private val cameraDao: CameraDao,
    private val connectionDao: ConnectionDao
) {
    fun getAllCameras(): Flow<List<CameraModel>> =
        combine(cameraDao.getAllCameras(), connectionDao.getAllConnections()) { cameras, connections ->
            val connectionMap = connections.associateBy { it.cameraId }
            cameras.map { camera -> camera.toModel(connectionMap[camera.cameraId]) }
        }

    suspend fun getCameraById(id: Long): CameraModel? {
        val camera = cameraDao.getCameraById(id) ?: return null
        val connection = connectionDao.getByCameraIdOnce(id)
        return camera.toModel(connection)
    }

    suspend fun addCamera(camera: CameraModel): Long = cameraDao.insert(camera.toEntity())

    suspend fun updateCamera(camera: CameraModel) {
        cameraDao.update(camera.toEntity())
    }

    suspend fun deleteCamera(id: Long) {
        cameraDao.deleteById(id)
    }

    fun getCameraConnections(): Flow<Map<Long, ConnectionEntity>> =
        connectionDao.getAllConnections().map { list -> list.associateBy { it.cameraId } }

    private fun CameraEntity.toModel(connection: ConnectionEntity?): CameraModel = CameraModel(
        cameraId = cameraId,
        name = name,
        ipAddress = ipAddress,
        port = port,
        protocolType = protocolType,
        rtspUrl = rtspUrl,
        isActive = isActive,
        lastConnected = lastConnected,
        username = username,
        password = password,
        connectionState = connection.toConnectionState()
    )

    private fun CameraModel.toEntity(): CameraEntity = CameraEntity(
        cameraId = cameraId,
        name = name,
        ipAddress = ipAddress,
        port = port,
        username = username,
        password = password,
        rtspUrl = rtspUrl,
        protocolType = protocolType,
        isActive = isActive,
        lastConnected = lastConnected
    )

    private fun ConnectionEntity?.toConnectionState(): ConnectionState = when (this?.status) {
        "CONNECTED" -> ConnectionState.Connected
        "CONNECTING" -> ConnectionState.Connecting
        "RECONNECTING" -> ConnectionState.Reconnecting
        "FAILED" -> ConnectionState.Failed(this.errorMessage ?: "Connection failed", this.failureCount)
        "DISCONNECTED" -> ConnectionState.Disconnected
        else -> ConnectionState.Idle
    }
}
