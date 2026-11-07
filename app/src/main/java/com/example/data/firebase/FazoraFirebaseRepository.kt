package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FazoraFirebase"

class FazoraFirebaseRepository(
    private val db: FirebaseFirestore,
    val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: error("FirebaseUser is null after Google sign-in")

            // Sync user profile document
            val userDocRef = db.collection("users").document(user.uid)
            val existing = userDocRef.get().await()
            if (!existing.exists()) {
                val profile = FirestoreUserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Audiophile",
                    email = user.email,
                    createdAt = null,
                    updatedAt = null
                )
                userDocRef.set(profile).await()
            } else {
                userDocRef.update(
                    mapOf(
                        "displayName" to (user.displayName ?: "Audiophile"),
                        "email" to (user.email ?: ""),
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            }

            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Interactive Google sign-in was cancelled by user: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun observeCloudPlaylists(userId: String): Flow<List<FirestoreCloudPlaylist>> {
        return db.collection("users").document(userId)
            .collection("cloud_playlists")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreCloudPlaylist::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    suspend fun saveCloudPlaylist(userId: String, playlistId: String, name: String, trackIds: List<String>) {
        require(auth.currentUser?.uid == userId) { "Operation requires authenticated user" }
        val docRef = db.collection("users").document(userId)
            .collection("cloud_playlists").document(playlistId)

        val existing = docRef.get().await()
        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "name" to name,
                    "trackIds" to trackIds,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
        } else {
            val playlist = FirestoreCloudPlaylist(
                userId = userId,
                name = name,
                trackIds = trackIds,
                createdAt = null,
                updatedAt = null
            )
            docRef.set(playlist).await()
        }
    }

    suspend fun deleteCloudPlaylist(userId: String, playlistId: String) {
        require(auth.currentUser?.uid == userId) { "Operation requires authenticated user" }
        db.collection("users").document(userId)
            .collection("cloud_playlists").document(playlistId)
            .delete()
            .await()
    }

    fun observeCloudEqPresets(userId: String): Flow<List<FirestoreCloudEqPreset>> {
        return db.collection("users").document(userId)
            .collection("cloud_eq_presets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreCloudEqPreset::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    suspend fun saveCloudEqPreset(userId: String, presetId: String, name: String, gainsDb: List<Double>) {
        require(auth.currentUser?.uid == userId) { "Operation requires authenticated user" }
        val docRef = db.collection("users").document(userId)
            .collection("cloud_eq_presets").document(presetId)

        val existing = docRef.get().await()
        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "name" to name,
                    "gainsDb" to gainsDb,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
        } else {
            val preset = FirestoreCloudEqPreset(
                userId = userId,
                name = name,
                gainsDb = gainsDb,
                createdAt = null,
                updatedAt = null
            )
            docRef.set(preset).await()
        }
    }
}
