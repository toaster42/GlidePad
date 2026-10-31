package com.example.model

data class CloudMacro(
    val id: String = "",
    val label: String = "",
    val description: String = "",
    val shortcutKeys: List<String> = emptyList(),
    val actionType: String = "KEYBOARD",
    val targetPayload: String = "",
    val sortOrder: Int = 0
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "label" to label,
            "description" to description,
            "shortcutKeys" to shortcutKeys,
            "actionType" to actionType,
            "targetPayload" to targetPayload,
            "sortOrder" to sortOrder
        )
        return map
    }
}
