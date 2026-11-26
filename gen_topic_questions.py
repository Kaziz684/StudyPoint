#!/usr/bin/env python3
"""Generate TopicQuestions.kt from the verified topic maps — ALL FOUR CLASSES.

Final app bundle (Milestone 6, v160): Class 9 (103 chapters) + Class 10
(126 files) + Class 11 (138 files) + Class 12 (73 files) = 440 map files,
2,573 topics, 38,520 questions (MCQ 19,063 / Short 13,392 / Long 6,065),
653 ambiguous chapter-level items.

The APP wires 437 of the 440 files (2,551 topics, 38,412 questions):
  MCQ 19,063 (18,787 topic + 276 ambiguous) /
  Short 13,311 (13,094 topic + 217 ambiguous) /
  Long 6,038 (5,879 topic + 159 ambiguous).
Three maps are WEBSITE-ONLY and are deliberately NOT in the app — their
rows have a BLANK chapter_number in the sheet, so the app (which addresses
chapters by number) has no chapter to attach them to; they stay visible in
the app's flat view, exactly like the English-9 Grammar/Vocabulary bank
rows (Phase 2):
  - eng12_mrchips (81 Shorts + 1 ambiguous, topics MC.1-MC.10)
  - eng12_bank_essays (17 Longs, topics E.1-E.10)
  - urdu12_letters (10 Longs, topics L.1-L.2)
One (class, subject, chapter) is covered by TWO maps and is MERGED into a
single chapter entry (asserted below — the only such case):
  - (12, English, 13): eng12_ch13 (new-book "The Pearl", topics 13.1-13.6)
    + eng12_old_unit13 (old-book Unit 13, topics 13.1-13.4). Both file under
    chapter_number 13 in the sheet. The merged list keeps both sets of
    topics with their original numbers, so numbers 13.1-13.4 appear twice
    (different titles). KNOWN EDGE (documented, not hidden): in MCQ
    Practice, mcqIdsForTopic() looks a topic up by number and returns the
    FIRST match — the old-book 13.1-13.4 have no MCQs of their own, so
    picking one returns the new-book 13.1's MCQs instead of "Coming Soon".
    The Paper Generator shows both topic sets completely. The old-book
    rows are flagged OLD PCTB 2020 in the Phase 4 summary; dropping them
    is a user decision, not taken here.

Usage: python3 gen_topic_questions.py <output-TopicQuestions.kt-path>
The output path is a required argument (no default work dir).
Sub-topic lists are SUBSETS of the topic lists (never double-counted).
Ambiguous items belong to NO topic and are shown at chapter level via
chapterLevelMcqIds() / chapterLevelLongIds() / chapterLevelShortIds().

Id spaces are PER TAB (MCQ 534 and Short 534 are different rows) and
PER CLASS (Class_9th and Class_10th share 5,097 MCQ ids; the same pattern
applies to 11/12) — the app never looks an MCQ up by bare id: every
lookup goes through topicsFor(classDigits, subject, chapterNumber), so a
Class-10 MCQ can never resolve to a Class-9 chapter (the Phase 3 website
bug, fixed there by keying on (classNum, id); the app is safe by
construction). Overlap / duplicate checks compare within one (class, tab).

Two deliberate choices (v153 lesson, kept from v159/v160):
- Id sets use the compact ids("...") string form (AlpQuestions.kt
  precedent), NOT setOf(...) literals: 38,412 literal ints would blow the
  JVM 64 KiB static-initializer limit.
- Chapter topic lists are private FUNCTIONS, not vals: 2,551 TopicDef
  constructions as val initializers would put all of the data in the
  object's <clinit>, again risking the 64 KiB method limit. A function
  body is compiled separately, so <clinit> stays near-empty. Largest
  single chapter is 25 topics (tarj10_ch2, tarj12_ch4) — no per-method
  size risk either. Callers only use topicsFor() / chapterLevel*() /
  mcqIdsForTopic(), whose signatures are unchanged.
- Single TopicQuestions.kt (not split by class): ~1.5 MB, ~32k lines.
  One object, one dispatcher, one generator — the repo generator must
  reproduce the shipped file byte-identically (the v159 lesson).

Ambiguous tab spellings are INCONSISTENT across maps ("MCQ" vs
"Class_9th" / "Class_10th" / "Class_11th" / "Class_12th" for MCQs) — all
are accepted here. Topic numbers are usually dotted numerics ("1.1");
two app maps carry a letter suffix ("15.3b", "9.8b") — sort_key handles
those (numeric prefix first, then the suffix).
"""
import json
import os
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: gen_topic_questions.py <output-TopicQuestions.kt-path>")

MAPS_DIR = "/home/hatch/workspace/topic_mockups"
OUT = sys.argv[1]

# File stem -> chapter number for maps whose file name carries no chapter.
# (Chapter numbers verified against the sheet's chapter_number values and
# the maps' own topic numbers, e.g. eng10 grammar topics 11.1-19.2.)
NONCH = {
    "eng10_grammar_active_passive_voice": 11,
    "eng10_grammar_correct_use_of_verb": 12,
    "eng10_grammar_direct_indirect_speech": 13,
    "eng10_grammar_pair_of_words": 14,
    "eng10_grammar_parts_of_speech_sentence_types": 15,
    "eng10_grammar_prepositions": 16,
    "eng10_grammar_pronunciation": 17,
    "eng10_grammar_spelling": 18,
    "eng10_grammar_vocabulary": 19,
    "eng10_bank_essays": 21,
    "eng10_bank_paragraphs": 22,
    "eng10_bank_translation_urdu_english": 23,
    "eng10_bank_translation_english_urdu": 24,
    "eng10_bank_poem_summaries": 25,
    "eng10_bank_direct_indirect_speech": 26,
    "eng10_bank_pair_of_words": 27,
    "eng10_bank_paraphrase": 28,
    "eng11_grammar_correct_use_of_verb": 17,
    "eng11_bank_stories": 21,
    "eng11_bank_letters": 22,
    "eng11_bank_applications": 23,
    "eng11_bank_pair_of_words": 24,
    # Blank chapter_number in the sheet -> website-only, NOT in the app.
    "eng12_mrchips": None,
    "eng12_bank_essays": None,
    "urdu12_letters": None,
}

SUBJ = {"phy": "physics", "bio": "biology", "chem": "chemistry",
        "math": "math", "cs": "computer science", "eng": "english",
        "urdu": "urdu", "isl": "islamiyat", "pak": "pak studies",
        "tarj": "tarjuma-tul-quran", "stat": "statistics",
        "gensci": "general science"}

AMB_TABS = {"mcq": ("MCQ", "Class_9th", "Class_10th", "Class_11th",
                    "Class_12th"),
            "short": ("Shorts",), "long": ("Longs",)}


def ids(v):
    return sorted(int(x) for x in v)


def kt_ids(v):
    xs = ids(v)
    if not xs:
        return 'ids("")'
    return 'ids("' + ",".join(str(x) for x in xs) + '")'


def kt_str(s):
    out = str(s).replace("\\", "\\\\").replace('"', '\\"')
    out = out.replace("$", "${'$'}")
    return '"' + out + '"'


def sort_key(num):
    # Dotted numerics ("1.1", "15.3b"): numeric prefix first, then suffix.
    # Never raises — a non-numeric part sorts after numerics.
    parts = []
    for p in str(num).split("."):
        m = re.match(r"(\d+)(.*)$", p)
        if m:
            parts.append((0, int(m.group(1)), m.group(2)))
        else:
            parts.append((1, 0, p))
    return tuple(parts)


def fn_name(stem, ch):
    """Builder function name from a file stem + chapter: phy9_ch1 -> phy9Ch1;
    eng10_grammar_active_passive_voice (ch 11) -> eng10GrammarActivePassiveVoiceCh11."""
    base = re.sub(r"_ch\d+$", "", stem)
    parts = base.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:]) + f"Ch{ch}"


# ---- scan + classify all map files -------------------------------------
# (class, subject, chapter) -> [(file stem, map json)] in filename order
chapters = {}
excluded = []
scanned = 0
for path in sorted(os.listdir(MAPS_DIR)):
    if not path.endswith("_topic_map.json"):
        continue
    stem = path[:-len("_topic_map.json")]
    scanned += 1
    m = re.match(r"([a-z]+)(9|10|11|12)_ch(\d+)$", stem)
    ou = re.match(r"eng12_old_unit(\d+)$", stem)
    if m:
        slug, cls, ch = m.group(1), m.group(2), int(m.group(3))
        subj = SUBJ[slug]
    elif ou:
        cls, subj, ch = "12", "english", int(ou.group(1))
    elif stem in NONCH:
        cls = re.search(r"(9|10|11|12)", stem).group(1)
        slug = re.match(r"([a-z]+)\d", stem).group(1)
        subj = SUBJ[slug]
        ch = NONCH[stem]
        if ch is None:
            excluded.append(stem)
            continue
    else:
        raise SystemExit(f"unrecognised map file: {stem}")
    with open(os.path.join(MAPS_DIR, path), encoding="utf-8") as f:
        chapters.setdefault((cls, subj, ch), []).append((stem, json.load(f)))

assert scanned == 440, scanned
assert sorted(excluded) == ["eng12_bank_essays", "eng12_mrchips",
                            "urdu12_letters"], excluded
assert len(chapters) == 437 - 1, len(chapters)  # 437 files, 1 merged chapter
merges = {k: v for k, v in chapters.items() if len(v) > 1}
assert list(merges.keys()) == [("12", "english", 13)], merges
assert [s for s, _ in merges[("12", "english", 13)]] == [
    "eng12_ch13", "eng12_old_unit13"], merges

lines = []
lines.append("package com.example.studypoint.practicetest")
lines.append("")
lines.append("/**")
lines.append(" * Topic-wise question map for the Paper Generator and MCQ Practice.")
lines.append(" *")
lines.append(" * Milestone 6 final bundle (v160): ALL FOUR CLASSES — Class 9 (103")
lines.append(" * chapters, 546 topics), Class 10 (126 files, 715 topics), Class 11")
lines.append(" * (138 files, 820 topics), Class 12 (73 files, 492 topics). 440 map")
lines.append(" * files in total; the app wires 437 of them (2,551 topics, 38,412")
lines.append(" * questions: MCQ 19,063 / Short 13,311 / Long 6,038, incl. 652")
lines.append(" * ambiguous chapter-level items: 276 MCQ, 217 Shorts, 159 Longs).")
lines.append(" * Three maps are website-only (blank chapter_number in the sheet —")
lines.append(" * eng12_mrchips, eng12_bank_essays, urdu12_letters — 108 questions);")
lines.append(" * they stay visible in the app's flat view. One chapter is merged")
lines.append(" * from two maps: (12, English, 13) = eng12_ch13 + eng12_old_unit13")
lines.append(" * (old-book Unit 13 files under the same chapter_number 13); topic")
lines.append(" * numbers 13.1-13.4 therefore appear twice (different titles) —")
lines.append(" * see the generator header for the known MCQ Practice edge this")
lines.append(" * creates. Other chapters, subjects and classes keep the current")
lines.append(" * chapter-level lists — topicsFor() returns null for them, and")
lines.append(" * callers fall back to the flat view.")
lines.append(" *")
lines.append(" * Source of truth: ~/workspace/topic_mockups/<slug>_ch<N>_topic_map.json")
lines.append(" * (Milestone 6 Phases 1-4, verified against the live sheet). Sub-topic")
lines.append(" * lists are SUBSETS of their topic lists — a question is never")
lines.append(" * counted twice. Ambiguous items belong to NO topic and are shown at")
lines.append(" * chapter level via chapterLevelMcqIds() / chapterLevelLongIds() /")
lines.append(" * chapterLevelShortIds(). Id spaces are per tab AND per class — every")
lines.append(" * lookup goes through topicsFor(classDigits, subject, chapterNumber),")
lines.append(" * so a Class-10 MCQ can never resolve to a Class-9 chapter.")
lines.append(" *")
lines.append(" * Id sets use the compact ids(\"...\") string form (AlpQuestions.kt")
lines.append(" * precedent): 38,412 literal ints would blow the JVM 64 KiB")
lines.append(" * static-initializer limit (the v153 lesson). Chapter topic lists")
lines.append(" * are private functions, not vals, for the same reason.")
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
lines.append("    /** Parse a comma-separated id list into a Set (see class doc). */")
lines.append("    private fun ids(s: String): Set<Int> =")
lines.append("        if (s.isEmpty()) emptySet() else s.split(\",\").map { it.toInt() }.toSet()")
lines.append("")
lines.append("    /** Normalise a UI/sheet subject string (mirrors the website's")
lines.append("     *  normSubjectWeb and AlpQuestions' subject handling). */")
lines.append("    private fun normSubject(s: String): String {")
lines.append("        val t = s.trim().lowercase()")
lines.append("        return when (t) {")
lines.append("            \"mathematics\", \"math\" -> \"math\"")
lines.append("            \"computer science\", \"computer\", \"cs\" -> \"computer science\"")
lines.append("            \"tarjuma-tul-quran\", \"tarjuma\", \"quran\" -> \"tarjuma-tul-quran\"")
lines.append("            \"pak studies\", \"pakistan studies\", \"pak study\" -> \"pak studies\"")
lines.append("            else -> t")
lines.append("        }")
lines.append("    }")
lines.append("")

tot_topics = tot_mcq = tot_short = tot_long = 0
tot_amb_mcq = tot_amb_short = tot_amb_long = 0
tot_files = 0
# Per (class, subject) chapter lists, in order, for the dispatchers.
dispatch_table = {}  # cls -> subject -> [(chapter, fn)]
for (cls, subj, ch) in sorted(chapters.keys(),
                              key=lambda k: (int(k[0]), k[1], k[2])):
    files = chapters[(cls, subj, ch)]
    # Merge: concatenate topics (filename order) + ambiguous lists.
    topics = []  # (number, topic dict, source stem)
    amb = []
    for stem, m in files:
        for num in sorted(m["topics"].keys(), key=sort_key):
            topics.append((num, m["topics"][num], stem))
        amb.extend(m.get("ambiguous", []))
    # Per-file checks (before merge): topic-number uniqueness, subtopic
    # subsets, ambiguous-vs-assigned disjoint per type, known tab spellings.
    for stem, m in files:
        assert len(m["topics"]) == len(set(m["topics"].keys())), (stem, "dup topic number")
        for num, t in m["topics"].items():
            for name, s in (t.get("subtopics") or {}).items():
                for k in ("mcq", "short", "long"):
                    assert set(int(x) for x in s.get(k, [])) <= set(
                        int(x) for x in t.get(k, [])
                    ), (stem, num, name, k)
    amb_mcq = sorted(int(a["id"]) for a in amb if a["tab"] in AMB_TABS["mcq"])
    amb_short = sorted(int(a["id"]) for a in amb if a["tab"] in AMB_TABS["short"])
    amb_long = sorted(int(a["id"]) for a in amb if a["tab"] in AMB_TABS["long"])
    assert len(amb_mcq) + len(amb_short) + len(amb_long) == len(amb), (cls, subj, ch, amb)
    for k, amb_k in (("mcq", amb_mcq), ("short", amb_short), ("long", amb_long)):
        assigned_k = set()
        for _num, t, _stem in topics:
            assigned_k.update(int(x) for x in t.get(k, []))
        assert not set(amb_k) & assigned_k, (cls, subj, ch, k, "ambiguous id also assigned")
        # No duplicate ids at topic level within the merged chapter.
        seen = set()
        for _num, t, _stem in topics:
            for x in t.get(k, []):
                assert int(x) not in seen, (cls, subj, ch, k, x, "duplicate id")
                seen.add(int(x))
    # Builder function name from the first file's stem (eng12_ch13 for the
    # merged chapter) + chapter number.
    stem0 = files[0][0]
    fn = fn_name(stem0, ch)
    lines.append(f"    /** {subj} Class {cls} Chapter {ch} topics, in book order. */")
    lines.append(f"    private fun {fn}Topics(): List<TopicDef> = listOf(")
    for num, t, _stem in topics:
        lines.append("        TopicDef(")
        lines.append(f"            number = {kt_str(num)},")
        lines.append(f"            title = {kt_str(t['title'])},")
        lines.append(f"            mcqIds = {kt_ids(t.get('mcq', []))},")
        lines.append(f"            shortIds = {kt_ids(t.get('short', []))},")
        lines.append(f"            longIds = {kt_ids(t.get('long', []))},")
        subs = t.get("subtopics", {})
        if subs:
            lines.append("            subtopics = listOf(")
            for name, sv in subs.items():
                lines.append(
                    f"                SubtopicDef({kt_str(name)}, "
                    f"{kt_ids(sv.get('mcq', []))}, "
                    f"{kt_ids(sv.get('short', []))}, "
                    f"{kt_ids(sv.get('long', []))}),"
                )
            lines.append("            )")
        lines.append("        ),")
    lines.append("    )")
    lines.append("")
    lines.append("    /** MCQs that belong to no topic (shown at chapter level). */")
    lines.append(f"    private fun {fn}ChapterMcqIds(): Set<Int> = {kt_ids(amb_mcq)}")
    lines.append("")
    lines.append("    /** Longs that belong to no topic (shown at chapter level). */")
    lines.append(f"    private fun {fn}ChapterLongIds(): Set<Int> = {kt_ids(amb_long)}")
    lines.append("")
    lines.append("    /** Shorts that belong to no topic (shown at chapter level). */")
    lines.append(f"    private fun {fn}ChapterShortIds(): Set<Int> = {kt_ids(amb_short)}")
    lines.append("")
    tot_topics += len(topics)
    tot_mcq += sum(len(ids(t.get("mcq", []))) for _n, t, _s in topics)
    tot_short += sum(len(ids(t.get("short", []))) for _n, t, _s in topics)
    tot_long += sum(len(ids(t.get("long", []))) for _n, t, _s in topics)
    tot_amb_mcq += len(amb_mcq)
    tot_amb_short += len(amb_short)
    tot_amb_long += len(amb_long)
    tot_files += len(files)
    dispatch_table.setdefault(cls, {}).setdefault(subj, []).append((ch, fn))

# Expected grand totals (all 440 maps minus the 3 website-only maps;
# coordinators verified live, parent re-counted from the JSONs):
# 437 files, 2,551 topics, 38,412 questions
# (MCQ 19,063 / Short 13,311 / Long 6,038), ambiguous (276, 217, 159).
assert tot_files == 437, tot_files
assert tot_topics == 2551, tot_topics
assert (tot_mcq + tot_amb_mcq,
        tot_short + tot_amb_short,
        tot_long + tot_amb_long) == (19063, 13311, 6038), (
    tot_mcq + tot_amb_mcq, tot_short + tot_amb_short, tot_long + tot_amb_long)
assert (tot_amb_mcq, tot_amb_short, tot_amb_long) == (276, 217, 159), (
    tot_amb_mcq, tot_amb_short, tot_amb_long)


def dispatch(fun, ret, empty_ret):
    """Emit a class+subject+chapter dispatcher (topicsFor / chapterLevel*)."""
    lines.append(f"    fun {fun}(classDigits: String, subject: String, chapterNumber: Int?): {ret} {{")
    lines.append("        return when (classDigits) {")
    for cls in ("9", "10", "11", "12"):
        lines.append(f"            \"{cls}\" -> when (normSubject(subject)) {{")
        for subj in sorted(dispatch_table[cls].keys()):
            lines.append(f"                {kt_str(subj)} -> when (chapterNumber) {{")
            for ch, fn in dispatch_table[cls][subj]:
                if fun == "topicsFor":
                    lines.append(f"                    {ch} -> {fn}Topics()")
                elif fun == "chapterLevelMcqIds":
                    lines.append(f"                    {ch} -> {fn}ChapterMcqIds()")
                elif fun == "chapterLevelLongIds":
                    lines.append(f"                    {ch} -> {fn}ChapterLongIds()")
                elif fun == "chapterLevelShortIds":
                    lines.append(f"                    {ch} -> {fn}ChapterShortIds()")
            lines.append(f"                    else -> {empty_ret}")
            lines.append("                }")
        lines.append(f"                else -> {empty_ret}")
        lines.append("            }")
    lines.append(f"            else -> {empty_ret}")
    lines.append("        }")
    lines.append("    }")
    lines.append("")


lines.append("    /**")
lines.append("     * Topics for a chapter, or null when the chapter has no topic map.")
lines.append("     * Milestone 6 final bundle scope: Classes 9-12, all mapped")
lines.append("     * subjects/chapters (437 files; 3 website-only maps excluded —")
lines.append("     * see the class doc).")
lines.append("     */")
dispatch("topicsFor", "List<TopicDef>?", "null")
lines.append("    /** MCQs shown at chapter level (no topic) for a chapter. */")
dispatch("chapterLevelMcqIds", "Set<Int>", "emptySet()")
lines.append("    /** Longs shown at chapter level (no topic) for a chapter. */")
dispatch("chapterLevelLongIds", "Set<Int>", "emptySet()")
lines.append("    /** Shorts shown at chapter level (no topic) for a chapter. */")
dispatch("chapterLevelShortIds", "Set<Int>", "emptySet()")
lines.append("    /** MCQ ids of one topic (for MCQ Practice), or empty when unknown. */")
lines.append("    fun mcqIdsForTopic(classDigits: String, subject: String, chapterNumber: Int?, topicNumber: String): Set<Int> =")
lines.append("        topicsFor(classDigits, subject, chapterNumber)")
lines.append("            ?.firstOrNull { it.number == topicNumber }?.mcqIds ?: emptySet()")
lines.append("}")
lines.append("")

os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w").write("\n".join(lines))
print("wrote", OUT)
print(f"files={tot_files} chapters={len(chapters)} topics={tot_topics} "
      f"mcq={tot_mcq} short={tot_short} long={tot_long} "
      f"chapterMcqs={tot_amb_mcq} chapterLongs={tot_amb_long} chapterShorts={tot_amb_short}")
print(f"assigned total={tot_mcq + tot_short + tot_long} "
      f"(+ {tot_amb_mcq + tot_amb_short + tot_amb_long} ambiguous = "
      f"{tot_mcq + tot_short + tot_long + tot_amb_mcq + tot_amb_short + tot_amb_long})")
