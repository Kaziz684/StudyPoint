package com.example.studypoint.teacher

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/** One student record owned by a teacher (collection: teacher_students). */
data class TeacherStudent(
    var id: String = "",
    var name: String = "",
    var rollNumber: String = "",
    var campus: String = "",
    var school: String = "",
    var className: String = "",
    var section: String = "",
    var gender: String = "",
    var guardianName: String = "",
    var guardianPhone: String = ""
)

/** Marks of one student inside one award list. */
data class MarkEntry(
    var obtained: Double? = null,
    var total: Double? = null,
    var remarks: String = "",
    var status: String = "" // "" = present, "A" = absent, "L" = leave
)

/** One award list (collection: award_lists). */
data class AwardListData(
    var id: String = "",
    var title: String = "",
    var campus: String = "",
    var school: String = "",
    var className: String = "",
    var section: String = "",
    var gender: String = "All",
    var session: String = "",
    var subject: String = "",
    var examType: String = "",
    var examDate: String = "",
    var returnDate: String = "",
    var listCode: String = "", // short numeric ID shown to teachers, e.g. 4871
    var status: String = STATUS_ACTIVE, // DRAFT or ACTIVE
    var studentIds: List<String> = emptyList(),
    var marks: MutableMap<String, MarkEntry> = mutableMapOf(),
    var createdAt: Long = 0L
) {
    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_ACTIVE = "ACTIVE"
    }
}

/** The exact exam types required by the school system — do not add or remove. */
object ExamTypes {
    val ALL: List<String> = listOf(
        "RNT", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8",
        "Pre-Board", "First Round Book", "Second Round Book",
        "Schedule One", "Schedule Two", "Monthly Test"
    )

    fun label(value: String): String = when (value) {
        "RNT" -> "RNT"
        "T1" -> "T1 (Test 1)"
        "T2" -> "T2 (Test 2)"
        "T3" -> "T3 (Test 3)"
        "T4" -> "T4 (Test 4)"
        "T5" -> "T5 (Test 5)"
        "T6" -> "T6 (Test 6)"
        "T7" -> "T7 (Test 7)"
        "T8" -> "T8 (Test 8)"
        else -> value
    }
}

/**
 * Firestore access for teacher-owned data. Every record carries the
 * teacher's UID and every query is scoped to it; Firestore security
 * rules enforce the same ownership on the server side.
 */
object TeacherRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ---------------- Students ----------------

    fun loadStudents(onOk: (List<TeacherStudent>) -> Unit, onErr: (String) -> Unit) {
        db.collection("teacher_students")
            .whereEqualTo("teacherUid", uid)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { d ->
                    TeacherStudent(
                        id = d.id,
                        name = d.getString("name") ?: "",
                        rollNumber = d.getString("rollNumber") ?: "",
                        campus = d.getString("campus") ?: "",
                        school = d.getString("school") ?: "",
                        className = d.getString("className") ?: "",
                        section = d.getString("section") ?: "",
                        gender = d.getString("gender") ?: "",
                        guardianName = d.getString("guardianName") ?: "",
                        guardianPhone = d.getString("guardianPhone") ?: ""
                    )
                }.sortedWith(compareBy({ it.className }, { it.section }, { it.rollNumber }, { it.name }))
                onOk(list)
            }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not load students") }
    }

    fun saveStudent(s: TeacherStudent, onOk: () -> Unit, onErr: (String) -> Unit) {
        val data = hashMapOf<String, Any>(
            "teacherUid" to uid,
            "name" to s.name,
            "rollNumber" to s.rollNumber,
            "campus" to s.campus,
            "school" to s.school,
            "className" to s.className,
            "section" to s.section,
            "gender" to s.gender,
            "guardianName" to s.guardianName,
            "guardianPhone" to s.guardianPhone,
            "updatedAt" to System.currentTimeMillis()
        )
        val col = db.collection("teacher_students")
        val write = if (s.id.isEmpty()) {
            data["createdAt"] = System.currentTimeMillis()
            col.add(data)
        } else {
            col.document(s.id).set(data, com.google.firebase.firestore.SetOptions.merge())
        }
        write.addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not save student") }
    }

    fun deleteStudent(id: String, onOk: () -> Unit, onErr: (String) -> Unit) {
        db.collection("teacher_students").document(id).delete()
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not delete student") }
    }

    /** Duplicate guard: same roll number in the same campus+school+class+section. */
    fun findDuplicate(
        candidate: TeacherStudent,
        all: List<TeacherStudent>,
        onResult: (TeacherStudent?) -> Unit
    ) {
        val dup = all.firstOrNull {
            it.id != candidate.id &&
                it.rollNumber.equals(candidate.rollNumber, ignoreCase = true) &&
                it.campus.equals(candidate.campus, ignoreCase = true) &&
                it.school.equals(candidate.school, ignoreCase = true) &&
                it.className.equals(candidate.className, ignoreCase = true) &&
                it.section.equals(candidate.section, ignoreCase = true)
        }
        onResult(dup)
    }

    // ---------------- Award lists ----------------

    fun loadAwardLists(onOk: (List<AwardListData>) -> Unit, onErr: (String) -> Unit) {
        db.collection("award_lists")
            .whereEqualTo("teacherUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                onOk(snap.documents.map { fromDoc(it.id, it.data ?: emptyMap()) })
            }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not load award lists") }
    }

    fun loadAwardList(id: String, onOk: (AwardListData?) -> Unit, onErr: (String) -> Unit) {
        db.collection("award_lists").document(id).get()
            .addOnSuccessListener { d ->
                if (d.exists()) onOk(fromDoc(d.id, d.data ?: emptyMap())) else onOk(null)
            }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not load award list") }
    }

    fun createAwardList(a: AwardListData, onOk: (String) -> Unit, onErr: (String) -> Unit) {
        if (a.listCode.isNotEmpty()) {
            insertAwardList(a, onOk, onErr)
            return
        }
        // Short human-readable ID (like 4871): a per-teacher counter kept on
        // the user document, incremented atomically in a transaction.
        val userRef = db.collection("users").document(uid)
        db.runTransaction { tx ->
            val snap = tx.get(userRef)
            val current = snap.getLong("award_counter") ?: 0L
            val next = current + 1
            tx.set(userRef, mapOf("award_counter" to next), com.google.firebase.firestore.SetOptions.merge())
            next
        }.addOnSuccessListener { next ->
            a.listCode = (1000 + next).toString()
            insertAwardList(a, onOk, onErr)
        }.addOnFailureListener {
            // Counter unavailable (e.g. offline): fall back to a random code.
            a.listCode = (1000..9999).random().toString()
            insertAwardList(a, onOk, onErr)
        }
    }

    private fun insertAwardList(a: AwardListData, onOk: (String) -> Unit, onErr: (String) -> Unit) {
        val data = toDoc(a)
        data["teacherUid"] = uid
        data["createdAt"] = System.currentTimeMillis()
        db.collection("award_lists").add(data)
            .addOnSuccessListener { ref -> onOk(ref.id) }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not create award list") }
    }

    fun updateAwardList(a: AwardListData, onOk: () -> Unit, onErr: (String) -> Unit) {
        val data = toDoc(a)
        data["teacherUid"] = uid
        db.collection("award_lists").document(a.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not save award list") }
    }

    fun deleteAwardList(id: String, onOk: () -> Unit, onErr: (String) -> Unit) {
        db.collection("award_lists").document(id).delete()
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e -> onErr(e.message ?: "Could not delete award list") }
    }

    private fun toDoc(a: AwardListData): MutableMap<String, Any> {
        val marksMap = HashMap<String, Any>()
        for ((sid, m) in a.marks) {
            val mm = HashMap<String, Any>()
            m.obtained?.let { mm["obtained"] = it }
            m.total?.let { mm["total"] = it }
            mm["remarks"] = m.remarks
            mm["status"] = m.status
            marksMap[sid] = mm
        }
        return mutableMapOf(
            "listCode" to a.listCode,
            "title" to a.title,
            "campus" to a.campus,
            "school" to a.school,
            "className" to a.className,
            "section" to a.section,
            "gender" to a.gender,
            "session" to a.session,
            "subject" to a.subject,
            "examType" to a.examType,
            "examDate" to a.examDate,
            "returnDate" to a.returnDate,
            "status" to a.status,
            "studentIds" to a.studentIds,
            "marks" to marksMap,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    private fun fromDoc(id: String, d: Map<String, Any>): AwardListData {
        fun s(k: String) = d[k]?.toString() ?: ""
        val ids = (d["studentIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val marks = mutableMapOf<String, MarkEntry>()
        val rawMarks = d["marks"] as? Map<*, *>
        if (rawMarks != null) {
            for ((k, v) in rawMarks) {
                val sid = k?.toString() ?: continue
                val mm = v as? Map<*, *> ?: continue
                marks[sid] = MarkEntry(
                    obtained = (mm["obtained"] as? Number)?.toDouble(),
                    total = (mm["total"] as? Number)?.toDouble(),
                    remarks = mm["remarks"]?.toString() ?: "",
                    status = mm["status"]?.toString() ?: ""
                )
            }
        }
        return AwardListData(
            id = id,
            listCode = s("listCode"),
            title = s("title"),
            campus = s("campus"),
            school = s("school"),
            className = s("className"),
            section = s("section"),
            gender = s("gender").ifEmpty { "All" },
            session = s("session"),
            subject = s("subject"),
            examType = s("examType"),
            examDate = s("examDate"),
            returnDate = s("returnDate"),
            status = s("status").ifEmpty { AwardListData.STATUS_ACTIVE },
            studentIds = ids,
            marks = marks,
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
