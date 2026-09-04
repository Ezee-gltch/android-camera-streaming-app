package com.camerastreamingapp.data.api

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.camerastreamingapp.config.AppConfig
import com.camerastreamingapp.util.CameraLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.util.concurrent.ConcurrentHashMap

class CameraStreamManager(private val context: Context) {
    data class StreamState(
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val error: String? = null
    )

    private val players = ConcurrentHashMap<Long, ExoPlayer>()
    private val streamStates = MutableLiveData<Map<Long, StreamState>>(emptyMap())
    private val frameBitmaps = MutableLiveData<Map<Long, Bitmap?>>(emptyMap())

    fun getStreamStates(): LiveData<Map<Long, StreamState>> = streamStates
    fun getFrameBitmaps(): LiveData<Map<Long, Bitmap?>> = frameBitmaps

    fun buildRtspUrl(
        ipAddress: String,
        port: Int,
        username: String? = null,
        password: String? = null,
        path: String = "/stream"
    ): String {
        val credentials = if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            "${Uri.encode(username)}:${Uri.encode(password)}@"
        } else {
            ""
        }
        return "rtsp://$credentials$ipAddress:$port$path"
    }

    suspend fun play(cameraId: Long, rtspUrl: String): Boolean {
        val connected = runCatching {
            val player = players[cameraId] ?: createPlayer(cameraId)
            player.setMediaItem(MediaItem.fromUri(Uri.parse(rtspUrl)))
            player.playWhenReady = true
            player.prepare()
            awaitReadyOrError(cameraId, player)
        }.onFailure {
            CameraLogger.error("Failed to start stream for camera $cameraId", it)
            updateState(cameraId, StreamState(error = it.message ?: "Unknown stream error"))
        }.getOrDefault(false)
        if (!connected) {
            stop(cameraId)
        }
        return connected
    }

    fun pause(cameraId: Long) {
        players[cameraId]?.pause()
        updateState(cameraId, StreamState(isPlaying = false))
    }

    suspend fun stop(cameraId: Long) {
        players.remove(cameraId)?.let { player ->
            player.stop()
            player.release()
        }
        updateState(cameraId, StreamState(isPlaying = false))
    }

    fun release(cameraId: Long) {
        players.remove(cameraId)?.release()
        updateState(cameraId, StreamState())
    }

    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
        streamStates.postValue(emptyMap())
        frameBitmaps.postValue(emptyMap())
    }

    fun isStreamPlaying(cameraId: Long): Boolean = players[cameraId]?.isPlaying == true

    private fun createPlayer(cameraId: Long): ExoPlayer {
        return ExoPlayer.Builder(context).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> updateState(cameraId, StreamState(isBuffering = true))
                        Player.STATE_READY -> updateState(
                            cameraId,
                            StreamState(isPlaying = player.playWhenReady, isBuffering = false)
                        )
                        Player.STATE_ENDED, Player.STATE_IDLE -> updateState(cameraId, StreamState(isPlaying = false))
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    updateState(cameraId, StreamState(error = error.localizedMessage ?: "Playback error"))
                    CameraLogger.error("Stream playback error for camera $cameraId", error)
                }
            })
            players[cameraId] = player
        }
    }

    private fun updateState(cameraId: Long, state: StreamState) {
        val map = streamStates.value.orEmpty().toMutableMap()
        map[cameraId] = state
        streamStates.postValue(map)
    }

    private suspend fun awaitReadyOrError(cameraId: Long, player: ExoPlayer): Boolean {
        if (player.playbackState == Player.STATE_READY) {
            updateState(cameraId, StreamState(isPlaying = true))
            return true
        }
        return withTimeoutOrNull(AppConfig.CONNECTION_TIMEOUT_MS.toLong()) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && continuation.isActive) {
                            updateState(cameraId, StreamState(isPlaying = true))
                            player.removeListener(this)
                            continuation.resume(true)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (continuation.isActive) {
                            updateState(cameraId, StreamState(error = error.localizedMessage ?: "Playback error"))
                            player.removeListener(this)
                            continuation.resume(false)
                        }
                    }
                }
                player.addListener(listener)
                continuation.invokeOnCancellation { player.removeListener(listener) }
            }
        } ?: false
    }
}
