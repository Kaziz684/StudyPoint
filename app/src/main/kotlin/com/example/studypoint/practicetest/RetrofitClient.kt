package com.example.studypoint.practicetest

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbxZXV6mqswyoEZDEOmFqQcaEUeWFJpVwAHpulVaiOf1I15STM8jloR7sHVBMpmaj7sl5/"

    val apiService: QuizApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuizApiService::class.java)
    }
}
