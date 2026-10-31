package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.model.UserFeedback
import kotlinx.coroutines.tasks.await

class FeedbackRepository(private val context: Context) {

    private val firestore by lazy { FirestoreProvider.get(context) }

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
