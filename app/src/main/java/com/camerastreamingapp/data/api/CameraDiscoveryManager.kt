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
        val prefixes = listOf("192.168", "10")

        prefixes.forEach { prefix ->
            val secondOctetRange = if (prefix == "192.168") 0..1 else 0..0
            for (second in secondOctetRange) {
                for (host in 1..30) {
                    val ip = if (prefix == "192.168") "$prefix.$second.$host" else "$prefix.$second.0.$host"
                    for (port in candidatePorts) {
                        if (isPortOpen(ip, port)) {
                            devices.add(DiscoveredDevice("Camera $ip", ip, port))
                            break
                        }
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
