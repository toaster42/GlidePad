package com.example.service

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Firebase Auth init failed", e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val serverClientId = activityContext.getString(R.string.default_web_client_id)

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            val user = authResult.user ?: throw IllegalStateException("Signed in user is null")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in with Google failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(activityContext: Context): Result<Unit> {
        return try {
            auth.signOut()
            val credentialManager = CredentialManager.create(activityContext)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-out failed", e)
            Result.failure(e)
        }
    }
}
