package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class NetworkStreamManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var socket: DatagramSocket? = null
    var pcIpAddress: String = "192.168.1.100"
    var pcPort: Int = 8089

    init {
        try {
            socket = DatagramSocket()
        } catch (e: Exception) {
            e.printStackTrace()
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
                // Ignore network dropped packet
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
                // Ignore dropped packet
            }
        }
    }

    private fun sendRaw(data: ByteArray) {
        try {
            val addr = InetAddress.getByName(pcIpAddress)
            val packet = DatagramPacket(data, data.size, addr, pcPort)
            socket?.send(packet)
        } catch (e: Exception) {
            // Silently handle if unreachable
        }
    }

    companion object {
        val PYTHON_RECEIVER_SCRIPT = """
# GlidePad Ultra-Low-Latency PC Receiver
# Requirements: pip install pynput
import socket
import json
from pynput.mouse import Button, Controller as MouseController
from pynput.keyboard import Key, Controller as KeyController

mouse = MouseController()
keyboard = KeyController()

UDP_IP = "0.0.0.0"
UDP_PORT = 8089

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))
print(f"[*] GlidePad Receiver running on port {UDP_PORT}...")

prev_btn = 0

while True:
    data, addr = sock.recvfrom(1024)
    try:
        msg = json.loads(data.decode('utf-8'))
        if msg.get("type") == "mouse":
            dx = msg.get("dx", 0)
            dy = msg.get("dy", 0)
            btn = msg.get("btn", 0)
            wheel = msg.get("wheel", 0)
            if dx != 0 or dy != 0:
                mouse.move(dx, dy)
            if wheel != 0:
                mouse.scroll(0, wheel)
            if btn != prev_btn:
                if (btn & 1) and not (prev_btn & 1): mouse.press(Button.left)
                elif not (btn & 1) and (prev_btn & 1): mouse.release(Button.left)
                if (btn & 2) and not (prev_btn & 2): mouse.press(Button.right)
                elif not (btn & 2) and (prev_btn & 2): mouse.release(Button.right)
                if (btn & 4) and not (prev_btn & 4): mouse.press(Button.middle)
                elif not (btn & 4) and (prev_btn & 4): mouse.release(Button.middle)
                prev_btn = btn
    except Exception as e:
        pass
        """.trimIndent()
    }
}
