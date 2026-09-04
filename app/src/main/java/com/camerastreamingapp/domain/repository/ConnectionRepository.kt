package com.camerastreamingapp.domain.repository

import androidx.lifecycle.LiveData
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import com.camerastreamingapp.domain.connection.ConnectionManager
import com.camerastreamingapp.domain.connection.ConnectionState
import com.camerastreamingapp.domain.model.ConnectionConfig
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

class ConnectionRepository(
    private val connectionDao: ConnectionDao,
    private val connectionManager: ConnectionManager
) {
    private val connectionConfigs = ConcurrentHashMap<Long, ConnectionConfig>()

    fun getConnectionState(cameraId: Long): StateFlow<ConnectionState> =
        connectionManager.getConnectionStateFlow(cameraId)

    suspend fun setConnectionConfig(cameraId: Long, config: ConnectionConfig) {
        connectionConfigs[cameraId] = config
        connectionDao.insert(
            ConnectionEntity(
                cameraId = cameraId,
                connectionType = config.toConnectionType(),
                status = "DISCONNECTED"
            )
        )
    }

    suspend fun getConnectionConfig(cameraId: Long): ConnectionConfig? = connectionConfigs[cameraId]

    fun getAllConnectionStates(): LiveData<Map<Long, ConnectionState>> =
        connectionManager.connectionStatesLiveData

    private fun ConnectionConfig.toConnectionType(): String = when (this) {
        is ConnectionConfig.LocalNetwork -> "LOCAL"
        is ConnectionConfig.VpnConfig -> "VPN"
        is ConnectionConfig.CloudRelayConfig -> "CLOUD_RELAY"
        is ConnectionConfig.PortForward -> "PORT_FORWARD"
    }
}
