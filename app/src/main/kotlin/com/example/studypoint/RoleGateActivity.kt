package com.example.studypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studypoint.teacher.TeacherDashboardActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Post-login router: sends a signed-in user to the correct dashboard
 * based on the saved role, or to RoleSelectionActivity when no role
 * has been chosen yet.
 */
class RoleGateActivity : AppCompatActivity() {

    // Guards the one-shot board dialog: a second callback (fetchRole) must
    // never re-route while the dialog is open, and the dialog must never
    // be shown twice.
    private var routed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_gate)

        // The router must never be the crash: any failure here falls
        // back to Login instead of a "keeps stopping" dialog.
        try {
            if (FirebaseAuth.getInstance().currentUser == null) {
                goTo(LoginActivity::class.java)
                return
            }
            val cached = RoleManager.cachedRole(this)
            if (cached != null) {
                routeWithBoardCheck(cached)
                // Keep the cache fresh in the background.
                RoleManager.fetchRole(this) { }
                return
            }
            RoleManager.fetchRole(this) { role ->
                if (role != null) {
                    routeWithBoardCheck(role)
                } else {
                    goTo(RoleSelectionActivity::class.java)
                }
            }
        } catch (_: Throwable) {
            try {
                goTo(LoginActivity::class.java)
            } catch (ignored: Throwable) {
            }
        }
    }

    /**
     * First app run only: if no textbook board is saved yet, ask once
     * ("Select Your Board") before routing. Cancel keeps Punjab for this
     * session without saving, so the dialog asks again next launch.
     */
    private fun routeWithBoardCheck(role: String) {
        if (routed) return
        if (TextbookBoard.isBoardSaved(this)) {
            routed = true
            route(role)
            return
        }
        try {
            TextbookBoard.showBoardPicker(
                this,
                onChosen = {
                    if (!routed) {
                        routed = true
                        route(role)
                    }
                },
                onCancel = {
                    if (!routed) {
                        routed = true
                        route(role)
                    }
                }
            )
        } catch (_: Throwable) {
            if (!routed) {
                routed = true
                route(role)
            }
        }
    }

    private fun route(role: String) {
        if (role == RoleManager.ROLE_TEACHER) {
            goTo(TeacherDashboardActivity::class.java)
        } else {
            goTo(HomeActivity::class.java)
        }
    }

    private fun goTo(target: Class<*>) {
        startActivity(
            Intent(this, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
