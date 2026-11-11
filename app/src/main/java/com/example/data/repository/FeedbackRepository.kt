package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.model.UserFeedback
import kotlinx.coroutines.tasks.await

/**
 * Repository submitting user feedback and feature requests to the public `feedback` Firestore collection.
 *
 * @param context Context used to resolve the database client.
 */
class FeedbackRepository(private val context: Context) {

    private val firestore by lazy { FirestoreProvider.get(context) }

    /**
     * Submits a [UserFeedback] entry to Firestore.
     *
     * @param feedback The [UserFeedback] record to persist.
     * @return [Result] containing generated document ID on success.
     */
    suspend fun submitFeedback(feedback: UserFeedback): Result<String> {
        return try {
            val docRef = firestore.collection("feedback").document()
            docRef.set(feedback.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FeedbackRepository", "Failed to submit feedback", e)
            Result.failure(e)
        }
    }
}
