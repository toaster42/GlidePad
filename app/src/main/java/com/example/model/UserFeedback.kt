package com.example.model

/**
 * User-submitted feedback item or feature request stored in Firestore.
 *
 * @property id Unique Firestore document identifier.
 * @property userId Firebase Authentication UID of the submitter.
 * @property title Short summary of the feedback or feature request.
 * @property description In-depth description of user suggestions or issues.
 * @property category Categorization string (e.g. `"FEATURE"`, `"BUG"`, `"USABILITY"`).
 * @property appVersion App release version string when feedback was submitted.
 * @property userEmail Contact email address of the user.
 */
data class UserFeedback(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "FEATURE",
    val appVersion: String = "5.0",
    val userEmail: String = ""
) {
    /**
     * Converts the [UserFeedback] model into a Firestore document map.
     */
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
