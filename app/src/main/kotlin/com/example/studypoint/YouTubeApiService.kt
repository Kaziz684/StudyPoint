package com.example.studypoint

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ==========================================
// 1. YouTube Data API v3 Response Models
// ==========================================

data class YouTubeSearchResponse(
    @SerializedName("items") val items: List<YouTubeSearchItem>? = null
)

data class YouTubeSearchItem(
    @SerializedName("id") val id: YouTubeSearchId? = null,
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null
)

data class YouTubeSearchId(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("videoId") val videoId: String? = null
)

data class YouTubePlaylistResponse(
    @SerializedName("items") val items: List<YouTubePlaylistItem>? = null
)

data class YouTubePlaylistItem(
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null
)

data class YouTubeSnippet(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("publishedAt") val publishedAt: String? = null,
    @SerializedName("channelTitle") val channelTitle: String? = null,
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnailsContainer? = null,
    @SerializedName("resourceId") val resourceId: YouTubeResourceId? = null
)

data class YouTubeResourceId(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("videoId") val videoId: String? = null
)

data class YouTubeThumbnailsContainer(
    @SerializedName("default") val default: YouTubeThumbnail? = null,
    @SerializedName("medium") val medium: YouTubeThumbnail? = null,
    @SerializedName("high") val high: YouTubeThumbnail? = null,
    @SerializedName("maxres") val maxres: YouTubeThumbnail? = null
)

data class YouTubeThumbnail(
    @SerializedName("url") val url: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

// ==========================================
// 2. Clean Educational Video Domain Model
// ==========================================

data class YouTubeVideo(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelTitle: String,
    val publishedAt: String,
    val durationLabel: String = "Lecture"
) {
    val videoUrl: String
        get() = "https://www.youtube.com/watch?v=$id"
}

// ==========================================
// 3. Retrofit API Interface
// ==========================================

interface YouTubeApiService {

    @GET("search")
    suspend fun getChannelVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 12,
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>

    @GET("playlistItems")
    suspend fun getPlaylistVideos(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 12,
        @Query("key") apiKey: String
    ): Response<YouTubePlaylistResponse>

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

        fun create(): YouTubeApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YouTubeApiService::class.java)
        }
    }
}

// ==========================================
// 4. Robust YouTube Repository with Fallback
// ==========================================

object YouTubeRepository {
    private const val TAG = "YouTubeRepository"
    private val api: YouTubeApiService by lazy { YouTubeApiService.create() }

    /**
     * Curated fallback list of PCTB Educational Lectures
     * Guaranteed to display immediately even if offline or API key is not yet provided.
     */
    val sampleLectures: List<YouTubeVideo> = listOf(
        YouTubeVideo(
            id = "Vv3uW4eQG8E",
            title = "Class 10 Physics - Chapter 1: Simple Harmonic Motion & Waves",
            description = "Detailed lecture on SHM, Hooke's Law, and Wave Motion according to PCTB Punjab Board.",
            thumbnailUrl = "https://i.ytimg.com/vi/Vv3uW4eQG8E/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Physics"
        ),
        YouTubeVideo(
            id = "tY8Kx8k5j4Y",
            title = "Class 9 Mathematics - Chapter 1: Matrices & Determinants",
            description = "Complete solved exercise and concepts of Cramer's Rule and Matrix Inversion Method.",
            thumbnailUrl = "https://i.ytimg.com/vi/tY8Kx8k5j4Y/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Math"
        ),
        YouTubeVideo(
            id = "aA5G5m-Zz4A",
            title = "Class 10 Chemistry - Chapter 9: Dynamic Equilibrium & Kc",
            description = "Forward and reverse reactions, Law of Mass Action, and Kc equilibrium constant derivation.",
            thumbnailUrl = "https://i.ytimg.com/vi/aA5G5m-Zz4A/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Chemistry"
        ),
        YouTubeVideo(
            id = "7EWe3G9Vv88",
            title = "Class 9 & 10 Tarjuma-tul-Quran - Surah Maryam Complete",
            description = "Recitation, word-by-word Urdu translation and exercise questions (ترجمۃ القرآن لازمی).",
            thumbnailUrl = "https://i.ytimg.com/vi/7EWe3G9Vv88/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Quran"
        ),
        YouTubeVideo(
            id = "oK1gW4w3e4U",
            title = "Class 10 Pakistan Studies - Chapter 1: History & Constitution",
            description = "Comprehensive review of 1973 Constitution, nationalization, and democratic timeline.",
            thumbnailUrl = "https://i.ytimg.com/vi/oK1gW4w3e4U/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Pak Studies"
        ),
        YouTubeVideo(
            id = "k9H1m5U4_90",
            title = "Class 11 Biology - Chapter 1: Cellular Biology & Microscopy",
            description = "Cell organelles, ultrastructure of plant vs animal cells, and electron microscopy concepts.",
            thumbnailUrl = "https://i.ytimg.com/vi/k9H1m5U4_90/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Biology"
        ),
        YouTubeVideo(
            id = "m5Q7z7gK6L0",
            title = "Class 12 Computer Science - Database Systems & SQL Basics",
            description = "Entity-relationship diagrams, SQL tables, normalization, and relational databases for FSC ICS.",
            thumbnailUrl = "https://i.ytimg.com/vi/m5Q7z7gK6L0/hqdefault.jpg",
            channelTitle = "StudyPoint Lectures",
            publishedAt = "2026",
            durationLabel = "Comp Science"
        )
    )

    /**
     * Fetches latest videos dynamically from the YouTube Channel.
     * Uses BuildConfig.YOUTUBE_API_KEY and BuildConfig.YOUTUBE_CHANNEL_ID.
     * Falls back to sample educational lectures when keys are empty or on network error.
     */
    suspend fun fetchLatestVideos(
        apiKey: String = BuildConfig.YOUTUBE_API_KEY,
        channelId: String = BuildConfig.YOUTUBE_CHANNEL_ID
    ): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || channelId.isBlank()) {
            Log.d(TAG, "YouTube API key or Channel ID not provided in local.properties. Using educational sample feed.")
            return@withContext sampleLectures
        }

        try {
            // First attempt: search endpoint
            val response = api.getChannelVideos(
                channelId = channelId,
                apiKey = apiKey,
                maxResults = 12
            )

            if (response.isSuccessful && response.body()?.items != null) {
                val videos = response.body()!!.items!!.mapNotNull { item ->
                    val videoId = item.id?.videoId
                    val snippet = item.snippet
                    if (!videoId.isNullOrBlank() && snippet != null) {
                        val thumb = snippet.thumbnails?.high?.url
                            ?: snippet.thumbnails?.medium?.url
                            ?: snippet.thumbnails?.default?.url
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        YouTubeVideo(
                            id = videoId,
                            title = snippet.title ?: "Educational Lecture",
                            description = snippet.description ?: "",
                            thumbnailUrl = thumb,
                            channelTitle = snippet.channelTitle ?: "StudyPoint",
                            publishedAt = snippet.publishedAt?.take(10) ?: "Recent",
                            durationLabel = "Lecture"
                        )
                    } else null
                }

                if (videos.isNotEmpty()) {
                    Log.d(TAG, "Fetched ${videos.size} live videos from YouTube Channel successfully.")
                    return@withContext videos
                }
            } else {
                Log.w(TAG, "YouTube API response failed: ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching YouTube videos: ${e.message}", e)
        }

        // Return offline fallback lectures
        sampleLectures
    }
}
