package com.example.studypoint.practicetest

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.studypoint.HomeActivity
import com.example.studypoint.R
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuizResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 10)
        val correct = intent.getIntExtra("correct", 0)
        val incorrect = intent.getIntExtra("incorrect", 0)

        val percentage = if (total > 0) (score * 100) / total else 0

        findViewById<TextView>(R.id.tvScorePercentage).text = "$percentage%"
        findViewById<TextView>(R.id.tvScoreFraction).text = "Score: $score / $total"
        findViewById<TextView>(R.id.tvCorrectCount).text = correct.toString()
        findViewById<TextView>(R.id.tvIncorrectCount).text = incorrect.toString()

        // Review sheet: every MCQ with the correct answer and the
        // student's pick (green ✓ = correct, red ✗ = their wrong pick).
        val reviewJson = intent.getStringExtra("review_json")
        if (!reviewJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<QuizReviewItem>>() {}.type
                val review: List<QuizReviewItem> = Gson().fromJson(reviewJson, type)
                renderReview(review)
            } catch (_: Exception) {
                findViewById<TextView>(R.id.tvReviewTitle).visibility = View.GONE
            }
        } else {
            findViewById<TextView>(R.id.tvReviewTitle).visibility = View.GONE
        }

        val tvFeedbackTitle = findViewById<TextView>(R.id.tvFeedbackTitle)
        val tvFeedbackMessage = findViewById<TextView>(R.id.tvFeedbackMessage)

        when {
            percentage == 100 -> {
                tvFeedbackTitle.text = "Outstanding Performance! 🎉"
                tvFeedbackMessage.text = "Excellent / Congratulations! You nailed every single question!"
                tvFeedbackTitle.setTextColor(ContextCompat.getColor(this, R.color.edu_emerald))
            }
            percentage >= 80 -> {
                tvFeedbackTitle.text = "Great Job! 👍"
                tvFeedbackMessage.text = "Very Good Effort! You have a strong grasp of this topic."
                tvFeedbackTitle.setTextColor(ContextCompat.getColor(this, R.color.edu_blue))
            }
            percentage >= 50 -> {
                tvFeedbackTitle.text = "Good Attempt! 💪"
                tvFeedbackMessage.text = "Keep practicing regularly to score higher next time."
                tvFeedbackTitle.setTextColor(ContextCompat.getColor(this, R.color.secondary_gold))
            }
            else -> {
                tvFeedbackTitle.text = "Needs Improvement! 📚"
                tvFeedbackMessage.text = "Don't give up! Review your notes, try again, and work harder."
                tvFeedbackTitle.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            }
        }

        findViewById<MaterialButton>(R.id.btnRetakeQuiz).setOnClickListener {
            val intent = Intent(this, QuizSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        findViewById<MaterialButton>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** One card per question: question text, all four options with the
     *  correct one in green (✓) and the student's wrong pick in red (✗),
     *  plus the explanation when the source has one. */
    private fun renderReview(review: List<QuizReviewItem>) {
        val container = findViewById<LinearLayout>(R.id.layoutReviewPaper)
        container.removeAllViews()
        for ((index, item) in review.withIndex()) {
            val q = item.question
            val picked = item.selected
            val correctIndex = q.correctAnswerIndex
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_role_card)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            card.addView(TextView(this).apply {
                text = "Q${index + 1}. ${q.questionText}"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
            })
            card.addView(TextView(this).apply {
                text = q.chapter
                textSize = 11f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, dp(2), 0, dp(6))
            })
            for (i in q.options.indices) {
                val letter = ('A' + i).toString()
                val isCorrect = i == correctIndex
                val isPicked = i == picked
                val mark = when {
                    isCorrect -> "✓ "
                    isPicked -> "✗ "
                    else -> ""
                }
                val suffix = when {
                    isCorrect && isPicked -> "  (Your answer — Correct)"
                    isCorrect -> "  (Correct answer)"
                    isPicked -> "  (Your answer)"
                    else -> ""
                }
                card.addView(TextView(this).apply {
                    text = "$mark$letter) ${q.options[i]}$suffix"
                    textSize = 13f
                    setTextColor(
                        when {
                            isCorrect -> Color.parseColor("#047857")
                            isPicked -> Color.parseColor("#DC2626")
                            else -> Color.parseColor("#475569")
                        }
                    )
                    setTypeface(null, if (isCorrect || isPicked) Typeface.BOLD else Typeface.NORMAL)
                    setPadding(0, dp(2), 0, dp(2))
                })
            }
            if (picked == -1) {
                card.addView(TextView(this).apply {
                    text = "Not answered — the correct answer is shown in green above."
                    textSize = 12f
                    setTextColor(Color.parseColor("#B45309"))
                    setPadding(0, dp(4), 0, 0)
                })
            }
            if (q.explanation.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = "Explanation: ${q.explanation}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#475569"))
                    setPadding(0, dp(6), 0, 0)
                })
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, dp(8))
            card.layoutParams = lp
            container.addView(card)
        }
    }
}
