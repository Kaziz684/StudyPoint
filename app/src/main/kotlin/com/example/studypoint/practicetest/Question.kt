package com.example.studypoint.practicetest

data class Question(
    val id: Int,
    val class_num: String,
    val subject: String,
    val chapter: String,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)
