package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {
    fun get(context: Context): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        val app = FirebaseApp.getInstance()
        return FirebaseFirestore.getInstance(app, dbId)
    }
}
