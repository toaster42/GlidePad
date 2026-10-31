package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.model.ErrorReport
import kotlinx.coroutines.tasks.await

class ErrorReportsRepository(private val context: Context) {

    private val firestore by lazy { FirestoreProvider.get(context) }

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
