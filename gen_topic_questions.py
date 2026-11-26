#!/usr/bin/env python3
"""Generate TopicQuestions.kt from the verified Physics-9 topic maps.

Source of truth: ~/workspace/topic_mockups/phy9_ch<N>_topic_map.json
(Ch1-Ch9, Milestone 6 Phase 1, verified: 88 topics, 908 questions —
MCQ 90 / Short 635 / Long 183, incl. 16 ambiguous chapter-level items:
9 Longs + 7 Shorts).

Usage: python3 gen_topic_questions.py <output-TopicQuestions.kt-path>
The output path is a required argument (no default work dir).
Sub-topic lists are SUBSETS of the topic lists (never double-counted).
Ambiguous items belong to NO topic and are shown at chapter level via
chapterLevelLongIds() / chapterLevelShortIds().
"""
import json
import os
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: gen_topic_questions.py <output-TopicQuestions.kt-path>")

MAPS_DIR = "/home/hatch/workspace/topic_mockups"
OUT = sys.argv[1]

CHAPTER_TITLES = {
    1: "Physical Quantities and Measurements",
    2: "Kinematics",
    3: "Dynamics",
    4: "Turning Effects of Force",
    5: "Work, Energy and Power",
    6: "Mechanical Properties of Matter",
    7: "Thermal Properties of Matter",
    8: "Magnetism",
    9: "Nature of Science",
}

maps = {}
for ch in range(1, 10):
    with open(os.path.join(MAPS_DIR, f"phy9_ch{ch}_topic_map.json"), encoding="utf-8") as f:
        maps[ch] = json.load(f)


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
lines.append(" * Milestone 6 Phase 1 (v159): Physics, Class 9, Chapters 1-9")
lines.append(" * (Kinematics, Dynamics, Turning Effects of Force, Work/Energy/Power,")
lines.append(" * Mechanical Properties, Thermal Properties, Magnetism, Nature of Science")
lines.append(" * + Ch1 Physical Quantities and Measurements). Other chapters, subjects")
lines.append(" * and classes keep the current chapter-level lists — topicsFor() returns")
lines.append(" * null for them, and callers fall back to the flat view.")
lines.append(" *")
lines.append(" * Source of truth: ~/workspace/topic_mockups/phy9_ch<N>_topic_map.json")
lines.append(" * (Milestone 4 pilot Ch1 + Milestone 6 Phase 1 Ch2-Ch9, verified against")
lines.append(" * the live sheet: 88 topics, 908 questions). Topic structure is the")
lines.append(" * CURRENT PCTB book (Ch1: 13 sections 1.1-1.13; Ch4 list textbook-")
lines.append(" * verified pp. 80-98). Sub-topic lists are SUBSETS of their topic")
lines.append(" * lists — a question is never counted twice. The 16 ambiguous items")
lines.append(" * (9 Longs + 7 Shorts) belong to NO topic and are shown at chapter")
lines.append(" * level via chapterLevelLongIds() / chapterLevelShortIds().")
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

tot_topics = tot_mcq = tot_short = tot_long = 0
tot_amb_long = tot_amb_short = 0
for ch in range(1, 10):
    m = maps[ch]
    topics = m["topics"]
    amb = m.get("ambiguous", [])
    amb_longs = sorted(int(a["id"]) for a in amb if a["tab"] == "Longs")
    amb_shorts = sorted(int(a["id"]) for a in amb if a["tab"] == "Shorts")
    assert len(amb_longs) + len(amb_shorts) == len(amb), (ch, amb)
    lines.append(f"    /** Physics-9 Chapter {ch} topics ({CHAPTER_TITLES[ch]}), in book order. */")
    lines.append(f"    val phy9Ch{ch}Topics: List<TopicDef> = listOf(")
    for num in sorted(topics.keys(), key=lambda s: [int(p) for p in s.split(".")]):
        t = topics[num]
        lines.append("        TopicDef(")
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
    lines.append(f"    val phy9Ch{ch}ChapterLongIds: Set<Int> = {kt_set(amb_longs)}")
    lines.append("")
    lines.append("    /** Shorts that belong to no topic (shown at chapter level). */")
    lines.append(f"    val phy9Ch{ch}ChapterShortIds: Set<Int> = {kt_set(amb_shorts)}")
    lines.append("")
    tot_topics += len(topics)
    tot_mcq += sum(len(ids(t.get("mcq", []))) for t in topics.values())
    tot_short += sum(len(ids(t.get("short", []))) for t in topics.values())
    tot_long += sum(len(ids(t.get("long", []))) for t in topics.values())
    tot_amb_long += len(amb_longs)
    tot_amb_short += len(amb_shorts)

# Expected totals (verified live by the Milestone 6 Phase 1 coordinator,
# re-counted from the JSON maps by the parent agent).
assert (tot_topics, tot_mcq, tot_short, tot_long) == (88, 90, 628, 174), (
    tot_topics, tot_mcq, tot_short, tot_long)
assert (tot_amb_long, tot_amb_short) == (9, 7), (tot_amb_long, tot_amb_short)

lines.append("    /**")
lines.append("     * Topics for a chapter, or null when the chapter has no topic map.")
lines.append("     * Milestone 6 Phase 1 scope: Class 9 Physics Chapters 1-9.")
lines.append("     */")
lines.append("    fun topicsFor(classDigits: String, subject: String, chapterNumber: Int?): List<TopicDef>? {")
lines.append("        if (classDigits != \"9\" || !subject.trim().equals(\"Physics\", ignoreCase = true)) return null")
lines.append("        return when (chapterNumber) {")
for ch in range(1, 10):
    lines.append(f"            {ch} -> phy9Ch{ch}Topics")
lines.append("            else -> null")
lines.append("        }")
lines.append("    }")
lines.append("")
lines.append("    /** Longs shown at chapter level (no topic) for a chapter. */")
lines.append("    fun chapterLevelLongIds(classDigits: String, subject: String, chapterNumber: Int?): Set<Int> {")
lines.append("        if (classDigits != \"9\" || !subject.trim().equals(\"Physics\", ignoreCase = true)) return emptySet()")
lines.append("        return when (chapterNumber) {")
for ch in range(1, 10):
    lines.append(f"            {ch} -> phy9Ch{ch}ChapterLongIds")
lines.append("            else -> emptySet()")
lines.append("        }")
lines.append("    }")
lines.append("")
lines.append("    /** Shorts shown at chapter level (no topic) for a chapter. */")
lines.append("    fun chapterLevelShortIds(classDigits: String, subject: String, chapterNumber: Int?): Set<Int> {")
lines.append("        if (classDigits != \"9\" || !subject.trim().equals(\"Physics\", ignoreCase = true)) return emptySet()")
lines.append("        return when (chapterNumber) {")
for ch in range(1, 10):
    lines.append(f"            {ch} -> phy9Ch{ch}ChapterShortIds")
lines.append("            else -> emptySet()")
lines.append("        }")
lines.append("    }")
lines.append("")
lines.append("    /** MCQ ids of one topic (for MCQ Practice), or empty when unknown. */")
lines.append("    fun mcqIdsForTopic(classDigits: String, subject: String, chapterNumber: Int?, topicNumber: String): Set<Int> =")
lines.append("        topicsFor(classDigits, subject, chapterNumber)")
lines.append("            ?.firstOrNull { it.number == topicNumber }?.mcqIds ?: emptySet()")
lines.append("}")
lines.append("")

os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w").write("\n".join(lines))
print("wrote", OUT)
print(f"chapters=9 topics={tot_topics} mcq={tot_mcq} short={tot_short} long={tot_long} "
      f"chapterLongs={tot_amb_long} chapterShorts={tot_amb_short}")
print(f"assigned total={tot_mcq + tot_short + tot_long} "
      f"(+ {tot_amb_long + tot_amb_short} ambiguous = "
      f"{tot_mcq + tot_short + tot_long + tot_amb_long + tot_amb_short})")
