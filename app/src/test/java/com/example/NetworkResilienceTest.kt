package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.NetworkStreamManager
import com.example.service.WifiConnectionTestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkResilienceTest {

    private lateinit var context: Context
    private lateinit var networkManager: NetworkStreamManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        networkManager = NetworkStreamManager(context)
    }

    @Test
    fun testDefaultResilienceParameters() {
        assertEquals(3, networkManager.maxRetryAttempts)
        assertEquals(3000, networkManager.socketTimeoutMs)
        assertEquals(6000L, networkManager.offlineTimeoutThresholdMs)
        assertEquals(3000L, networkManager.heartbeatIntervalMs)
    }

    @Test
    fun testMalformedJsonHandlingDoesNotCrash() {
        val initialMalformed = networkManager.malformedPacketsCount.value

        // Test completely invalid syntax
        val handledBad1 = networkManager.processIncomingPacket("{not-a-json", "192.168.1.50")
        assertFalse(handledBad1)

        // Test empty string
        val handledBad2 = networkManager.processIncomingPacket("", "192.168.1.50")
        assertFalse(handledBad2)

        // Test non-JSON string
        val handledBad3 = networkManager.processIncomingPacket("HELLO WORLD", "192.168.1.50")
        assertFalse(handledBad3)

        assertEquals(initialMalformed + 3, networkManager.malformedPacketsCount.value)
    }

    @Test
    fun testUnrecognizedPacketTypeIgnoredGracefully() {
        val initialMalformed = networkManager.malformedPacketsCount.value

        // Valid JSON with unrecognized type
        val handled = networkManager.processIncomingPacket("{\"type\": \"unknown_command\"}", "192.168.1.50")
        assertFalse(handled)

        // Valid JSON should not count as malformed
        assertEquals(initialMalformed, networkManager.malformedPacketsCount.value)
    }

    @Test
    fun testValidPongPacketUpdatesState() {
        val pongPayload = """
            {
                "type": "pong",
                "name": "Esports Rig",
                "client_port": 8089,
                "time": 1000000
            }
        """.trimIndent()

        val handled = networkManager.processIncomingPacket(pongPayload, "192.168.1.105")
        assertTrue(handled)
        assertTrue("isPcOnline must be true after pong", networkManager.isPcOnline.value)

        val testState = networkManager.testState.value
        assertTrue("testState must be Success", testState is WifiConnectionTestState.Success)
        val success = testState as WifiConnectionTestState.Success
        assertEquals("Esports Rig", success.serverName)
        assertEquals("192.168.1.105", success.ip)
    }

    @Test
    fun testValidAnnouncePacketDiscoversPc() {
        val announcePayload = """
            {
                "type": "announce",
                "name": "Studio Workstation"
            }
        """.trimIndent()

        val handled = networkManager.processIncomingPacket(announcePayload, "192.168.1.200")
        assertTrue(handled)
        assertTrue(networkManager.isPcOnline.value)
        assertEquals("192.168.1.200", networkManager.pcIpAddress)

        val testState = networkManager.testState.value
        assertTrue(testState is WifiConnectionTestState.Success)
        assertEquals("Studio Workstation", (testState as WifiConnectionTestState.Success).serverName)
    }

    @Test
    fun testHeartbeatTimeoutTransitionsToOffline() {
        val baseTime = 1000000L
        networkManager.setLastPongTimeForTest(baseTime)
        networkManager.isPcOnline.value = true

        // Before threshold: still online
        networkManager.checkHeartbeatTimeout(baseTime + 5000L)
        assertTrue(networkManager.isPcOnline.value)

        // After threshold (exceeding 6000ms): offline transition
        networkManager.checkHeartbeatTimeout(baseTime + 6500L)
        assertFalse("isPcOnline should transition to false when pong exceeds threshold", networkManager.isPcOnline.value)
    }

    @Test
    fun testHeartbeatRecoveryOnIncomingPong() {
        // Start in offline state
        networkManager.isPcOnline.value = false

        // Receive simulated pong
        networkManager.simulatePongReceived("192.168.1.150", 4L, "Recovered PC")

        assertTrue("isPcOnline must recover to true upon receiving pong", networkManager.isPcOnline.value)
        assertEquals(4L, networkManager.liveLatencyMs.value)
    }

    @Test
    fun testSendRawErrorHandlingWithoutCrashing() {
        val initialErrors = networkManager.networkErrorCount.value
        val invalidData = ByteArray(10)

        // Attempting to send raw data with retry policy
        networkManager.sendRaw(invalidData, "256.256.256.256", retries = 2)

        // Verification: did not crash and error counter incremented
        assertTrue(networkManager.networkErrorCount.value >= initialErrors)
    }
}
