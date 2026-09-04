package com.camerastreamingapp.domain.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAccessConfigTest {

    @Test
    fun `vpn config validates required fields`() {
        assertTrue(RemoteAccessConfig.vpn("10.0.0.1", "WireGuard", "user", "pass").isValid())
        assertFalse(RemoteAccessConfig.vpn("", "WireGuard", "user", "pass").isValid())
    }

    @Test
    fun `port forward config validates port range`() {
        assertTrue(RemoteAccessConfig.portForward("192.168.1.1", 8554).isValid())
        assertFalse(RemoteAccessConfig.portForward("192.168.1.1", 70000).isValid())
    }

    @Test
    fun `cloud relay config requires http url and api key`() {
        assertTrue(RemoteAccessConfig.cloudRelay("https://relay.example.com", "key").isValid())
        assertTrue(RemoteAccessConfig.cloudRelay("http://relay.example.com", "key").isValid())
        assertFalse(RemoteAccessConfig.cloudRelay("relay.example.com", "key").isValid())
        assertFalse(RemoteAccessConfig.cloudRelay("httpbad", "key").isValid())
        assertFalse(RemoteAccessConfig.cloudRelay("https://relay.example.com", "").isValid())
    }
}
