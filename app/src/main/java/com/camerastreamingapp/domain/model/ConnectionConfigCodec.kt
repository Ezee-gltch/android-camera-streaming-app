package com.camerastreamingapp.domain.model

import com.camerastreamingapp.data.security.CredentialEncryptor
import com.google.gson.Gson

private data class ConnectionConfigPayload(
    val type: String,
    val routerIp: String? = null,
    val forwardedPort: Int? = null,
    val homeNatPunchUrl: String? = null,
    val vpnServerAddress: String? = null,
    val protocol: String? = null,
    val username: String? = null,
    val password: String? = null,
    val relayServerUrl: String? = null,
    val apiKey: String? = null
)

private val gson = Gson()
private val credentialEncryptor = CredentialEncryptor()

fun ConnectionConfig.toPersistedJson(): String {
    val payload = when (this) {
        is ConnectionConfig.LocalNetwork -> ConnectionConfigPayload(type = "LOCAL")
        is ConnectionConfig.PortForward -> ConnectionConfigPayload(
            type = "PORT_FORWARD",
            routerIp = routerIp,
            forwardedPort = forwardedPort,
            homeNatPunchUrl = homeNatPunchUrl
        )

        is ConnectionConfig.VpnConfig -> ConnectionConfigPayload(
            type = "VPN",
            vpnServerAddress = vpnServerAddress,
            protocol = protocol,
            username = credentialEncryptor.encrypt(username),
            password = credentialEncryptor.encrypt(password)
        )

        is ConnectionConfig.CloudRelayConfig -> ConnectionConfigPayload(
            type = "CLOUD_RELAY",
            relayServerUrl = relayServerUrl,
            apiKey = apiKey
        )
    }
    return gson.toJson(payload)
}

fun String?.toConnectionConfig(): ConnectionConfig? {
    if (this.isNullOrBlank()) return null
    val payload = runCatching { gson.fromJson(this, ConnectionConfigPayload::class.java) }.getOrNull() ?: return null
    return when (payload.type) {
        "LOCAL" -> ConnectionConfig.LocalNetwork
        "PORT_FORWARD" -> {
            val port = payload.forwardedPort ?: return null
            val ip = payload.routerIp ?: return null
            ConnectionConfig.PortForward(
                routerIp = ip,
                forwardedPort = port,
                homeNatPunchUrl = payload.homeNatPunchUrl
            )
        }

        "VPN" -> {
            val server = payload.vpnServerAddress?.takeIf { it.isNotBlank() } ?: return null
            val protocol = payload.protocol?.takeIf { it.isNotBlank() } ?: return null
            val encryptedUsername = payload.username?.takeIf { it.isNotBlank() } ?: return null
            val encryptedPassword = payload.password?.takeIf { it.isNotBlank() } ?: return null
            val username = credentialEncryptor.decrypt(encryptedUsername)?.takeIf { it.isNotBlank() } ?: return null
            val password = credentialEncryptor.decrypt(encryptedPassword)?.takeIf { it.isNotBlank() } ?: return null
            ConnectionConfig.VpnConfig(
                vpnServerAddress = server,
                protocol = protocol,
                username = username,
                password = password
            )
        }

        "CLOUD_RELAY" -> {
            val relayUrl = payload.relayServerUrl?.takeIf { it.isNotBlank() } ?: return null
            val apiKey = payload.apiKey?.takeIf { it.isNotBlank() } ?: return null
            ConnectionConfig.CloudRelayConfig(
                relayServerUrl = relayUrl,
                apiKey = apiKey
            )
        }

        else -> null
    }
}
