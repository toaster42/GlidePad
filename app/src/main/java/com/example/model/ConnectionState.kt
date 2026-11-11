package com.example.model

/**
 * Transport medium used to communicate with the host computer.
 *
 * @property title Human-readable label for UI selection and status headers.
 */
enum class ConnectionMode(val title: String) {
    /** Native Bluetooth Human Interface Device profile for driverless direct pairing. */
    BLUETOOTH_HID("Bluetooth HID (Direct)"),

    /** Low-latency UDP datagram streaming over local Wi-Fi / Ethernet LAN. */
    LOCAL_WIFI("Wi-Fi / LAN UDP"),

    /** Internal loopback simulator for headless testing without an external host. */
    SIMULATION("In-App Simulator")
}

/**
 * Real-time operational connection state between GlidePad and the target PC.
 *
 * @property label User-facing badge label.
 */
enum class ConnectionStatus(val label: String) {
    /** No socket or Bluetooth connection active. */
    DISCONNECTED("Disconnected"),

    /** Discovery broadcast or Bluetooth scan in progress. */
    PAIRING("Pairing / Searching"),

    /** Connected and verified via heartbeat ping/pong. */
    CONNECTED("Connected (Low-Latency)"),

    /** Active real-time gesture or keyboard data stream underway. */
    STREAMING("Streaming Packets")
}

/**
 * Diagnostic and telemetry metrics snapshot for connection health monitoring.
 *
 * @property packetsSent Total number of mouse/keyboard packets dispatched during session.
 * @property lastLatencyMs Most recent round-trip latency measurement in milliseconds.
 * @property currentHz Real-time packet dispatch frequency (packets per second).
 * @property connectionMode Active transport channel.
 * @property connectionStatus Current connection lifecycle phase.
 * @property connectedDeviceName Resolved hostname or IP of the paired machine.
 * @property recentLogs Circular buffer of recent connection state transitions and events.
 */
data class DiagnosticStats(
    val packetsSent: Long = 0,
    val lastLatencyMs: Long = 3,
    val currentHz: Int = 120,
    val connectionMode: ConnectionMode = ConnectionMode.BLUETOOTH_HID,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectedDeviceName: String = "No PC Connected",
    val recentLogs: List<String> = emptyList()
)

/**
 * Local or pre-bundled quick macro button item.
 *
 * @property id Identifier for the macro item.
 * @property label Button label text.
 * @property description Subtitle describing the action.
 * @property shortcutKeys List of keyboard keys executed when pressed.
 * @property gestureAction Optional mapped gesture trigger.
 */
data class MacroItem(
    val id: String,
    val label: String,
    val description: String,
    val shortcutKeys: List<String>,
    val gestureAction: GestureAction? = null
)
