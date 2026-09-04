package com.camerastreamingapp.domain.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.camerastreamingapp.config.AppConfig
import com.camerastreamingapp.data.api.CameraStreamManager
import com.camerastreamingapp.data.db.daos.CameraDao
import com.camerastreamingapp.data.db.daos.ConnectionDao
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import com.camerastreamingapp.domain.model.ConnectionConfig
import com.camerastreamingapp.util.CameraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(
    private val cameraDao: CameraDao,
    private val connectionDao: ConnectionDao,
    private val cameraStreamManager: CameraStreamManager,
    private val context: Context
) {
    private val connectionStates = MutableLiveData<Map<Long, ConnectionState>>(emptyMap())
    val connectionStatesLiveData: LiveData<Map<Long, ConnectionState>> = connectionStates

    private val stateFlows = ConcurrentHashMap<Long, MutableStateFlow<ConnectionState>>()
    private val retryJobs = ConcurrentHashMap<Long, Job>()
    private val retryCounts = ConcurrentHashMap<Long, Int>()
    private val connectionConfigs = ConcurrentHashMap<Long, ConnectionConfig>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val backoffDelays = longArrayOf(1000L, 2000L, 4000L, 8000L, 16000L, 60000L)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        scope.launch {
            connectionDao.getAllConnections().collectLatest { persisted ->
                val restored = persisted.associate { connection ->
                    connection.cameraId to connection.toDomainState()
                }
                connectionStates.postValue(restored)
                restored.forEach { (cameraId, state) ->
                    stateFlows.getOrPut(cameraId) { MutableStateFlow(state) }.value = state
                }
            }
        }
    }

    fun connectToCamera(cameraId: Long, config: ConnectionConfig): StateFlow<ConnectionState> {
        connectionConfigs[cameraId] = config
        val flow = stateFlows.getOrPut(cameraId) { MutableStateFlow(ConnectionState.Idle()) }

        scope.launch {
            emitState(cameraId, ConnectionState.Connecting())
            val camera = cameraDao.getCameraById(cameraId)
            if (camera == null) {
                emitState(cameraId, ConnectionState.Failed("Camera not found", retryCounts[cameraId] ?: 0))
                return@launch
            }

            val rtspUrl = buildRtspUrl(cameraId, camera.ipAddress, camera.port, camera.username, camera.password, config)
            val connected = runCatching { cameraStreamManager.play(cameraId, rtspUrl) }
                .getOrElse {
                    CameraLogger.error("Failed stream startup for camera $cameraId", it)
                    false
                }

            if (connected) {
                retryCounts[cameraId] = 0
                val now = System.currentTimeMillis()
                emitState(cameraId, ConnectionState.Connected(now))
                persistStatus(cameraId, config, "CONNECTED", 0, null, now)
            } else {
                onConnectionFailure(cameraId, config, "Unable to start stream")
            }
        }

        return flow.asStateFlow()
    }

    fun disconnectFromCamera(cameraId: Long) {
        retryJobs.remove(cameraId)?.cancel()
        scope.launch {
            cameraStreamManager.stop(cameraId)
            emitState(cameraId, ConnectionState.Disconnected())
            persistStatus(cameraId, connectionConfigs[cameraId], "DISCONNECTED", 0, null, System.currentTimeMillis())
        }
    }

    fun getConnectionState(cameraId: Long): ConnectionState =
        stateFlows[cameraId]?.value ?: ConnectionState.Idle()

    fun getAllConnectionStates(): Map<Long, ConnectionState> = connectionStates.value.orEmpty()

    fun getConnectionStateFlow(cameraId: Long): StateFlow<ConnectionState> =
        stateFlows.getOrPut(cameraId) { MutableStateFlow(ConnectionState.Idle()) }.asStateFlow()

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun initializeAutoReconnect() {
        if (networkCallback != null) return

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                CameraLogger.network("Network available")
                retryCounts.clear()
                stateFlows.forEach { (cameraId, stateFlow) ->
                    if (stateFlow.value is ConnectionState.Failed || stateFlow.value is ConnectionState.Disconnected) {
                        attemptReconnect(cameraId)
                    }
                }
            }

            override fun onLost(network: Network) {
                CameraLogger.network("Network lost")
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    fun attemptReconnect(cameraId: Long) {
        val config = connectionConfigs[cameraId] ?: ConnectionConfig.LocalNetwork
        retryJobs.remove(cameraId)?.cancel()
        retryJobs[cameraId] = scope.launch {
            val retryCount = retryCounts[cameraId] ?: 0
            if (retryCount >= AppConfig.MAX_RETRY_ATTEMPTS) {
                emitState(cameraId, ConnectionState.Failed("Max retry attempts reached", retryCount))
                persistStatus(cameraId, config, "FAILED", retryCount, null, System.currentTimeMillis())
                return@launch
            }

            val delayMs = backoffDelays.getOrElse(retryCount) { AppConfig.MAX_BACKOFF_MS }
            val nextRetryAt = System.currentTimeMillis() + delayMs
            emitState(cameraId, ConnectionState.Reconnecting(retryCount + 1, nextRetryAt))
            persistStatus(cameraId, config, "RECONNECTING", retryCount + 1, nextRetryAt, System.currentTimeMillis())
            delay(delayMs)

            try {
                connectToCamera(cameraId, config)
            } catch (timeout: SocketTimeoutException) {
                onConnectionFailure(cameraId, config, "Connection timeout")
            } catch (security: SecurityException) {
                onConnectionFailure(cameraId, config, "Authentication failed")
            } catch (throwable: Throwable) {
                onConnectionFailure(cameraId, config, throwable.message ?: "Unknown stream error")
            }
        }
    }

    fun stopAutoReconnect() {
        networkCallback?.let {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()
    }

    fun clear() {
        stopAutoReconnect()
        cameraStreamManager.releaseAll()
        scope.cancel()
    }

    private suspend fun onConnectionFailure(cameraId: Long, config: ConnectionConfig, reason: String) {
        val retryCount = (retryCounts[cameraId] ?: 0) + 1
        retryCounts[cameraId] = retryCount
        emitState(cameraId, ConnectionState.Failed(reason, retryCount))
        persistStatus(cameraId, config, "FAILED", retryCount, null, System.currentTimeMillis())
        attemptReconnect(cameraId)
    }

    private suspend fun emitState(cameraId: Long, state: ConnectionState) {
        stateFlows.getOrPut(cameraId) { MutableStateFlow(ConnectionState.Idle()) }.emit(state)
        val updated = connectionStates.value.orEmpty().toMutableMap()
        updated[cameraId] = state
        connectionStates.postValue(updated)
    }

    private suspend fun persistStatus(
        cameraId: Long,
        config: ConnectionConfig?,
        status: String,
        failureCount: Int,
        nextRetryTime: Long?,
        timestamp: Long
    ) {
        val connectionType = when (config) {
            is ConnectionConfig.PortForward -> "PORT_FORWARD"
            is ConnectionConfig.VpnConfig -> "VPN"
            is ConnectionConfig.CloudRelayConfig -> "CLOUD_RELAY"
            else -> "LOCAL"
        }

        connectionDao.insert(
            ConnectionEntity(
                cameraId = cameraId,
                connectionType = connectionType,
                status = status,
                lastStatusChange = timestamp,
                failureCount = failureCount,
                nextRetryTime = nextRetryTime
            )
        )
    }

    private fun ConnectionEntity.toDomainState(): ConnectionState = when (status) {
        "CONNECTED" -> ConnectionState.Connected(lastStatusChange)
        "CONNECTING" -> ConnectionState.Connecting(lastStatusChange)
        "RECONNECTING" -> ConnectionState.Reconnecting(
            retryCount = failureCount,
            nextRetryAt = nextRetryTime ?: lastStatusChange,
            timestamp = lastStatusChange
        )
        "FAILED" -> ConnectionState.Failed(
            errorMessage = "Last failure recorded",
            retryCount = failureCount,
            timestamp = lastStatusChange
        )
        else -> ConnectionState.Disconnected(lastStatusChange)
    }

    private fun buildRtspUrl(
        cameraId: Long,
        ipAddress: String,
        port: Int,
        username: String?,
        password: String?,
        config: ConnectionConfig
    ): String {
        val credentials = if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            "${username}:${password}@"
        } else {
            ""
        }

        return when (config) {
            is ConnectionConfig.LocalNetwork -> "rtsp://$credentials$ipAddress:$port/stream"
            is ConnectionConfig.PortForward -> "rtsp://$credentials${config.routerIp}:${config.forwardedPort}/stream"
            is ConnectionConfig.VpnConfig -> "rtsp://$credentials$ipAddress:$port/stream"
            is ConnectionConfig.CloudRelayConfig -> "${config.relayServerUrl.trimEnd('/')}/cameras/$cameraId/stream"
        }
    }
}
