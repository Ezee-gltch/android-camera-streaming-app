package com.camerastreamingapp.network

import android.util.Log
import com.camerastreamingapp.data.db.ConnectionDao
import com.camerastreamingapp.data.models.Connection
import com.camerastreamingapp.data.models.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.pow

/**
 * Connection manager with auto-reconnect logic
 * Handles network resilience and reconnection strategies
 */
class ConnectionManager(
    private val cameraId: Int,
    private val rtspUrl: String,
    private val connectionDao: ConnectionDao
) {
    private val tag = "ConnectionManager"

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var rtspReader: RTSPStreamReader? = null
    private var reconnectJob: kotlinx.coroutines.Job? = null
    private var failureCount = 0
    private val maxRetries = 10

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Exponential backoff configuration
    private val initialBackoffMs = 1000L
    private val maxBackoffMs = 60000L // 1 minute
    private val backoffMultiplier = 2.0

    /**
     * Connect to camera with auto-reconnect on failure
     */
    fun connect() {
        coroutineScope.launch {
            updateStatus(ConnectionStatus.CONNECTING, "")
            
            if (connectToCamer()) {
                failureCount = 0
                updateStatus(ConnectionStatus.CONNECTED, "")
                _isConnected.value = true
                connectionDao.resetFailureCount(cameraId)
                Log.d(tag, "Camera $cameraId connected successfully")
            } else {
                handleConnectionFailure()
            }
        }
    }

    /**
     * Disconnect from camera
     */
    fun disconnect() {
        rtspReader?.disconnect()
        rtspReader = null
        reconnectJob?.cancel()
        reconnectJob = null
        _isConnected.value = false
        coroutineScope.launch {
            updateStatus(ConnectionStatus.DISCONNECTED, "")
        }
        Log.d(tag, "Camera $cameraId disconnected")
    }

    /**
     * Attempt actual connection to camera
     */
    private suspend fun connectToCamer(): Boolean {
        return try {
            rtspReader = RTSPStreamReader(rtspUrl)
            rtspReader?.connect() == true
        } catch (e: Exception) {
            Log.e(tag, "Connection attempt failed for camera $cameraId", e)
            false
        }
    }

    /**
     * Handle connection failure with exponential backoff retry
     */
    private suspend fun handleConnectionFailure() {
        failureCount++
        connectionDao.incrementFailureCount(cameraId)

        if (failureCount > maxRetries) {
            Log.e(tag, "Max retries reached for camera $cameraId")
            updateStatus(ConnectionStatus.ERROR, "Max reconnection attempts exceeded")
            return
        }

        // Calculate backoff delay with exponential increase
        val backoffDelay = calculateBackoffDelay(failureCount)
        Log.w(tag, "Connection failed for camera $cameraId. Attempt $failureCount/$maxRetries. Retrying in ${backoffDelay}ms")

        updateStatus(ConnectionStatus.RECONNECTING, "Attempting to reconnect...")

        reconnectJob = coroutineScope.launch {
            delay(backoffDelay)
            connect()
        }
    }

    /**
     * Calculate exponential backoff delay
     * Formula: min(initialBackoff * (multiplier ^ attempt), maxBackoff) + jitter
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = (initialBackoffMs * backoffMultiplier.pow((attempt - 1).toDouble())).toLong()
        val cappedDelay = min(exponentialDelay, maxBackoffMs)
        
        // Add jitter to prevent thundering herd
        val jitter = (Math.random() * cappedDelay * 0.1).toLong()
        
        return cappedDelay + jitter
    }

    /**
     * Get current connection status
     */
    fun getStatus(): ConnectionStatus = _connectionStatus.value

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = _isConnected.value

    /**
     * Get current RTSP reader
     */
    fun getRTSPReader(): RTSPStreamReader? = rtspReader

    /**
     * Update connection status in database and state
     */
    private suspend fun updateStatus(status: ConnectionStatus, errorMessage: String) {
        _connectionStatus.value = status
        try {
            connectionDao.updateStatus(cameraId, status.toString(), errorMessage)
        } catch (e: Exception) {
            Log.e(tag, "Error updating connection status", e)
        }
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        disconnect()
        coroutineScope.cancel()
    }
}
