package com.example.studypoint

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

object AuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserSignedIn(): Boolean = getCurrentUser() != null

    fun signOut(context: Context, onComplete: () -> Unit) {
        // Sign out from Firebase
        auth.signOut()

        // Also sign out from Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun getUserDisplayName(): String? = getCurrentUser()?.displayName

    fun getUserEmail(): String? = getCurrentUser()?.email

    fun getUserPhotoUrl(): String? = getCurrentUser()?.photoUrl?.toString()

    fun getAuthProvider(): String? {
        return getCurrentUser()?.providerData?.firstOrNull()?.providerId
    }
}
