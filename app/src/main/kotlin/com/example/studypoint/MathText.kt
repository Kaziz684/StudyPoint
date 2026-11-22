// MathText.kt — maths renderer for the StudyPoint app (v154).
// Port of ~/workspace/mathrender/proto.py (v4) to Kotlin, with the v5
// edge-case fixes applied in Kotlin (single-letter fraction parts allowed,
// trig/log function + argument as a denominator, signed bare tokens).
// Sheet plain text -> structured markup -> Spanned with real stacked
// fractions (FractionSpan), radicals with overline (RadicalSpan),
// superscripts and subscripts. Works in a TextView and in StaticLayout
// (PaperPdfGenerator), because the spans are ReplacementSpans measured
// with StaticLayout, not WebView/KaTeX.
//
// Verification status:
//   * The markup stage is a line-by-line port of proto.py, which was validated
//     on the live sheet (2,933 math cells, 925 with math tokens, 0 leftover ^,
//     0 bare √). Expected markup for the 29 test inputs lives in
//     ~/workspace/mathrender/expected_markup.txt — the Kotlin output should
//     be diffed against it (CI build is the compile check; there is no
//     kotlinc on this VM).
//   * Wiring: QuizActivity (question + options), QuizResultActivity (review),
//     PaperGeneratorActivity mcqRow/writtenRow (picker), PaperPdfGenerator
//     (main paper) and TestPdfGenerator. Board-pattern PDFs and Word export
//     (OMML) are NOT wired in v154; website KaTeX comes after the app.
//
// Usage:
//   tvQuestionText.text = MathText.render(q.questionText)
//   button.text = MathText.render(options[i])

package com.example.studypoint

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ReplacementSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import kotlin.math.ceil
import kotlin.math.max

object MathText {

    // ---- Character tables (identical to proto.py SUP / SUB) ----
    private val SUP: Map<Char, Char> = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        'ⁿ' to 'n', 'ˣ' to 'x', '⁻' to '-', '⁺' to '+', 'ⁱ' to 'i',
        'ᵗ' to 't', 'ᵃ' to 'a', 'ᵇ' to 'b', 'ᶜ' to 'c', 'ᵈ' to 'd',
        'ᵉ' to 'e', 'ʰ' to 'h', 'ʲ' to 'j', 'ᵏ' to 'k', 'ˡ' to 'l',
        'ᵐ' to 'm', 'ᵒ' to 'o', 'ᵖ' to 'p', 'ʳ' to 'r', 'ˢ' to 's',
        'ᵘ' to 'u', 'ᵛ' to 'v', 'ʷ' to 'w', 'ʸ' to 'y', 'ᶻ' to 'z'
    )
    private val SUB: Map<Char, Char> = mapOf(
        '₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4',
        '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9',
        '₋' to '-', '₊' to '+', 'ₐ' to 'a', 'ₑ' to 'e', 'ₒ' to 'o',
        'ₓ' to 'x', 'ₔ' to 'd', 'ₕ' to 'h', 'ₖ' to 'k', 'ₗ' to 'l',
        'ₘ' to 'm', 'ₙ' to 'n', 'ₚ' to 'p', 'ₛ' to 's', 'ₜ' to 't'
    )

    private const val OVER: Char = '\u0305' // combining overline (radicand bar)
    private const val FRAC: Char = '\u2044' // fraction slash ⁄
    private const val MARK: Char = '\u0000' // internal fraction placeholder
    private const val GREEK: String =
        "αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"

    // v5 note: single letters (a, b, x) ARE allowed as fraction parts; only
    // multi-letter English words are rejected.
    private val STOP: Set<String> = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "or",
        "and", "of", "to", "in", "on", "by", "for", "with", "as", "at",
        "from", "into", "over", "under", "per", "via", "if", "so", "do",
        "go", "up", "us", "we", "he", "she", "it", "its", "this", "that",
        "then", "than", "them", "they"
    )

    private val BARE_RE = Regex(
        "^([A-Za-z" + GREEK + "][A-Za-z0-9" + GREEK + "]*" +
            "|\\d+[A-Za-z0-9" + GREEK + "]*|\\d+\\.\\d+)$"
    )
    private val SIGNED_NUMBER_RE = Regex("[+-]?\\d+(\\.\\d+)?")
    private val TRIPLE_FRACTION_RE = Regex("\\d+/\\d+/\\d+")

    /** Public entry point: sheet plain text -> renderable Spanned. */
    fun render(raw: String): Spanned = buildSpanned(toMarkup(raw))

    /** Stage 1: sheet plain text -> structured markup
     *  (<s> superscript, <b> subscript, <f>a|b</f> fraction, <r> root).
     *  Faithful port of proto.py convert(). */
    fun toMarkup(raw: String): String = convert(raw).first

    // ---- helpers (ported from proto.py) ----

    private fun normBare(unit: String): String =
        unit.map { SUP[it] ?: SUB[it] ?: it }.joinToString("")

    private fun isBare(unit: String): Boolean =
        unit.isNotEmpty() && BARE_RE.matches(normBare(unit)) &&
            !(unit.length >= 2 && unit.lowercase() in STOP)

    private fun matchParen(s: String, i: Int): Int {
        var d = 0
        for (j in i until s.length) {
            if (s[j] == '(') d++
            else if (s[j] == ')') {
                d--
                if (d == 0) return j + 1
            }
        }
        return -1
    }

    private fun matchBrace(s: String, i: Int): Int {
        var d = 0
        for (j in i until s.length) {
            if (s[j] == '{') d++
            else if (s[j] == '}') {
                d--
                if (d == 0) return j + 1
            }
        }
        return -1
    }

    private fun isAtomChar(c: Char): Boolean =
        c.isLetterOrDigit() || c in GREEK

    private fun identBack(s: String, k0: Int): Int {
        var k = k0
        while (k >= 0 && isAtomChar(s[k])) k--
        return k + 1
    }

    /** caret = index of '^'. Returns (base, start) for the atom just before it. */
    private fun grabBaseBeforeCaret(s: String, caret: Int): Pair<String, Int> {
        var j = caret - 1
        while (j >= 0 && s[j] == ' ') j--
        if (j < 0) return "" to caret
        if (s[j] == ')') {
            var d = 0
            var k = j
            while (k >= 0) {
                if (s[k] == ')') d++
                else if (s[k] == '(') {
                    d--
                    if (d == 0) {
                        val st = identBack(s, k - 1)
                        return s.substring(st, j + 1) to st
                    }
                }
                k--
            }
            return "" to caret
        }
        var k = j
        while (k >= 0 && (isAtomChar(s[k]) || s[k] in SUP || s[k] in SUB)) k--
        val unit = s.substring(k + 1, j + 1)
        if (isBare(unit)) return unit to (k + 1)
        return "" to caret
    }

    /** Atom immediately before index i (i is the '/' position). */
    private fun grabBack(s: String, i: Int): Pair<String, Int> {
        var j = i - 1
        while (j >= 0 && s[j] == ' ') j--
        if (j < 0) return "" to i
        if (s[j] == ')') {
            var d = 0
            var k = j
            while (k >= 0) {
                if (s[k] == ')') d++
                else if (s[k] == '(') {
                    d--
                    if (d == 0) {
                        val st = identBack(s, k - 1)
                        return s.substring(st, j + 1) to st
                    }
                }
                k--
            }
            return "" to i
        }
        if (s[j] == '√') {
            var k = j + 1
            val rad = StringBuilder()
            while (k + 1 < s.length && s[k + 1] == OVER) {
                rad.append(s[k]); k += 2
            }
            if (rad.isNotEmpty()) return s.substring(j, k) to j
            if (k < s.length && s[k].isLetterOrDigit()) return s.substring(j, k + 1) to j
            return s.substring(j, j + 1) to j
        }
        var k = j
        while (k >= 0 && (isAtomChar(s[k]) || s[k] in SUP || s[k] in SUB)) k--
        val unit = s.substring(k + 1, j + 1)
        if (isBare(unit)) {
            var u = unit
            var st = k + 1
            // Signed number: -5 / 7 -> include the leading sign.
            if (st >= 1 && (s[st - 1] == '+' || s[st - 1] == '-') &&
                (st - 1 == 0 || s[st - 2] in " (,=<")
            ) {
                u = s[st - 1] + u; st--
            }
            // Exponent before it? A^2 / B -> numerator is A^2.
            if (st >= 2 && s[st - 1] == '^') {
                val (base, bs) = grabBaseBeforeCaret(s, st - 1)
                if (base.isNotEmpty()) return (base + "^" + u) to bs
            }
            return u to st
        }
        return "" to i
    }

    /** Atom immediately after index i (i is just past the '/'). */
    private fun grabFwd(s: String, i: Int): Pair<String, Int> {
        var j = i
        while (j < s.length && s[j] == ' ') j++
        if (j >= s.length) return "" to i
        // Signed token: / -7 -> -7.
        var sign = ""
        if ((s[j] == '+' || s[j] == '-') && j + 1 < s.length &&
            (isAtomChar(s[j + 1]) || s[j + 1] in SUP || s[j + 1] in SUB)
        ) {
            sign = s[j].toString(); j++
        }
        if (s[j] == '√') {
            var k = j + 1
            val rad = StringBuilder()
            while (k + 1 < s.length && s[k + 1] == OVER) {
                rad.append(s[k]); k += 2
            }
            if (rad.isNotEmpty()) return (sign + s.substring(j, k)) to k
            if (k < s.length && s[k].isLetterOrDigit()) return (sign + s.substring(j, k + 1)) to (k + 1)
            return (sign + s.substring(j, j + 1)) to (j + 1)
        }
        if (s[j] == '(') {
            val e = matchParen(s, j)
            return if (e > 0) (sign + s.substring(j, e)) to e else "" to i
        }
        var k = j
        while (k < s.length && isAtomChar(s[k])) k++
        if (k < s.length && s[k] == '(') {
            val e = matchParen(s, k)
            if (e > 0) return (sign + s.substring(j, e)) to e
        }
        // Trig/log function + argument: sin A, cos alpha, log 2.
        val ident = s.substring(j, k)
        if (ident.lowercase() in setOf(
                "sin", "cos", "tan", "sec", "cosec", "csc", "cot", "log", "ln", "lim"
            )
        ) {
            var m = k
            while (m < s.length && s[m] == ' ') m++
            if (m < s.length && isAtomChar(s[m])) {
                var n = m
                while (n < s.length && isAtomChar(s[n])) n++
                return (sign + s.substring(j, n)) to n
            }
            if (m < s.length && s[m] == '(') {
                val e = matchParen(s, m)
                if (e > 0) return (sign + s.substring(j, e)) to e
            }
        }
        k = j
        while (k < s.length && (isAtomChar(s[k]) || s[k] in SUP || s[k] in SUB)) k++
        val unit = s.substring(j, k)
        if (isBare(unit)) return (sign + unit) to k
        return "" to i
    }

    private fun fracPass(t: String): String {
        var out = t
        repeat(6) {
            var idx = out.indexOf('/')
            var changed = false
            while (idx >= 0) {
                val head = out.substring(max(0, idx - 4), minOf(out.length, idx + 8))
                val urlGuard = out.substring(max(0, idx - 10), idx)
                if (TRIPLE_FRACTION_RE.find(head)?.range?.first == 0 ||
                    "http" in urlGuard
                ) {
                    idx = out.indexOf('/', idx + 1); continue
                }
                val (num, ns) = grabBack(out, idx)
                val (den, de) = grabFwd(out, idx + 1)
                if (num.isNotEmpty() && den.isNotEmpty()) {
                    out = out.substring(0, ns) + MARK + "{" + num + "|" + den + "}" +
                        MARK + out.substring(de)
                    changed = true
                    idx = out.indexOf('/', ns + 1)
                } else {
                    idx = out.indexOf('/', idx + 1)
                }
            }
            if (!changed) return out
        }
        return out
    }

    private fun supRun(s: String, i: Int): Pair<String, Int> {
        var j = i
        val o = StringBuilder()
        while (j < s.length && s[j] in SUP) {
            o.append(SUP.getValue(s[j])); j++
        }
        return o.toString() to j
    }

    private fun subRun(s: String, i: Int): Pair<String, Int> {
        var j = i
        val o = StringBuilder()
        while (j < s.length && s[j] in SUB) {
            o.append(SUB.getValue(s[j])); j++
        }
        return o.toString() to j
    }

    /** Core converter: returns (markup, tokenCount). Port of proto.py convert(). */
    private fun convert(t0: String): Pair<String, Int> {
        val t = fracPass(t0)
        val out = StringBuilder()
        var i = 0
        var n = 0
        while (i < t.length) {
            val ch = t[i]
            when {
                ch == MARK -> {
                    val j = t.indexOf(MARK, i + 1)
                    if (j < 0) {
                        out.append(ch); i++; continue
                    }
                    val inner = t.substring(i + 2, j - 1)
                    val parts = inner.split("|", limit = 2)
                    val num = if (parts.isNotEmpty()) parts[0] else ""
                    val den = if (parts.size > 1) parts[1] else ""
                    out.append("<f>").append(convert(num).first)
                        .append("|").append(convert(den).first).append("</f>")
                    n++; i = j + 1; continue
                }
                ch in SUP -> {
                    val (num, j) = supRun(t, i)
                    if (j < t.length && t[j] == FRAC) {
                        val (den, k) = subRun(t, j + 1)
                        if (den.isNotEmpty()) {
                            val frac = "<f>$num|$den</f>"
                            if (i > 0 && (t[i - 1].isLetterOrDigit() || t[i - 1] == ')')) {
                                out.append("<s>").append(frac).append("</s>")
                            } else {
                                out.append(frac)
                            }
                            n++; i = k; continue
                        }
                    }
                    out.append("<s>").append(num).append("</s>"); n++; i = j; continue
                }
                ch in SUB -> {
                    val (den, j) = subRun(t, i)
                    out.append("<b>").append(den).append("</b>"); n++; i = j; continue
                }
                ch == '^' -> {
                    if (i + 1 < t.length && t[i + 1] == '{') {
                        val e = matchBrace(t, i + 1)
                        if (e > 0) {
                            out.append("<s>").append(convert(t.substring(i + 2, e - 1)).first)
                                .append("</s>"); n++; i = e; continue
                        }
                    }
                    if (i + 1 < t.length && t[i + 1] == '(') {
                        val e = matchParen(t, i + 1)
                        if (e > 0) {
                            out.append("<s>").append(convert(t.substring(i + 2, e - 1)).first)
                                .append("</s>"); n++; i = e; continue
                        }
                    }
                    if (i + 1 < t.length &&
                        (t[i + 1].isLetterOrDigit() || t[i + 1] == '+' || t[i + 1] == '-')
                    ) {
                        out.append("<s>").append(t[i + 1]).append("</s>"); n++; i += 2; continue
                    }
                    out.append(ch); i++; continue
                }
                ch == '√' -> {
                    if (i + 1 < t.length && t[i + 1] == '(') {
                        val e = matchParen(t, i + 1)
                        if (e > 0) {
                            out.append("<r>").append(convert(t.substring(i + 2, e - 1)).first)
                                .append("</r>"); n++; i = e; continue
                        }
                    }
                    if (i + 1 < t.length && t[i + 1] == MARK) {
                        val j = t.indexOf(MARK, i + 2)
                        if (j > 0) {
                            val inner = t.substring(i + 3, j - 1)
                            val parts = inner.split("|", limit = 2)
                            val num = if (parts.isNotEmpty()) parts[0] else ""
                            val den = if (parts.size > 1) parts[1] else ""
                            out.append("<r><f>").append(convert(num).first)
                                .append("|").append(convert(den).first)
                                .append("</f></r>"); n++; i = j + 1; continue
                        }
                    }
                    if (i + 1 < t.length && t[i + 1] in SUP) {
                        val (num, j) = supRun(t, i + 1)
                        if (j < t.length && t[j] == FRAC) {
                            val (den, k) = subRun(t, j + 1)
                            if (den.isNotEmpty()) {
                                out.append("<r><f>").append(num)
                                    .append("|").append(den).append("</f></r>")
                                n++; i = k; continue
                            }
                        }
                    }
                    // Overline radicand: √3̅  (char + combining overline, repeated)
                    var j = i + 1
                    val rad = StringBuilder()
                    while (j + 1 < t.length && t[j + 1] == OVER) {
                        rad.append(t[j]); j += 2
                    }
                    if (rad.isNotEmpty()) {
                        out.append("<r>").append(convert(rad.toString()).first)
                            .append("</r>"); n++; i = j; continue
                    }
                    // Signed number radicand: √-3, √2.5
                    val m = SIGNED_NUMBER_RE.find(t, i + 1)
                    if (m != null && m.range.first == i + 1) {
                        val end = i + 1 + m.value.length
                        if (end >= t.length || !t[end].isLetter()) {
                            out.append("<r>").append(m.value).append("</r>")
                            n++; i = end; continue
                        }
                    }
                    if (i + 1 < t.length && t[i + 1].isLetterOrDigit()) {
                        out.append("<r>").append(t[i + 1]).append("</r>"); n++; i += 2; continue
                    }
                    out.append(ch); i++; continue
                }
                ch == '(' -> {
                    val e = matchParen(t, i)
                    if (e > 0) {
                        out.append("(").append(convert(t.substring(i + 1, e - 1)).first)
                            .append(")"); i = e; continue
                    }
                    out.append(ch); i++; continue
                }
                else -> {
                    out.append(ch); i++
                }
            }
        }
        return out.toString() to n
    }

    // ---- Stage 2: markup -> Spanned ----

    private fun buildSpanned(markup: String): Spanned {
        val out = SpannableStringBuilder()
        parseInto(markup, 0, markup.length, out)
        return out
    }

    /** Finds the index just past the matching close tag, counting nesting of
     *  the same open tag. Returns -1 when unmatched. */
    private fun findMatchingEnd(s: String, from: Int, open: String, close: String): Int {
        var depth = 1
        var i = from
        while (i < s.length) {
            if (s.startsWith(open, i)) {
                depth++; i += open.length
            } else if (s.startsWith(close, i)) {
                depth--
                if (depth == 0) return i + close.length
                i += close.length
            } else {
                i++
            }
        }
        return -1
    }

    /** Index of the '|' that splits a fraction at nesting depth 0, or -1. */
    private fun topLevelPipe(s: String, from: Int, to: Int): Int {
        var depth = 0
        var i = from
        while (i < to) {
            if (s.startsWith("<f>", i)) {
                depth++; i += 3
            } else if (s.startsWith("</f>", i)) {
                depth--; i += 4
            } else if (s[i] == '|' && depth == 0) {
                return i
            } else {
                i++
            }
        }
        return -1
    }

    private fun parseInto(s: String, start: Int, end: Int, out: SpannableStringBuilder) {
        var i = start
        while (i < end) {
            when {
                s.startsWith("<s>", i) -> {
                    val close = findMatchingEnd(s, i + 3, "<s>", "</s>")
                    if (close < 0) {
                        out.append(s[i]); i++; continue
                    }
                    val from = out.length
                    parseInto(s, i + 3, close - 4, out)
                    out.setSpan(SuperscriptSpan(), from, out.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(RelativeSizeSpan(0.72f), from, out.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = close
                }
                s.startsWith("<b>", i) -> {
                    val close = findMatchingEnd(s, i + 3, "<b>", "</b>")
                    if (close < 0) {
                        out.append(s[i]); i++; continue
                    }
                    val from = out.length
                    parseInto(s, i + 3, close - 4, out)
                    out.setSpan(SubscriptSpan(), from, out.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(RelativeSizeSpan(0.72f), from, out.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = close
                }
                s.startsWith("<f>", i) -> {
                    val close = findMatchingEnd(s, i + 3, "<f>", "</f>")
                    if (close < 0) {
                        out.append(s[i]); i++; continue
                    }
                    val innerEnd = close - 4
                    val pipe = topLevelPipe(s, i + 3, innerEnd)
                    if (pipe < 0) {
                        out.append(s[i]); i++; continue
                    }
                    val num = SpannableStringBuilder()
                    parseInto(s, i + 3, pipe, num)
                    val den = SpannableStringBuilder()
                    parseInto(s, pipe + 1, innerEnd, den)
                    val at = out.length
                    out.append("\uFFFC")
                    out.setSpan(FractionSpan(num, den), at, at + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = close
                }
                s.startsWith("<r>", i) -> {
                    val close = findMatchingEnd(s, i + 3, "<r>", "</r>")
                    if (close < 0) {
                        out.append(s[i]); i++; continue
                    }
                    val radicand = SpannableStringBuilder()
                    parseInto(s, i + 3, close - 4, radicand)
                    val at = out.length
                    out.append("\uFFFC")
                    out.setSpan(RadicalSpan(radicand), at, at + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = close
                }
                else -> {
                    out.append(s[i]); i++
                }
            }
        }
    }
}

/** Measures a short (usually single-line) CharSequence with its spans kept,
 *  so a nested fraction / superscript inside a numerator still renders. */
private fun measureLayout(text: CharSequence, paint: TextPaint): StaticLayout {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(text, 0, text.length, paint, 10000)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(
            text, paint, 10000, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false
        )
    }
}

private fun contentWidth(layout: StaticLayout): Int {
    var w = 0f
    for (i in 0 until layout.lineCount) w = max(w, layout.getLineWidth(i))
    return ceil(w).toInt()
}

/**
 * Stacked fraction: numerator over a rule over denominator, centred.
 * Numerator/denominator are Spanned (they may themselves contain
 * superscripts or nested fractions), drawn at 72% of the base text size.
 * The span is vertically centred on the text baseline: the rule sits on
 * the baseline, numerator above it, denominator below it.
 */
class FractionSpan(
    private val numerator: CharSequence,
    private val denominator: CharSequence
) : ReplacementSpan() {

    private var numLayout: StaticLayout? = null
    private var denLayout: StaticLayout? = null
    private var width = 0
    private var numW = 0
    private var denW = 0
    private var numH = 0
    private var denH = 0
    private var gap = 2

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val p = TextPaint(paint)
        p.textSize = paint.textSize * 0.72f
        val nl = measureLayout(numerator, p)
        val dl = measureLayout(denominator, p)
        numLayout = nl
        denLayout = dl
        numW = contentWidth(nl)
        denW = contentWidth(dl)
        numH = nl.height
        denH = dl.height
        gap = max(2, (paint.textSize * 0.10f).toInt())
        width = max(numW, denW) + max(4, (paint.textSize * 0.12f).toInt())
        fm?.let {
            it.ascent = -(numH + gap / 2)
            it.descent = denH + gap / 2
            it.top = it.ascent
            it.bottom = it.descent
        }
        return width
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val nl = numLayout ?: return
        val dl = denLayout ?: return
        // Numerator: bottom edge gap/2 above the baseline.
        canvas.save()
        canvas.translate(x + (width - numW) / 2f, y - gap / 2f - numH)
        nl.draw(canvas)
        canvas.restore()
        // Rule on the baseline, spanning the wider part.
        val linePaint = Paint(paint)
        linePaint.strokeWidth = max(1f, paint.textSize * 0.045f)
        val half = max(numW, denW) / 2f
        val cx = x + width / 2f
        canvas.drawLine(cx - half, y.toFloat(), cx + half, y.toFloat(), linePaint)
        // Denominator: top edge gap/2 below the baseline.
        canvas.save()
        canvas.translate(x + (width - denW) / 2f, y + gap / 2f)
        dl.draw(canvas)
        canvas.restore()
    }
}

/**
 * Radical: a √ glyph, the radicand, and a vinculum (overline) above the
 * radicand. The radicand is Spanned, so a fraction under a root
 * (√(3/2)) keeps its stacked form.
 */
class RadicalSpan(
    private val radicand: CharSequence
) : ReplacementSpan() {

    private var radLayout: StaticLayout? = null
    private var radW = 0
    private var sqrtW = 0

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val p = TextPaint(paint)
        val rl = measureLayout(radicand, p)
        radLayout = rl
        radW = contentWidth(rl)
        sqrtW = ceil(paint.measureText("√")).toInt()
        // Grow the line only if the radicand (e.g. a fraction) is taller
        // than the surrounding font; otherwise leave the metrics alone.
        fm?.let {
            val lineH = it.descent - it.ascent
            if (rl.height > lineH) {
                val extra = rl.height - lineH
                it.ascent -= extra / 2
                it.descent += extra - extra / 2
                it.top = it.ascent
                it.bottom = it.descent
            }
        }
        return sqrtW + radW + 2
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val rl = radLayout ?: return
        canvas.drawText("√", x, y.toFloat(), paint)
        // Radicand baseline aligned with the surrounding baseline.
        canvas.save()
        canvas.translate(x + sqrtW, y + paint.ascent())
        rl.draw(canvas)
        canvas.restore()
        // Vinculum over the radicand.
        val linePaint = Paint(paint)
        linePaint.strokeWidth = max(1f, paint.textSize * 0.045f)
        val lineY = y + paint.ascent()
        canvas.drawLine(
            x + sqrtW, lineY, x + sqrtW + radW, lineY, linePaint
        )
    }
}
