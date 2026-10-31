package com.example.model

data class UserFeedback(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "FEATURE",
    val appVersion: String = "5.0",
    val userEmail: String = ""
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "category" to category,
            "appVersion" to appVersion,
            "userEmail" to userEmail
        )
        return map
    }
}
