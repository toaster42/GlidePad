package com.example.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance In-App UDP Receiver for ChromeOS and large-screen Android devices.
 *
 * Runs an embedded UDP server listening on port 8089 (or custom ephemeral port) that receives
 * real-time touchpad gestures, mouse deltas, scroll wheel events, and keyboard keys from remote
 * GlidePad phone controllers. Provides live telemetry, HUD coordinate tracking, and bidirectional
 * ping/pong heartbeat echo.
 */
class InAppReceiverManager(private val context: Context) {

    private val tag = "InAppReceiverManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private var serverSocket: DatagramSocket? = null
    private var serverJob: Job? = null
    private var ppsMonitorJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8089)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _hostIp = MutableStateFlow("127.0.0.1")
    val hostIp: StateFlow<String> = _hostIp.asStateFlow()

    private val _lastClientIp = MutableStateFlow<String?>(null)
    val lastClientIp: StateFlow<String?> = _lastClientIp.asStateFlow()

    private val _activeClients = MutableStateFlow<List<String>>(emptyList())
    val activeClients: StateFlow<List<String>> = _activeClients.asStateFlow()

    private val _packetsPerSecond = MutableStateFlow(0)
    val packetsPerSecond: StateFlow<Int> = _packetsPerSecond.asStateFlow()

    private val _totalPacketsReceived = MutableStateFlow(0L)
    val totalPacketsReceived: StateFlow<Long> = _totalPacketsReceived.asStateFlow()

    private val _cursorNormalizedX = MutableStateFlow(0.5f)
    val cursorNormalizedX: StateFlow<Float> = _cursorNormalizedX.asStateFlow()

    private val _cursorNormalizedY = MutableStateFlow(0.5f)
    val cursorNormalizedY: StateFlow<Float> = _cursorNormalizedY.asStateFlow()

    private val _lastButtonState = MutableStateFlow(0)
    val lastButtonState: StateFlow<Int> = _lastButtonState.asStateFlow()

    private val _lastWheelDelta = MutableStateFlow(0)
    val lastWheelDelta: StateFlow<Int> = _lastWheelDelta.asStateFlow()

    private val _lastReceivedType = MutableStateFlow("None")
    val lastReceivedType: StateFlow<String> = _lastReceivedType.asStateFlow()

    private val _lastReceivedSummary = MutableStateFlow("Waiting for incoming connection...")
    val lastReceivedSummary: StateFlow<String> = _lastReceivedSummary.asStateFlow()

    private val _recentPacketsLog = MutableStateFlow<List<String>>(emptyList())
    val recentPacketsLog: StateFlow<List<String>> = _recentPacketsLog.asStateFlow()

    private val clientTimestamps = ConcurrentHashMap<String, Long>()
    private val packetCounterForPps = AtomicLong(0)

    init {
        refreshHostIp()
    }

    /**
     * Finds the local IP address of the device on Wi-Fi, Ethernet, or ARC container virtual interface.
     */
    fun refreshHostIp(): String {
        var detectedIp = "127.0.0.1"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                            detectedIp = host
                            break
                        }
                    }
                }
                if (detectedIp != "127.0.0.1") break
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not resolve network interfaces: ${e.message}")
        }
        _hostIp.value = detectedIp
        return detectedIp
    }

    /**
     * Starts the embedded In-App UDP receiver server.
     */
    @Synchronized
    fun start(port: Int = 8089): Boolean {
        if (_isRunning.value && serverSocket != null && !serverSocket!!.isClosed) {
            return true
        }

        refreshHostIp()

        return try {
            val socket = DatagramSocket(port).apply {
                reuseAddress = true
                try {
                    receiveBufferSize = 65536
                } catch (_: Exception) {}
                soTimeout = 2000
            }
            serverSocket = socket
            _serverPort.value = socket.localPort
            _isRunning.value = true

            appendLog("Host Receiver started on ${_hostIp.value}:${_serverPort.value}")

            startListenerJob(socket)
            startPpsMonitor()
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind In-App UDP Receiver on port $port", e)
            appendLog("Error: Failed to bind port $port (${e.message})")
            _isRunning.value = false
            false
        }
    }

    private fun startListenerJob(socket: DatagramSocket) {
        serverJob?.cancel()
        serverJob = scope.launch {
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && _isRunning.value) {
                try {
                    socket.receive(packet)
                    val rawStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val senderIp = packet.address.hostAddress ?: "Unknown"
                    val senderPort = packet.port

                    packetCounterForPps.incrementAndGet()
                    _totalPacketsReceived.value += 1
                    _lastClientIp.value = senderIp
                    clientTimestamps[senderIp] = System.currentTimeMillis()

                    processPacket(rawStr, senderIp, senderPort, socket)
                } catch (e: SocketException) {
                    if (!_isRunning.value) break
                } catch (e: java.net.SocketTimeoutException) {
                    // Timeout is normal to check isActive
                } catch (e: Exception) {
                    if (isActive && _isRunning.value) {
                        Log.w(tag, "Error processing incoming packet: ${e.message}")
                    }
                }
            }
        }
    }

    private fun processPacket(jsonStr: String, senderIp: String, senderPort: Int, socket: DatagramSocket) {
        try {
            val json = JSONObject(jsonStr)
            val type = json.optString("type", "unknown")
            _lastReceivedType.value = type

            when (type) {
                "mouse" -> {
                    val dx = json.optInt("dx", 0)
                    val dy = json.optInt("dy", 0)
                    val btn = json.optInt("btn", 0)
                    val wheel = json.optInt("wheel", 0)

                    _lastButtonState.value = btn
                    _lastWheelDelta.value = wheel

                    // Update virtual canvas position (normalized with smooth clamping)
                    val sensitivity = 0.0012f
                    val newX = (_cursorNormalizedX.value + dx * sensitivity).coerceIn(0.02f, 0.98f)
                    val newY = (_cursorNormalizedY.value + dy * sensitivity).coerceIn(0.02f, 0.98f)
                    _cursorNormalizedX.value = newX
                    _cursorNormalizedY.value = newY

                    val actionDesc = when {
                        btn != 0 -> "Click btn=$btn"
                        wheel != 0 -> "Scroll wheel=$wheel"
                        else -> "Move dx=$dx, dy=$dy"
                    }
                    _lastReceivedSummary.value = "$senderIp: $actionDesc"
                }

                "keyboard" -> {
                    val text = json.optString("text", "")
                    val keys = json.optJSONArray("keys")
                    val keysStr = if (keys != null) (0 until keys.length()).map { keys.getString(it) }.joinToString("+") else ""
                    val desc = if (text.isNotEmpty()) "Text: \"$text\"" else "Keys: $keysStr"
                    _lastReceivedSummary.value = "$senderIp: $desc"
                    appendLog("Key from $senderIp: $desc")
                }

                "ping" -> {
                    val clientPort = json.optInt("client_port", senderPort)
                    val clientTime = json.optLong("time", 0L)
                    _lastReceivedSummary.value = "Heartbeat Ping from $senderIp:$clientPort"

                    // Respond with Pong to keep connection active and calculate latency
                    val pongJson = JSONObject().apply {
                        put("type", "pong")
                        put("name", "GlidePad ChromeOS Host")
                        put("status", "ok")
                        put("echo_time", clientTime)
                        put("server_time", System.currentTimeMillis())
                    }
                    val bytes = pongJson.toString().toByteArray(Charsets.UTF_8)
                    val pongPacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName(senderIp), clientPort)
                    socket.send(pongPacket)
                }

                "discover" -> {
                    val clientPort = json.optInt("client_port", senderPort)
                    _lastReceivedSummary.value = "Discovery Request from $senderIp"

                    val announceJson = JSONObject().apply {
                        put("type", "announce")
                        put("name", "GlidePad ChromeOS Host")
                        put("status", "ok")
                        put("port", _serverPort.value)
                    }
                    val bytes = announceJson.toString().toByteArray(Charsets.UTF_8)
                    val announcePacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName(senderIp), clientPort)
                    socket.send(announcePacket)
                    appendLog("Client discovered: $senderIp")
                }

                "benchmark_ping" -> {
                    val clientPort = json.optInt("client_port", senderPort)
                    val seq = json.optInt("seq", 0)
                    val pongJson = JSONObject().apply {
                        put("type", "benchmark_pong")
                        put("seq", seq)
                        put("server_time", System.currentTimeMillis())
                    }
                    val bytes = pongJson.toString().toByteArray(Charsets.UTF_8)
                    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(senderIp), clientPort))
                }

                else -> {
                    _lastReceivedSummary.value = "$senderIp: $type"
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "JSON parsing error on incoming packet: ${e.message}")
        }
    }

    private fun startPpsMonitor() {
        ppsMonitorJob?.cancel()
        ppsMonitorJob = scope.launch {
            while (isActive && _isRunning.value) {
                delay(1000L)
                val count = packetCounterForPps.getAndSet(0)
                _packetsPerSecond.value = count.toInt()

                // Purge clients not seen in 15 seconds
                val now = System.currentTimeMillis()
                clientTimestamps.entries.removeIf { now - it.value > 15000L }
                _activeClients.value = clientTimestamps.keys.toList()
            }
        }
    }

    private fun appendLog(msg: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$timeStr] $msg"
        val current = _recentPacketsLog.value.toMutableList()
        if (current.size >= 8) {
            current.removeAt(0)
        }
        current.add(entry)
        _recentPacketsLog.value = current
    }

    /**
     * Stops the In-App UDP receiver server.
     */
    @Synchronized
    fun stop() {
        _isRunning.value = false
        serverJob?.cancel()
        serverJob = null
        ppsMonitorJob?.cancel()
        ppsMonitorJob = null

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        _packetsPerSecond.value = 0
        appendLog("Host Receiver stopped")
    }

    fun resetStats() {
        _totalPacketsReceived.value = 0L
        _packetsPerSecond.value = 0
        packetCounterForPps.set(0)
        _recentPacketsLog.value = emptyList()
        _cursorNormalizedX.value = 0.5f
        _cursorNormalizedY.value = 0.5f
    }
}
