package com.camerastreamingapp.domain.connection

import androidx.lifecycle.LiveData
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.domain.model.ConnectionConfig
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
    }

    suspend fun getConnectionConfig(cameraId: Long): ConnectionConfig? = configStore[cameraId]

    fun getAllConnectionStates(): LiveData<Map<Long, ConnectionState>> =
        connectionManager.connectionStatesLiveData
}
