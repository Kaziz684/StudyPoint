package com.example.studypoint.teacher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.example.studypoint.practicetest.QuestionBankStats
import kotlin.math.min

/**
 * Simple donut chart for the teacher dashboard: MCQs vs Short vs Long
 * questions. Hand-drawn on purpose — the app has no chart library and a
 * three-slice donut does not justify a new dependency.
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var mcqs: Int = 0
        private set
    var shorts: Int = 0
        private set
    var longs: Int = 0
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textAlign = Paint.Align.CENTER
    }

    private val colors = intArrayOf(
        Color.parseColor("#2563EB"), // MCQs — blue
        Color.parseColor("#10B981"), // Short — green
        Color.parseColor("#F59E0B") // Long — amber
    )

    fun setCounts(mcqs: Int, shorts: Int, longs: Int) {
        this.mcqs = mcqs
        this.shorts = shorts
        this.longs = longs
        invalidate()
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        val fallback = dp(160f).toInt()
        val resolved = if (size > 0) size else fallback
        setMeasuredDimension(resolved, resolved)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val stroke = dp(26f)
        val radius = (min(width, height) / 2f) - stroke / 2f - dp(2f)
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val total = mcqs + shorts + longs
        paint.strokeWidth = stroke
        if (total <= 0) {
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawArc(oval, 0f, 360f, false, paint)
        } else {
            val values = intArrayOf(mcqs, shorts, longs)
            var start = -90f
            for (i in values.indices) {
                if (values[i] <= 0) continue
                val sweep = values[i] * 360f / total
                paint.color = colors[i]
                canvas.drawArc(oval, start, sweep, false, paint)
                start += sweep
            }
        }
        textPaint.textSize = sp(19f)
        canvas.drawText(
            QuestionBankStats.formatCount(total),
            cx,
            cy + sp(2f),
            textPaint
        )
        labelPaint.textSize = sp(11f)
        canvas.drawText("Questions", cx, cy + sp(17f), labelPaint)
    }
}
