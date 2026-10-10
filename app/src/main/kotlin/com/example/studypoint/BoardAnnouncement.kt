package com.example.studypoint

data class BoardAnnouncement(
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val link_url: String = "",
    val date: String = "",
    val is_active: Boolean = true
)
