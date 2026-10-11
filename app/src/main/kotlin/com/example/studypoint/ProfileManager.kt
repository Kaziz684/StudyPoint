package com.example.studypoint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class UserProfile(
    val fullName: String = "Muhammad Ahmed",
    val age: String = "16",
    val schoolCollege: String = "Govt Higher Secondary School, Bahawalpur",
    val grade: String = "Class 10th",
    val imagePath: String = ""
)

object ProfileManager {
    private const val PREFS_NAME = "studypoint_user_profile"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_AGE = "age"
    private const val KEY_SCHOOL = "school_college"
    private const val KEY_GRADE = "grade"
    private const val KEY_IMAGE_PATH = "profile_image_path"
    private const val TAG = "ProfileManager"

    fun getProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultName = FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "Muhammad Ahmed"

        val fullName = prefs.getString(KEY_FULL_NAME, defaultName) ?: defaultName
        val age = prefs.getString(KEY_AGE, "16") ?: "16"
        val school = prefs.getString(KEY_SCHOOL, "Govt Comprehensive High School, Bahawalpur") ?: "Govt Comprehensive High School, Bahawalpur"
        val grade = prefs.getString(KEY_GRADE, "Class 10th") ?: "Class 10th"
        val imagePath = prefs.getString(KEY_IMAGE_PATH, "") ?: ""

        return UserProfile(
            fullName = fullName,
            age = age,
            schoolCollege = school,
            grade = grade,
            imagePath = imagePath
        )
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_FULL_NAME, profile.fullName.trim())
            putString(KEY_AGE, profile.age.trim())
            putString(KEY_SCHOOL, profile.schoolCollege.trim())
            putString(KEY_GRADE, profile.grade.trim())
            putString(KEY_IMAGE_PATH, profile.imagePath)
            apply()
        }
        Log.d(TAG, "User profile saved successfully: ${profile.fullName}, ${profile.grade}")
    }

    /**
     * Copies an image from a content Uri into local app files directory
     * so that the profile image permanently persists without permission expiration.
     */
    fun saveProfileImageFromUri(context: Context, uri: Uri): String {
        return try {
            val destination = File(context.filesDir, "profile_avatar.jpg")
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destination)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile image from Uri: ${e.message}", e)
            ""
        }
    }

    /**
     * Saves a camera thumbnail bitmap directly into local app files directory
     */
    fun saveProfileImageBitmap(context: Context, bitmap: Bitmap): String {
        return try {
            val destination = File(context.filesDir, "profile_avatar.jpg")
            val outputStream = FileOutputStream(destination)
            outputStream.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            destination.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile image from Bitmap: ${e.message}", e)
            ""
        }
    }
}
