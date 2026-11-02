package com.example.service

import android.content.Context
import android.net.wifi.WifiManager
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
import java.net.InetAddress
import java.net.SocketTimeoutException

sealed class WifiConnectionTestState {
    object Idle : WifiConnectionTestState()
    object Testing : WifiConnectionTestState()
    data class Success(val latencyMs: Long, val serverName: String, val ip: String) : WifiConnectionTestState()
    data class Error(val message: String) : WifiConnectionTestState()
}

class NetworkStreamManager(private val context: Context) {

    private val tag = "NetworkStreamManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    val targetPcIp = MutableStateFlow("192.168.1.100")
    var pcIpAddress: String
        get() = targetPcIp.value
        set(value) {
            targetPcIp.value = value
        }
    var pcPort: Int = 8089

    private var sendSocket: DatagramSocket? = null
    private var receiveSocket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null

    val isPcOnline = MutableStateFlow(false)
    val liveLatencyMs = MutableStateFlow(3L)

    private val _testState = MutableStateFlow<WifiConnectionTestState>(WifiConnectionTestState.Idle)
    val testState: StateFlow<WifiConnectionTestState> = _testState.asStateFlow()

    private val _logCallback = MutableStateFlow<String?>(null)
    val logCallback: StateFlow<String?> = _logCallback.asStateFlow()

    var socketTimeoutMs: Int = 3000
    var offlineTimeoutThresholdMs: Long = 6000L
    var heartbeatIntervalMs: Long = 3000L
    var maxRetryAttempts: Int = 3
    var retryBackoffMs: Long = 50L

    val malformedPacketsCount = MutableStateFlow(0)
    val networkErrorCount = MutableStateFlow(0)
    val packetsSentCount = MutableStateFlow(0L)
    val packetsReceivedCount = MutableStateFlow(0L)

    private var pingStartTime: Long = 0
    private var lastPongReceivedTime: Long = 0

    init {
        initSockets()
    }

    @Synchronized
    fun initSockets() {
        try {
            if (sendSocket == null || sendSocket?.isClosed == true) {
                sendSocket = DatagramSocket().apply {
                    broadcast = true
                }
            }
            startReceiver()
        } catch (e: Exception) {
            networkErrorCount.value++
            Log.e(tag, "Failed to initialize UDP socket", e)
        }
    }

    private fun startReceiver() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            try {
                if (receiveSocket == null || receiveSocket?.isClosed == true) {
                    receiveSocket = DatagramSocket().apply {
                        soTimeout = socketTimeoutMs
                    }
                }
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        receiveSocket?.receive(packet)
                        val text = String(packet.data, 0, packet.length)
                        handleIncomingPacket(text, packet.address.hostAddress ?: "")
                    } catch (e: SocketTimeoutException) {
                        // Keep receiver alive - timeout is expected when idle
                    } catch (e: Exception) {
                        networkErrorCount.value++
                        if (!isActive) break
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                networkErrorCount.value++
                Log.e(tag, "Receiver exception", e)
            }
        }
    }

    fun handleIncomingPacket(rawJson: String, senderIp: String) {
        processIncomingPacket(rawJson, senderIp)
    }

    /**
     * Processes an incoming raw JSON packet safely.
     * Returns true if packet was parsed and recognized; false if malformed or unrecognized.
     */
    fun processIncomingPacket(rawJson: String, senderIp: String): Boolean {
        return try {
            val json = JSONObject(rawJson)
            packetsReceivedCount.value++
            when (json.optString("type")) {
                "pong" -> {
                    val wasOffline = !isPcOnline.value
                    val now = System.currentTimeMillis()
                    val rtt = if (pingStartTime > 0) (now - pingStartTime).coerceAtLeast(1) else 3L
                    lastPongReceivedTime = now
                    isPcOnline.value = true
                    liveLatencyMs.value = rtt
                    pcIpAddress = senderIp

                    val server = json.optString("name", "GlidePad PC")
                    _testState.value = WifiConnectionTestState.Success(
                        latencyMs = rtt,
                        serverName = server,
                        ip = senderIp
                    )
                    // Only log connection status changes or explicit tests to prevent StateFlow log thrashing
                    if (wasOffline) {
                        _logCallback.value = "[WIFI CONNECTED] $server ($senderIp) • ${rtt}ms"
                    } else if (_testState.value is WifiConnectionTestState.Testing) {
                        _logCallback.value = "[WIFI PONG] $server ($senderIp) • ${rtt}ms"
                    }
                    true
                }
                "announce" -> {
                    val server = json.optString("name", "GlidePad PC")
                    pcIpAddress = senderIp
                    isPcOnline.value = true
                    liveLatencyMs.value = 3L
                    _testState.value = WifiConnectionTestState.Success(
                        latencyMs = 3,
                        serverName = server,
                        ip = senderIp
                    )
                    _logCallback.value = "[WIFI DISCOVERY] Discovered PC: $server at $senderIp"
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            malformedPacketsCount.value++
            Log.w(tag, "Failed to parse incoming packet: $rawJson", e)
            false
        }
    }

    fun checkHeartbeatTimeout(currentTime: Long = System.currentTimeMillis()) {
        if (lastPongReceivedTime > 0 && (currentTime - lastPongReceivedTime) > offlineTimeoutThresholdMs) {
            if (isPcOnline.value) {
                isPcOnline.value = false
                _logCallback.value = "[WIFI OFFLINE] Heartbeat timed out (> ${offlineTimeoutThresholdMs}ms)"
            }
        }
    }

    fun simulatePongReceived(senderIp: String = pcIpAddress, latencyMs: Long = 4L, serverName: String = "Test PC") {
        val now = System.currentTimeMillis()
        lastPongReceivedTime = now
        isPcOnline.value = true
        liveLatencyMs.value = latencyMs
        pcIpAddress = senderIp
        _testState.value = WifiConnectionTestState.Success(
            latencyMs = latencyMs,
            serverName = serverName,
            ip = senderIp
        )
    }

    fun setLastPongTimeForTest(timeMs: Long) {
        lastPongReceivedTime = timeMs
    }

    fun setPingStartTimeForTest(timeMs: Long) {
        pingStartTime = timeMs
    }

    @Volatile
    var lastMousePacketSentTime: Long = 0L

    fun isTrackpadActive(currentTime: Long = System.currentTimeMillis(), thresholdMs: Long = 1200L): Boolean {
        return (currentTime - lastMousePacketSentTime) < thresholdMs
    }

    fun shouldSendHeartbeatPing(currentTime: Long = System.currentTimeMillis(), thresholdMs: Long = 1200L): Boolean {
        return !isTrackpadActive(currentTime, thresholdMs)
    }

    fun isHeartbeatActive(): Boolean = heartbeatJob?.isActive == true

    fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) {
            return
        }
        heartbeatJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                // If trackpad is actively streaming gestures/moves (< 1200ms), defer ping
                // to dedicate 100% of UDP socket and receiver bandwidth to real-time pointer events
                if (shouldSendHeartbeatPing(now)) {
                    try {
                        pingStartTime = System.currentTimeMillis()
                        val clientPort = receiveSocket?.localPort ?: 0
                        val pingJson = JSONObject().apply {
                            put("type", "ping")
                            put("client_port", clientPort)
                            put("time", pingStartTime)
                        }
                        sendRaw(pingJson.toString().toByteArray(), retries = 1)
                    } catch (e: Exception) {
                        networkErrorCount.value++
                    }
                } else {
                    // Trackpad motion packets prove connection is actively functioning
                    lastPongReceivedTime = now
                    isPcOnline.value = true
                }

                checkHeartbeatTimeout()
                delay(heartbeatIntervalMs)
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun testConnection(targetIp: String = pcIpAddress) {
        _testState.value = WifiConnectionTestState.Testing
        pingStartTime = System.currentTimeMillis()
        scope.launch {
            try {
                pcIpAddress = targetIp
                val clientPort = receiveSocket?.localPort ?: 0
                val pingJson = JSONObject().apply {
                    put("type", "ping")
                    put("client_port", clientPort)
                    put("time", pingStartTime)
                }
                sendRaw(pingJson.toString().toByteArray(), targetIp)

                delay(2500)
                if (_testState.value is WifiConnectionTestState.Testing) {
                    _testState.value = WifiConnectionTestState.Error(
                        "No reply from $targetIp:$pcPort. Make sure the Python receiver is running on your PC and your firewall allows UDP port $pcPort."
                    )
                    _logCallback.value = "[WIFI ERROR] Ping timeout to $targetIp"
                }
            } catch (e: Exception) {
                _testState.value = WifiConnectionTestState.Error("Connection error: ${e.localizedMessage}")
                _logCallback.value = "[WIFI ERROR] ${e.localizedMessage}"
            }
        }
    }

    fun discoverPcOnNetwork() {
        _testState.value = WifiConnectionTestState.Testing
        scope.launch {
            try {
                val clientPort = receiveSocket?.localPort ?: 0
                val discoverJson = JSONObject().apply {
                    put("type", "discover")
                    put("client_port", clientPort)
                }
                val data = discoverJson.toString().toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddr, pcPort)
                sendSocket?.send(packet)
                _logCallback.value = "[WIFI] Broadcast discovery sent on port $pcPort"

                delay(3000)
                if (_testState.value is WifiConnectionTestState.Testing) {
                    _testState.value = WifiConnectionTestState.Error(
                        "No PC auto-detected. Please type your PC's IP address directly (shown in the receiver terminal)."
                    )
                }
            } catch (e: Exception) {
                _testState.value = WifiConnectionTestState.Error("Scan failed: ${e.localizedMessage}")
            }
        }
    }

    fun sendMousePacket(dx: Int, dy: Int, buttons: Byte, wheel: Int = 0) {
        lastMousePacketSentTime = System.currentTimeMillis()
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "mouse")
                    put("dx", dx)
                    put("dy", dy)
                    put("btn", buttons.toInt())
                    put("wheel", wheel)
                }
                // Send immediately with 1 attempt to prevent backoff sleep jitter during rapid pointer streaming
                sendRaw(json.toString().toByteArray(), retries = 1)
            } catch (e: Exception) {
                Log.w(tag, "sendMousePacket failed", e)
            }
        }
    }

    fun sendKeyPacket(keys: List<String>, action: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "keyboard")
                    put("action", action)
                    put("keys", keys.joinToString("+"))
                }
                sendRaw(json.toString().toByteArray())
            } catch (e: Exception) {
                Log.w(tag, "sendKeyPacket failed", e)
            }
        }
    }

    fun sendTextDirect(text: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                }
                sendRaw(json.toString().toByteArray())
            } catch (e: Exception) {
                Log.w(tag, "sendTextDirect failed", e)
            }
        }
    }

    fun sendOpenUrl(url: String) {
        scope.launch {
            try {
                val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else url
                val json = JSONObject().apply {
                    put("type", "open_url")
                    put("url", cleanUrl)
                }
                sendRaw(json.toString().toByteArray())
                _logCallback.value = "[WIFI URL] Opened: $cleanUrl"
            } catch (e: Exception) {
                Log.w(tag, "sendOpenUrl failed", e)
            }
        }
    }

    fun sendSystemCommand(command: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "system_cmd")
                    put("command", command)
                }
                sendRaw(json.toString().toByteArray())
                _logCallback.value = "[WIFI CMD] $command"
            } catch (e: Exception) {
                Log.w(tag, "sendSystemCommand failed", e)
            }
        }
    }

    fun sendWakeOnLan(macAddress: String) {
        scope.launch {
            try {
                val cleanMac = macAddress.replace(":", "").replace("-", "")
                if (cleanMac.length != 12) {
                    _logCallback.value = "[WOL ERR] Invalid MAC address: $macAddress"
                    return@launch
                }
                val macBytes = ByteArray(6) { i ->
                    cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                val bytes = ByteArray(6 + 16 * 6)
                for (i in 0 until 6) bytes[i] = 0xFF.toByte()
                for (i in 0 until 16) {
                    System.arraycopy(macBytes, 0, bytes, 6 + i * 6, 6)
                }
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet9 = DatagramPacket(bytes, bytes.size, broadcastAddr, 9)
                val packet7 = DatagramPacket(bytes, bytes.size, broadcastAddr, 7)
                sendSocket?.send(packet9)
                sendSocket?.send(packet7)
                _logCallback.value = "[WOL] Magic Packet broadcast sent to $macAddress"
            } catch (e: Exception) {
                _logCallback.value = "[WOL ERR] ${e.message}"
            }
        }
    }

    @Volatile
    private var cachedTargetIp: String? = null
    @Volatile
    private var cachedInetAddress: InetAddress? = null

    private fun getResolvedAddress(targetIp: String): InetAddress {
        if (targetIp == cachedTargetIp && cachedInetAddress != null) {
            return cachedInetAddress!!
        }
        val addr = InetAddress.getByName(targetIp)
        cachedTargetIp = targetIp
        cachedInetAddress = addr
        return addr
    }

    fun sendRaw(data: ByteArray, targetIp: String = pcIpAddress, retries: Int = maxRetryAttempts) {
        var attempts = 0
        var sent = false
        var lastErr: Exception? = null

        while (attempts < retries && !sent) {
            attempts++
            try {
                if (sendSocket == null || sendSocket?.isClosed == true) {
                    initSockets()
                }
                val addr = getResolvedAddress(targetIp)
                val packet = DatagramPacket(data, data.size, addr, pcPort)
                sendSocket?.send(packet)
                packetsSentCount.value++
                sent = true
            } catch (e: Exception) {
                lastErr = e
                networkErrorCount.value++
                if (attempts < retries) {
                    try {
                        Thread.sleep(retryBackoffMs)
                    } catch (_: InterruptedException) {}
                }
            }
        }

        if (!sent && lastErr != null) {
            _logCallback.value = "[UDP ERR] Failed sending to $targetIp:$pcPort after $retries attempts: ${lastErr.message}"
        }
    }

    fun getDeviceLocalIp(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else {
                "Wi-Fi not connected"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    companion object {
        const val RECEIVER_SCRIPT_VERSION = "2.3"
        const val RECEIVER_SCRIPT_FILENAME = "glidepad_receiver_v2_3.py"

        const val BENCHMARK_RECEIVER_SCRIPT_VERSION = "1.1"
        const val BENCHMARK_RECEIVER_SCRIPT_FILENAME = "glidepad_benchmark_receiver_v1_1.py"

        val PYTHON_RECEIVER_SCRIPT = """
# =====================================================================
#  GlidePad Ultra-Low-Latency PC Receiver (v2.3 - Wi-Fi Power Edition)
# =====================================================================
#  Requirements:
#    pip install pynput
#
#  Usage:
#    python glidepad_receiver_v2_3.py
# =====================================================================

import socket
import json
import sys
import platform
import subprocess
import webbrowser

try:
    from pynput.mouse import Button, Controller as MouseController
    from pynput.keyboard import Key, KeyCode, Controller as KeyController
except ImportError:
    print("\n[!] Missing requirement: 'pynput'")
    print("    Please run:  pip install pynput\n")
    sys.exit(1)

mouse = MouseController()
keyboard = KeyController()

UDP_PORT = 8089

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

local_ip = get_local_ip()
hostname = socket.gethostname()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
try:
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)
except Exception:
    pass
sock.bind(('0.0.0.0', UDP_PORT))

print("=" * 64)
print("  GLIDEPAD PC RECEIVER ACTIVE (v2.3)")
print("=" * 64)
print(f"  System       : {platform.system()} ({hostname})")
print(f"  UDP Port     : {UDP_PORT}")
print(f"  >>> ENTER THIS IP IN GLIDEPAD APP:  {local_ip}")
print("=" * 64)
print("  Ready for touch gestures, keys, URLs & system commands...\n")

SPECIAL_KEYS = {
    'Enter': Key.enter, 'Return': Key.enter,
    'Backspace': Key.backspace,
    'Tab': Key.tab,
    'Space': Key.space,
    'Esc': Key.esc, 'Escape': Key.esc,
    'Del': Key.delete, 'Delete': Key.delete,
    'Up': Key.up, 'Down': Key.down, 'Left': Key.left, 'Right': Key.right,
    'Home': Key.home, 'End': Key.end,
    'PgUp': Key.page_up, 'PgDn': Key.page_down,
    'Shift': Key.shift, 'Ctrl': Key.ctrl, 'Alt': Key.alt, 'Win': Key.cmd,
    'F1': Key.f1, 'F2': Key.f2, 'F3': Key.f3, 'F4': Key.f4,
    'F5': Key.f5, 'F6': Key.f6, 'F7': Key.f7, 'F8': Key.f8,
    'F9': Key.f9, 'F10': Key.f10, 'F11': Key.f11, 'F12': Key.f12,
    'MediaPlayPause': Key.media_play_pause,
    'AudioVolUp': Key.media_volume_up,
    'AudioVolDown': Key.media_volume_down,
    'AudioMute': Key.media_volume_mute
}

prev_btn = 0

while True:
    try:
        data, addr = sock.recvfrom(2048)
        msg = json.loads(data.decode('utf-8'))
        msg_type = msg.get('type')

        # 1. PING / HEARTBEAT / AUTO-DISCOVER
        if msg_type in ('ping', 'discover'):
            client_port = msg.get('client_port', addr[1])
            resp = json.dumps({
                'type': 'pong' if msg_type == 'ping' else 'announce',
                'name': f"{hostname} ({platform.system()})",
                'status': 'ok'
            }).encode('utf-8')
            sock.sendto(resp, (addr[0], client_port if client_port else addr[1]))

        # 2. MOUSE MOVEMENT, CLICKS, & SCROLL
        elif msg_type == 'mouse':
            dx = msg.get('dx', 0)
            dy = msg.get('dy', 0)
            btn = msg.get('btn', 0)
            wheel = msg.get('wheel', 0)

            if dx != 0 or dy != 0:
                mouse.move(dx, dy)
            if wheel != 0:
                mouse.scroll(0, wheel)

            if btn != prev_btn:
                # Left Click
                if (btn & 1) and not (prev_btn & 1):
                    mouse.press(Button.left)
                elif not (btn & 1) and (prev_btn & 1):
                    mouse.release(Button.left)
                # Right Click
                if (btn & 2) and not (prev_btn & 2):
                    mouse.press(Button.right)
                elif not (btn & 2) and (prev_btn & 2):
                    mouse.release(Button.right)
                # Middle Click
                if (btn & 4) and not (prev_btn & 4):
                    mouse.press(Button.middle)
                elif not (btn & 4) and (prev_btn & 4):
                    mouse.release(Button.middle)
                prev_btn = btn

        # 3. DIRECT TEXT ENTRY
        elif msg_type == 'text':
            text = msg.get('text', '')
            keyboard.type(text)
            print(f"[TEXT] Typed: {text}")

        # 4. KEYBOARD SHORTCUTS & HOTKEYS
        elif msg_type == 'keyboard':
            action = msg.get('action')
            keys_str = msg.get('keys', '')
            parts = keys_str.split('+')
            held = []

            for k in parts:
                k = k.strip()
                target_key = SPECIAL_KEYS.get(k)
                if not target_key:
                    if len(k) == 1:
                        target_key = KeyCode.from_char(k.lower())
                if target_key:
                    keyboard.press(target_key)
                    held.append(target_key)

            for target_key in reversed(held):
                keyboard.release(target_key)

        # 5. OPEN WEB URL IN DEFAULT BROWSER
        elif msg_type == 'open_url':
            url = msg.get('url', '')
            if url:
                webbrowser.open(url)
                print(f"[URL] Opened in browser: {url}")

        # 6. SYSTEM COMMANDS (Terminal, Explorer, Task Manager, Lock)
        elif msg_type == 'system_cmd':
            cmd = msg.get('command', '')
            print(f"[CMD] Received: {cmd}")
            sys_name = platform.system()
            if cmd == 'terminal':
                if sys_name == 'Windows':
                    subprocess.Popen('cmd.exe')
                elif sys_name == 'Darwin':
                    subprocess.Popen(['open', '-a', 'Terminal'])
                else:
                    subprocess.Popen('x-terminal-emulator')
            elif cmd == 'explorer':
                if sys_name == 'Windows':
                    subprocess.Popen('explorer.exe')
                elif sys_name == 'Darwin':
                    subprocess.Popen(['open', '.'])
                else:
                    subprocess.Popen(['xdg-open', '.'])
            elif cmd == 'taskmgr':
                if sys_name == 'Windows':
                    subprocess.Popen('taskmgr.exe')
                elif sys_name == 'Darwin':
                    subprocess.Popen(['open', '-a', 'Activity Monitor'])
            elif cmd == 'lock':
                if sys_name == 'Windows':
                    import ctypes
                    ctypes.windll.user32.LockWorkStation()
                elif sys_name == 'Darwin':
                    subprocess.Popen(['pmset', 'displaysleepnow'])

    except KeyboardInterrupt:
        print("\n[*] GlidePad Receiver stopped.")
        break
    except Exception as e:
        pass
        """.trimIndent()

        val PYTHON_BENCHMARK_RECEIVER_SCRIPT = """
#!/usr/bin/env python3
# ==============================================================================
#  GlidePad Latency & Throughput Benchmarking Receiver (v1.1)
# ==============================================================================
#  Dedicated receiver script for measuring network performance between Android
#  device and PC host over Wi-Fi / Local Area Network.
#
#  Measures:
#    - Round-Trip Time (RTT) & One-Way Latency (Min, Mean, P95, P99, Max)
#    - Inter-packet Arrival Jitter (RFC 3550 standard)
#    - Packet Throughput (Hz / packets per second & Bandwidth in KB/s)
#    - Packet Loss, Sequence Gaps, and Out-of-Order Delivery
#
#  Usage:
#    python glidepad_benchmark_receiver_v1_1.py [--port 8089] [--headless] [--output results.json]
#
#  Requirements:
#    - Python 3.7+
#    - pynput (Optional: for executing live cursor moves during benchmarks)
# ==============================================================================

import argparse
import json
import math
import os
import platform
import socket
import sys
import time
from datetime import datetime

PYNPUT_AVAILABLE = False
try:
    from pynput.mouse import Button, Controller as MouseController
    from pynput.keyboard import Key, KeyCode, Controller as KeyController
    mouse = MouseController()
    keyboard = KeyController()
    PYNPUT_AVAILABLE = True
except (ImportError, Exception):
    mouse = None
    keyboard = None


class BenchmarkSession:
    def __init__(self, session_id="default"):
        self.session_id = session_id
        self.start_time = None
        self.end_time = None
        self.expected_seq = 0
        self.total_packets_received = 0
        self.packets_lost = 0
        self.out_of_order = 0
        self.latencies_ms = []
        self.inter_arrival_times_ms = []
        self.last_arrival_time = None
        self.total_bytes = 0
        self.jitter = 0.0

    def start(self):
        self.start_time = time.time()
        self.expected_seq = 0
        self.total_packets_received = 0
        self.packets_lost = 0
        self.out_of_order = 0
        self.latencies_ms.clear()
        self.inter_arrival_times_ms.clear()
        self.last_arrival_time = None
        self.total_bytes = 0
        self.jitter = 0.0

    def record_packet(self, seq, client_timestamp_ms, data_len):
        now = time.time()
        now_ms = now * 1000.0
        self.total_bytes += data_len
        self.total_packets_received += 1

        transit_ms = max(0.0, now_ms - client_timestamp_ms) if client_timestamp_ms > 0 else 0.0
        self.latencies_ms.append(transit_ms)

        if self.last_arrival_time is not None:
            interval_ms = (now - self.last_arrival_time) * 1000.0
            self.inter_arrival_times_ms.append(interval_ms)
            if len(self.inter_arrival_times_ms) > 1:
                diff = abs(interval_ms - self.inter_arrival_times_ms[-2])
                self.jitter += (diff - self.jitter) / 16.0
        self.last_arrival_time = now

        if seq > self.expected_seq:
            self.packets_lost += (seq - self.expected_seq)
            self.expected_seq = seq + 1
        elif seq < self.expected_seq:
            self.out_of_order += 1
        else:
            self.expected_seq += 1

    def finish(self):
        self.end_time = time.time()
        duration = max(0.001, self.end_time - (self.start_time or self.end_time))
        avg_hz = self.total_packets_received / duration
        kb_per_sec = (self.total_bytes / 1024.0) / duration

        sorted_lats = sorted(self.latencies_ms) if self.latencies_ms else [0.0]
        n = len(sorted_lats)
        avg_lat = sum(sorted_lats) / n if n > 0 else 0.0
        min_lat = sorted_lats[0]
        p50_lat = sorted_lats[int(n * 0.50)] if n > 0 else 0.0
        p95_lat = sorted_lats[int(n * 0.95)] if n > 0 else 0.0
        p99_lat = sorted_lats[int(n * 0.99)] if n > 0 else 0.0
        max_lat = sorted_lats[-1]

        total_expected = self.total_packets_received + self.packets_lost
        loss_pct = (self.packets_lost / total_expected * 100.0) if total_expected > 0 else 0.0

        return {
            "session_id": self.session_id,
            "duration_sec": round(duration, 3),
            "packets_received": self.total_packets_received,
            "packets_lost": self.packets_lost,
            "packet_loss_pct": round(loss_pct, 2),
            "out_of_order": self.out_of_order,
            "throughput_hz": round(avg_hz, 1),
            "bandwidth_kb_s": round(kb_per_sec, 2),
            "jitter_ms": round(self.jitter, 3),
            "latency": {
                "min_ms": round(min_lat, 2),
                "avg_ms": round(avg_lat, 2),
                "p50_ms": round(p50_lat, 2),
                "p95_ms": round(p95_lat, 2),
                "p99_ms": round(p99_lat, 2),
                "max_ms": round(max_lat, 2)
            }
        }


def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip


def print_banner(local_ip, port, headless):
    print("\n" + "=" * 70)
    print("     GLIDEPAD HIGH-PRECISION BENCHMARK RECEIVER (v1.1)     ")
    print("=" * 70)
    print(f"  Target Host OS : {platform.system()} ({socket.gethostname()})")
    print(f"  Local IP Addr  : {local_ip}")
    print(f"  UDP Port       : {port}")
    print(f"  Pynput HID     : {'Enabled (Live emulation)' if (PYNPUT_AVAILABLE and not headless) else 'Disabled (Headless Benchmark Mode)'}")
    print("-" * 70)
    print(f"  >>> Enter this IP into GlidePad app: {local_ip}")
    print("  Listening for Live Control, Ping RTT, & High-Speed Stream Tests...")
    print("=" * 70 + "\n")


def print_summary_card(summary):
    print("\n" + "╔" + "═" * 68 + "╗")
    print("║" + "             BENCHMARK TRIAL COMPLETED SUMMARY".center(68) + "║")
    print("╠" + "═" * 68 + "╣")
    print(f"║  Duration         : {summary['duration_sec']} seconds".ljust(69) + "║")
    print(f"║  Packets Received : {summary['packets_received']} pkts".ljust(69) + "║")
    print(f"║  Packets Lost     : {summary['packets_lost']} pkts ({summary['packet_loss_pct']}%)".ljust(69) + "║")
    print(f"║  Out of Order     : {summary['out_of_order']} pkts".ljust(69) + "║")
    print(f"║  Stream Frequency : {summary['throughput_hz']} Hz (Target: 120-240Hz)".ljust(69) + "║")
    print(f"║  Data Bandwidth   : {summary['bandwidth_kb_s']} KB/s".ljust(69) + "║")
    print(f"║  Network Jitter   : {summary['jitter_ms']} ms".ljust(69) + "║")
    print("╟" + "─" * 68 + "╢")
    lat = summary['latency']
    print(f"║  RTT / Transit Latencies:".ljust(69) + "║")
    print(f"║    • Min Latency  : {lat['min_ms']} ms".ljust(69) + "║")
    print(f"║    • Mean Latency : {lat['avg_ms']} ms".ljust(69) + "║")
    print(f"║    • Median (P50) : {lat['p50_ms']} ms".ljust(69) + "║")
    print(f"║    • 95th %tile   : {lat['p95_ms']} ms".ljust(69) + "║")
    print(f"║    • 99th %tile   : {lat['p99_ms']} ms".ljust(69) + "║")
    print(f"║    • Max Latency  : {lat['max_ms']} ms".ljust(69) + "║")
    print("╚" + "═" * 68 + "╝\n")


def main():
    parser = argparse.ArgumentParser(description="GlidePad Wi-Fi Latency & Throughput Benchmark Server (v1.1)")
    parser.add_argument("--port", type=int, default=8089, help="UDP listening port (default: 8089)")
    parser.add_argument("--headless", action="store_true", help="Run without simulating real mouse cursor moves")
    parser.add_argument("--output", type=str, default="", help="Path to save JSON benchmark logs")
    args = parser.parse_args()

    local_ip = get_local_ip()
    print_banner(local_ip, args.port, args.headless)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)
    except Exception:
        pass
    sock.bind(('0.0.0.0', args.port))

    session = BenchmarkSession()
    all_summaries = []

    last_stat_print = time.time()
    packet_rate_window = 0
    recent_rtts = []

    try:
        while True:
            data, addr = sock.recvfrom(4096)
            recv_time_ms = time.time() * 1000.0
            packet_rate_window += 1

            try:
                msg = json.loads(data.decode('utf-8'))
            except Exception:
                continue

            msg_type = msg.get("type", "")

            if msg_type == "benchmark_start":
                session_id = msg.get("session_id", f"trial_{int(time.time())}")
                session = BenchmarkSession(session_id)
                session.start()
                ack = json.dumps({"type": "benchmark_start_ack", "session_id": session_id, "status": "ready"}).encode('utf-8')
                sock.sendto(ack, addr)
                print(f"[BENCHMARK] Trial Started: '{session_id}' from {addr[0]}")

            elif msg_type == "benchmark_packet":
                seq = msg.get("seq", 0)
                client_time = msg.get("time", 0)
                session.record_packet(seq, client_time, len(data))

            elif msg_type == "benchmark_finish":
                summary = session.finish()
                all_summaries.append(summary)
                print_summary_card(summary)

                resp_payload = {
                    "type": "benchmark_summary",
                    "summary": summary
                }
                sock.sendto(json.dumps(resp_payload).encode('utf-8'), addr)

                if args.output:
                    try:
                        with open(args.output, "w") as f:
                            json.dump(all_summaries, f, indent=2)
                        print(f"[BENCHMARK] Saved results to {args.output}")
                    except Exception as e:
                        print(f"[!] Failed to write output file: {e}")

            elif msg_type == "benchmark_ping":
                client_seq = msg.get("seq", 0)
                client_time = msg.get("time", 0)
                send_time_ms = time.time() * 1000.0
                rtt_estimate = max(0.0, send_time_ms - client_time) if client_time > 0 else 0.0
                recent_rtts.append(rtt_estimate)
                if len(recent_rtts) > 50:
                    recent_rtts.pop(0)

                resp = json.dumps({
                    "type": "benchmark_pong",
                    "seq": client_seq,
                    "client_time": client_time,
                    "server_recv_time": recv_time_ms,
                    "server_send_time": send_time_ms
                }).encode('utf-8')
                client_port = msg.get("client_port", addr[1])
                sock.sendto(resp, (addr[0], client_port if client_port else addr[1]))

            elif msg_type in ("ping", "discover"):
                client_port = msg.get("client_port", addr[1])
                resp = json.dumps({
                    "type": "pong" if msg_type == "ping" else "announce",
                    "name": f"{socket.gethostname()} (Benchmark PC)",
                    "benchmark_supported": True,
                    "status": "ok"
                }).encode('utf-8')
                sock.sendto(resp, (addr[0], client_port if client_port else addr[1]))

            elif msg_type == "mouse":
                dx = msg.get("dx", 0)
                dy = msg.get("dy", 0)
                wheel = msg.get("wheel", 0)
                if PYNPUT_AVAILABLE and not args.headless:
                    if dx != 0 or dy != 0:
                        mouse.move(dx, dy)
                    if wheel != 0:
                        mouse.scroll(0, wheel)

            now = time.time()
            if now - last_stat_print >= 2.0:
                elapsed = now - last_stat_print
                hz = packet_rate_window / elapsed
                avg_rtt = (sum(recent_rtts) / len(recent_rtts)) if recent_rtts else 0.0
                sys.stdout.write(f"\r[STATUS] Ingress: {hz:.1f} pkts/s | Avg RTT: {avg_rtt:.1f}ms | Jitter: {session.jitter:.2f}ms   ")
                sys.stdout.flush()
                packet_rate_window = 0
                last_stat_print = now

    except KeyboardInterrupt:
        print("\n\n[*] Benchmarking server stopped by user.")
        if all_summaries:
            print(f"[*] Completed {len(all_summaries)} benchmark trials in this run.")
        sys.exit(0)


if __name__ == "__main__":
    main()
        """.trimIndent()
    }
}
