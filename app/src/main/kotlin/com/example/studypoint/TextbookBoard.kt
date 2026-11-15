package com.example.studypoint

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Textbook-board choice (first-run "Select Your Board" dialog).
 *
 * This is the TEXTBOOK board the student's content comes from. It is
 * SEPARATE from the Paper Generator's board-pattern paper type
 * (PaperType.BOARD — a fixed BISE-structure paper), which is untouched.
 *
 * Content today is Punjab PCTB only: Punjab shows the full app as normal;
 * Federal, Sindh and KP show a "Coming Soon" state and never Punjab
 * content under another board's name.
 */
object TextbookBoard {

    const val PUNJAB = "punjab"
    const val FEDERAL = "federal"
    const val SINDH = "sindh"
    const val KP = "kp"

    val ALL: List<String> = listOf(PUNJAB, FEDERAL, SINDH, KP)

    private const val PREFS = "textbook_board"
    private const val KEY_BOARD = "textbook_board"

    fun displayName(board: String): String = when (board) {
        FEDERAL -> "Federal Board (FBISE)"
        SINDH -> "Sindh Text Book Board"
        KP -> "KP Text Book Board"
        else -> "Punjab Text Book Board"
    }

    /** Saved board, or Punjab when nothing (or a stale value) is saved. */
    fun getBoard(context: Context): String {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BOARD, null)
        return if (saved in ALL) saved!! else PUNJAB
    }

    fun isBoardSaved(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_BOARD)

    fun setBoard(context: Context, board: String) {
        val value = if (board in ALL) board else PUNJAB
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BOARD, value).apply()
    }

    fun isPunjab(context: Context): Boolean = getBoard(context) == PUNJAB

    /**
     * "Select Your Board" dialog: simple list, Punjab marked Default, with a
     * Cancel option. [onChosen] fires only for a real tap on a board;
     * [onCancel] fires for Cancel or back-press (exactly once).
     */
    fun showBoardPicker(
        activity: Activity,
        onChosen: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        var handled = false
        val names = ALL.map { displayName(it) + if (it == PUNJAB) " (Default)" else "" }.toTypedArray()
        val current = ALL.indexOf(getBoard(activity)).coerceAtLeast(0)
        AlertDialog.Builder(activity)
            .setTitle("Select Your Board")
            .setSingleChoiceItems(names, current) { dialog, which ->
                if (handled) return@setSingleChoiceItems
                handled = true
                setBoard(activity, ALL[which])
                dialog.dismiss()
                onChosen(ALL[which])
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (handled) return@setNegativeButton
                handled = true
                onCancel()
            }
            .setCancelable(true)
            .setOnCancelListener {
                if (handled) return@setOnCancelListener
                handled = true
                onCancel()
            }
            .show()
    }

    /** "Coming Soon" state for the non-Punjab boards. [onDismiss] runs on OK. */
    fun showComingSoon(activity: Activity, onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(activity)
            .setTitle("Coming Soon")
            .setMessage(
                "Content for ${displayName(getBoard(activity))} is coming soon.\n\n" +
                    "StudyPoint currently offers the complete Punjab Text Book Board " +
                    "syllabus (Classes 9–12). Switch to Punjab Text Book Board to continue."
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            .setCancelable(false)
            .show()
    }
}
