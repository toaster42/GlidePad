package com.example.model

enum class ConnectionMode(val title: String) {
    BLUETOOTH_HID("Bluetooth HID (Direct)"),
    LOCAL_WIFI("Wi-Fi / LAN UDP"),
    SIMULATION("In-App Simulator")
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    PAIRING("Pairing / Searching"),
    CONNECTED("Connected (Low-Latency)"),
    STREAMING("Streaming Packets")
}

data class DiagnosticStats(
    val packetsSent: Long = 0,
    val lastLatencyMs: Long = 3,
    val currentHz: Int = 120,
    val connectionMode: ConnectionMode = ConnectionMode.BLUETOOTH_HID,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectedDeviceName: String = "No PC Connected",
    val recentLogs: List<String> = emptyList()
)

data class MacroItem(
    val id: String,
    val label: String,
    val description: String,
    val shortcutKeys: List<String>,
    val gestureAction: GestureAction? = null
)
