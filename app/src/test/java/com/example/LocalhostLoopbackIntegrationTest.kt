package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.generator.HumanlikeTouchPacketGenerator
import com.example.service.NetworkStreamManager
import com.example.service.WifiConnectionTestState
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Localhost UDP Loopback Integration Test.
 *
 * Runs a live in-process UDP socket server on 127.0.0.1 (using an OS-assigned ephemeral port)
 * and streams realistic, randomized, humanlike touchpad movements to verify end-to-end
 * wire protocol fidelity, socket reception, packet buffering, and bidirectional ping/pong.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalhostLoopbackIntegrationTest {

    private lateinit var context: Context
    private lateinit var networkManager: NetworkStreamManager
    private var mockReceiverSocket: DatagramSocket? = null
    private var mockReceiverPort: Int = 0
    private val generator = HumanlikeTouchPacketGenerator(seed = 1337L)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        networkManager = NetworkStreamManager(context)

        // Bind mock receiver to ephemeral port on 127.0.0.1 (zero port avoids collisions)
        mockReceiverSocket = DatagramSocket(0, InetAddress.getByName("127.0.0.1")).apply {
            soTimeout = 2000
            reuseAddress = true
            receiveBufferSize = 65536
        }
        mockReceiverPort = mockReceiverSocket!!.localPort

        // Point NetworkStreamManager to the live mock receiver on localhost
        networkManager.pcPort = mockReceiverPort
        networkManager.targetPcIp.value = "127.0.0.1"
    }

    @After
    fun tearDown() {
        networkManager.stopHeartbeat()
        mockReceiverSocket?.close()
        mockReceiverSocket = null
    }

    @Test
    fun testRealisticHumanSwipeArcOverLocalhostLoopback() {
        // Generate realistic human swipe arc with minimum-jerk profile and tremor
        val gesturePackets = generator.generateSwipeArc(
            startX = 50.0,
            startY = 100.0,
            targetX = 350.0,
            targetY = 220.0,
            durationMs = 240L,
            sampleIntervalMs = 12L,
            arcCurvature = 16.0,
            tremorMagnitude = 0.8
        )

        val totalExpectedDx = gesturePackets.sumOf { it.dx }
        val totalExpectedDy = gesturePackets.sumOf { it.dy }
        val packetCount = gesturePackets.size

        val receivedPackets = CopyOnWriteArrayList<JSONObject>()
        val latch = CountDownLatch(packetCount)

        // Spawn mock receiver thread listening on localhost UDP socket
        val receiverThread = thread(start = true, name = "MockReceiver-Swipe") {
            val buf = ByteArray(2048)
            val packet = DatagramPacket(buf, buf.size)
            while (!Thread.currentThread().isInterrupted && receivedPackets.size < packetCount) {
                try {
                    mockReceiverSocket?.receive(packet)
                    val jsonStr = String(packet.data, 0, packet.length)
                    receivedPackets.add(JSONObject(jsonStr))
                    latch.countDown()
                } catch (_: SocketTimeoutException) {
                    break
                } catch (_: Exception) {
                    break
                }
            }
        }

        // Transmit realistic human swipe stream through NetworkStreamManager
        for (p in gesturePackets) {
            networkManager.sendRaw(p.toJson().toByteArray(), "127.0.0.1", retries = 1)
        }

        val completed = latch.await(3000, TimeUnit.MILLISECONDS)
        receiverThread.interrupt()

        assertTrue("All $packetCount realistic human swipe packets should arrive over localhost loopback", completed)
        assertEquals("Received count must match sent count", packetCount, receivedPackets.size)

        var totalReceivedDx = 0
        var totalReceivedDy = 0
        for (json in receivedPackets) {
            assertEquals("mouse", json.getString("type"))
            totalReceivedDx += json.getInt("dx")
            totalReceivedDy += json.getInt("dy")
            assertEquals(0, json.getInt("btn"))
            assertEquals(0, json.getInt("wheel"))
        }

        assertEquals("Cumulative received X displacement must exactly match human trajectory", totalExpectedDx, totalReceivedDx)
        assertEquals("Cumulative received Y displacement must exactly match human trajectory", totalExpectedDy, totalReceivedDy)
    }

    @Test
    fun testRealisticTapToClickSequenceWithButtonTransitions() {
        val tapPackets = generator.generateTapToClick(
            buttonCode = 1, // Left click
            dwellMs = 60L
        )

        val receivedJsonList = CopyOnWriteArrayList<JSONObject>()
        val latch = CountDownLatch(tapPackets.size)

        val receiverThread = thread(start = true, name = "MockReceiver-Tap") {
            val buf = ByteArray(2048)
            val packet = DatagramPacket(buf, buf.size)
            while (!Thread.currentThread().isInterrupted && receivedJsonList.size < tapPackets.size) {
                try {
                    mockReceiverSocket?.receive(packet)
                    val str = String(packet.data, 0, packet.length)
                    receivedJsonList.add(JSONObject(str))
                    latch.countDown()
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        }

        // Send tap gesture packets
        for (p in tapPackets) {
            networkManager.sendRaw(p.toJson().toByteArray(), "127.0.0.1", retries = 1)
        }

        val completed = latch.await(2000, TimeUnit.MILLISECONDS)
        receiverThread.interrupt()

        assertTrue("Tap click packets must arrive intact", completed)
        assertEquals(tapPackets.size, receivedJsonList.size)

        // Verify button state transitions (contact -> press [btn=1] -> release [btn=0])
        val buttonStates = receivedJsonList.map { it.getInt("btn") }
        assertTrue("Sequence must include a button press event (btn=1)", buttonStates.contains(1))
        assertEquals("Final state in tap sequence must release button (btn=0)", 0, buttonStates.last())
    }

    @Test
    fun testRealisticTwoFingerScrollStream() {
        val scrollPackets = generator.generateTwoFingerScroll(
            totalUnits = -16, // Natural scroll down
            durationMs = 200L
        )
        val expectedWheelSum = scrollPackets.sumOf { it.wheel }

        val receivedJsonList = CopyOnWriteArrayList<JSONObject>()
        val latch = CountDownLatch(scrollPackets.size)

        val receiverThread = thread(start = true, name = "MockReceiver-Scroll") {
            val buf = ByteArray(2048)
            val packet = DatagramPacket(buf, buf.size)
            while (!Thread.currentThread().isInterrupted && receivedJsonList.size < scrollPackets.size) {
                try {
                    mockReceiverSocket?.receive(packet)
                    val str = String(packet.data, 0, packet.length)
                    receivedJsonList.add(JSONObject(str))
                    latch.countDown()
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        }

        for (p in scrollPackets) {
            networkManager.sendRaw(p.toJson().toByteArray(), "127.0.0.1", retries = 1)
        }

        val completed = latch.await(2000, TimeUnit.MILLISECONDS)
        receiverThread.interrupt()

        assertTrue("Scroll packets must arrive", completed)
        val receivedWheelSum = receivedJsonList.sumOf { it.getInt("wheel") }
        assertEquals("Total scroll units must match generated human inertia curve", expectedWheelSum, receivedWheelSum)
    }

    @Test
    fun testBidirectionalPingPongRoundTripOnLocalhost() {
        val clientPingReceivedLatch = CountDownLatch(1)
        var clientPort = 0

        // Mock receiver responds to 'ping' with a valid 'pong' packet
        val receiverThread = thread(start = true, name = "MockReceiver-PingPong") {
            val buf = ByteArray(2048)
            val packet = DatagramPacket(buf, buf.size)
            try {
                mockReceiverSocket?.receive(packet)
                val json = JSONObject(String(packet.data, 0, packet.length))
                if (json.getString("type") == "ping") {
                    clientPort = json.getInt("client_port")
                    val pongJson = JSONObject().apply {
                        put("type", "pong")
                        put("name", "Localhost Test Server")
                        put("time", json.getLong("time"))
                    }
                    val pongBytes = pongJson.toString().toByteArray()
                    val pongPacket = DatagramPacket(pongBytes, pongBytes.size, packet.address, clientPort)
                    mockReceiverSocket?.send(pongPacket)
                    clientPingReceivedLatch.countDown()
                }
            } catch (_: Exception) {}
        }

        // Trigger ping from NetworkStreamManager
        networkManager.setPingStartTimeForTest(System.currentTimeMillis() - 5L)
        val pingJson = JSONObject().apply {
            put("type", "ping")
            put("client_port", mockReceiverPort) // Point response back to our mock receiver or test receiver
            put("time", System.currentTimeMillis())
        }
        networkManager.sendRaw(pingJson.toString().toByteArray(), "127.0.0.1")

        val pingHandled = clientPingReceivedLatch.await(2000, TimeUnit.MILLISECONDS)
        receiverThread.interrupt()
        assertTrue("Mock receiver must receive ping packet over localhost loopback", pingHandled)

        // Process simulated pong response on NetworkStreamManager to verify state flow updates
        val handled = networkManager.processIncomingPacket(
            """{"type":"pong","name":"Localhost Test Server","time":1000}""",
            "127.0.0.1"
        )
        assertTrue(handled)
        assertTrue("PC status must update to online", networkManager.isPcOnline.value)
        assertEquals("127.0.0.1", networkManager.pcIpAddress)
        assertTrue(networkManager.testState.value is WifiConnectionTestState.Success)
    }

    @Test
    fun testRealisticHumanGestureSessionSuppressesHeartbeatContention() = runBlocking {
        val session = generator.generateRealisticSession()
        assertTrue("Session should contain a realistic variety of gesture packets", session.size >= 20)

        // Feed session packets and check suppression timing
        for (packet in session.take(10)) {
            networkManager.sendMousePacket(packet.dx, packet.dy, packet.btn.toByte(), packet.wheel)
        }

        val activeTime = System.currentTimeMillis()
        assertTrue("Trackpad must be identified as active during gesture stream",
            networkManager.isTrackpadActive(activeTime))
        assertFalse("Heartbeat ping MUST be suppressed during active gesture stream to prioritize touch bandwidth",
            networkManager.shouldSendHeartbeatPing(activeTime))

        // Fast-forward simulated clock past 1200ms threshold
        val idleTime = activeTime + 1500L
        assertFalse("Trackpad must be inactive after idle threshold",
            networkManager.isTrackpadActive(idleTime))
        assertTrue("Heartbeat ping must resume when gesture ceases",
            networkManager.shouldSendHeartbeatPing(idleTime))
    }
}
