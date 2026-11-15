package com.example.studypoint

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp

class StudyPointApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply the persisted theme choice (Settings / drawer toggle) to the
        // whole app — DayNight base theme + values-night resources follow it,
        // so View screens (teacher dashboard, Paper Generator, My Papers,
        // dialogs) go dark too. Default is System when nothing is saved.
        when (AppSettings.getThemeMode(this)) {
            AppSettings.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppSettings.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        // Crash recorder: any uncaught crash (including one that happens
        // before any screen loads) is saved to last_crash.txt first, so
        // the NEXT launch can show the exact error instead of guessing.
        // The system handler still runs afterwards, so the usual crash
        // dialog + process kill behaviour is unchanged.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                java.io.File(filesDir, "last_crash.txt").writeText(
                    error.stackTraceToString().take(8000)
                )
            } catch (ignored: Throwable) {
            }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(1)
            }
        }
        try {
            FirebaseApp.initializeApp(this) // ✅ Firebase Init
        } catch (_: Throwable) {
            // Firebase init must never kill the app at startup.
        }
    }
}
