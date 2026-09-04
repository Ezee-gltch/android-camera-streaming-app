package com.camerastreamingapp.network

import android.util.Log
import kotlinx.coroutines.delay
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.math.min
import kotlin.math.pow

/**
 * RTSP stream reader for IP camera communication
 * Handles RTSP protocol and stream management
 */
class RTSPStreamReader(
    private val rtspUrl: String,
    private val connectionTimeout: Int = 10000, // ms
    private val readTimeout: Int = 5000 // ms
) {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var sessionId: String? = null
    private var cSeq: Int = 0

    private val tag = "RTSPStreamReader"

    /**
     * Connect to RTSP stream
     */
    suspend fun connect(): Boolean {
        return try {
            val (host, port, path) = parseRtspUrl(rtspUrl)

            socket = Socket().apply {
                soTimeout = readTimeout
            }

            socket?.connect(java.net.InetSocketAddress(host, port), connectionTimeout)
            inputStream = socket?.inputStream

            // Send RTSP OPTIONS request
            sendRTSPRequest("OPTIONS", path)

            // Send DESCRIBE request to get media description
            sendRTSPRequest("DESCRIBE", path)

            // Send SETUP request
            sendRTSPRequest("SETUP", path)

            // Send PLAY request
            sendRTSPRequest("PLAY", path)

            Log.d(tag, "Successfully connected to RTSP stream: $rtspUrl")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to RTSP stream", e)
            disconnect()
            false
        }
    }

    /**
     * Read frame from stream
     */
    suspend fun readFrame(): ByteArray? {
        return try {
            inputStream?.let { stream ->
                val header = ByteArray(4)
                val bytesRead = stream.read(header)

                if (bytesRead != 4) return null

                // Parse frame length from RTP header
                val frameLength = ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
                val frame = ByteArray(frameLength)

                var totalRead = 0
                while (totalRead < frameLength) {
                    val read = stream.read(frame, totalRead, frameLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }

                frame.take(totalRead).toByteArray()
            }
        } catch (e: SocketTimeoutException) {
            Log.w(tag, "Socket read timeout")
            null
        } catch (e: Exception) {
            Log.e(tag, "Error reading frame", e)
            null
        }
    }

    /**
     * Disconnect from stream
     */
    fun disconnect() {
        try {
            sessionId?.let { id ->
                sendRTSPRequest("TEARDOWN", "/")
            }
            inputStream?.close()
            socket?.close()
            socket = null
            inputStream = null
            Log.d(tag, "Disconnected from RTSP stream")
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting", e)
        }
    }

    /**
     * Send RTSP request
     */
    private suspend fun sendRTSPRequest(method: String, path: String) {
        try {
            cSeq++
            val request = buildRTSPRequest(method, path, rtspUrl)

            socket?.outputStream?.let { output ->
                output.write(request.toByteArray())
                output.flush()
                Log.d(tag, "Sent RTSP $method request")
            }

            // Read response
            val response = readRTSPResponse()
            parseRTSPResponse(response, method)

        } catch (e: Exception) {
            Log.e(tag, "Error sending RTSP request", e)
        }
    }

    /**
     * Build RTSP request string
     */
    private fun buildRTSPRequest(method: String, path: String, url: String): String {
        val request = StringBuilder()
        request.append("$method $url RTSP/1.0\r\n")
        request.append("CSeq: $cSeq\r\n")
        request.append("User-Agent: CameraStreamingApp\r\n")

        sessionId?.let {
            request.append("Session: $it\r\n")
        }

        request.append("Connection: keep-alive\r\n")
        request.append("\r\n")

        return request.toString()
    }

    /**
     * Read RTSP response
     */
    private suspend fun readRTSPResponse(): String {
        val response = StringBuilder()
        inputStream?.let { stream ->
            val buffer = ByteArray(4096)
            val bytesRead = stream.read(buffer)
            if (bytesRead > 0) {
                response.append(String(buffer, 0, bytesRead))
            }
        }
        return response.toString()
    }

    /**
     * Parse RTSP response
     */
    private fun parseRTSPResponse(response: String, method: String) {
        try {
            val lines = response.split("\r\n")
            val statusLine = lines.firstOrNull() ?: return

            if (statusLine.contains("200 OK")) {
                Log.d(tag, "RTSP $method response OK")

                // Extract Session ID if present
                lines.find { it.startsWith("Session:") }?.let {
                    sessionId = it.substring("Session:".length).trim().split(";")[0]
                }
            } else {
                Log.w(tag, "RTSP $method response: $statusLine")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing RTSP response", e)
        }
    }

    /**
     * Parse RTSP URL into components
     */
    private fun parseRtspUrl(url: String): Triple<String, Int, String> {
        val rtspRegex = """rtsp://(?:([^:@]+):([^@]+)@)?([^:/]+)(?::(\d+))?(/.*)?""" .toRegex()
        val matchResult = rtspRegex.find(url) ?: throw IllegalArgumentException("Invalid RTSP URL")

        val host = matchResult.groupValues[3]
        val port = matchResult.groupValues[4].takeIf { it.isNotEmpty() }?.toInt() ?: 554
        val path = matchResult.groupValues[5].takeIf { it.isNotEmpty() } ?: "/"

        return Triple(host, port, path)
    }

    /**
     * Check if stream is connected
     */
    fun isConnected(): Boolean = socket?.isConnected == true && inputStream != null
}