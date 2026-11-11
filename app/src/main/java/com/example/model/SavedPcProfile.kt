package com.example.model

/**
 * Saved target host profile containing IP, port, and per-device sensitivity overrides.
 *
 * @property id Firestore document identifier.
 * @property name Friendly device name (e.g., "Living Room PC", "MacBook Pro").
 * @property ipAddress Target host IP address on local network.
 * @property port UDP port on host machine (defaults to `8089`).
 * @property macAddress Optional MAC address for Wake-on-LAN magic packet triggering.
 * @property isDefault If true, automatically loaded as default connection profile on launch.
 * @property pointerSensitivity Per-profile sensitivity scaling override.
 * @property accelerationMultiplier Per-profile acceleration gain multiplier.
 * @property accelerationProfile Profile curve name (`"LINEAR"`, `"DYNAMIC"`, `"PRECISION"`).
 */
data class SavedPcProfile(
    val id: String = "",
    val name: String = "",
    val ipAddress: String = "",
    val port: Int = 8089,
    val macAddress: String = "",
    val isDefault: Boolean = false,
    val pointerSensitivity: Double = 1.25,
    val accelerationMultiplier: Double = 1.20,
    val accelerationProfile: String = "DYNAMIC"
) {
    /**
     * Converts the profile to a Firestore-compatible key-value map.
     */
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "name" to name,
            "ipAddress" to ipAddress,
            "port" to port,
            "macAddress" to macAddress,
            "isDefault" to isDefault,
            "pointerSensitivity" to pointerSensitivity,
            "accelerationMultiplier" to accelerationMultiplier,
            "accelerationProfile" to accelerationProfile
        )
        return map
    }
}
