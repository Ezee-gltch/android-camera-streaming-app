package com.camerastreamingapp.config

object AppConfig {
    const val DEFAULT_RTSP_PORT = 554
    const val DEFAULT_HTTP_PORT = 80
    const val CONNECTION_TIMEOUT_MS = 10000
    const val READ_TIMEOUT_MS = 15000
    const val MAX_RETRY_ATTEMPTS = 5
    const val INITIAL_BACKOFF_MS = 1000L
    const val MAX_BACKOFF_MS = 60000L
}
