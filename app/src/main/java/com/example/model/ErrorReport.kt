package com.example.model

/**
 * Diagnostic crash and connection error report submitted to Firestore for debugging.
 *
 * @property id Unique Firestore document identifier.
 * @property userId Firebase Authentication UID of the reporting user.
 * @property errorMessage Primary exception message or connection error string.
 * @property connectionMode Transport mode active when error occurred.
 * @property targetIp Target IP address being accessed.
 * @property userComments Optional comments entered by user describing reproduction steps.
 * @property recentLogs Recent rolling log entries captured leading up to the failure.
 * @property appVersion App version code/name string.
 * @property osVersion Android OS release version on the client device.
 */
data class ErrorReport(
    val id: String = "",
    val userId: String = "",
    val errorMessage: String = "",
    val connectionMode: String = "",
    val targetIp: String = "",
    val userComments: String = "",
    val recentLogs: List<String> = emptyList(),
    val appVersion: String = "5.0",
    val osVersion: String = android.os.Build.VERSION.RELEASE ?: "Unknown"
) {
    /**
     * Converts the [ErrorReport] into a Firestore-serializable map.
     */
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
