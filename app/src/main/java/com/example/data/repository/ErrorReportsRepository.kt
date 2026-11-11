package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.model.ErrorReport
import kotlinx.coroutines.tasks.await

/**
 * Repository submitting diagnostic crash logs and network failure reports to the `reports` Firestore collection.
 *
 * @param context Context used to resolve the Firestore database client.
 */
class ErrorReportsRepository(private val context: Context) {

    private val firestore by lazy { FirestoreProvider.get(context) }

    /**
     * Submits an [ErrorReport] to Firestore.
     *
     * @param report [ErrorReport] record to upload.
     * @return [Result] containing generated report document ID.
     */
    suspend fun submitErrorReport(report: ErrorReport): Result<String> {
        return try {
            val docRef = firestore.collection("reports").document()
            docRef.set(report.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("ErrorReportsRepository", "Failed to submit error report", e)
            Result.failure(e)
        }
    }
}
