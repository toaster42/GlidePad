package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.model.SavedPcProfile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PcProfilesRepository(private val context: Context) {

    private val firestore by lazy { FirestoreProvider.get(context) }

    fun observeProfiles(userId: String): Flow<List<SavedPcProfile>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users").document(userId).collection("pc_profiles")

        val listener: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("PcProfilesRepo", "Listen failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val profiles = snapshot.documents.mapNotNull { doc ->
                    try {
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val ipAddress = doc.getString("ipAddress") ?: return@mapNotNull null
                        val port = doc.getLong("port")?.toInt() ?: 8889
                        val macAddress = doc.getString("macAddress") ?: ""
                        val isDefault = doc.getBoolean("isDefault") ?: false
                        val pointerSensitivity = doc.getDouble("pointerSensitivity") ?: 1.25
                        val accelerationMultiplier = doc.getDouble("accelerationMultiplier") ?: 1.20
                        val accelerationProfile = doc.getString("accelerationProfile") ?: "DYNAMIC"

                        SavedPcProfile(
                            id = doc.id,
                            name = name,
                            ipAddress = ipAddress,
                            port = port,
                            macAddress = macAddress,
                            isDefault = isDefault,
                            pointerSensitivity = pointerSensitivity,
                            accelerationMultiplier = accelerationMultiplier,
                            accelerationProfile = accelerationProfile
                        )
                    } catch (e: Exception) {
                        Log.e("PcProfilesRepo", "Error parsing profile", e)
                        null
                    }
                }
                trySend(profiles)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun saveProfile(userId: String, profile: SavedPcProfile): Result<String> {
        return try {
            val profilesCol = firestore.collection("users").document(userId).collection("pc_profiles")
            val docRef = if (profile.id.isNotBlank()) {
                profilesCol.document(profile.id)
            } else {
                profilesCol.document()
            }
            docRef.set(profile.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("PcProfilesRepo", "Failed to save profile", e)
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(userId: String, profileId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("pc_profiles").document(profileId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PcProfilesRepo", "Failed to delete profile", e)
            Result.failure(e)
        }
    }
}
