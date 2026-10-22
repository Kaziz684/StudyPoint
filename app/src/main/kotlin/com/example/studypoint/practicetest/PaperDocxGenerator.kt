package com.example.studypoint.practicetest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * MS Word (.docx) twin of [PaperPdfGenerator]. A .docx is just a zip of XML
 * parts, so the document is assembled directly (no external library) with
 * the same school header, sections and answer keys as the PDF version.
 */
object PaperDocxGenerator {

    private const val NAVY = "0F172A"
    private const val TEXT = "1E293B"
    private const val MUTED = "64748B"
    private const val ANS = "065F46"
    private const val GREEN = "10B981"
    private const val GREY_LINE = "CBD5E1"
    private const val HEAD_SHADE = "D9E2F3"

    private val ROMAN = listOf("(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)", "(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(xix)", "(xx)", "(xxi)", "(xxii)", "(xxiii)", "(xxiv)", "(xxv)", "(xxvi)", "(xxvii)", "(xxviii)", "(xxix)", "(xxx)", "(xxxi)", "(xxxii)", "(xxxiii)", "(xxxiv)", "(xxxv)", "(xxxvi)", "(xxxvii)", "(xxxviii)", "(xxxix)", "(xl)")
    private fun roman(i: Int): String = if (i in ROMAN.indices) ROMAN[i] else "(${i + 1})"

    // Urdu-script papers (Urdu / Islamiyat / Tarjuma): paragraphs whose text is
    // Urdu/Arabic script become true RTL paragraphs (bidi + right aligned +
    // Nastaliq complex-script font) in Word, like real board papers. Latin
    // paragraphs in the same document stay LTR. Set per generate() call and
    // reset by writeDocx.
    private var docxRtlSubject = false

    private fun scriptRtl(text: String): Boolean =
        docxRtlSubject && text.any { it in '\u0600'..'\u06FF' }

    // ---------------- XML helpers ----------------

    private fun esc(s: String): String {
        // Strip characters that are illegal in XML 1.0 (control characters,
        // unpaired surrogates, U+FFFE/U+FFFF): a single one in a question or
        // answer makes MS Word refuse to open the whole file.
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val code = c.code
            when {
                c == '\t' || c == '\n' || c == '\r' -> sb.append(c)
                code >= 0x20 && code < 0xD800 && code != 0xFFFE && code != 0xFFFF -> sb.append(c)
                code in 0xD800..0xDBFF && i + 1 < s.length && s[i + 1].code in 0xDC00..0xDFFF -> {
                    sb.append(c); sb.append(s[i + 1]); i++
                }
            }
            i++
        }
        return sb.toString()
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /** One text run. [size] is in half-points (23 = 11.5pt). */
    private fun run(
        text: String, bold: Boolean = false, size: Int = 23,
        color: String = TEXT, font: String? = null, rtl: Boolean = false
    ): String {
        val rpr = StringBuilder("<w:rPr>")
        if (font != null || rtl) {
            rpr.append("<w:rFonts")
            if (font != null) {
                rpr.append(" w:ascii=\"").append(font)
                    .append("\" w:hAnsi=\"").append(font).append("\"")
            }
            if (rtl) rpr.append(" w:cs=\"Noto Nastaliq Urdu\"")
            rpr.append("/>")
        }
        if (bold) rpr.append("<w:b/>")
        rpr.append("<w:color w:val=\"").append(color).append("\"/>")
        rpr.append("<w:sz w:val=\"").append(size).append("\"/>")
        rpr.append("<w:szCs w:val=\"").append(size).append("\"/>")
        if (rtl) rpr.append("<w:rtl/>")
        rpr.append("</w:rPr>")
        return "<w:r>" + rpr + "<w:t xml:space=\"preserve\">" + esc(text) + "</w:t></w:r>"
    }

    private fun para(
        runs: String, align: String? = null, after: Int = 120, indent: Int = 0,
        tabs: Boolean = false, borderBottom: String? = null, borderSz: Int = 12,
        rtl: Boolean = false
    ): String {
        val ppr = StringBuilder("<w:pPr>")
        if (borderBottom != null) {
            ppr.append(
                "<w:pBdr><w:bottom w:val=\"single\" w:sz=\"" + borderSz +
                    "\" w:space=\"4\" w:color=\"" + borderBottom + "\"/></w:pBdr>"
            )
        }
        if (tabs) {
            ppr.append(
                "<w:tabs><w:tab w:val=\"left\" w:pos=\"2475\"/>" +
                    "<w:tab w:val=\"left\" w:pos=\"4950\"/>" +
                    "<w:tab w:val=\"left\" w:pos=\"7425\"/></w:tabs>"
            )
        }
        if (rtl) ppr.append("<w:bidi/>")
        ppr.append("<w:spacing w:after=\"").append(after).append("\"/>")
        if (indent > 0) ppr.append("<w:ind w:left=\"").append(indent).append("\"/>")
        if (align != null) ppr.append("<w:jc w:val=\"").append(align).append("\"/>")
        else if (rtl) ppr.append("<w:jc w:val=\"right\"/>")
        ppr.append("</w:pPr>")
        return "<w:p>" + ppr + runs + "</w:p>"
    }

    private fun pText(
        text: String, bold: Boolean = false, size: Int = 23, color: String = TEXT,
        align: String? = null, after: Int = 120, indent: Int = 0, font: String? = null,
        borderBottom: String? = null, borderSz: Int = 12, rtl: Boolean = false
    ): String {
        val effRtl = rtl || scriptRtl(text)
        return para(
            run(text, bold, size, color, font, effRtl), align, after, indent,
            borderBottom = borderBottom, borderSz = borderSz, rtl = effRtl
        )
    }

    /** Paragraph whose leading [prefix] is bold and the rest regular. */
    private fun pBoldPrefix(
        prefix: String, rest: String, size: Int = 23, color: String = TEXT, after: Int = 120,
        rtl: Boolean = false
    ): String {
        val effRtl = rtl || scriptRtl(rest)
        return para(
            run(prefix, bold = true, size = size, color = color, rtl = effRtl) +
                run(rest, size = size, color = color, rtl = effRtl),
            after = after, rtl = effRtl
        )
    }

    private fun pageBreakPara(): String = "<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>"

    private fun tabRun(): String = "<w:r><w:tab/></w:r>"

    private fun imageRun(relId: String, emu: Long): String =
        "<w:r><w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
            "<wp:extent cx=\"" + emu + "\" cy=\"" + emu + "\"/>" +
            "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>" +
            "<wp:docPr id=\"1\" name=\"School Logo\"/>" +
            "<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect=\"1\"/></wp:cNvGraphicFramePr>" +
            "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
            "<pic:pic><pic:nvPicPr><pic:cNvPr id=\"0\" name=\"logo.png\"/><pic:cNvPicPr/></pic:nvPicPr>" +
            "<pic:blipFill><a:blip r:embed=\"" + relId + "\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
            "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + emu + "\" cy=\"" + emu + "\"/></a:xfrm>" +
            "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>" +
            "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"

    // ---------------- Tables ----------------

    private class Cell(
        val paras: List<String>,
        val span: Int = 1,
        val shade: String? = null,
        val width: Int = 0
    )

    private fun textCell(
        text: String, bold: Boolean = false, size: Int = 22, color: String = TEXT,
        span: Int = 1, shade: String? = null, width: Int = 0, rtl: Boolean = false
    ): Cell {
        val effRtl = rtl || scriptRtl(text)
        return Cell(
            listOf(para(run(text, bold, size, color, rtl = effRtl), after = 40, rtl = effRtl)),
            span, shade, width
        )
    }

    private fun table(widths: List<Int>, rows: List<List<Cell>>, borderColor: String = "475569", borderSz: Int = 6, rtl: Boolean = false): String {
        val sb = StringBuilder()
        val total = widths.sum()
        sb.append("<w:tbl><w:tblPr>")
        if (rtl) sb.append("<w:bidiVisual/>")
        sb.append("<w:tblW w:w=\"").append(total).append("\" w:type=\"dxa\"/>")
        sb.append("<w:tblBorders>")
        for (edge in listOf("top", "left", "bottom", "right", "insideH", "insideV")) {
            sb.append("<w:").append(edge).append(" w:val=\"single\" w:sz=\"").append(borderSz)
                .append("\" w:space=\"0\" w:color=\"").append(borderColor).append("\"/>")
        }
        sb.append("</w:tblBorders></w:tblPr><w:tblGrid>")
        for (w in widths) sb.append("<w:gridCol w:w=\"").append(w).append("\"/>")
        sb.append("</w:tblGrid>")
        for (row in rows) {
            sb.append("<w:tr>")
            var colIdx = 0
            for (cell in row) {
                val cellW = if (cell.width > 0) cell.width else
                    (colIdx until minOf(colIdx + cell.span, widths.size)).sumOf { widths[it] }
                colIdx += cell.span
                sb.append("<w:tc><w:tcPr><w:tcW w:w=\"").append(cellW)
                    .append("\" w:type=\"dxa\"/>")
                if (cell.span > 1) sb.append("<w:gridSpan w:val=\"").append(cell.span).append("\"/>")
                if (cell.shade != null) {
                    sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"")
                        .append(cell.shade).append("\"/>")
                }
                sb.append("</w:tcPr>")
                for (p in cell.paras) sb.append(p)
                sb.append("</w:tc>")
            }
            sb.append("</w:tr>")
        }
        sb.append("</w:tbl>")
        return sb.toString()
    }

    // ---------------- Shared paper pieces ----------------

    private fun logoPng(context: Context, header: PaperPdfGenerator.SchoolHeader): ByteArray? {
        if (header.logoPath.isNotBlank()) {
            try {
                val bmp = BitmapFactory.decodeFile(header.logoPath)
                if (bmp != null) return pngBytes(bmp)
            } catch (e: Exception) { /* fall through to the app icon */ }
        }
        return try {
            val d: Drawable = context.packageManager.getApplicationIcon(context.packageName)
            val bmp = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            d.setBounds(0, 0, 112, 112)
            d.draw(cv)
            pngBytes(bmp)
        } catch (e: Exception) {
            null
        }
    }

    private fun pngBytes(bmp: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    /** School banner + paper title + info grid — same content as the PDF header. */
    private fun headerXml(
        header: PaperPdfGenerator.SchoolHeader, title: String, subject: String,
        classLabel: String, timeText: String, totalMarksText: String,
        chaptersLabel: String?, hasLogo: Boolean
): String {
        docxRtlSubject = PaperSections.isRtlSubject(subject)
        val sb = StringBuilder()
        val nameParas = mutableListOf<String>()
        nameParas += para(
            run(header.schoolName.uppercase(), bold = true, size = 40, color = NAVY, font = "Cambria"),
            align = "center", after = 40
        )
        if (header.address.isNotBlank()) {
            nameParas += para(run(header.address, size = 20, color = MUTED), align = "center", after = 20)
        }
        if (header.phone.isNotBlank()) {
            nameParas += para(run(header.phone, bold = true, size = 20, color = MUTED), align = "center", after = 0)
        }
        val logoCell = if (hasLogo) {
            Cell(listOf(para(imageRun("rId2", 711200), align = "center", after = 0)), width = 1100)
        } else {
            Cell(listOf(para("", after = 0)), width = 1100)
        }
        sb.append(
            table(
                listOf(1100, 8800),
                listOf(listOf(logoCell, Cell(nameParas, width = 8800))),
                borderColor = NAVY, borderSz = 12
            )
        )
        sb.append(pText("", size = 10, after = 60))
        sb.append(
            pText(title.uppercase(), bold = true, size = 28, color = NAVY, align = "center", after = 140)
        )

        fun infoCell(label: String, value: String, width: Int): Cell = Cell(
            listOf(
                para(run(label, bold = true, size = 17, color = NAVY), after = 0),
                para(run(value, size = 21), after = 0)
            ),
            width = width
        )
        val widths = listOf(2970, 2178, 2376, 2376)
        val dateText = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val rows = mutableListOf<List<Cell>>()
        rows += listOf(
            infoCell("SUBJECT:", subject, widths[0]),
            infoCell("CLASS:", classLabel, widths[1]),
            infoCell("ROLL NO:", "______________", widths[2]),
            infoCell("PAPER CODE:", header.paperCode.ifBlank { "____________" }, widths[3])
        )
        rows += listOf(
            infoCell("TIME:", timeText, widths[0]),
            infoCell("TOTAL MARKS:", totalMarksText, widths[1]),
            infoCell("OBTAINED MARKS:", "____________", widths[2]),
            infoCell("DATE:", dateText, widths[3])
        )
        rows += listOf(
            Cell(
                listOf(
                    para(run("STUDENT NAME:", bold = true, size = 17, color = NAVY), after = 0),
                    para(run("____________________________________________________________", size = 21), after = 0)
                ),
                span = 4, width = 9900
            )
        )
        sb.append(table(widths, rows))
        sb.append(pText("", size = 10, after = 40))
        if (!chaptersLabel.isNullOrBlank()) {
            sb.append(pText("Chapter(s):  " + chaptersLabel, size = 21, after = 100))
        }
        return sb.toString()
    }

    private fun instructionsXml(text: String): String =
        pText(text, size = 21, color = MUTED, after = 180, borderBottom = GREY_LINE, borderSz = 6)

    private fun bannerXml(text: String): String = pText(
        text, bold = true, size = 28, color = NAVY, align = "center",
        after = 160, borderBottom = GREEN, borderSz = 10
    )

    private fun qHeadingXml(text: String): String =
        pText(text, bold = true, size = 25, color = NAVY, after = 110)

    private fun orXml(): String =
        pText("OR", bold = true, size = 23, color = NAVY, align = "center", after = 100)

    /** MCQ table for a run of 4-option questions; numbering starts at [startIndex]. */
    private fun mcqTableXml(mcqs: List<Question>, startIndex: Int): String {
        val widths = listOf(700, 4700, 1125, 1125, 1125, 1125)
        val rows = mutableListOf<List<Cell>>()
        rows += listOf(
            textCell("Q. No", bold = true, size = 21, shade = HEAD_SHADE, width = widths[0]),
            textCell("Question", bold = true, size = 21, shade = HEAD_SHADE, width = widths[1]),
            textCell("A", bold = true, size = 21, shade = HEAD_SHADE, width = widths[2]),
            textCell("B", bold = true, size = 21, shade = HEAD_SHADE, width = widths[3]),
            textCell("C", bold = true, size = 21, shade = HEAD_SHADE, width = widths[4]),
            textCell("D", bold = true, size = 21, shade = HEAD_SHADE, width = widths[5])
        )
        mcqs.forEachIndexed { i, q ->
            rows += listOf(
                textCell((startIndex + i + 1).toString(), size = 21, width = widths[0]),
                textCell(q.questionText, size = 21, width = widths[1]),
                textCell(q.options.getOrElse(0) { "" }, size = 21, width = widths[2]),
                textCell(q.options.getOrElse(1) { "" }, size = 21, width = widths[3]),
                textCell(q.options.getOrElse(2) { "" }, size = 21, width = widths[4]),
                textCell(q.options.getOrElse(3) { "" }, size = 21, width = widths[5])
            )
        }
        return table(widths, rows, rtl = docxRtlSubject) + pText("", size = 10, after = 80)
    }

    /** MCQs exactly like the PDF: table rows where a question has 4 options,
     *  the stacked layout otherwise. */
    private fun mcqsXml(mcqs: List<Question>): String {
        val sb = StringBuilder()
        var i = 0
        while (i < mcqs.size) {
            if (mcqs[i].options.size == 4) {
                val group = mutableListOf<Question>()
                var j = i
                while (j < mcqs.size && mcqs[j].options.size == 4) {
                    group += mcqs[j]; j++
                }
                sb.append(mcqTableXml(group, i))
                i = j
            } else {
                val q = mcqs[i]
                sb.append(pText((i + 1).toString() + ".  " + q.questionText, size = 23, after = 40))
                q.options.forEachIndexed { oi, opt ->
                    sb.append(pText("        " + ('A' + oi) + ")   " + opt, size = 22, after = 40))
                }
                sb.append(pText("", size = 10, after = 80))
                i++
            }
        }
        return sb.toString()
    }

    /** "N.  L" answer entries, four per line — same as the PDF key. */
    private fun keyEntriesXml(mcqs: List<Question>): String {
        val sb = StringBuilder()
        val entries = mcqs.mapIndexed { idx, q ->
            val letter = if (q.correctAnswerIndex in q.options.indices) {
                ('A' + q.correctAnswerIndex).toString()
            } else "?"
            (idx + 1).toString() + ".  " + letter
        }
        entries.chunked(4).forEach { row ->
            var runs = ""
            row.forEachIndexed { col, text ->
                if (col > 0) runs += tabRun()
                runs += run(text, size = 23)
            }
            sb.append(para(runs, after = 70, tabs = true))
        }
        return sb.toString()
    }

    private fun keyTitleXml(text: String): String = pageBreakPara() + pText(
        text, bold = true, size = 30, color = NAVY, align = "center",
        after = 200, borderBottom = GREEN, borderSz = 12
    )

    // ---------------- Custom paper ----------------

    fun generate(context: Context, data: PaperPdfGenerator.PaperData, outFile: File): File {
        docxRtlSubject = PaperSections.isRtlSubject(data.subject)
        val logo = logoPng(context, data.header)
        val totalMarks = data.mcqs.size +
            data.shorts.sumOf { it.marksValue } + data.longs.sumOf { it.marksValue }
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                data.timeAllowed, totalMarks.toString(), data.chaptersLabel, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key and solutions are given at the end."
                } else {
                    "Instructions: Attempt all questions. Write your answers neatly in the answer copy."
                }
            )
        )

        var qCounter = 0
        var mcqQ = 0
        var shortQ = 0
        var longQ = 0
        fun qHeading(text: String) {
            sb.append(pText(text, bold = true, size = 25, color = NAVY, after = 120))
        }

        if (data.mcqs.isNotEmpty()) {
            qCounter++; mcqQ = qCounter
            qHeading("Q." + mcqQ + "  Choose the correct option:   (" + data.mcqs.size + " x 1 = " + data.mcqs.size + ")")
            sb.append(mcqsXml(data.mcqs))
        }

        fun writtenSection(questions: List<WrittenQuestion>, heading: String, isShort: Boolean) {
            if (questions.isEmpty()) return
            qCounter++
            if (isShort) shortQ = qCounter else longQ = qCounter
            qHeading("Q." + qCounter + "  " + heading)
            questions.forEachIndexed { idx, wq ->
                val marksSuffix = if (wq.marks.isNotBlank()) "   (Marks: " + wq.marks + ")" else ""
                sb.append(
                    pBoldPrefix(
                        roman(idx) + "  ", wq.question + marksSuffix,
                        after = 130
                    )
                )
            }
        }
        writtenSection(data.shorts, "Write short answers to the following questions:", true)
        writtenSection(data.longs, "Write detailed answers to the following questions:", false)

        if (data.includeAnswers && (data.mcqs.isNotEmpty() || data.shorts.isNotEmpty() || data.longs.isNotEmpty())) {
            sb.append(keyTitleXml("ANSWER KEY & SOLUTIONS"))
            if (data.mcqs.isNotEmpty()) {
                sb.append(pText("Q." + mcqQ + "  MCQs \u2014 Answer Key", bold = true, size = 26, color = NAVY, after = 120))
                sb.append(keyEntriesXml(data.mcqs))
            }
            fun solutionSection(questions: List<WrittenQuestion>, title: String) {
                if (questions.isEmpty()) return
                sb.append(pText(title, bold = true, size = 26, color = NAVY, after = 120))
                questions.forEachIndexed { idx, wq ->
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(roman(idx) + "  ", wq.answer, size = 22, color = ANS, after = 130))
                    } else {
                        sb.append(pText(roman(idx) + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 130))
                    }
                }
            }
            solutionSection(data.shorts, "Q." + shortQ + "  Short Questions \u2014 Answers")
            solutionSection(data.longs, "Q." + longQ + "  Long Questions \u2014 Solutions")
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 9 Computer Science) ----------------

    fun generateBoardPatternCs(context: Context, data: PaperPdfGenerator.CsBoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours", "50", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (10 x 1 = 10)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("Time Allowed: 1 Hour 45 Minutes   •   Total Marks: 40", bold = true, size = 22, color = NAVY, after = 140))

        data.shortSections.forEachIndexed { sIdx, section ->
            sb.append(qHeadingXml("Q." + (sIdx + 2) + "  Write short answers to any FOUR (4) questions.   (2 x 4 = 8)"))
            section.forEachIndexed { i, wq ->
                sb.append(pText(roman(i) + "  " + wq.question, after = 100))
            }
        }

        sb.append(qHeadingXml("Write detailed answers to any TWO (2) questions.   (2 x 8 = 16)"))
        data.longs.forEachIndexed { i, wq ->
            sb.append(qHeadingXml("Q." + (i + 5) + "  " + wq.question))
        }

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))
            data.shortSections.forEachIndexed { sIdx, section ->
                sb.append(qHeadingXml("Q." + (sIdx + 2) + "  (Short Answers)"))
                section.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            sb.append(qHeadingXml("Long Questions  (Answers)"))
            data.longs.forEachIndexed { i, wq ->
                val label = "Q." + (i + 5) + "  "
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(label, wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(label + "(Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (60 marks — Phy/Bio 9&10, Pak St 10, Chem 9) ----------------

    fun generateBoardPattern60(context: Context, data: PaperPdfGenerator.SixtyBoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours", "60", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (12 x 1 = 12)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("Time Allowed: 1 Hour 45 Minutes   •   Total Marks: 48", bold = true, size = 22, color = NAVY, after = 140))

        data.shortSections.forEachIndexed { sIdx, section ->
            sb.append(qHeadingXml("Q." + (sIdx + 2) + "  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)"))
            section.forEachIndexed { i, wq ->
                sb.append(pText(roman(i) + "  " + wq.question, after = 100))
            }
        }

        sb.append(qHeadingXml("Write detailed answers to any TWO (2) questions.   (2 x 9 = 18)"))
        data.longs.forEachIndexed { i, wq ->
            sb.append(qHeadingXml("Q." + (i + 5) + "  " + wq.question))
        }

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))
            data.shortSections.forEachIndexed { sIdx, section ->
                sb.append(qHeadingXml("Q." + (sIdx + 2) + "  (Short Answers)"))
                section.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            sb.append(qHeadingXml("Long Questions  (Answers)"))
            data.longs.forEachIndexed { i, wq ->
                val label = "Q." + (i + 5) + "  "
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(label, wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(label + "(Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 10 Tarjuma-tul-Quran) ----------------

    fun generateBoardPatternTq(context: Context, data: PaperPdfGenerator.TqBoardPaperData, outFile: File): File {
        docxRtlSubject = PaperSections.isRtlSubject(data.subject)
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours", "50", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (10 x 1 = 10)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("Time Allowed: 1 Hour 45 Minutes   •   Total Marks: 40", bold = true, size = 22, color = NAVY, after = 140))

        sb.append(qHeadingXml("Q.2  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)"))
        data.shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(qHeadingXml("Q.3  Write the meanings of any FIVE (5) of the following words.   (1 x 5 = 5)"))
        val meanEntries = data.meanings.mapIndexed { idx, m -> roman(idx) + "  " + m.word }
        meanEntries.chunked(2).forEach { row ->
            var runs = ""
            row.forEachIndexed { col, text ->
                if (col > 0) runs += tabRun()
                runs += run(text, size = 23)
            }
            sb.append(para(runs, after = 70, tabs = true))
        }

        sb.append(qHeadingXml("Q.4  Translate the following verses into idiomatic Urdu. Attempt any THREE (3).   (3 x 5 = 15)"))
        data.ayat.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(qHeadingXml("Q.5  Write a detailed answer to any ONE (1) of the following questions.   (1 x 10 = 10)"))
        data.longs.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))
            sb.append(qHeadingXml("Q.2  (Short Answers)"))
            data.shorts.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
            sb.append(qHeadingXml("Q.3  (Word Meanings)"))
            data.meanings.forEachIndexed { i, m ->
                sb.append(pBoldPrefix(roman(i) + "  ", m.word + " — " + m.meaning, size = 22, color = ANS, after = 90))
            }
            sb.append(qHeadingXml("Q.4  (Translations)"))
            data.ayat.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
            sb.append(qHeadingXml("Q.5  (Detailed Answer)"))
            data.longs.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 9/10 Urdu) ----------------

    /** Urdu Board Pattern — same BISE structure as the PDF twin; every
     *  Urdu paragraph is a true RTL Nastaliq paragraph via the rtl helpers. */
    fun generateBoardPatternUrdu(context: Context, data: PaperPdfGenerator.UrduBoardPaperData, outFile: File): File {
        docxRtlSubject = true
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours 10 Minutes", "75", null, logo != null
            )
        )
        sb.append(instructionsXml("نوٹ: تمام سوالات کے جوابات اپنی جوابی کاپی پر صاف اور خوشخط تحریر کیجیے۔ ہر سوال کے سامنے اس کے نمبر درج ہیں۔"))

        sb.append(bannerXml("حصہ معروضی (Objective)"))
        sb.append(qHeadingXml("سوال نمبر 1: درست جواب کا انتخاب کیجیے۔ (15x1=15)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("حصہ انشائیہ (Subjective)"))

        fun items(list: List<WrittenQuestion>) {
            list.forEachIndexed { i, wq ->
                sb.append(pText(roman(i) + "  " + wq.question, after = 100))
            }
        }
        fun ansItems(list: List<WrittenQuestion>) {
            list.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
        }

        sb.append(qHeadingXml("سوال نمبر 2: درج ذیل اشعار کی تشریح کیجیے۔ (10=2x5)"))
        sb.append(pText("(الف)  " + data.q2aLabel, bold = true, size = 23, color = NAVY, after = 90))
        items(data.q2a)
        sb.append(pText("(ب)  " + data.q2bLabel, bold = true, size = 23, color = NAVY, after = 90))
        items(data.q2b)

        sb.append(qHeadingXml("سوال نمبر 3: درج ذیل عبارات کی تشریح کیجیے اور مصنف و کتاب کا حوالہ بھی تحریر کیجیے۔ (10=5+5)"))
        data.q3.forEachIndexed { i, wq ->
            sb.append(pText((if (i == 0) "(الف)" else "(ب)") + "  " + wq.question, after = 100))
        }

        sb.append(qHeadingXml("سوال نمبر 4: درج ذیل سوالات میں سے کوئی سے پانچ کے مختصر جوابات تحریر کیجیے۔ (10=2x5)"))
        items(data.q4)

        sb.append(qHeadingXml("سوال نمبر 5: درج ذیل میں سے کسی ایک سبق کا خلاصہ تحریر کیجیے۔ (5)"))
        items(data.q5)

        if (data.q6Fallback) {
            val poem = data.q6.firstOrNull()?.chapterName ?: ""
            sb.append(qHeadingXml("سوال نمبر 6: نظم «$poem» کا مرکزی خیال تحریر کیجیے اور شاعر کا نام بھی لکھیں۔ (5=1+4)"))
        } else {
            sb.append(qHeadingXml("سوال نمبر 6: درج ذیل میں سے کسی ایک نظم کا مرکزی خیال تحریر کیجیے اور شاعر کا نام بھی لکھیں۔ (5=1+4)"))
            items(data.q6)
        }

        if (data.q7Kind == "MAZMOON") {
            sb.append(qHeadingXml("سوال نمبر 7: درج ذیل عنوانات میں سے کسی ایک پر جامع مضمون لکھیں (200 تا 250 الفاظ)۔ (10)"))
        } else {
            sb.append(qHeadingXml("سوال نمبر 7: درج ذیل میں سے کوئی ایک خط یا درخواست تحریر کیجیے۔ (10)"))
        }
        items(data.q7)

        sb.append(qHeadingXml("سوال نمبر 8: درج ذیل میں سے کوئی ایک سوال حل کیجیے۔ (5)"))
        items(data.q8)

        sb.append(qHeadingXml("سوال نمبر 9: درج ذیل محاوروں کو جملوں میں استعمال کیجیے (کوئی سے " + data.q9Attempt + ")۔ (5)"))
        items(data.q9)

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY & SOLUTIONS"))
            sb.append(qHeadingXml("سوال نمبر 1 (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))
            sb.append(qHeadingXml("سوال نمبر 2 (الف) — تشریح اشعار"))
            ansItems(data.q2a)
            sb.append(qHeadingXml("سوال نمبر 2 (ب) — تشریح اشعار"))
            ansItems(data.q2b)
            sb.append(qHeadingXml("سوال نمبر 3 — تشریح عبارات"))
            ansItems(data.q3)
            sb.append(qHeadingXml("سوال نمبر 4 — مختصر جوابات"))
            ansItems(data.q4)
            sb.append(qHeadingXml("سوال نمبر 5 — خلاصہ"))
            ansItems(data.q5)
            sb.append(qHeadingXml("سوال نمبر 6 — مرکزی خیال"))
            ansItems(data.q6)
            sb.append(qHeadingXml("سوال نمبر 7"))
            ansItems(data.q7)
            sb.append(qHeadingXml("سوال نمبر 8"))
            ansItems(data.q8)
            sb.append(qHeadingXml("سوال نمبر 9 — محاورے"))
            ansItems(data.q9)
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 10 English) ----------------

    fun generateBoardPattern(context: Context, data: PaperPdfGenerator.BoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours 30 Minutes", "75", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (19 x 1 = 19)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("SECTION-I", bold = true, size = 25, color = NAVY, after = 110))

        sb.append(qHeadingXml("Q.2  Answer the following questions in your own words. (Attempt any FIVE)   (Marks: 10)"))
        data.shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(pText("SECTION-II", bold = true, size = 25, color = NAVY, after = 110))

        sb.append(qHeadingXml("Q.3  Translate the following paragraph into Urdu:   (Marks: 08)"))
        sb.append(pText(data.q3.text, after = 120))
        sb.append(orXml())
        sb.append(pText("Rewrite the above paragraph in simple English.", after = 140))

        sb.append(qHeadingXml("Q.4  Write the summary of the poem \"" + data.poemSummary + "\":   (Marks: 05)"))
        sb.append(orXml())
        sb.append(pText("Paraphrase the following lines of the poem \"" + data.paraphrase.poem + "\":", after = 100))
        data.paraphrase.lines.split("\n").filter { it.isNotBlank() }.forEach { line ->
            sb.append(pText(line, after = 40, indent = 720))
        }
        sb.append(pText("", size = 10, after = 100))

        sb.append(qHeadingXml("Q.5  Write an essay of 150-200 words on any ONE of the following topics:   (Marks: 15)"))
        data.essayTopics.forEachIndexed { i, t ->
            sb.append(pText(roman(i) + "  " + t, after = 100))
        }
        sb.append(orXml())
        sb.append(pText("Write a paragraph of 100-150 words on any ONE of the following topics:", bold = true, after = 100))
        data.paragraphTopics.forEachIndexed { i, t ->
            sb.append(pText(roman(i) + "  " + t, after = 100))
        }

        sb.append(qHeadingXml("Q.6  Change the following sentences into indirect speech. (Attempt any FIVE)   (Marks: 05)"))
        data.narration.forEachIndexed { i, n ->
            sb.append(pText(roman(i) + "  " + n.sentence, after = 100))
        }

        sb.append(qHeadingXml("Q.7  Use the following pairs of words in your own sentences. (Attempt any FIVE)   (Marks: 05)"))
        data.pairs.chunked(4).forEach { rowPairs ->
            val text = rowPairs.joinToString(" ,   ") { it.first + " : " + it.second }
            sb.append(pText(text, after = 90))
        }
        sb.append(pText("", size = 10, after = 80))

        sb.append(qHeadingXml("Q.8  Translate the following paragraph into English:   (Marks: 08)"))
        sb.append(pText(data.q8.urdu, after = 120))
        sb.append(orXml())
        sb.append(pText("Write TEN sentences on the topic: \"" + data.tenSentenceTopic + "\".", after = 140))

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))

            sb.append(qHeadingXml("Q.2  (Short Answers)"))
            data.shorts.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }

            sb.append(qHeadingXml("Q.3  (Urdu Translation)"))
            if (data.q3.urdu.isNotBlank()) {
                sb.append(pText(data.q3.urdu, size = 22, color = ANS, after = 110))
            } else {
                sb.append(pText("(Model translation not available for this paragraph)", size = 21, color = MUTED, after = 110))
            }

            sb.append(qHeadingXml("Q.6  (Indirect Speech)"))
            data.narration.forEachIndexed { i, n ->
                sb.append(pBoldPrefix(roman(i) + "  ", n.indirect, size = 22, color = ANS, after = 90))
            }

            sb.append(qHeadingXml("Q.7  (Pair of Words - model sentences)"))
            data.pairs.forEach { p ->
                sb.append(pBoldPrefix(p.first + ":  ", p.exampleFirst, size = 22, color = ANS, after = 30))
                sb.append(pBoldPrefix(p.second + ":  ", p.exampleSecond, size = 22, color = ANS, after = 90))
            }

            sb.append(qHeadingXml("Q.8  (English Translation)"))
            sb.append(pText(data.q8.english, size = 22, color = ANS, after = 110))
            sb.append(pText("Note: Q.4 and Q.5 answers will vary from student to student.", size = 21, color = MUTED, after = 110))
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }


    // ---------------- Board Pattern (Class 11 English) ----------------

    fun generateBoardPatternEng11(context: Context, data: PaperPdfGenerator.Eng11BoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "3 Hours", "100", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (20 x 1 = 20)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("SECTION-I", bold = true, size = 25, color = NAVY, after = 110))

        sb.append(qHeadingXml("Q.2  Write short answers (in 3-5 lines/sentences) of any SEVEN questions of the following:   (2 x 7 = 14)"))
        data.proseShorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.3  Write short answers (in 3-5 lines/sentences) of any FIVE questions of the following:   (2 x 5 = 10)"))
        data.poemShorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.4  Write short answers (in 3-5 lines/sentences) of any THREE questions of the following:   (2 x 3 = 6)"))
        data.playShorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(pText("SECTION-II", bold = true, size = 25, color = NAVY, after = 110))

        sb.append(qHeadingXml("Q.5  " + data.letterTopic + "   (10)"))
        sb.append(orXml())
        sb.append(pText(data.applicationTopic, after = 140))

        sb.append(qHeadingXml("Q.6  Write a story on the moral lesson:   (10)"))
        sb.append(pText("\"" + data.storyA + "\"", after = 100))
        sb.append(orXml())
        sb.append(pText("\"" + data.storyB + "\"", after = 140))

        sb.append(qHeadingXml("Q.7  (a) Explain the following lines with reference to the context:   (5)"))
        data.refLines.split("\n").filter { it.isNotBlank() }.forEach { line ->
            sb.append(pText(line, after = 40, indent = 720))
        }
        sb.append(pText("", size = 10, after = 80))
        sb.append(pText("(b) Punctuate the following lines:   (5)", bold = true, after = 100))
        sb.append(pText(data.punctuation, after = 120))
        sb.append(pText("(c) Use any five of the following pairs of words in your own sentences.   (1 x 5 = 5)", bold = true, after = 100))
        data.pairs.chunked(4).forEach { rowPairs ->
            sb.append(pText(rowPairs.joinToString(" ,   ") { it.first + " : " + it.second }, after = 90))
        }
        sb.append(pText("", size = 10, after = 80))

        sb.append(qHeadingXml("Q.8  Translate the following passage into Urdu.   (15)"))
        sb.append(pText(data.translation.text, after = 120))
        sb.append(
            pText(
                "Alternate Question for Foreign / English Medium Candidates: instead of translation, write an essay on the given topic: \"" + data.essayTopic + "\"   (15)",
                size = 21, color = MUTED, after = 140
            )
        )

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                sb.append(qHeadingXml(heading))
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            keyShorts("Q.2  (Prose Short Answers)", data.proseShorts)
            keyShorts("Q.3  (Poem Short Answers)", data.poemShorts)
            keyShorts("Q.4  (Play Short Answers)", data.playShorts)

            sb.append(qHeadingXml("Q.7(c)  (Pair of Words - model sentences)"))
            data.pairs.forEach { p ->
                if (p.exampleFirst.isNotBlank()) {
                    sb.append(pBoldPrefix(p.first + ":  ", p.exampleFirst, size = 22, color = ANS, after = 30))
                } else {
                    sb.append(pText(p.first + ":  (Model sentence not available)", size = 21, color = MUTED, after = 30))
                }
                if (p.exampleSecond.isNotBlank()) {
                    sb.append(pBoldPrefix(p.second + ":  ", p.exampleSecond, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(p.second + ":  (Model sentence not available)", size = 21, color = MUTED, after = 90))
                }
            }

            sb.append(qHeadingXml("Q.8  (Urdu Translation)"))
            if (data.translation.urdu.isNotBlank()) {
                sb.append(pText(data.translation.urdu, size = 22, color = ANS, after = 110))
            } else {
                sb.append(pText("(Model translation not available for this passage)", size = 21, color = MUTED, after = 110))
            }
            sb.append(pText("Note: Q.5, Q.6, Q.7(a), Q.7(b) and the Q.8 alternate essay answers will vary from student to student.", size = 21, color = MUTED, after = 110))
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }


    // ---------------- Board Pattern (Class 9 English) ----------------

    fun generateBoardPatternEng9(context: Context, data: PaperPdfGenerator.Eng9BoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours 30 Minutes", "75", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (19 x 1 = 19)"))
        sb.append(pText("(a)  Form of Verbs   (5)", bold = true, after = 90))
        sb.append(mcqsXml(data.verbs))
        sb.append(pText("(b)  Correct Spelling   (4)", bold = true, after = 90))
        sb.append(mcqsXml(data.spelling))
        sb.append(pText("(c)  Correct Meanings   (5)", bold = true, after = 90))
        sb.append(mcqsXml(data.meanings))
        sb.append(pText("(d)  Grammar   (5)", bold = true, after = 90))
        sb.append(mcqsXml(data.grammar))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("Time Allowed: 2 Hours 10 Minutes   •   Total Marks: 56", bold = true, size = 22, color = NAVY, after = 140))

        sb.append(qHeadingXml("Q.2  Write short answers to any FOUR questions — any THREE from (i) to (v) and any ONE from (vi) to (vii).   (3 x 2 + 1 x 4 = 10)"))
        data.q2Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(qHeadingXml("Q.3  Translate any TWO of the following paragraphs into Urdu.   (2 x 4 = 8)"))
        data.q3Translations.forEachIndexed { i, t ->
            sb.append(pText(roman(i) + "  " + t.english, after = 110))
        }

        sb.append(qHeadingXml("Q.4  Write the summary of the following poem.   (5)"))
        sb.append(pText("Poem: \"" + data.q4Summary.poem + "\"", after = 120))

        sb.append(qHeadingXml("Q.5  Use any FIVE of the following words in your own sentences.   (5 x 1 = 5)"))
        data.q5Words.forEachIndexed { i, w ->
            sb.append(pText(roman(i) + "  " + w.word, after = 100))
        }

        sb.append(qHeadingXml("Q.6  Attempt any ONE of the following.   (8)"))
        data.q6Prompts.forEachIndexed { i, p ->
            sb.append(pText(roman(i) + "  " + p.prompt, after = 110))
        }

        sb.append(qHeadingXml("Q.7  Read the following passage carefully and answer the questions given at the end.   (10)"))
        sb.append(pText(data.q7Passage.english, after = 120))
        data.q7Questions.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(qHeadingXml("Q.8  Translate any FIVE of the following paragraphs into English.   (5 x 1 = 5)"))
        data.q8Options.forEachIndexed { i, t ->
            sb.append(pText(roman(i) + "  " + t.urdu, after = 110))
        }

        sb.append(qHeadingXml("Q.9  Choose the correct option (Active / Passive Voice).   (5 x 1 = 5)"))
        sb.append(mcqsXml(data.voice))

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs — Verbs / Spelling / Meanings / Grammar)"))
            sb.append(keyEntriesXml(data.verbs + data.spelling + data.meanings + data.grammar))
            sb.append(pText("", size = 10, after = 80))
            sb.append(qHeadingXml("Q.9  (Active / Passive Voice)"))
            sb.append(keyEntriesXml(data.voice))
            sb.append(pText("", size = 10, after = 80))

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                sb.append(qHeadingXml(heading))
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.7  (Comprehension Questions)", data.q7Questions)

            sb.append(qHeadingXml("Q.3  (Urdu Translations — model)"))
            data.q3Translations.forEach { t ->
                if (t.urdu.isNotBlank()) {
                    sb.append(pText(t.urdu, size = 22, color = ANS, after = 110))
                } else {
                    sb.append(pText("(Model translation not available)", size = 21, color = MUTED, after = 110))
                }
            }

            sb.append(qHeadingXml("Q.4  (Poem Summary — model)"))
            sb.append(pText(data.q4Summary.summary, size = 22, color = ANS, after = 110))

            sb.append(qHeadingXml("Q.5  (Model Sentences)"))
            data.q5Words.forEach { w ->
                if (w.sentence.isNotBlank()) {
                    sb.append(pBoldPrefix(w.word + ":  ", w.sentence, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(w.word + ":  (Model sentence not available)", size = 21, color = MUTED, after = 90))
                }
            }

            sb.append(qHeadingXml("Q.8  (English Translations — model)"))
            data.q8Options.forEach { t ->
                if (t.english.isNotBlank()) {
                    sb.append(pText(t.english, size = 22, color = ANS, after = 110))
                } else {
                    sb.append(pText("(Model translation not available)", size = 21, color = MUTED, after = 110))
                }
            }
            sb.append(pText("Note: Q.6 answers will vary from student to student.", size = 21, color = MUTED, after = 110))
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 12 Physics) ----------------

    fun generateBoardPatternPhy12(context: Context, data: PaperPdfGenerator.Phy12BoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "3 Hours", "85", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (17 x 1 = 17)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("PART-II: SHORT ANSWER QUESTIONS", bold = true, size = 23, color = NAVY, after = 120))

        sb.append(qHeadingXml("Q.2  Write short answers to any EIGHT (8) questions.   (2 x 8 = 16)      [Chapters 13, 14, 15]"))
        data.q2Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.3  Write short answers to any EIGHT (8) questions.   (2 x 8 = 16)      [Chapters 16, 17, 18]"))
        data.q3Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.4  Write short answers to any SIX (6) questions.   (2 x 6 = 12)      [Chapters 19, 20, 21]"))
        data.q4Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(pText("PART-III: DETAILED QUESTIONS / NUMERICALS", bold = true, size = 23, color = NAVY, after = 120))
        sb.append(pText("Attempt any THREE (3) questions. Each question carries 8 marks — a long / theoretical part (5 marks) and a numerical part (3 marks).", after = 140))
        data.detailed.forEachIndexed { i, wq ->
            sb.append(qHeadingXml("Q." + (i + 5) + "  " + wq.question))
        }

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                sb.append(qHeadingXml(heading))
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.3  (Short Answers)", data.q3Shorts)
            keyShorts("Q.4  (Short Answers)", data.q4Shorts)

            sb.append(qHeadingXml("Detailed Questions  (Answers)"))
            data.detailed.forEachIndexed { i, wq ->
                val label = "Q." + (i + 5) + "  "
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(label, wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(label + "(Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 10 Chemistry) ----------------

    fun generateBoardPatternChem10(context: Context, data: PaperPdfGenerator.Chem10BoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours", "60", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
                } else {
                    "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE PART"))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (12 x 1 = 12)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE PART"))
        sb.append(pText("PART-II: SHORT ANSWER QUESTIONS", bold = true, size = 23, color = NAVY, after = 120))

        sb.append(qHeadingXml("Q.2  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)      [Chapters 14, 15, 16, 17, 18]"))
        data.q2Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.3  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)      [Chapters 19, 20, 21, 22]"))
        data.q3Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.4  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)      [Chapters 23, 24, 25, 26]"))
        data.q4Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(pText("PART-III: DETAILED QUESTIONS", bold = true, size = 23, color = NAVY, after = 120))
        sb.append(pText("Attempt any TWO (2) questions. Each question carries 9 marks: part (a) 5 marks and part (b) 4 marks, from the paired chapters — Q.5: Chapters 14 and 15; Q.6: Chapters 20 and 22; Q.7: Chapters 23 and 25.", after = 140))
        data.detailed.forEachIndexed { i, wq ->
            sb.append(qHeadingXml("Q." + (i + 5) + "  " + wq.question))
        }

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                sb.append(qHeadingXml(heading))
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.3  (Short Answers)", data.q3Shorts)
            keyShorts("Q.4  (Short Answers)", data.q4Shorts)

            sb.append(qHeadingXml("Detailed Questions  (Answers)"))
            data.detailed.forEachIndexed { i, wq ->
                val label = "Q." + (i + 5) + "  "
                if (wq.answer.isNotBlank()) {
                    sb.append(pBoldPrefix(label, wq.answer, size = 22, color = ANS, after = 90))
                } else {
                    sb.append(pText(label + "(Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                }
            }
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Board Pattern (Class 10 Mathematics) ----------------

    fun generateBoardPatternMath10(context: Context, data: PaperPdfGenerator.Math10BoardPaperData, outFile: File): File {
        val logo = logoPng(context, data.header)
        val sb = StringBuilder()
        sb.append(
            headerXml(
                data.header, data.testTitle, data.subject, data.classLabel,
                "2 Hours 30 Minutes", "75", null, logo != null
            )
        )
        sb.append(
            instructionsXml(
                if (data.includeAnswers) {
                    "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. Attempt only the required number of questions in each part."
                } else {
                    "Note: Q.1 is compulsory. Attempt only the required number of questions in each part. Write your answers neatly in the answer copy."
                }
            )
        )

        sb.append(bannerXml("OBJECTIVE TYPE"))
        sb.append(pText("Time Allowed: 20 Minutes   -   Max. Marks: 15", bold = true, size = 22, color = NAVY, after = 120))
        sb.append(qHeadingXml("Q.1  Choose the correct option:   (15 x 1 = 15)"))
        sb.append(mcqsXml(data.mcqs))

        sb.append(bannerXml("SUBJECTIVE TYPE"))
        sb.append(pText("Time Allowed: 2 Hours 10 Minutes   -   Max. Marks: 60", bold = true, size = 22, color = NAVY, after = 120))
        sb.append(pText("(Part-I)", bold = true, size = 23, color = NAVY, after = 120))
        sb.append(qHeadingXml("Q.2  Write short answers to any SIX (06) questions.   (2 x 6 = 12)      [Chapters 1, 2, 3, 4]"))
        data.q2Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.3  Write short answers to any SIX (06) questions.   (2 x 6 = 12)      [Chapters 5, 6, 7, 8]"))
        data.q3Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }
        sb.append(qHeadingXml("Q.4  Write short answers to any SIX (06) questions.   (2 x 6 = 12)      [Chapters 9, 10, 11, 12]"))
        data.q4Shorts.forEachIndexed { i, wq ->
            sb.append(pText(roman(i) + "  " + wq.question, after = 100))
        }

        sb.append(pText("(Part-II)", bold = true, size = 23, color = NAVY, after = 120))
        sb.append(pText("Note: Attempt any TWO (02) questions. Each question has two parts (a) and (b), carrying 4 marks each.   (2 x 8 = 16)", after = 140))
        data.partII.forEachIndexed { i, pair ->
            sb.append(qHeadingXml("Q." + (i + 5)))
            sb.append(pBoldPrefix("(a)  ", pair.first.question, after = 90))
            sb.append(pBoldPrefix("(b)  ", pair.second.question, after = 110))
        }

        sb.append(pText("(Part-III)", bold = true, size = 23, color = NAVY, after = 120))
        sb.append(pText("Note: Attempt any ONE (01) question. Each question has two parts (a) and (b), carrying 4 marks each.   (1 x 8 = 8)", after = 140))
        data.partIII.forEachIndexed { i, pair ->
            sb.append(qHeadingXml("Q." + (i + 8)))
            sb.append(pBoldPrefix("(a)  ", pair.first.question, after = 90))
            sb.append(pBoldPrefix("(b)  ", pair.second.question, after = 110))
        }

        if (data.includeAnswers) {
            sb.append(keyTitleXml("ANSWER KEY"))
            sb.append(qHeadingXml("Q.1  (MCQs)"))
            sb.append(keyEntriesXml(data.mcqs))
            sb.append(pText("", size = 10, after = 80))

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                sb.append(qHeadingXml(heading))
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(r + "  ", wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(r + "  (Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.3  (Short Answers)", data.q3Shorts)
            keyShorts("Q.4  (Short Answers)", data.q4Shorts)

            fun keyPair(heading: String, pair: Pair<WrittenQuestion, WrittenQuestion>) {
                sb.append(qHeadingXml(heading))
                listOf("(a)  " to pair.first, "(b)  " to pair.second).forEach { (label, wq) ->
                    if (wq.answer.isNotBlank()) {
                        sb.append(pBoldPrefix(label, wq.answer, size = 22, color = ANS, after = 90))
                    } else {
                        sb.append(pText(label + "(Answer not available in the source data)", size = 21, color = MUTED, after = 90))
                    }
                }
            }
            data.partII.forEachIndexed { i, pair -> keyPair("Q." + (i + 5) + "  (Long Question)", pair) }
            data.partIII.forEachIndexed { i, pair -> keyPair("Q." + (i + 8) + "  (Long Question)", pair) }
        }
        return writeDocx(sb.toString(), data.testTitle, logo, outFile)
    }

    // ---------------- Package assembly ----------------

    private fun writeDocx(bodyXml: String, paperTitle: String, logo: ByteArray?, outFile: File): File {
        docxRtlSubject = false
        val hasLogo = logo != null
        val documentXml = StringBuilder()
        documentXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        documentXml.append(
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"" +
                " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"" +
                " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\"" +
                " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"" +
                " xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
        )
        documentXml.append("<w:body>").append(bodyXml)
        // A body must not end with a table (strict readers reject it) - close
        // with an empty paragraph before the section properties.
        documentXml.append("<w:p/>")
        documentXml.append(
            "<w:sectPr><w:footerReference w:type=\"default\" r:id=\"rId1\"/>" +
                "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>" +
                "<w:pgMar w:top=\"1000\" w:right=\"1000\" w:bottom=\"1000\" w:left=\"1000\"" +
                " w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/></w:sectPr>"
        )
        documentXml.append("</w:body></w:document>")

        val contentTypes = StringBuilder()
        contentTypes.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        contentTypes.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        contentTypes.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        contentTypes.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        if (hasLogo) {
            contentTypes.append("<Default Extension=\"png\" ContentType=\"image/png\"/>")
        }
        contentTypes.append("<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>")
        contentTypes.append("<Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>")
        contentTypes.append("<Override PartName=\"/word/footer1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml\"/>")
        contentTypes.append("</Types>")

        val rootRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
            "</Relationships>"

        val docRels = StringBuilder()
        docRels.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        docRels.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        docRels.append("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer\" Target=\"footer1.xml\"/>")
        docRels.append("<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        if (hasLogo) {
            docRels.append("<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/logo.png\"/>")
        }
        docRels.append("</Relationships>")

        val styles = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:docDefaults><w:rPrDefault><w:rPr>" +
            "<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:cs=\"Calibri\"/>" +
            "<w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>" +
            "</w:rPr></w:rPrDefault></w:docDefaults>" +
            "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\">" +
            "<w:name w:val=\"Normal\"/><w:qFormat/></w:style></w:styles>"

        val footerText = "StudyPoint — " + paperTitle
        val footer = StringBuilder()
        footer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        footer.append(
            "<w:ftr xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                "<w:p><w:pPr><w:tabs><w:tab w:val=\"right\" w:pos=\"9900\"/></w:tabs>" +
                "<w:spacing w:after=\"0\"/></w:pPr>"
        )
        footer.append(run(footerText, size = 17, color = MUTED))
        footer.append(tabRun())
        footer.append(run("Page ", size = 17, color = MUTED))
        footer.append("<w:fldSimple w:instr=\" PAGE \"><w:r><w:t>1</w:t></w:r></w:fldSimple>")
        footer.append(run(" of ", size = 17, color = MUTED))
        footer.append("<w:fldSimple w:instr=\" NUMPAGES \"><w:r><w:t>1</w:t></w:r></w:fldSimple>")
        footer.append("</w:p></w:ftr>")

        outFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outFile)).use { z ->
            fun add(name: String, bytes: ByteArray) {
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
            add("[Content_Types].xml", contentTypes.toString().toByteArray(Charsets.UTF_8))
            add("_rels/.rels", rootRels.toByteArray(Charsets.UTF_8))
            add("word/document.xml", documentXml.toString().toByteArray(Charsets.UTF_8))
            add("word/styles.xml", styles.toByteArray(Charsets.UTF_8))
            add("word/footer1.xml", footer.toString().toByteArray(Charsets.UTF_8))
            add("word/_rels/document.xml.rels", docRels.toString().toByteArray(Charsets.UTF_8))
            if (logo != null) add("word/media/logo.png", logo)
        }
        return outFile
    }
}
