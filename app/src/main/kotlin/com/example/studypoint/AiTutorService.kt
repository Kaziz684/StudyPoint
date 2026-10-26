package com.example.studypoint

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Intelligent Academic and General AI Tutor Service.
 *
 * Features:
 * 1. General Intelligent Tutor: Answers any user query directly, concisely, and accurately.
 * 2. Unrestricted: No hardcoded board exam disclaimers or repetitive PCTB headers.
 * 3. Internet Web Search Grounding: Queries Google Search grounding via Gemini API,
 *    with dynamic web fallbacks via DuckDuckGo Instant Answer and Wikipedia REST APIs.
 * 4. Offline Universal Knowledge Base: Clean, structured Markdown responses across academic,
 *    scientific, technological, and general knowledge disciplines.
 */
object AiTutorService {

    private const val TAG = "AiTutorService"

    const val SYSTEM_PROMPT = """You are StudyPoint AI Tutor, an intelligent, helpful, and versatile academic and general knowledge assistant.
Guidelines:
1. Answer ANY user query directly, concisely, clearly, and accurately.
2. Provide direct explanations, intuitive summaries, formulas, or code without imposing board exam templates or repetitive PCTB headers.
3. If the user asks a general question (e.g., "What is artificial intelligence?"), answer directly with a clean definition and key points.
4. Only discuss Punjab Board (PCTB) or specific board syllabus when the user explicitly requests board-specific information.
5. Use clean, readable Markdown formatting."""

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** A tutor answer: text plus an optional educational image and its source. */
    data class AiAnswer(
        val text: String,
        val imageUrl: String? = null,
        val sourceLabel: String? = null
    )

    /**
     * Resolves an intelligent response for any user query (text only).
     */
    suspend fun getAnswer(query: String): String = getAnswerRich(query).text

    /**
     * Resolves a rich response: answer text, and whenever the web result has
     * one, a real educational image (e.g. a biology diagram from Wikipedia).
     */
    suspend fun getAnswerRich(query: String): AiAnswer = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext AiAnswer("Please type a question, topic, or concept to begin.")
        }

        // 1. If Gemini API key is configured in BuildConfig / local.properties, call Gemini 2.5 Flash with Search Grounding
        val geminiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Throwable) { "" }
        if (geminiKey.isNotBlank()) {
            try {
                val geminiAnswer = callGeminiWithSearch(trimmed, geminiKey)
                if (!geminiAnswer.isNullOrBlank()) {
                    return@withContext AiAnswer(geminiAnswer)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini grounding request failed: ${e.message}")
            }
        }

        // The PDF viewer sends document context + question in one string
        // ("... Their question: <question>. Answer concisely for a student.").
        // Search engines and the keyword matcher need the bare question,
        // not the whole context blob — otherwise every search fails and
        // the student gets a false "check your internet" refusal.
        val searchQuery = extractBareQuestion(trimmed)

        // 2. Wikipedia SEARCH (works for any study question; also returns a topic image)
        try {
            val wikiAnswer = searchWikipedia(searchQuery)
            if (wikiAnswer != null) {
                return@withContext wikiAnswer
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search failed: ${e.message}")
        }

        // 3. Dynamic Real-Time Web Search Grounding (DuckDuckGo + Wikipedia REST)
        try {
            val liveWebResult = fetchLiveWebSummary(searchQuery)
            if (!liveWebResult.isNullOrBlank()) {
                return@withContext AiAnswer(liveWebResult)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dynamic web search failed: ${e.message}")
        }

        // 4. Clean Universal Knowledge Engine (Unrestricted, direct, clean markdown)
        return@withContext AiAnswer(generateUniversalTutorAnswer(searchQuery))
    }

    /**
     * The PDF viewer calls getAnswerRich() with document context + question
     * in one string ("... Their question: <question>. Answer concisely for
     * a student."). Returns just the student's question, so web search and
     * the keyword matcher receive a clean query.
     */
    private fun extractBareQuestion(query: String): String {
        val marker = "Their question:"
        val idx = query.indexOf(marker)
        if (idx < 0) return query
        var q = query.substring(idx + marker.length).trim()
        q = q.replace(Regex("(?i)\\.?\\s*Answer concisely for a student\\.?\\s*$"), "").trim()
        return q.ifBlank { query }
    }

    /**
     * Searches Wikipedia properly (full-text search API instead of guessing an
     * article title) and returns the top article's intro extract together with
     * its picture, so study questions get a real answer and, when available, a
     * real educational image/diagram.
     */
    private fun searchWikipedia(query: String): AiAnswer? {
        val cleanTerm = query
            .replace(Regex("(?i)^(what is|what are|explain|who was|who is|define|tell me about|how does|why is)\\s+"), "")
            .replace(Regex("(?i)\\b(picture|image|diagram|photo|pic)\\b"), "")
            .replace("?", "")
            .trim()
        if (cleanTerm.length < 2) return null

        val encoded = URLEncoder.encode(cleanTerm, StandardCharsets.UTF_8.name())
        val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&redirects=1" +
            "&generator=search&gsrnamespace=0&gsrlimit=3" +
            "&prop=pageimages%7Cextracts&pithumbsize=800&exintro&explaintext&exsentences=6" +
            "&gsrsearch=$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "StudyPointApp/1.0 (Android; educational)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val pagesArr = JSONObject(body).optJSONObject("query")?.optJSONArray("pages") ?: return null

            val pages = mutableListOf<JSONObject>()
            for (i in 0 until pagesArr.length()) {
                val p = pagesArr.getJSONObject(i)
                if (p.optString("extract").trim().length >= 60) pages += p
            }
            if (pages.isEmpty()) return null
            pages.sortBy { it.optInt("index", 99) }

            val wantsImage = query.contains(Regex("(?i)picture|image|diagram|photo|pic"))
            fun imageOf(p: JSONObject): String? =
                p.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
            val hit = if (wantsImage) {
                pages.firstOrNull { imageOf(it) != null } ?: pages.first()
            } else {
                pages.first()
            }
            val title = hit.optString("title")
            val extract = hit.optString("extract").trim()
            if (extract.isBlank()) return null
            return AiAnswer(
                text = "### $title\n\n$extract",
                imageUrl = imageOf(hit),
                sourceLabel = "Wikipedia - $title"
            )
        }
    }

    /**
     * Calls Google Gemini 2.5 Flash API with Google Search Grounding enabled.
     */
    private fun callGeminiWithSearch(query: String, apiKey: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val bodyJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
            })
            val contentsArr = JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", query)))
            })
            put("contents", contentsArr)

            // Enable Google Search Grounding
            val toolsArr = JSONArray().put(JSONObject().apply {
                put("googleSearch", JSONObject())
            })
            put("tools", toolsArr)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: return null
                val root = JSONObject(respStr)
                val candidates = root.optJSONArray("candidates") ?: return null
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return text.trim()
                    }
                }
            }
        }
        return null
    }

    /**
     * Dynamic internet web search fallback using DuckDuckGo Instant Answers and Wikipedia REST API.
     */
    private fun fetchLiveWebSummary(query: String): String? {
        // Normalize search term
        val cleanTerm = query
            .replace(Regex("(?i)^(what is|what are|explain|who was|who is|define|tell me about|how does|why is)\\s+"), "")
            .replace("?", "")
            .trim()

        if (cleanTerm.length < 2) return null

        val encodedTerm = URLEncoder.encode(cleanTerm, StandardCharsets.UTF_8.name())

        // Try DuckDuckGo Instant Answer
        try {
            val ddgUrl = "https://api.duckduckgo.com/?q=$encodedTerm&format=json&no_html=1&skip_disambig=1"
            val ddgRequest = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "StudyPointApp/1.0 (Android)")
                .build()

            httpClient.newCall(ddgRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val abstractText = json.optString("AbstractText")
                    val heading = json.optString("Heading")

                    if (abstractText.isNotBlank()) {
                        val title = if (heading.isNotBlank()) heading else cleanTerm.replaceFirstChar { it.uppercase() }
                        return "### $title\n\n$abstractText"
                    }
                }
            }
        } catch (_: Exception) {}

        // Try Wikipedia Summary API
        try {
            val wikiTopic = cleanTerm.replace(" ", "_")
            val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(wikiTopic, StandardCharsets.UTF_8.name())}"
            val wikiRequest = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", "StudyPointApp/1.0 (Android)")
                .build()

            httpClient.newCall(wikiRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val title = json.optString("title")
                    val extract = json.optString("extract")
                    val description = json.optString("description")

                    if (extract.isNotBlank() && extract.length > 35) {
                        val header = if (description.isNotBlank()) "$title — $description" else title
                        return "### $header\n\n$extract"
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Direct, unrestricted academic & general intelligence response generator.
     */
    fun generateUniversalTutorAnswer(query: String): String {
        val q = query.lowercase().trim()

        return when {
            // 1. Artificial Intelligence / Tech
            q.contains("artificial intelligence") || q.contains("what is ai") || q.equals("ai") ->
                "### Artificial Intelligence (AI)\n\n" +
                "Artificial Intelligence refers to the simulation of human intelligence in machines programmed to think, reason, and learn.\n\n" +
                "**Core Branches:**\n" +
                "• **Machine Learning (ML):** Systems learning patterns from historical data.\n" +
                "• **Deep Learning:** Multi-layered artificial neural networks inspired by biological brains.\n" +
                "• **Natural Language Processing (NLP):** Understanding and generating human language.\n" +
                "• **Computer Vision:** Interpreting and extracting information from digital images and video.\n\n" +
                "**Real-World Applications:**\n" +
                "Autonomous driving, diagnostic healthcare, generative assistants, voice recognition, and personalized recommendations."

            q.contains("machine learning") || q.contains("what is ml") ->
                "### Machine Learning (ML)\n\n" +
                "Machine Learning is a subset of AI that allows systems to automatically learn and improve from experience without being explicitly programmed.\n\n" +
                "**Main Paradigms:**\n" +
                "1. **Supervised Learning:** Training with labeled input-output pairs (e.g., classification, regression).\n" +
                "2. **Unsupervised Learning:** Discovering hidden patterns in unlabeled data (e.g., clustering, dimensionality reduction).\n" +
                "3. **Reinforcement Learning:** Agents learning optimal actions through rewards and penalties."

            // 2. Physics
            q.contains("newton") || q.contains("f=ma") || q.contains("force") ->
                "### Newton's Laws of Motion\n\n" +
                "1. **First Law (Inertia):** A body remains at rest or in uniform motion along a straight line unless acted upon by an external net force.\n" +
                "2. **Second Law (Acceleration):** Force equals the rate of change of momentum, expressed as **F = ma** (where *F* is force in Newtons, *m* is mass in kg, and *a* is acceleration in m/s²).\n" +
                "3. **Third Law (Action & Reaction):** To every action there is an equal and opposite reaction."

            q.contains("shm") || q.contains("simple harmonic") || q.contains("oscillation") ->
                "### Simple Harmonic Motion (SHM)\n\n" +
                "SHM is a periodic oscillatory motion in which acceleration is directly proportional to displacement from the equilibrium position and always directed toward that position: **a ∝ -x**.\n\n" +
                "**Time Period Formulas:**\n" +
                "• Mass-Spring System: **T = 2π√(m / k)**\n" +
                "• Simple Pendulum: **T = 2π√(L / g)**"

            q.contains("ohm") || q.contains("v=ir") || q.contains("coulomb") || q.contains("electric") ->
                "### Electricity & Electrodynamics\n\n" +
                "• **Ohm's Law:** Current flowing through a conductor is directly proportional to potential difference across its ends at constant temperature: **V = I · R**.\n" +
                "• **Coulomb's Law:** Electrostatic force between two point charges: **F = k · (|q₁ · q₂| / r²)**, where *k ≈ 8.99 × 10⁹ N·m²/C²*."

            q.contains("gravity") || q.contains("gravitation") ->
                "### Universal Gravitation\n\n" +
                "Every particle in the universe attracts every other particle with a gravitational force directly proportional to the product of their masses and inversely proportional to the square of the distance between their centers:\n\n" +
                "**F = G · (m₁ · m₂ / r²)**\n\n" +
                "• *G* = 6.674 × 10⁻¹¹ N·m²/kg² (Gravitational constant)\n" +
                "• On Earth's surface, acceleration due to gravity is approximately **g ≈ 9.8 m/s²**."

            // 3. Mathematics
            q.contains("quadratic") ->
                "### Quadratic Equation & Formula\n\n" +
                "A standard quadratic equation has the form: **ax² + bx + c = 0** (where a ≠ 0).\n\n" +
                "**Quadratic Formula:**\n" +
                "**x = [-b ± √(b² - 4ac)] / (2a)**\n\n" +
                "• **Discriminant (D = b² - 4ac):**\n" +
                "  - If D > 0: Two distinct real roots\n" +
                "  - If D = 0: One real repeated root\n" +
                "  - If D < 0: Two complex conjugate roots"

            q.contains("cramer") || q.contains("matrix") || q.contains("determinant") ->
                "### Matrices & Cramer's Rule\n\n" +
                "For a 2×2 system of linear equations **AX = B**:\n" +
                "• **Determinant |A|:** For matrix [[a, b], [c, d]], **|A| = ad - bc**.\n" +
                "• **Cramer's Rule:** **x = |Ax| / |A|**, **y = |Ay| / |A|** (valid whenever |A| ≠ 0)."

            q.contains("trig") || q.contains("sin") || q.contains("cos") || q.contains("pythagor") ->
                "### Trigonometry Essentials\n\n" +
                "• **Pythagorean Theorem:** In a right-angled triangle, **Hypotenuse² = Base² + Perpendicular²**.\n" +
                "• **Fundamental Identity:** **sin²θ + cos²θ = 1**\n" +
                "• **Tangents:** **tan θ = sin θ / cos θ**, and **1 + tan²θ = sec²θ**."

            // 4. Chemistry
            q.contains("equilibrium") || q.contains("le chatelier") ->
                "### Chemical Equilibrium\n\n" +
                "Dynamic chemical equilibrium occurs when the forward reaction rate equals the reverse reaction rate in a closed system.\n\n" +
                "• **Equilibrium Constant (Kc):** For aA + bB ⇌ cC + dD, **Kc = [C]ᶜ[D]ᵈ / ([A]ᵃ[B]ᵇ)**.\n" +
                "• **Le Chatelier's Principle:** If an equilibrium system is subjected to a change in concentration, temperature, or pressure, the system adjusts to counteract the applied change."

            q.contains("acid") || q.contains("base") || q.contains("ph") ->
                "### Acids, Bases & pH\n\n" +
                "• **Arrhenius:** Acids produce H⁺ ions in water; bases produce OH⁻ ions.\n" +
                "• **Brønsted-Lowry:** Acids are proton (H⁺) donors; bases are proton acceptors.\n" +
                "• **pH Scale:** **pH = -log[H⁺]** (pH < 7 is acidic, pH = 7 is neutral, pH > 7 is alkaline)."

            q.contains("organic") || q.contains("hydrocarbon") || q.contains("alkane") ->
                "### Hydrocarbons Overview\n\n" +
                "Organic compounds consisting exclusively of Carbon and Hydrogen:\n" +
                "• **Alkanes (Saturated):** Single bonds only. General formula: **CₙH₂ₙ₊₂**\n" +
                "• **Alkenes (Unsaturated):** At least one carbon-carbon double bond: **CₙH₂ₙ**\n" +
                "• **Alkynes (Unsaturated):** At least one carbon-carbon triple bond: **CₙH₂ₙ₋₂**"

            // 5. Biology
            q.contains("photosynthesis") ->
                "### Photosynthesis\n\n" +
                "Photosynthesis is the biochemical process by which green plants and certain organisms transform light energy into chemical energy stored in glucose.\n\n" +
                "**Balanced Equation:**\n" +
                "**6CO₂ + 12H₂O + Sunlight → C₆H₁₂O₆ + 6O₂ + 6H₂O**\n\n" +
                "• **Light Reactions:** Occur in thylakoid membranes, producing ATP and NADPH while releasing O₂.\n" +
                "• **Calvin Cycle (Dark Reactions):** Occurs in the stroma to fix carbon into glucose."

            q.contains("cell") || q.contains("mitochondria") || q.contains("nucleus") ->
                "### Cell Structure & Organelles\n\n" +
                "The cell is the basic structural and functional unit of life.\n\n" +
                "• **Nucleus:** Houses genetic material (DNA) and directs cellular metabolism.\n" +
                "• **Mitochondria:** 'Powerhouse of the cell' generating ATP through cellular respiration.\n" +
                "• **Ribosomes:** Sites of protein synthesis.\n" +
                "• **Chloroplasts (Plants):** Contain chlorophyll for photosynthesis."

            // 6. Computer Science & Programming
            q.contains("c++") || q.contains("python") || q.contains("loop") || q.contains("programming") ->
                "### Programming Fundamentals\n\n" +
                "• **Control Flow:** Conditional branching (`if-else`) and repetitive execution loops (`for`, `while`).\n" +
                "• **Data Structures:** Arrays, lists, stacks, and hash tables for organized storage and retrieval.\n" +
                "• **Object-Oriented Programming (OOP):** Encapsulation, Inheritance, Polymorphism, and Abstraction."

            q.contains("database") || q.contains("sql") || q.contains("normalization") ->
                "### Relational Databases & SQL\n\n" +
                "• **Relational Model:** Data stored in structured tables with rows (records) and columns (attributes).\n" +
                "• **Keys:** Primary Key guarantees unique row identification; Foreign Key links related tables.\n" +
                "• **Normalization:** Standardizing tables (1NF, 2NF, 3NF) to minimize redundancy and prevent anomalies."

            // 6b. Pakistan Studies — History & Constitutional Development
            q.contains("maqasid") || q.contains("objectives resolution") ||
                q.contains("مقاصد") ->
                "### Objectives Resolution (Qarardad-e-Maqasid)\n\n" +
                "The Objectives Resolution was passed on **12 March 1949** by Pakistan's first Constituent Assembly. " +
                "It laid down that sovereignty belongs to Allah, and that the state would exercise its powers through the chosen representatives of the people, " +
                "with democracy, freedom, equality and social justice. It later became the preamble of Pakistan's constitutions."
            q.contains("قرارداد") || q.contains("qarardad") ||
                q.contains("lahore resolution") || q.contains("pakistan resolution") ->
                "### Lahore Resolution (Qarardad-e-Pakistan)\n\n" +
                "The Lahore Resolution was passed on **23 March 1940** at Minto Park, Lahore, during the annual session of the All-India Muslim League. " +
                "It was presented by A. K. Fazlul Huq. The resolution demanded that the Muslim-majority areas of north-western and eastern India " +
                "be grouped into independent states — this demand became the foundation of the movement for Pakistan."
            q.contains("two nation") || q.contains("two-nation") ||
                q.contains("nazaria") || q.contains("دو قومی") ->
                "### Two-Nation Theory\n\n" +
                "The Two-Nation Theory states that the Muslims and Hindus of the Indian subcontinent were two separate nations, " +
                "with different religions, laws, heroes and customs — and therefore the Muslims needed a separate homeland. " +
                "It was the ideological basis of the Pakistan Movement, most famously presented by Muhammad Ali Jinnah."
            q.contains("constitution") || q.contains("aain") || q.contains("آئین") ->
                "### Constitutions of Pakistan\n\n" +
                "• **1956 Constitution:** Pakistan's first constitution, enforced on 23 March 1956; Pakistan became an Islamic Republic.\n" +
                "• **1962 Constitution:** Enforced on 8 June 1962; it introduced a presidential system.\n" +
                "• **1973 Constitution:** Passed on 10 April 1973 and enforced on 14 August 1973; it is the current constitution, " +
                "providing a parliamentary system with Islam as the state religion."
            q.contains("azadi") || q.contains("independence of pakistan") ||
                q.contains("pakistan kab bana") || q.contains("pakistan kab bani") ||
                (q.contains("14 august") && !q.contains("1973")) || q.contains("14 اگست") ||
                q.contains("partition") || q.contains("taqseem") ->
                "### Independence of Pakistan\n\n" +
                "Pakistan came into being on **14 August 1947** as a result of the partition of British India under the Indian Independence Act 1947. " +
                "Muhammad Ali Jinnah became its first Governor-General and Liaquat Ali Khan its first Prime Minister."
            // 7. Explicit 2026-27 Syllabus / Punjab Board inquiries
            q.contains("2026") || q.contains("2027") || q.contains("new syllabus") || q.contains("pctb") || q.contains("punjab board") ->
                "### Punjab Board (PCTB) New Syllabus 2026-27\n\n" +
                "The curriculum includes the following updated distribution:\n\n" +
                "• **Tarjuma-tul-Quran (ترجمۃ القرآن):** Dedicated compulsory subject across all four classes (9th, 10th, 11th, and 12th).\n" +
                "• **Islamiyat (اسلامیات):** Compulsory for 9th and 11th Classes.\n" +
                "• **Pakistan Studies (مطالعہ پاکستان):** Dedicated compulsory subject for 10th and 12th Classes (replaces Islamiyat in these grades).\n" +
                "• **Subjects Content:** All science and arts subjects are updated to the latest single conceptual syllabus editions with SLO-based testing (Student Learning Outcomes)."

            // 8. Honest fallback (no invented content, no false internet blame)
            else ->
                "### ${query.replaceFirstChar { it.uppercase() }}\n\n" +
                "I could not find a detailed answer for this topic in the search results just now. " +
                "Please try asking in shorter, simpler words — for example, just the name of the topic — " +
                "or check this topic in the app's Notes, MCQs and Paper Generator sections."
        }
    }
}
