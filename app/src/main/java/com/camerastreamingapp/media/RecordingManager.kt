package com.camerastreamingapp.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.camerastreamingapp.data.models.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recording manager for capturing camera streams
 */
class RecordingManager(private val context: Context) {
    private val tag = "RecordingManager"
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecording: Recording? = null
    private val recordingsDir = File(context.cacheDir, "recordings").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Start recording camera stream
     */
    suspend fun startRecording(cameraId: Int, cameraName: String): Recording? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "camera_${cameraId}_${timestamp}.mp4"
                val filePath = File(recordingsDir, fileName).absolutePath

                mediaRecorder = MediaRecorder().apply {
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setVideoSize(1280, 720)
                    setVideoFrameRate(30)
                    setAudioSamplingRate(44100)
                    setAudioChannels(2)
                    setAudioEncodingBitRate(128000)
                    setVideoEncodingBitRate(5000000)
                    setOutputFile(filePath)

                    try {
                        prepare()
                        start()
                    } catch (e: Exception) {
                        Log.e(tag, "Error preparing/starting MediaRecorder", e)
                        release()
                        return@withContext null
                    }
                }

                val recording = Recording(
                    cameraId = cameraId,
                    cameraName = cameraName,
                    filePath = filePath,
                    fileName = fileName,
                    startTime = System.currentTimeMillis(),
                    isComplete = false
                )

                currentRecording = recording
                Log.d(tag, "Recording started: $fileName")
                recording
            } catch (e: Exception) {
                Log.e(tag, "Error starting recording", e)
                null
            }
        }
    }

    /**
     * Stop current recording
     */
    suspend fun stopRecording(): Recording? {
        return withContext(Dispatchers.IO) {
            try {
                mediaRecorder?.apply {
                    try {
                        stop()
                        release()
                    } catch (e: Exception) {
                        Log.e(tag, "Error stopping recording", e)
                    }
                }
                mediaRecorder = null

                currentRecording?.let { recording ->
                    val file = File(recording.filePath)
                    val updatedRecording = recording.copy(
                        fileSize = file.length(),
                        endTime = System.currentTimeMillis(),
                        isComplete = true,
                        duration = (System.currentTimeMillis() - recording.startTime)
                    )
                    currentRecording = null
                    Log.d(tag, "Recording stopped: ${recording.fileName}")
                    updatedRecording
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in stopRecording", e)
                null
            }
        }
    }

    /**
     * Cancel current recording
     */
    suspend fun cancelRecording() {
        withContext(Dispatchers.IO) {
            try {
                mediaRecorder?.apply {
                    try {
                        stop()
                        release()
                    } catch (e: Exception) {
                        Log.w(tag, "Error cancelling recording", e)
                    }
                }
                mediaRecorder = null

                currentRecording?.let { recording ->
                    File(recording.filePath).delete()
                    currentRecording = null
                    Log.d(tag, "Recording cancelled")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in cancelRecording", e)
            }
        }
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = mediaRecorder != null && currentRecording != null
}
