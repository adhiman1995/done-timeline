package com.donetimeline.app.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.donetimeline.app.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    val user: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Shows Google's account picker and signs in to Firebase.
     * [activityContext] must be an Activity. Returns an error message, or null on success or cancel.
     */
    suspend fun signIn(activityContext: Context): String? = withGoogle(activityContext) { credential ->
        auth.signInWithCredential(credential).await()
    }

    /**
     * Confirms with Google (fresh sign-in, as Firebase requires for deletion), then deletes the
     * user's data via [deleteData] and finally the account itself. Returns an error message, or null.
     */
    suspend fun deleteAccount(activityContext: Context, deleteData: suspend (uid: String) -> Unit): String? {
        val user = auth.currentUser ?: return null
        var confirmed = false
        val error = withGoogle(activityContext) { credential ->
            user.reauthenticate(credential).await()
            confirmed = true
            deleteData(user.uid)
            user.delete().await()
        }
        if (error == null && !confirmed) return "Account deletion was cancelled."
        if (error == null) runCatching { CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest()) }
        return error
    }

    /** Shows Google's account picker, then runs [block] with a Firebase credential. Returns an error message, or null. */
    private suspend fun withGoogle(activityContext: Context, block: suspend (AuthCredential) -> Unit): String? {
        val option = GetSignInWithGoogleOption.Builder(activityContext.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return "Google sign-in returned an unexpected response."
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            block(GoogleAuthProvider.getCredential(idToken, null))
            null
        } catch (e: GetCredentialCancellationException) {
            null
        } catch (e: NoCredentialException) {
            "No Google account is available. Add one in the phone's Settings → Accounts."
        } catch (e: GetCredentialException) {
            if (e.message.orEmpty().contains("28444") || e.message.orEmpty().contains("not set up correctly", ignoreCase = true)) {
                "Google sign-in isn't set up for this build. Add the app's SHA-1 fingerprint in Firebase project settings."
            } else {
                e.message ?: "Google sign-in failed."
            }
        } catch (e: Exception) {
            e.message ?: "Sign-in failed."
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
