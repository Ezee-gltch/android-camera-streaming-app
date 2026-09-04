package com.camerastreamingapp.util

import timber.log.Timber

object CameraLogger {
    fun connection(cameraId: Long, message: String) = Timber.tag("Connection-$cameraId").d(message)
    fun stream(cameraId: Long, message: String) = Timber.tag("Stream-$cameraId").d(message)
    fun network(message: String) = Timber.tag("Network").d(message)
    fun error(message: String, throwable: Throwable? = null) = Timber.tag("CameraError").e(throwable, message)
}
