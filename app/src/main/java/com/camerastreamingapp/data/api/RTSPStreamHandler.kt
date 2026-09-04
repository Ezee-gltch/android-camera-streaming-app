package com.camerastreamingapp.data.api

import com.camerastreamingapp.config.AppConfig
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

interface RTSPStreamHandler {
    suspend fun startStream(cameraId: Long, rtspUrl: String): Boolean
    suspend fun stopStream(cameraId: Long)
    fun isStreamActive(cameraId: Long): Boolean
}

class DefaultRTSPStreamHandler(
    private val cameraStreamManager: CameraStreamManager
) : RTSPStreamHandler {
    override suspend fun startStream(cameraId: Long, rtspUrl: String): Boolean {
        return try {
            withTimeout(AppConfig.CONNECTION_TIMEOUT_MS.toLong()) {
                cameraStreamManager.play(cameraId, rtspUrl)
            }
        } catch (_: TimeoutCancellationException) {
            false
        }
    }

    override suspend fun stopStream(cameraId: Long) {
        cameraStreamManager.stop(cameraId)
    }

    override fun isStreamActive(cameraId: Long): Boolean =
        cameraStreamManager.isStreamPlaying(cameraId)
}
