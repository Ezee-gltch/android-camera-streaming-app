package com.camerastreamingapp.domain.connection

sealed class RemoteAccessConfig {
    abstract fun isValid(): Boolean

    data class Vpn(
        val vpnServerAddress: String,
        val protocol: String,
        val username: String,
        val password: String
    ) : RemoteAccessConfig() {
        override fun isValid(): Boolean =
            vpnServerAddress.isNotBlank() && protocol.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    data class PortForward(
        val routerIp: String,
        val forwardedPort: Int,
        val homeNatPunchUrl: String? = null
    ) : RemoteAccessConfig() {
        override fun isValid(): Boolean = routerIp.isNotBlank() && forwardedPort in 1..65535
    }

    data class CloudRelay(
        val relayServerUrl: String,
        val apiKey: String
    ) : RemoteAccessConfig() {
        override fun isValid(): Boolean {
            val uri = runCatching { java.net.URI(relayServerUrl) }.getOrNull() ?: return false
            return (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank() && apiKey.isNotBlank()
        }
    }

    companion object {
        fun vpn(vpnServerAddress: String, protocol: String, username: String, password: String) =
            Vpn(vpnServerAddress, protocol, username, password)

        fun portForward(routerIp: String, forwardedPort: Int, homeNatPunchUrl: String? = null) =
            PortForward(routerIp, forwardedPort, homeNatPunchUrl)

        fun cloudRelay(relayServerUrl: String, apiKey: String) =
            CloudRelay(relayServerUrl, apiKey)
    }
}
