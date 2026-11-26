#!/usr/bin/env python3
"""Generate TopicQuestions.kt from the verified topic map JSON.

Source of truth: ~/workspace/topic_mockups/phy9_ch1_topic_map.json
(Milestone 4 pilot, verified 102/102 questions, 13 topics, 2 ambiguous Longs).
Output: app TopicQuestions.kt in the m5_work checkout.
Sub-topic lists are SUBSETS of the topic lists (never double-counted).
"""
import json

SRC = "/home/hatch/workspace/topic_mockups/phy9_ch1_topic_map.json"
OUT = "/home/hatch/workspace/m5_work/app/src/main/kotlin/com/example/studypoint/practicetest/TopicQuestions.kt"

m = json.load(open(SRC))
topics = m["topics"]
amb = m["ambiguous"]
amb_longs = sorted(int(a["id"]) for a in amb if a["tab"] == "Longs")


def ids(v):
    return sorted(int(x) for x in v)


def kt_set(v):
    xs = ids(v)
    if not xs:
        return "emptySet()"
    return "setOf(" + ", ".join(str(x) for x in xs) + ")"


lines = []
lines.append("package com.example.studypoint.practicetest")
lines.append("")
lines.append("/**")
lines.append(" * Topic-wise question map for the Paper Generator and MCQ Practice.")
lines.append(" *")
lines.append(" * PILOT (Milestone 5, v158): Physics, Class 9, Chapter 1")
lines.append(" * \"Physical Quantities and Measurements\" only. Other chapters and")
lines.append(" * classes keep the current chapter-level lists — topicsFor() returns")
lines.append(" * null for them, and callers fall back to the flat view.")
lines.append(" *")
lines.append(" * Source of truth: ~/workspace/topic_mockups/phy9_ch1_topic_map.json")
lines.append(" * (Milestone 4, verified 102/102 questions against the live sheet).")
lines.append(" * Topic structure is the CURRENT PCTB book (13 sections, 1.1-1.13):")
lines.append(" * prefixes live inside 1.3 SI Units (no separate section), measuring")
lines.append(" * instruments split by quantity (1.5 Length, 1.6 Mass, 1.7 Time,")
lines.append(" * 1.8 Volume), and there is NO Density section. Sub-topic lists are")
lines.append(" * SUBSETS of their topic lists — a question is never counted twice.")
lines.append(" * The two ambiguous Longs (2237, 6246) belong to NO topic and are")
lines.append(" * shown at chapter level via chapterLevelLongIds().")
lines.append(" */")
lines.append("data class SubtopicDef(")
lines.append("    val name: String,")
lines.append("    val mcqIds: Set<Int> = emptySet(),")
lines.append("    val shortIds: Set<Int> = emptySet(),")
lines.append("    val longIds: Set<Int> = emptySet()")
lines.append(")")
lines.append("")
lines.append("data class TopicDef(")
lines.append("    val number: String,")
lines.append("    val title: String,")
lines.append("    val mcqIds: Set<Int> = emptySet(),")
lines.append("    val shortIds: Set<Int> = emptySet(),")
lines.append("    val longIds: Set<Int> = emptySet(),")
lines.append("    val subtopics: List<SubtopicDef> = emptyList()")
lines.append(")")
lines.append("")
lines.append("object TopicQuestions {")
lines.append("")
lines.append("    /** Physics-9 Chapter 1 topics, in book order (1.1-1.13). */")
lines.append("    val phy9Ch1Topics: List<TopicDef> = listOf(")
for num in sorted(topics.keys(), key=lambda s: [int(p) for p in s.split(".")]):
    t = topics[num]
    lines.append(f"        TopicDef(")
    lines.append(f"            number = \"{num}\",")
    lines.append(f"            title = \"{t['title']}\",")
    lines.append(f"            mcqIds = {kt_set(t.get('mcq', []))},")
    lines.append(f"            shortIds = {kt_set(t.get('short', []))},")
    lines.append(f"            longIds = {kt_set(t.get('long', []))},")
    subs = t.get("subtopics", {})
    if subs:
        lines.append("            subtopics = listOf(")
        for name, sv in subs.items():
            lines.append(
                f"                SubtopicDef(\"{name}\", "
                f"{kt_set(sv.get('mcq', []))}, "
                f"{kt_set(sv.get('short', []))}, "
                f"{kt_set(sv.get('long', []))}),"
            )
        lines.append("            )")
    lines.append("        ),")
lines.append("    )")
lines.append("")
lines.append("    /** Longs that belong to no topic (shown at chapter level). */")
lines.append(
    f"    val phy9Ch1ChapterLongIds: Set<Int> = {kt_set(amb_longs)}"
)
lines.append("")
lines.append("    /**")
lines.append("     * Topics for a chapter, or null when the chapter has no topic map.")
lines.append("     * Pilot scope: Class 9 Physics Chapter 1 only.")
lines.append("     */")
lines.append("    fun topicsFor(classDigits: String, subject: String, chapterNumber: Int?): List<TopicDef>? =")
lines.append("        if (classDigits == \"9\" && subject.trim().equals(\"Physics\", ignoreCase = true) && chapterNumber == 1)")
lines.append("            phy9Ch1Topics else null")
lines.append("")
lines.append("    /** Longs shown at chapter level (no topic) for a chapter. */")
lines.append("    fun chapterLevelLongIds(classDigits: String, subject: String, chapterNumber: Int?): Set<Int> =")
lines.append("        if (classDigits == \"9\" && subject.trim().equals(\"Physics\", ignoreCase = true) && chapterNumber == 1)")
lines.append("            phy9Ch1ChapterLongIds else emptySet()")
lines.append("")
lines.append("    /** MCQ ids of one topic (for MCQ Practice), or empty when unknown. */")
lines.append("    fun mcqIdsForTopic(classDigits: String, subject: String, chapterNumber: Int?, topicNumber: String): Set<Int> =")
lines.append("        topicsFor(classDigits, subject, chapterNumber)")
lines.append("            ?.firstOrNull { it.number == topicNumber }?.mcqIds ?: emptySet()")
lines.append("}")
lines.append("")

open(OUT, "w").write("\n".join(lines))
print("wrote", OUT)
print("topics:", len(topics), "| ambiguous longs:", amb_longs)
# sanity: totals
tm = sum(len(ids(t.get("mcq", []))) for t in topics.values())
ts = sum(len(ids(t.get("short", []))) for t in topics.values())
tl = sum(len(ids(t.get("long", []))) for t in topics.values())
print(f"assigned: MCQ {tm} / Short {ts} / Long {tl} = {tm+ts+tl} (+ {len(amb_longs)} ambiguous = {tm+ts+tl+len(amb_longs)})")
