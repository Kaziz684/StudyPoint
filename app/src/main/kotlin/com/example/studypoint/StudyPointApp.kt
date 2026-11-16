package com.example.studypoint

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp

class StudyPointApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Dark mode is REMOVED (v2.0-125): the app is always simple Light,
        // like before v2.0-123. Force MODE_NIGHT_NO and reset any saved
        // Dark/System choice to Light, so values-night resources are never
        // applied and no screen can go dark.
        AppSettings.setThemeMode(this, AppSettings.THEME_LIGHT)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
