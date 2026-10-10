package com.example.studypoint

data class QuickChipItem(
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit
)
