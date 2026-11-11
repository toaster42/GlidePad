package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.model.CloudMacro
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository managing real-time Firestore synchronization for user-defined macro decks.
 *
 * Persists and observes documents in the path `users/{userId}/macros`.
 *
 * @param context Context used to resolve Firestore instance.
 */
class CloudMacrosRepository(private val context: Context) {

    private val firestore by lazy { FirestoreProvider.get(context) }

    /**
     * Emits a real-time reactive [Flow] of macros configured for the authenticated [userId].
     *
     * @param userId Authenticated user UID.
     * @return Hot flow of macro items ordered by [CloudMacro.sortOrder].
     */
    fun observeMacros(userId: String): Flow<List<CloudMacro>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users").document(userId).collection("macros")
            .orderBy("sortOrder")

        val listener: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("CloudMacrosRepo", "Listen failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val macros = snapshot.documents.mapNotNull { doc ->
                    try {
                        val label = doc.getString("label") ?: return@mapNotNull null
                        val description = doc.getString("description") ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val shortcutKeys = doc.get("shortcutKeys") as? List<String> ?: emptyList()
                        val actionType = doc.getString("actionType") ?: "KEYBOARD"
                        val targetPayload = doc.getString("targetPayload") ?: ""
                        val sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0

                        CloudMacro(
                            id = doc.id,
                            label = label,
                            description = description,
                            shortcutKeys = shortcutKeys,
                            actionType = actionType,
                            targetPayload = targetPayload,
                            sortOrder = sortOrder
                        )
                    } catch (e: Exception) {
                        Log.e("CloudMacrosRepo", "Error parsing macro", e)
                        null
                    }
                }
                trySend(macros)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Creates or updates a [CloudMacro] in the user's private collection.
     *
     * @param userId Authenticated user UID.
     * @param macro Macro record to persist.
     * @return [Result] containing document ID upon success.
     */
    suspend fun saveMacro(userId: String, macro: CloudMacro): Result<String> {
        return try {
            val userMacros = firestore.collection("users").document(userId).collection("macros")
            val docRef = if (macro.id.isNotBlank()) {
                userMacros.document(macro.id)
            } else {
                userMacros.document()
            }
            docRef.set(macro.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("CloudMacrosRepo", "Failed to save macro", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a macro document from Firestore.
     *
     * @param userId Authenticated user UID.
     * @param macroId Document ID of the macro to delete.
     */
    suspend fun deleteMacro(userId: String, macroId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("macros").document(macroId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CloudMacrosRepo", "Failed to delete macro", e)
            Result.failure(e)
        }
    }
}
