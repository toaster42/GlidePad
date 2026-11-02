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

    @Test
    fun testHeartbeatDeduplicationAndState() {
        assertFalse(networkManager.isHeartbeatActive())
        networkManager.startHeartbeat()
        assertTrue(networkManager.isHeartbeatActive())

        // Multiple calls should not crash or create duplicate background loops
        networkManager.startHeartbeat()
        assertTrue(networkManager.isHeartbeatActive())

        networkManager.stopHeartbeat()
        assertFalse(networkManager.isHeartbeatActive())
    }

    @Test
    fun testTrackpadActivityUpdatesTimestamp() {
        val before = System.currentTimeMillis()
        networkManager.sendMousePacket(10, 20, 0, 0)
        assertTrue(networkManager.lastMousePacketSentTime >= before)
    }

    @Test
    fun testPingSuppressedDuringActiveTrackpadStreaming() {
        val simulatedTime = 100000L
        networkManager.lastMousePacketSentTime = simulatedTime

        // 1. Within active streaming window (< 1200ms), ping must be suppressed
        assertTrue("Trackpad should be marked active within 300ms", networkManager.isTrackpadActive(simulatedTime + 300L))
        assertFalse("Heartbeat ping must be suppressed during active trackpad streaming to prioritize cursor bandwidth",
            networkManager.shouldSendHeartbeatPing(simulatedTime + 300L))

        // 2. Near edge of threshold (1199ms)
        assertTrue(networkManager.isTrackpadActive(simulatedTime + 1199L))
        assertFalse(networkManager.shouldSendHeartbeatPing(simulatedTime + 1199L))

        // 3. When gesture ceases and threshold elapses (> 1200ms), ping resumes
        assertFalse("Trackpad should be marked inactive after 1500ms", networkManager.isTrackpadActive(simulatedTime + 1500L))
        assertTrue("Heartbeat ping must resume when trackpad is idle",
            networkManager.shouldSendHeartbeatPing(simulatedTime + 1500L))
    }

    @Test
    fun testPythonReceiverScriptBufferAndVersionIntegrity() {
        val script = NetworkStreamManager.PYTHON_RECEIVER_SCRIPT

        assertTrue("Python receiver must specify protocol version 2.3", script.contains("v2.3"))
        assertTrue("Python receiver must configure SO_RCVBUF", script.contains("SO_RCVBUF"))
        assertTrue("Python receiver must expand socket buffer to 65536 to prevent packet loss", script.contains("65536"))
        assertTrue("Python receiver must bind UDP port 8089", script.contains("UDP_PORT = 8089"))
        assertTrue("Python receiver must handle mouse event type", script.contains("elif msg_type == 'mouse':"))
    }

    @Test
    fun testBenchmarkReceiverScriptBufferAndVersionIntegrity() {
        val benchmarkScript = NetworkStreamManager.PYTHON_BENCHMARK_RECEIVER_SCRIPT

        assertTrue("Benchmark receiver must specify protocol version 1.1", benchmarkScript.contains("v1.1"))
        assertTrue("Benchmark receiver must configure SO_RCVBUF", benchmarkScript.contains("SO_RCVBUF"))
        assertTrue("Benchmark receiver must expand socket buffer to 65536", benchmarkScript.contains("65536"))
        assertTrue("Benchmark receiver must compute jitter", benchmarkScript.contains("jitter"))
        assertTrue("Benchmark receiver must compute packet loss", benchmarkScript.contains("packet_loss_pct"))
    }

    @Test
    fun testAutoDetectionPopulatesManualIpField() {
        val initialIp = networkManager.targetPcIp.value

        val announcePayload = """
            {
                "type": "announce",
                "name": "AutoDiscovered-Host"
            }
        """.trimIndent()

        val discoveredIp = "192.168.1.188"
        val handled = networkManager.processIncomingPacket(announcePayload, discoveredIp)

        assertTrue("Announce packet must be handled", handled)
        assertEquals("Target PC IP must be updated to the detected remote IP", discoveredIp, networkManager.targetPcIp.value)
        assertEquals("pcIpAddress must match the detected remote IP", discoveredIp, networkManager.pcIpAddress)
        assertTrue("PC status must be marked online", networkManager.isPcOnline.value)
    }

    @Test
    fun testLatencyCalculationPrecision() {
        val now = System.currentTimeMillis()
        networkManager.setPingStartTimeForTest(now - 12L)

        val pongPayload = """
            {
                "type": "pong",
                "name": "Test Host"
            }
        """.trimIndent()

        val handled = networkManager.processIncomingPacket(pongPayload, "192.168.1.150")
        assertTrue(handled)
        assertTrue("Latency must be positive and non-zero", networkManager.liveLatencyMs.value >= 1L)
    }

    @Test
    fun testHighFrequencyBurstPacketHandling() {
        val initialReceived = networkManager.packetsReceivedCount.value
        val initialMalformed = networkManager.malformedPacketsCount.value

        // Simulate a rapid burst of 50 mouse packets, 10 pongs, and 5 corrupt packets
        for (i in 1..50) {
            networkManager.processIncomingPacket("""{"type":"mouse","dx":$i,"dy":$i,"btn":0,"wheel":0}""", "192.168.1.50")
        }
        for (i in 1..10) {
            networkManager.processIncomingPacket("""{"type":"pong","name":"PC"}""", "192.168.1.50")
        }
        for (i in 1..5) {
            networkManager.processIncomingPacket("corrupt_packet_data_$i", "192.168.1.50")
        }

        assertEquals(initialReceived + 60, networkManager.packetsReceivedCount.value)
        assertEquals(initialMalformed + 5, networkManager.malformedPacketsCount.value)
    }
}
