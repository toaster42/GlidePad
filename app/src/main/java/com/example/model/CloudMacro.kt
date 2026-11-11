package com.example.model

/**
 * Cloud-synchronized macro item persisted to Firestore.
 *
 * Encapsulates a customizable action button or sequence (e.g. keyboard shortcuts, system commands)
 * that can be triggered from the quick macros deck.
 *
 * @property id Unique Firestore document identifier.
 * @property label Short display title shown on the macro button.
 * @property description Human-readable explanation of what the macro triggers.
 * @property shortcutKeys List of keyboard keys executed in sequence (e.g., `["Ctrl", "C"]`).
 * @property actionType The execution category (e.g., `"KEYBOARD"`, `"SHELL"`, `"MEDIA"`).
 * @property targetPayload Optional secondary parameter or text payload.
 * @property sortOrder Integer priority for ordering macro cards in the UI grid.
 */
data class CloudMacro(
    val id: String = "",
    val label: String = "",
    val description: String = "",
    val shortcutKeys: List<String> = emptyList(),
    val actionType: String = "KEYBOARD",
    val targetPayload: String = "",
    val sortOrder: Int = 0
) {
    /**
     * Converts the [CloudMacro] instance into a serializable Map for Firestore persistence.
     *
     * @return Map of document field names to values.
     */
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
