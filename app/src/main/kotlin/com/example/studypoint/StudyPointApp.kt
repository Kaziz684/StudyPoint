package com.example.studypoint

import android.app.Application
import com.google.firebase.FirebaseApp

class StudyPointApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // ✅ Firebase Init
    }
}
