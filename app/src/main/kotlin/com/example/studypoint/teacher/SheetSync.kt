package com.example.studypoint.teacher

import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mirrors teacher data into the owner's Google Sheet through a small
 * Apps Script web-app bridge (tabs are created per school + class).
 * The bridge URL + key live in Firestore `app_secrets/sheets_bridge`;
 * when that document is absent, every sync silently does nothing —
 * Firestore stays the source of truth and nothing ever blocks on this.
 */
object SheetSync {

    @Volatile private var bridgeUrl: String? = null
    @Volatile private var bridgeKey: String? = null
    @Volatile private var loading = false

    private fun ensureConfig(onReady: (String, String) -> Unit) {
        val u = bridgeUrl
        val k = bridgeKey
        if (u != null && k != null) {
            onReady(u, k)
            return
        }
        if (loading) return
        loading = true
        FirebaseFirestore.getInstance().collection("app_secrets").document("sheets_bridge")
            .get()
            .addOnSuccessListener { doc ->
                loading = false
                val url = doc.getString("url") ?: ""
                val key = doc.getString("key") ?: ""
                if (url.isNotEmpty()) {
                    bridgeUrl = url
                    bridgeKey = key
                    onReady(url, key)
                }
            }
            .addOnFailureListener { loading = false }
    }

    private fun post(body: JSONObject) {
        ensureConfig { url, key ->
            body.put("key", key)
            Thread {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    conn.inputStream?.use { it.readBytes() }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Mirror only — never surface failures in the app.
                }
            }.start()
        }
    }

    private fun studentJson(s: TeacherStudent): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("rollNumber", s.rollNumber)
        put("name", s.name)
        put("fatherName", s.fatherName)
        put("className", s.className)
        put("section", s.section)
        put("gender", s.gender)
        put("campus", s.campus)
        put("school", s.school)
        put("guardianName", s.guardianName)
        put("guardianPhone", s.guardianPhone)
        put("guardianRelation", s.guardianRelation)
    }

    fun student(s: TeacherStudent) {
        post(JSONObject().apply {
            put("kind", "student")
            put("student", studentJson(s))
        })
    }

    fun studentDelete(s: TeacherStudent) {
        post(JSONObject().apply {
            put("kind", "studentDelete")
            put("student", studentJson(s))
        })
    }

    fun awardList(a: AwardListData, studentsById: Map<String, TeacherStudent>) {
        val rows = JSONArray()
        val ordered = a.studentIds
            .mapNotNull { sid -> studentsById[sid]?.let { sid to it } }
            .sortedWith(compareBy({ it.second.rollNumber }, { it.second.name }))
        for ((sid, s) in ordered) {
            val m = a.marks[sid]
            val row = JSONObject()
            row.put("roll", s.rollNumber)
            row.put("name", s.name)
            if (m?.obtained != null) row.put("obtained", m.obtained) else row.put("obtained", JSONObject.NULL)
            if (m?.total != null) row.put("total", m.total) else row.put("total", JSONObject.NULL)
            row.put("status", m?.status ?: "")
            row.put("remarks", m?.remarks ?: "")
            rows.put(row)
        }
        post(JSONObject().apply {
            put("kind", "awardList")
            put("award", JSONObject().apply {
                put("id", a.id)
                put("listCode", a.listCode)
                put("title", a.title)
                put("campus", a.campus)
                put("school", a.school)
                put("className", a.className)
                put("section", a.section)
                put("subject", a.subject)
                put("examType", ExamTypes.labelList(a.examType))
                put("examDate", a.examDate)
                put("returnDate", a.returnDate)
                put("students", rows)
            })
        })
    }

    fun awardDelete(a: AwardListData) {
        post(JSONObject().apply {
            put("kind", "awardDelete")
            put("award", JSONObject().apply {
                put("id", a.id)
                put("listCode", a.listCode)
                put("school", a.school)
                put("campus", a.campus)
                put("className", a.className)
            })
        })
    }
}
