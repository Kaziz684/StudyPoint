package com.example.studypoint.practicetest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application.applicationContext)

    private val _quizState = MutableLiveData<UiState<List<Question>>>()
    val quizState: LiveData<UiState<List<Question>>> = _quizState

    private fun subjectsMatch(qSubject: String, targetSubject: String): Boolean {
        if (targetSubject.isBlank()) return true
        val s1 = qSubject.trim().lowercase()
        val s2 = targetSubject.trim().lowercase()
        if (s1 == s2) return true

        // Tarjuma-tul-Quran matching
        val isS1Quran = s1.contains("quran") || s1.contains("tarjuma") || s1.contains("قرآن") || s1.contains("ترجم")
        val isS2Quran = s2.contains("quran") || s2.contains("tarjuma") || s2.contains("قرآن") || s2.contains("ترجم")
        if (isS1Quran && isS2Quran) return true

        // Pak Studies matching
        val isS1Pak = s1.contains("pak") || s1.contains("pakistan") || s1.contains("مطالعہ")
        val isS2Pak = s2.contains("pak") || s2.contains("pakistan") || s2.contains("مطالعہ")
        if (isS1Pak && isS2Pak) return true

        // Islamiyat matching
        val isS1Isl = s1.contains("islam") || s1.contains("islamiat") || s1.contains("اسلام")
        val isS2Isl = s2.contains("islam") || s2.contains("islamiat") || s2.contains("اسلام")
        if (isS1Isl && isS2Isl) return true

        // Mathematics matching
        val isS1Math = s1.contains("math") || s1.contains("ریاضی")
        val isS2Math = s2.contains("math") || s2.contains("ریاضی")
        if (isS1Math && isS2Math) return true

        // Physics matching
        val isS1Phy = s1.contains("phy") || s1.contains("فزکس")
        val isS2Phy = s2.contains("phy") || s2.contains("فزکس")
        if (isS1Phy && isS2Phy) return true

        // Chemistry matching
        val isS1Chem = s1.contains("chem") || s1.contains("کیمسٹری")
        val isS2Chem = s2.contains("chem") || s2.contains("کیمسٹری")
        if (isS1Chem && isS2Chem) return true

        // Biology matching
        val isS1Bio = s1.contains("bio") || s1.contains("حیاتیات")
        val isS2Bio = s2.contains("bio") || s2.contains("حیاتیات")
        if (isS1Bio && isS2Bio) return true

        // Computer Science matching ("cs" only as a whole word: "physics"/"mathematics" contain "cs" as substring)
        val csWord = Regex("\\bcs\\b")
        val isS1CS = s1.contains("computer") || s1.contains(csWord) || s1.contains("کمپیوٹر")
        val isS2CS = s2.contains("computer") || s2.contains(csWord) || s2.contains("کمپیوٹر")
        if (isS1CS && isS2CS) return true

        // English matching
        val isS1Eng = s1.contains("eng") || s1.contains("انگریزی")
        val isS2Eng = s2.contains("eng") || s2.contains("انگریزی")
        if (isS1Eng && isS2Eng) return true

        // Urdu matching
        val isS1Urdu = s1.contains("urdu") || s1.contains("اردو")
        val isS2Urdu = s2.contains("urdu") || s2.contains("اردو")
        if (isS1Urdu && isS2Urdu) return true

        return false
    }

    private fun chapterNumber(chapter: String): Int? =
        Regex("^\\s*chapter\\s*(\\d+)", RegexOption.IGNORE_CASE).find(chapter)?.groupValues?.get(1)?.toIntOrNull()

    private fun chaptersMatch(questionChapter: String, selectedChapter: String): Boolean {
        val qNum = chapterNumber(questionChapter)
        val sNum = chapterNumber(selectedChapter)
        return if (qNum != null && sNum != null) {
            // Exact chapter-number match, so "Chapter 1" never pulls in Chapter 10-19 questions
            qNum == sNum
        } else {
            questionChapter.trim().contains(selectedChapter, ignoreCase = true) ||
                selectedChapter.contains(questionChapter.trim(), ignoreCase = true)
        }
    }

    fun loadQuestions(sheetName: String, subject: String, chapter: String) {
        _quizState.value = UiState.Loading
        viewModelScope.launch {
            val normalizedSheet = when {
                sheetName.contains("9") -> "Class_9th"
                sheetName.contains("10") -> "Class_10th"
                sheetName.contains("11") -> "Class_11th"
                sheetName.contains("12") -> "Class_12th"
                else -> sheetName.trim()
            }

            val questions = repository.fetchQuestions(normalizedSheet, getApplication())

            val trimmedSubject = subject.trim()
            val trimmedChapter = chapter.trim()

            // Filter by subject using robust alias matching
            val subjectFiltered = questions.filter { q ->
                subjectsMatch(q.subject, trimmedSubject)
            }

            // Filter by chapter if explicitly specified
            val targetPool = if (trimmedChapter.isBlank() || trimmedChapter.equals("All Chapters", ignoreCase = true)) {
                if (subjectFiltered.isNotEmpty()) subjectFiltered else questions
            } else {
                val chapterFiltered = subjectFiltered.filter { q ->
                    chaptersMatch(q.chapter, trimmedChapter)
                }
                if (chapterFiltered.isNotEmpty()) chapterFiltered else if (subjectFiltered.isNotEmpty()) subjectFiltered else questions
            }

            // Build a 10-question dynamic quiz session (or all if <= 10)
            val sessionQuestions = if (targetPool.size > 10) {
                targetPool.shuffled().take(10)
            } else if (targetPool.isNotEmpty()) {
                targetPool.shuffled()
            } else {
                emptyList()
            }

            if (sessionQuestions.isNotEmpty()) {
                _quizState.value = UiState.Success(sessionQuestions)
            } else {
                _quizState.value = UiState.Error("No questions found for selection.")
            }
        }
    }
}
