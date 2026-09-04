package com.camerastreamingapp.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class CameraDiscoveryManager {
    data class DiscoveredDevice(
        val name: String,
        val ipAddress: String,
        val port: Int
    )

    suspend fun discoverDevices(): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val devices = linkedSetOf<DiscoveredDevice>()
        val candidatePorts = listOf(554, 8554, 80)
        val subnets = listOf(
            intArrayOf(192, 168, 0),
            intArrayOf(192, 168, 1),
            intArrayOf(10, 0, 0)
        )

        subnets.forEach { subnet ->
            for (host in 1..30) {
                val ip = "${subnet[0]}.${subnet[1]}.${subnet[2]}.$host"
                for (port in candidatePorts) {
                    if (isPortOpen(ip, port)) {
                        devices.add(DiscoveredDevice("Camera $ip", ip, port))
                        break
                    }
                }
            }
        }

        devices.toList()
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 150)
            }
            true
        }.getOrDefault(false)
    }
}
