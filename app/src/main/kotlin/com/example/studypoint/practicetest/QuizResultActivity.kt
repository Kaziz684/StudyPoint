package com.example.studypoint.practicetest

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.studypoint.HomeActivity
import com.example.studypoint.R
import com.google.android.material.button.MaterialButton

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
}
