package com.example.studypoint.teacher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Small programmatic-UI helpers shared by the teacher screens. */
object TeacherUi {

    fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()

    fun divider(context: Context): View {
        val v = View(context)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)
        )
        v.setBackgroundColor(Color.parseColor("#E2E8F0"))
        return v
    }

    fun text(
        context: Context,
        value: String,
        sizeSp: Float,
        colorHex: String,
        bold: Boolean = false
    ): TextView {
        val t = TextView(context)
        t.text = value
        t.textSize = sizeSp
        t.setTextColor(Color.parseColor(colorHex))
        if (bold) t.setTypeface(null, Typeface.BOLD)
        t.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        return t
    }

    fun avatar(context: Context, name: String): TextView {
        val t = TextView(context)
        t.text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        t.textSize = 17f
        t.setTextColor(Color.WHITE)
        t.setTypeface(null, Typeface.BOLD)
        t.gravity = Gravity.CENTER
        val size = dp(context, 44)
        t.layoutParams = LinearLayout.LayoutParams(size, size)
        val bg = android.graphics.drawable.GradientDrawable()
        bg.shape = android.graphics.drawable.GradientDrawable.OVAL
        bg.setColor(Color.parseColor("#2563EB"))
        t.background = bg
        return t
    }

    fun fmtMarks(v: Double?): String {
        if (v == null) return ""
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}
