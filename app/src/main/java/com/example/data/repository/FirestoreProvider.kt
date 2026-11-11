package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton provider resolving configured Cloud Firestore database instances.
 */
object FirestoreProvider {
    /**
     * Resolves the [FirebaseFirestore] database instance configured for the application.
     *
     * @param context Context used to retrieve database resource IDs.
     * @return Configured [FirebaseFirestore] database client.
     */
    fun get(context: Context): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        val app = FirebaseApp.getInstance()
        return FirebaseFirestore.getInstance(app, dbId)
    }
}
