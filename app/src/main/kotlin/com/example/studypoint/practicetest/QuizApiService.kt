package com.example.studypoint.practicetest

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface QuizApiService {
    @GET("exec")
    suspend fun getQuestions(@Query("sheetName") sheetName: String): Response<List<Question>>
}
