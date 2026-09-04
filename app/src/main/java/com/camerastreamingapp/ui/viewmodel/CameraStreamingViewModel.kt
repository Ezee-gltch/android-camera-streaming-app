package com.camerastreamingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camerastreamingapp.data.models.Camera
import com.camerastreamingapp.data.models.ConnectionStatus
import com.camerastreamingapp.data.repository.CameraRepository
import com.camerastreamingapp.network.ConnectionManager
import com.camerastreamingapp.network.RemoteAccessHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel for camera streaming
 * Manages UI state and camera connections
 */
class CameraStreamingViewModel(
    private val repository: CameraRepository
) : ViewModel() {
    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()

    private val _selectedCamera = MutableStateFlow<Camera?>(null)
    val selectedCamera: StateFlow<Camera?> = _selectedCamera.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val connectionManagers = ConcurrentHashMap<Int, ConnectionManager>()
    private val remoteAccessHandlers = ConcurrentHashMap<Int, RemoteAccessHandler>()

    init {
        loadCameras()
    }

    /**
     * Load all active cameras from database
     */
    private fun loadCameras() {
        viewModelScope.launch {
            repository.getAllActiveCameras().collect { cameraList ->
                _cameras.value = cameraList
            }
        }
    }

    /**
     * Connect to selected camera
     */
    fun connectToCamera(camera: Camera) {
        viewModelScope.launch {
            try {
                _selectedCamera.value = camera
                _isLoading.value = true

                // Get remote access config
                val remoteConfig = repository.getRemoteAccessConfigOnce(camera.id)

                // Setup remote access if configured
                var rtspUrl = camera.rtspUrl
                if (remoteConfig != null) {
                    val remoteAccessHandler = RemoteAccessHandler(camera.id)
                    remoteAccessHandlers[camera.id] = remoteAccessHandler
                    rtspUrl = remoteAccessHandler.setupRemoteAccess(remoteConfig, camera.rtspUrl)
                }

                // Create and connect connection manager
                val connectionManager = ConnectionManager(camera.id, rtspUrl, repository)
                connectionManagers[camera.id] = connectionManager

                connectionManager.connectionStatus.collect { status ->
                    _connectionStatus.value = status
                }

                connectionManager.connect()
                _isLoading.value = false

            } catch (e: Exception) {
                _errorMessage.value = "Failed to connect: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Disconnect from current camera
     */
    fun disconnectFromCamera() {
        viewModelScope.launch {
            try {
                _selectedCamera.value?.let { camera ->
                    connectionManagers[camera.id]?.disconnect()
                    remoteAccessHandlers[camera.id]?.disconnectRemoteAccess()

                    connectionManagers.remove(camera.id)
                    remoteAccessHandlers.remove(camera.id)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to disconnect: ${e.message}"
            }
        }
    }

    /**
     * Add new camera
     */
    fun addCamera(camera: Camera) {
        viewModelScope.launch {
            try {
                repository.addCamera(camera)
                loadCameras()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add camera: ${e.message}"
            }
        }
    }

    /**
     * Delete camera
     */
    fun deleteCamera(camera: Camera) {
        viewModelScope.launch {
            try {
                disconnectFromCamera()
                repository.deleteCamera(camera)
                loadCameras()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete camera: ${e.message}"
            }
        }
    }

    /**
     * Get connection manager for current camera
     */
    fun getConnectionManager(): ConnectionManager? {
        return _selectedCamera.value?.let { connectionManagers[it.id] }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        connectionManagers.values.forEach { it.dispose() }
        connectionManagers.clear()
        remoteAccessHandlers.clear()
    }
}
