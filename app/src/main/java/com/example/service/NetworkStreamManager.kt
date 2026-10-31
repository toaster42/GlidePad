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

    var pcIpAddress: String = "192.168.1.100"
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

    private var pingStartTime: Long = 0
    private var lastPongReceivedTime: Long = 0

    init {
        initSockets()
    }

    @Synchronized
    private fun initSockets() {
        try {
            if (sendSocket == null || sendSocket?.isClosed == true) {
                sendSocket = DatagramSocket().apply {
                    broadcast = true
                }
            }
            startReceiver()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize UDP socket", e)
        }
    }

    private fun startReceiver() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            try {
                if (receiveSocket == null || receiveSocket?.isClosed == true) {
                    receiveSocket = DatagramSocket().apply {
                        soTimeout = 3000
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
                        // Keep receiver alive
                    } catch (e: Exception) {
                        if (!isActive) break
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Receiver exception", e)
            }
        }
    }

    private fun handleIncomingPacket(rawJson: String, senderIp: String) {
        try {
            val json = JSONObject(rawJson)
            when (json.optString("type")) {
                "pong" -> {
                    val now = System.currentTimeMillis()
                    val rtt = if (pingStartTime > 0) (now - pingStartTime).coerceAtLeast(1) else 3L
                    lastPongReceivedTime = now
                    isPcOnline.value = true
                    liveLatencyMs.value = rtt

                    val server = json.optString("name", "GlidePad PC")
                    _testState.value = WifiConnectionTestState.Success(
                        latencyMs = rtt,
                        serverName = server,
                        ip = senderIp
                    )
                    _logCallback.value = "[WIFI PONG] $server ($senderIp) • ${rtt}ms"
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
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse incoming packet: $rawJson")
        }
    }

    fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    pingStartTime = System.currentTimeMillis()
                    val clientPort = receiveSocket?.localPort ?: 0
                    val pingJson = JSONObject().apply {
                        put("type", "ping")
                        put("client_port", clientPort)
                        put("time", pingStartTime)
                    }
                    sendRaw(pingJson.toString().toByteArray())

                    // If no pong for >6 seconds, mark offline
                    if (lastPongReceivedTime > 0 && (System.currentTimeMillis() - lastPongReceivedTime) > 6000) {
                        isPcOnline.value = false
                    }
                } catch (e: Exception) {
                    // Ignore transient network hiccups
                }
                delay(3000)
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
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "mouse")
                    put("dx", dx)
                    put("dy", dy)
                    put("btn", buttons.toInt())
                    put("wheel", wheel)
                }
                sendRaw(json.toString().toByteArray())
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

    private fun sendRaw(data: ByteArray, targetIp: String = pcIpAddress) {
        try {
            if (sendSocket == null || sendSocket?.isClosed == true) {
                initSockets()
            }
            val addr = InetAddress.getByName(targetIp)
            val packet = DatagramPacket(data, data.size, addr, pcPort)
            sendSocket?.send(packet)
        } catch (e: Exception) {
            _logCallback.value = "[UDP ERR] To $targetIp:$pcPort: ${e.message}"
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
        val PYTHON_RECEIVER_SCRIPT = """
# =====================================================================
#  GlidePad Ultra-Low-Latency PC Receiver (v2.1 - Wi-Fi Power Edition)
# =====================================================================
#  Requirements:
#    pip install pynput
#
#  Usage:
#    python glidepad_receiver.py
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
sock.bind(('0.0.0.0', UDP_PORT))

print("=" * 64)
print("  GLIDEPAD PC RECEIVER ACTIVE (v2.1)")
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
    }
}
