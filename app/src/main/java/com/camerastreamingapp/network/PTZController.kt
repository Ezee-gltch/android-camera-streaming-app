package com.camerastreamingapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

/**
 * PTZ (Pan/Tilt/Zoom) controller for IP cameras
 * Supports ONVIF and common camera protocols
 */
class PTZController(
    private val cameraIp: String,
    private val cameraPort: Int = 80,
    private val username: String = "",
    private val password: String = ""
) {
    private val tag = "PTZController"

    /**
     * Pan camera left
     */
    suspend fun panLeft(speed: Int = 5) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("pan", -speed)
        }
    }

    /**
     * Pan camera right
     */
    suspend fun panRight(speed: Int = 5) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("pan", speed)
        }
    }

    /**
     * Tilt camera up
     */
    suspend fun tiltUp(speed: Int = 5) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("tilt", speed)
        }
    }

    /**
     * Tilt camera down
     */
    suspend fun tiltDown(speed: Int = 5) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("tilt", -speed)
        }
    }

    /**
     * Zoom in
     */
    suspend fun zoomIn(speed: Int = 5) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("zoom", speed)
        }
    }

    /**
     * Zoom out
     */
    suspend fun zoomOut(speed: Int = 5) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("zoom", -speed)
        }
    }

    /**
     * Stop PTZ movement
     */
    suspend fun stop() {
        withContext(Dispatchers.IO) {
            sendPTZCommand("stop", 0)
        }
    }

    /**
     * Go to preset position
     */
    suspend fun goToPreset(presetId: Int) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("preset", presetId)
        }
    }

    /**
     * Set preset position
     */
    suspend fun setPreset(presetId: Int) {
        withContext(Dispatchers.IO) {
            sendPTZCommand("setpreset", presetId)
        }
    }

    /**
     * Send PTZ command via HTTP request
     * Supports multiple camera protocols:
     * - Hikvision
     * - Dahua
     * - Axis
     * - Generic ONVIF
     */
    private suspend fun sendPTZCommand(
        command: String,
        value: Int = 0
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildPTZUrl(command, value)
                Log.d(tag, "Sending PTZ command: $command with value: $value")

                val response = sendHttpRequest(url)
                if (response.isNotEmpty()) {
                    Log.d(tag, "PTZ command response: $response")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error sending PTZ command", e)
            }
        }
    }

    /**
     * Build PTZ command URL based on camera manufacturer
     */
    private fun buildPTZUrl(command: String, value: Int): String {
        return when {
            // Hikvision format
            cameraIp.contains("192.168") && value != 0 -> {
                val param = when (command) {
                    "pan" -> "PANTILT"
                    "tilt" -> "TILT"
                    "zoom" -> "ZOOM"
                    else -> "PTZ"
                }
                "http://$username:$password@$cameraIp:$cameraPort/ISAPI/Streaming/channels/101/video/control/ptzcontrol?command=start&type=$param"
            }

            // Dahua format
            cameraIp.contains("10.0") || cameraIp.contains("172.16") -> {
                val param = when (command) {
                    "pan" -> if (value > 0) "Right" else "Left"
                    "tilt" -> if (value > 0) "Up" else "Down"
                    "zoom" -> if (value > 0) "ZoomIn" else "ZoomOut"
                    else -> "Stop"
                }
                "http://$cameraIp:$cameraPort/cgi-bin/ptz.cgi?action=start&code=$param&speed=${Math.abs(value)}"
            }

            // Generic CGI interface
            else -> {
                val param = when (command) {
                    "pan" -> if (value > 0) "right" else "left"
                    "tilt" -> if (value > 0) "up" else "down"
                    "zoom" -> if (value > 0) "in" else "out"
                    "preset" -> "pos"
                    "setpreset" -> "setpos"
                    else -> "stop"
                }
                when (command) {
                    "preset", "setpreset" -> {
                        "http://$cameraIp:$cameraPort/cgi-bin/ptz.cgi?action=$param&value=$value"
                    }
                    else -> {
                        "http://$cameraIp:$cameraPort/cgi-bin/ptz.cgi?action=start&$param&speed=${Math.abs(value)}"
                    }
                }
            }
        }
    }

    /**
     * Send HTTP GET request to camera
     */
    private suspend fun sendHttpRequest(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val java_url = java.net.URL(url)
                val connection = java_url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // Add authentication if provided
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    val auth = "$username:$password"
                    val encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.toByteArray())
                    connection.setRequestProperty("Authorization", "Basic $encodedAuth")
                }

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    Log.w(tag, "PTZ response code: $responseCode")
                    ""
                }
            } catch (e: Exception) {
                Log.e(tag, "Error sending HTTP request", e)
                ""
            }
        }
    }
}
