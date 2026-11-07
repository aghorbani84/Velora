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
import com.google.firebase.FirebaseApp
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "VeloraFirebase"

private fun initSafeFirestore(context: Context): FirebaseFirestore? = kotlin.runCatching {
    val appContext = context.applicationContext ?: context
    if (FirebaseApp.getApps(appContext).isEmpty()) {
        FirebaseApp.initializeApp(appContext)
    }
    if (FirebaseApp.getApps(appContext).isNotEmpty()) {
        FirebaseFirestore.getInstance(
            appContext.getString(R.string.firestore_database_id)
        )
    } else {
        null
    }
}.onFailure { err ->
    Log.w(TAG, "Firestore unavailable on this device; continuing in local-first mode: ${err.message}")
}.getOrNull()

private fun initSafeAuth(context: Context): FirebaseAuth? = kotlin.runCatching {
    val appContext = context.applicationContext ?: context
    if (FirebaseApp.getApps(appContext).isEmpty()) {
        FirebaseApp.initializeApp(appContext)
    }
    if (FirebaseApp.getApps(appContext).isNotEmpty()) {
        Firebase.auth
    } else {
        null
    }
}.onFailure { err ->
    Log.w(TAG, "FirebaseAuth unavailable on this device; continuing in local-first mode: ${err.message}")
}.getOrNull()

class VeloraFirebaseRepository(
    private val db: FirebaseFirestore? = null,
    val auth: FirebaseAuth? = null
) {
    constructor(context: Context) : this(
        db = initSafeFirestore(context),
        auth = initSafeAuth(context)
    )

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser)
        }
        runCatching {
            firebaseAuth.addAuthStateListener(listener)
        }.onFailure {
            trySend(null)
        }
        awaitClose {
            runCatching { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(
            IllegalStateException("Cloud services are not available on this device.")
        )
        val firestore = db
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
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: error("FirebaseUser is null after Google sign-in")

            if (firestore != null) {
                val userDocRef = firestore.collection("users").document(user.uid)
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
        runCatching { auth?.signOut() }
    }

    fun observeCloudPlaylists(userId: String): Flow<List<FirestoreCloudPlaylist>> {
        val firestore = db ?: return flowOf(emptyList())
        return firestore.collection("users").document(userId)
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
        require(auth?.currentUser?.uid == userId) { "Operation requires authenticated user" }
        val firestore = db ?: return
        val docRef = firestore.collection("users").document(userId)
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
        require(auth?.currentUser?.uid == userId) { "Operation requires authenticated user" }
        val firestore = db ?: return
        firestore.collection("users").document(userId)
            .collection("cloud_playlists").document(playlistId)
            .delete()
            .await()
    }

    fun observeCloudEqPresets(userId: String): Flow<List<FirestoreCloudEqPreset>> {
        val firestore = db ?: return flowOf(emptyList())
        return firestore.collection("users").document(userId)
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
        require(auth?.currentUser?.uid == userId) { "Operation requires authenticated user" }
        val firestore = db ?: return
        val docRef = firestore.collection("users").document(userId)
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
