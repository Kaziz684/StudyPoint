package com.example.studypoint

data class BoardModel(
    val name: String,
    val url: String,
    val subtitle: String = "Official Website & Result Portal",
    val logoRes: Int = R.mipmap.ic_logo_studypoint // Default placeholder
)
