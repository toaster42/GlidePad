package com.example.model

data class ErrorReport(
    val id: String = "",
    val userId: String = "",
    val errorMessage: String = "",
    val connectionMode: String = "",
    val targetIp: String = "",
    val userComments: String = "",
    val recentLogs: List<String> = emptyList(),
    val appVersion: String = "4.0",
    val osVersion: String = android.os.Build.VERSION.RELEASE ?: "Unknown"
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "errorMessage" to errorMessage,
            "connectionMode" to connectionMode,
            "targetIp" to targetIp,
            "userComments" to userComments,
            "recentLogs" to recentLogs,
            "appVersion" to appVersion,
            "osVersion" to osVersion
        )
        return map
    }
}
