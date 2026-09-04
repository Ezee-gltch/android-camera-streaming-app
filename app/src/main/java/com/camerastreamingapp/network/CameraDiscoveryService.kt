package com.camerastreamingapp.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import java.net.InetAddress

/**
 * Camera discovery service for local network
 * Uses mDNS (Bonjour) to discover cameras on LAN
 */
class CameraDiscoveryService(private val context: Context) {
    private val tag = "CameraDiscoveryService"
    private var jmdns: JmDNS? = null
    private val discoveredCameras = mutableListOf<DiscoveredCamera>()

    data class DiscoveredCamera(
        val name: String,
        val ipAddress: String,
        val port: Int = 554,
        val manufacturer: String = "Unknown"
    )

    /**
     * Start camera discovery on local network
     */
    suspend fun startDiscovery(): List<DiscoveredCamera> {
        return withContext(Dispatchers.IO) {
            try {
                discoveredCameras.clear()

                // Initialize mDNS
                val inetAddress = InetAddress.getLocalHost()
                jmdns = JmDNS.create(inetAddress)

                // Listen for RTSP camera services
                jmdns?.addServiceListener("_rtsp._tcp.local.", CameraServiceListener())
                jmdns?.addServiceListener("_http._tcp.local.", CameraServiceListener())

                Log.d(tag, "Camera discovery started")

                // Give it a few seconds to discover cameras
                kotlinx.coroutines.delay(5000)

                discoveredCameras
            } catch (e: Exception) {
                Log.e(tag, "Error during camera discovery", e)
                emptyList()
            }
        }
    }

    /**
     * Stop camera discovery
     */
    suspend fun stopDiscovery() {
        withContext(Dispatchers.IO) {
            try {
                jmdns?.close()
                jmdns = null
                Log.d(tag, "Camera discovery stopped")
            } catch (e: Exception) {
                Log.e(tag, "Error stopping discovery", e)
            }
        }
    }

    /**
     * Service listener for discovering cameras
     */
    private inner class CameraServiceListener : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            Log.d(tag, "Service added: ${event.name}")
        }

        override fun serviceRemoved(event: ServiceEvent) {
            Log.d(tag, "Service removed: ${event.name}")
        }

        override fun serviceResolved(event: ServiceEvent) {
            try {
                val info = event.info
                val name = info.name
                val ipAddress = info.inetAddresses.firstOrNull()?.hostAddress ?: return
                val port = info.port
                val manufacturer = info.properties?.getProperty("vendor") ?: "Unknown"

                val discoveredCamera = DiscoveredCamera(
                    name = name,
                    ipAddress = ipAddress,
                    port = port,
                    manufacturer = manufacturer
                )

                discoveredCameras.add(discoveredCamera)
                Log.d(tag, "Camera discovered: $name at $ipAddress:$port")
            } catch (e: Exception) {
                Log.e(tag, "Error resolving service", e)
            }
        }
    }

    /**
     * Perform network scan for cameras (alternative method)
     * Scans subnet for common camera ports
     */
    suspend fun performNetworkScan(subnet: String = "192.168.1"): List<DiscoveredCamera> {
        return withContext(Dispatchers.IO) {
            val cameras = mutableListOf<DiscoveredCamera>()
            val commonPorts = listOf(554, 8080, 8554, 10554)

            try {
                // Scan IP range 1-254 in subnet
                for (i in 1..10) { // Limited scan for performance
                    for (port in commonPorts) {
                        val ip = "$subnet.$i"
                        try {
                            val address = InetAddress.getByName(ip)
                            if (address.isReachable(1000)) {
                                Log.d(tag, "Host reachable: $ip")
                                
                                // Try to connect to RTSP port
                                if (testRtspConnection(ip, port)) {
                                    cameras.add(
                                        DiscoveredCamera(
                                            name = "Camera at $ip",
                                            ipAddress = ip,
                                            port = port
                                        )
                                    )
                                    Log.d(tag, "Camera found at $ip:$port")
                                }
                            }
                        } catch (e: Exception) {
                            // Continue scanning
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Network scan error", e)
            }

            cameras
        }
    }

    /**
     * Test if RTSP connection is available on given IP and port
     */
    private suspend fun testRtspConnection(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(ip, port), 2000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
