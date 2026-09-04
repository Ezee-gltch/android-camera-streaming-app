package com.camerastreamingapp.domain.repository

import com.camerastreamingapp.data.db.daos.CameraDao
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.data.db.entities.CameraEntity
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import com.camerastreamingapp.domain.model.CameraModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CameraRepository(
    private val cameraDao: CameraDao,
    private val connectionDao: ConnectionDao
) {
    fun getAllCameras(): Flow<List<CameraModel>> = cameraDao.getAllCameras().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun getCameraById(id: Long): CameraModel? = cameraDao.getCameraById(id)?.toModel()

    suspend fun addCamera(camera: CameraModel): Long = cameraDao.insert(camera.toEntity())

    suspend fun updateCamera(camera: CameraModel) {
        cameraDao.update(camera.toEntity())
    }

    suspend fun deleteCamera(id: Long) {
        cameraDao.deleteById(id)
    }

    fun getCameraConnections(): Flow<Map<Long, ConnectionEntity>> =
        connectionDao.getAllConnections().map { list -> list.associateBy { it.cameraId } }

    private fun CameraEntity.toModel(): CameraModel = CameraModel(
        cameraId = cameraId,
        name = name,
        ipAddress = ipAddress,
        port = port,
        protocolType = protocolType,
        rtspUrl = rtspUrl,
        isActive = isActive,
        lastConnected = lastConnected,
        username = username,
        password = password
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
}
