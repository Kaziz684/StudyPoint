package com.example.studypoint

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Role storage for the Student / Teacher role system.
 *
 * Source of truth: Firestore document users/{uid} field "user_role".
 * A per-UID SharedPreferences value is kept only as a fast local cache
 * (never the sole authorization source for teacher data — Firestore
 * security rules enforce ownership on teacher collections).
 */
object RoleManager {
    const val ROLE_STUDENT = "student"
    const val ROLE_TEACHER = "teacher"
    private const val PREFS = "role_prefs"

    fun currentUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun isValid(role: String?): Boolean = role == ROLE_STUDENT || role == ROLE_TEACHER

    fun cachedRole(context: Context): String? {
        val uid = currentUid() ?: return null
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("role_$uid", null)
        return if (isValid(v)) v else null
    }

    fun cacheRole(context: Context, role: String) {
        val uid = currentUid() ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("role_$uid", role).apply()
    }

    /** Reads the role from Firestore; falls back to the local cache on failure. */
    fun fetchRole(context: Context, onResult: (String?) -> Unit) {
        val uid = currentUid()
        if (uid == null) {
            onResult(null)
            return
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("user_role")
                if (isValid(role)) {
                    cacheRole(context, role!!)
                    onResult(role)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(cachedRole(context)) }
    }

    /**
     * Saves the role to Firestore (users/{uid}.user_role) and refreshes the
     * local cache. onDone(true) = saved to Firestore; onDone(false) = cached
     * locally only (e.g. offline) and retried on a later launch.
     */
    fun saveRole(context: Context, role: String, onDone: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onDone(false)
            return
        }
        val data = hashMapOf<String, Any>(
            "user_role" to role,
            "email" to (user.email ?: ""),
            "name" to (user.displayName ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                cacheRole(context, role)
                onDone(true)
            }
            .addOnFailureListener {
                cacheRole(context, role)
                onDone(false)
            }
    }
}
