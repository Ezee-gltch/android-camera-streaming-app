package com.camerastreamingapp.domain.model

sealed class ConnectionConfig {
    data object LocalNetwork : ConnectionConfig()

    data class PortForward(
        val routerIp: String,
        val forwardedPort: Int,
        val homeNatPunchUrl: String? = null
    ) : ConnectionConfig()

    data class VpnConfig(
        val vpnServerAddress: String,
        val protocol: String,
        val username: String,
        val password: String
    ) : ConnectionConfig()

    data class CloudRelayConfig(
        val relayServerUrl: String,
        val apiKey: String
    ) : ConnectionConfig()
}
