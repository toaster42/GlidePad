package com.example.model

data class SavedPcProfile(
    val id: String = "",
    val name: String = "",
    val ipAddress: String = "",
    val port: Int = 8889,
    val macAddress: String = "",
    val isDefault: Boolean = false,
    val pointerSensitivity: Double = 1.25,
    val accelerationMultiplier: Double = 1.20,
    val accelerationProfile: String = "DYNAMIC"
) {
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
