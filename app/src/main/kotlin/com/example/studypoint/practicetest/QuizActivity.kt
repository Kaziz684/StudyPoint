package com.example.studypoint.practicetest

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.studypoint.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuizActivity : AppCompatActivity() {

    private val viewModel: QuizViewModel by viewModels()

    private lateinit var tvQuestionCount: TextView
    private lateinit var tvTimer: TextView
    private lateinit var quizProgressBar: ProgressBar
    private lateinit var tvQuestionText: TextView
    private lateinit var btnOptions: List<MaterialButton>
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBookmark: ImageButton

    private var questions: List<Question> = emptyList()
    private var currentIndex = 0
    private var score = 0
    private var incorrectCount = 0
    private var timer: CountDownTimer? = null
    private var answered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        tvQuestionCount = findViewById(R.id.tvQuestionCount)
        tvTimer = findViewById(R.id.tvTimer)
        quizProgressBar = findViewById(R.id.quizProgressBar)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        btnNext = findViewById(R.id.btnNext)
        btnBookmark = findViewById(R.id.btnBookmark)

        btnBookmark.setOnClickListener {
            if (questions.isEmpty() || currentIndex >= questions.size) return@setOnClickListener
            val q = questions[currentIndex]
            val added = BookmarkManager.toggleBookmark(this, q)
            updateBookmarkIcon()
            Toast.makeText(
                this,
                if (added) "Saved! Find it in Bookmarks." else "Removed from bookmarks.",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnOptions = listOf(
            findViewById(R.id.btnOption1),
            findViewById(R.id.btnOption2),
            findViewById(R.id.btnOption3),
            findViewById(R.id.btnOption4)
        )

        val sheetName = intent.getStringExtra("sheetName") ?: "Class_9th"
        val subject = intent.getStringExtra("subject") ?: ""
        val chapter = intent.getStringExtra("chapter") ?: ""

        btnNext.setOnClickListener {
            if (questions.isEmpty() || currentIndex >= questions.size) return@setOnClickListener
            if (currentIndex < questions.size - 1) {
                currentIndex++
                displayQuestion()
            } else {
                navigateToResults()
            }
        }

        btnOptions.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (questions.isEmpty() || currentIndex >= questions.size) return@setOnClickListener
                if (!answered) {
                    checkAnswer(index)
                }
            }
        }

        viewModel.quizState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    tvQuestionText.text = "Loading questions from Google Sheets..."
                    btnNext.isEnabled = false
                    btnOptions.forEach { it.visibility = View.GONE }
                }
                is UiState.Success -> {
                    questions = state.data
                    if (questions.isNotEmpty()) {
                        currentIndex = 0
                        score = 0
                        incorrectCount = 0
                        displayQuestion()
                    } else {
                        showComingSoonAndDismiss()
                    }
                }
                is UiState.Error -> {
                    showComingSoonAndDismiss()
                }
            }
        }

        viewModel.loadQuestions(sheetName, subject, chapter)
    }

    private fun showComingSoonAndDismiss() {
        tvQuestionText.text = "Coming Soon (عنقریب دستیاب ہوگا)\n\nMCQs for this selection are currently being prepared."
        btnNext.isEnabled = false
        btnOptions.forEach { it.visibility = View.GONE }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Coming Soon (عنقریب دستیاب ہوگا)")
            .setMessage("MCQs for this selection are currently being prepared and will be available soon.")
            .setPositiveButton("Go Back") { dialogInterface, _ ->
                dialogInterface.dismiss()
                if (!isFinishing) finish()
            }
            .setCancelable(false)
            .show()

        // Auto-dismiss and finish after 1.5 seconds so the screen never gets stuck
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } catch (_: Exception) {}
            if (!isFinishing) {
                finish()
            }
        }, 1500)
    }

    private fun displayQuestion() {
        if (questions.isEmpty() || currentIndex >= questions.size) return
        answered = false
        btnNext.isEnabled = false

        val q = questions[currentIndex]
        tvQuestionCount.text = "Question ${currentIndex + 1} / ${questions.size}"
        quizProgressBar.progress = ((currentIndex + 1) * 100) / questions.size
        tvQuestionText.text = q.questionText
        updateBookmarkIcon()

        val options = q.options
        btnOptions.forEachIndexed { i, button ->
            if (i < options.size) {
                button.visibility = View.VISIBLE
                button.text = options[i]
                button.strokeColor = ContextCompat.getColorStateList(this, R.color.edu_border)
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.edu_card_bg)
                button.setTextColor(ContextCompat.getColor(this, R.color.edu_text_primary))
                button.isEnabled = true
            } else {
                button.visibility = View.GONE
            }
        }

        startTimer()
    }

    private fun updateBookmarkIcon() {
        if (questions.isEmpty() || currentIndex >= questions.size) return
        val bookmarked = BookmarkManager.isBookmarked(this, questions[currentIndex])
        btnBookmark.setImageResource(
            if (bookmarked) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvTimer.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                tvTimer.text = "00:00"
                if (!answered) {
                    handleTimeOut()
                }
            }
        }.start()
    }

    private fun handleTimeOut() {
        if (questions.isEmpty() || currentIndex >= questions.size) return
        answered = true
        incorrectCount++
        val q = questions[currentIndex]

        if (q.correctAnswerIndex in btnOptions.indices) {
            btnOptions[q.correctAnswerIndex].strokeColor = ContextCompat.getColorStateList(this, R.color.edu_emerald)
        }

        btnOptions.forEach { it.isEnabled = false }
        btnNext.isEnabled = true
        showExplanationDialog(q.explanation, "Time's up! Correct answer highlighted.")
    }

    private fun checkAnswer(selectedIndex: Int) {
        if (questions.isEmpty() || currentIndex >= questions.size) return
        timer?.cancel()
        answered = true
        val q = questions[currentIndex]
        val correctIndex = q.correctAnswerIndex

        if (selectedIndex in btnOptions.indices) {
            if (selectedIndex == correctIndex) {
                score++
                btnOptions[selectedIndex].strokeColor = ContextCompat.getColorStateList(this, R.color.edu_emerald)
                btnOptions[selectedIndex].setTextColor(ContextCompat.getColor(this, R.color.edu_emerald))
            } else {
                incorrectCount++
                btnOptions[selectedIndex].strokeColor = ContextCompat.getColorStateList(this, R.color.error_red)
                btnOptions[selectedIndex].setTextColor(ContextCompat.getColor(this, R.color.error_red))
                if (correctIndex in btnOptions.indices) {
                    btnOptions[correctIndex].strokeColor = ContextCompat.getColorStateList(this, R.color.edu_emerald)
                    btnOptions[correctIndex].setTextColor(ContextCompat.getColor(this, R.color.edu_emerald))
                }
            }
        }

        btnOptions.forEach { it.isEnabled = false }
        btnNext.isEnabled = true

        if (q.explanation.isNotBlank()) {
            showExplanationDialog(q.explanation, if (selectedIndex == correctIndex) "Correct!" else "Incorrect!")
        }
    }

    private fun showExplanationDialog(explanation: String, title: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(if (explanation.isBlank()) "No explanation provided." else explanation)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun navigateToResults() {
        timer?.cancel()
        val intent = Intent(this, QuizResultActivity::class.java).apply {
            putExtra("score", score)
            putExtra("total", questions.size)
            putExtra("correct", score)
            putExtra("incorrect", incorrectCount)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
