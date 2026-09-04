package com.camerastreamingapp.domain.connection

import androidx.lifecycle.LiveData
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import com.camerastreamingapp.domain.model.ConnectionConfig
import com.camerastreamingapp.domain.model.toConnectionConfig
import com.camerastreamingapp.domain.model.toPersistedJson
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

class ConnectionRepository(
    private val connectionDao: ConnectionDao,
    private val connectionManager: ConnectionManager
) {
    private val configStore = ConcurrentHashMap<Long, ConnectionConfig>()

    fun getConnectionState(cameraId: Long): StateFlow<ConnectionState> =
        connectionManager.getConnectionStateFlow(cameraId)

    suspend fun setConnectionConfig(cameraId: Long, config: ConnectionConfig) {
        configStore[cameraId] = config
        val connectionType = when (config) {
            is ConnectionConfig.LocalNetwork -> "LOCAL"
            is ConnectionConfig.VpnConfig -> "VPN"
            is ConnectionConfig.CloudRelayConfig -> "CLOUD_RELAY"
            is ConnectionConfig.PortForward -> "PORT_FORWARD"
        }
        val configJson = config.toPersistedJson()
        val updated = connectionDao.updateByCameraId(
            cameraId = cameraId,
            connectionType = connectionType,
            status = "DISCONNECTED",
            timestamp = System.currentTimeMillis(),
            failureCount = 0,
            nextRetryTime = null,
            configJson = configJson,
            errorMessage = null
        )
        if (updated == 0) {
            connectionDao.insert(
                ConnectionEntity(
                    cameraId = cameraId,
                    connectionType = connectionType,
                    status = "DISCONNECTED",
                    configJson = configJson,
                    errorMessage = null
                )
            )
        }
    }

    suspend fun getConnectionConfig(cameraId: Long): ConnectionConfig? =
        configStore[cameraId] ?: connectionDao.getByCameraIdOnce(cameraId)?.configJson?.toConnectionConfig()

    fun getAllConnectionStates(): LiveData<Map<Long, ConnectionState>> =
        connectionManager.connectionStatesLiveData
}
