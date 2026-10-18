package com.example.studypoint.teacher

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Award List exports — PDF and MS Word (.docx), generated with the same
 * techniques as the Paper Generator (PdfDocument canvas for PDF, a
 * hand-built zip of XML parts for DOCX). Files land in the app-specific
 * external folder so Word/PDF viewers open them reliably.
 */
object AwardListExporter {

    data class Row(
        val roll: String,
        val name: String,
        val obtained: Double?,
        val total: Double?,
        val remarks: String
    ) {
        fun percent(): String {
            val o = obtained ?: return "—"
            val t = total ?: return "—"
            if (t <= 0) return "—"
            return String.format(Locale.ENGLISH, "%.1f", o * 100.0 / t)
        }
    }

    fun buildRows(award: AwardListData, studentsById: Map<String, TeacherStudent>): List<Row> {
        return award.studentIds
            .mapNotNull { sid -> studentsById[sid]?.let { sid to it } }
            .sortedWith(compareBy({ it.second.rollNumber }, { it.second.name }))
            .map { (sid, s) ->
                val m = award.marks[sid]
                Row(s.rollNumber, s.name, m?.obtained, m?.total, m?.remarks ?: "")
            }
    }

    private fun exportDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.cacheDir
        return File(base, "award_lists").apply { mkdirs() }
    }

    private fun baseName(award: AwardListData, ext: String): String {
        val raw = "AwardList_${award.examType}_${award.className}${award.section}_${award.subject}"
        val clean = raw.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        return "${clean}_$stamp.$ext"
    }

    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = if (file.extension.equals("docx", ignoreCase = true)) {
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            } else {
                "application/pdf"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(null, uri)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "File saved as ${file.name} — no viewer app found to open it",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun infoLine1(a: AwardListData) =
        "Class: ${a.className} (${a.section})    Subject: ${a.subject}    Exam: ${ExamTypes.label(a.examType)}    Session: ${a.session}"

    private fun infoLine2(a: AwardListData) =
        "Exam Date: ${a.examDate}" +
            (if (a.returnDate.isNotEmpty()) "    Return Date: ${a.returnDate}" else "") +
            "    Students: ${a.studentIds.size}"

    // ---------------- PDF ----------------

    fun exportPdf(context: Context, award: AwardListData, studentsById: Map<String, TeacherStudent>): File {
        val rows = buildRows(award, studentsById)
        val pageW = 595
        val pageH = 842
        val margin = 40f
        val contentW = pageW - 2 * margin
        // Column boundaries (relative to margin)
        val cols = floatArrayOf(0f, 28f, 82f, 272f, 342f, 397f, 447f, contentW)
        val headers = listOf("S.No", "Roll No", "Student Name", "Obtained", "Total", "%", "Remarks")
        val headerRowH = 20f
        val rowH = 18f
        val footerY = pageH - 26f

        val doc = PdfDocument()

        val paint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.parseColor("#94A3B8"); strokeWidth = 0.7f }
        val shadePaint = Paint().apply { color = Color.parseColor("#D9E2F3"); style = Paint.Style.FILL }
        val altShadePaint = Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL }

        fun drawHeader(canvas: android.graphics.Canvas, first: Boolean): Float {
            var y = margin
            if (first) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 18f
                paint.color = Color.parseColor("#1E3A8A")
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(award.school.ifEmpty { "Award List" }, pageW / 2f, y + 18f, paint)
                y += 26f
                if (award.campus.isNotEmpty()) {
                    paint.typeface = Typeface.DEFAULT
                    paint.textSize = 11f
                    paint.color = Color.parseColor("#64748B")
                    canvas.drawText(award.campus, pageW / 2f, y + 12f, paint)
                    y += 18f
                }
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 14.5f
                paint.color = Color.parseColor("#111827")
                canvas.drawText("AWARD LIST", pageW / 2f, y + 16f, paint)
                y += 24f
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10.5f
                paint.color = Color.parseColor("#334155")
                canvas.drawText(infoLine1(award), pageW / 2f, y + 12f, paint)
                y += 17f
                canvas.drawText(infoLine2(award), pageW / 2f, y + 12f, paint)
                y += 22f
            } else {
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = Color.parseColor("#64748B")
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    "Award List — ${award.school} · ${award.className} (${award.section}) · ${ExamTypes.label(award.examType)} (continued)",
                    pageW / 2f, y + 12f, paint
                )
                y += 20f
            }
            // Table header row
            canvas.drawRect(margin, y, margin + contentW, y + headerRowH, shadePaint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            paint.color = Color.parseColor("#111827")
            for (i in headers.indices) {
                paint.textAlign = if (i <= 1) Paint.Align.CENTER else Paint.Align.LEFT
                val cx = margin + cols[i] + if (i <= 1) (cols[i + 1] - cols[i]) / 2f else 5f
                if (i <= 1) {
                    canvas.drawText(headers[i], cx, y + 14f, paint)
                } else {
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(headers[i], margin + cols[i] + 5f, y + 14f, paint)
                }
            }
            y += headerRowH
            return y
        }

        fun ellipsize(text: String, maxW: Float, p: Paint): String {
            if (p.measureText(text) <= maxW) return text
            var t = text
            while (t.isNotEmpty() && p.measureText("$t…") > maxW) t = t.dropLast(1)
            return "$t…"
        }

        // Placement pass: rows per page
        val firstPageCapacity = ((footerY - 30f - (margin + 115f + headerRowH)) / rowH).toInt().coerceAtLeast(1)
        val laterCapacity = ((footerY - 30f - (margin + 20f + headerRowH)) / rowH).toInt().coerceAtLeast(1)
        val pages = mutableListOf<List<Row>>()
        var idx = 0
        var cap = firstPageCapacity
        while (idx < rows.size) {
            val end = minOf(idx + cap, rows.size)
            pages.add(rows.subList(idx, end))
            idx = end
            cap = laterCapacity
        }
        if (pages.isEmpty()) pages.add(emptyList())

        var serialBase = 0
        pages.forEachIndexed { pageIdx, pageRows ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx + 1).create())
            val canvas = page.canvas
            var y = drawHeader(canvas, pageIdx == 0)
            pageRows.forEachIndexed { ri, r ->
                if (ri % 2 == 1) {
                    canvas.drawRect(margin, y, margin + contentW, y + rowH, altShadePaint)
                }
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = Color.parseColor("#1F2937")
                val baseLine = y + 13f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("${serialBase + ri + 1}", margin + cols[0] + (cols[1] - cols[0]) / 2f, baseLine, paint)
                canvas.drawText(r.roll, margin + cols[1] + (cols[2] - cols[1]) / 2f, baseLine, paint)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(ellipsize(r.name, cols[3] - cols[2] - 10f, paint), margin + cols[2] + 5f, baseLine, paint)
                canvas.drawText(r.obtained?.let { TeacherUi.fmtMarks(it) } ?: "—", margin + cols[3] + 5f, baseLine, paint)
                canvas.drawText(r.total?.let { TeacherUi.fmtMarks(it) } ?: "—", margin + cols[4] + 5f, baseLine, paint)
                canvas.drawText(r.percent(), margin + cols[5] + 5f, baseLine, paint)
                canvas.drawText(ellipsize(r.remarks, cols[7] - cols[6] - 10f, paint), margin + cols[6] + 5f, baseLine, paint)
                y += rowH
            }
            // Grid lines for the table on this page
            val tableTop = y - pageRows.size * rowH - headerRowH
            for (cx in cols) {
                canvas.drawLine(margin + cx, tableTop, margin + cx, y, linePaint)
            }
            var ly = tableTop
            while (ly <= y + 0.5f) {
                canvas.drawLine(margin, ly, margin + contentW, ly, linePaint)
                ly += if (ly == tableTop) headerRowH else rowH
            }
            // Signatures on the last page
            if (pageIdx == pages.lastIndex) {
                var sy = y + 46f
                if (sy > footerY - 30f) sy = footerY - 30f
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10.5f
                paint.color = Color.parseColor("#334155")
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Class Teacher: ____________________", margin, sy, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Principal: ____________________", margin + contentW, sy, paint)
            }
            // Footer
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            paint.color = Color.parseColor("#94A3B8")
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "Generated by StudyPoint · Page ${pageIdx + 1} of ${pages.size}",
                pageW / 2f, footerY + 12f, paint
            )
            doc.finishPage(page)
            serialBase += pageRows.size
        }

        val out = File(exportDir(context), baseName(award, "pdf"))
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return out
    }

    // ---------------- DOCX ----------------

    private fun esc(s: String): String {
        // Strip XML-1.0-illegal characters (same rule as PaperDocxGenerator),
        // then escape the XML specials.
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
                code in 0xDC00..0xDFFF -> Unit
                else -> Unit
            }
            i++
        }
        return sb.toString()
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }

    private fun run(text: String, bold: Boolean = false, size: Int = 22, color: String = "1E293B"): String {
        val rpr = StringBuilder("<w:rPr>")
        if (bold) rpr.append("<w:b/>")
        rpr.append("<w:color w:val=\"").append(color).append("\"/>")
        rpr.append("<w:sz w:val=\"").append(size).append("\"/>")
        rpr.append("<w:szCs w:val=\"").append(size).append("\"/>")
        rpr.append("</w:rPr>")
        return "<w:r>" + rpr + "<w:t xml:space=\"preserve\">" + esc(text) + "</w:t></w:r>"
    }

    private fun para(runs: String, align: String? = null, after: Int = 120): String {
        val ppr = StringBuilder("<w:pPr>")
        ppr.append("<w:spacing w:after=\"").append(after).append("\"/>")
        if (align != null) ppr.append("<w:jc w:val=\"").append(align).append("\"/>")
        ppr.append("</w:pPr>")
        return "<w:p>" + ppr + runs + "</w:p>"
    }

    private fun pText(
        text: String, bold: Boolean = false, size: Int = 22, color: String = "1E293B",
        align: String? = null, after: Int = 120
    ): String = para(run(text, bold, size, color), align, after)

    private fun cell(text: String, bold: Boolean = false, shade: String? = null, width: Int): String {
        val sb = StringBuilder("<w:tc><w:tcPr><w:tcW w:w=\"")
        sb.append(width).append("\" w:type=\"dxa\"/>")
        if (shade != null) {
            sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(shade).append("\"/>")
        }
        sb.append("</w:tcPr>")
        sb.append(para(run(text, bold, 21), after = 40))
        sb.append("</w:tc>")
        return sb.toString()
    }

    fun exportDocx(context: Context, award: AwardListData, studentsById: Map<String, TeacherStudent>): File {
        val rows = buildRows(award, studentsById)
        val widths = listOf(520, 820, 3360, 1080, 880, 880, 2360) // dxa, total 9900
        val body = StringBuilder()

        body.append(pText(award.school.ifEmpty { "Award List" }, bold = true, size = 34, color = "1E3A8A", align = "center", after = 60))
        if (award.campus.isNotEmpty()) {
            body.append(pText(award.campus, size = 22, color = "64748B", align = "center", after = 60))
        }
        body.append(pText("AWARD LIST", bold = true, size = 28, color = "111827", align = "center", after = 80))
        body.append(pText(infoLine1(award), size = 21, color = "334155", align = "center", after = 40))
        body.append(pText(infoLine2(award), size = 21, color = "334155", align = "center", after = 200))

        val tbl = StringBuilder()
        tbl.append("<w:tbl><w:tblPr><w:tblW w:w=\"").append(widths.sum()).append("\" w:type=\"dxa\"/>")
        tbl.append("<w:tblBorders>")
        for (edge in listOf("top", "left", "bottom", "right", "insideH", "insideV")) {
            tbl.append("<w:").append(edge).append(" w:val=\"single\" w:sz=\"6\" w:space=\"0\" w:color=\"475569\"/>")
        }
        tbl.append("</w:tblBorders></w:tblPr><w:tblGrid>")
        for (w in widths) tbl.append("<w:gridCol w:w=\"").append(w).append("\"/>")
        tbl.append("</w:tblGrid>")
        val headerCells = listOf("S.No", "Roll No", "Student Name", "Obtained", "Total", "%", "Remarks")
        tbl.append("<w:tr>")
        headerCells.forEachIndexed { i, h -> tbl.append(cell(h, bold = true, shade = "D9E2F3", width = widths[i])) }
        tbl.append("</w:tr>")
        rows.forEachIndexed { idx, r ->
            tbl.append("<w:tr>")
            val vals = listOf(
                "${idx + 1}", r.roll, r.name,
                r.obtained?.let { TeacherUi.fmtMarks(it) } ?: "—",
                r.total?.let { TeacherUi.fmtMarks(it) } ?: "—",
                r.percent(), r.remarks
            )
            vals.forEachIndexed { i, v -> tbl.append(cell(v, width = widths[i])) }
            tbl.append("</w:tr>")
        }
        tbl.append("</w:tbl>")
        body.append(tbl)

        body.append(pText("", after = 200))
        body.append(pText("Class Teacher: ____________________", after = 60))
        body.append(pText("Principal: ____________________", align = "right", after = 0))

        val documentXml = StringBuilder()
        documentXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        documentXml.append(
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"" +
                " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
        )
        documentXml.append("<w:body>").append(body)
        documentXml.append("<w:p/>")
        documentXml.append(
            "<w:sectPr><w:footerReference w:type=\"default\" r:id=\"rId1\"/>" +
                "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>" +
                "<w:pgMar w:top=\"1000\" w:right=\"1000\" w:bottom=\"1000\" w:left=\"1000\"" +
                " w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/></w:sectPr>"
        )
        documentXml.append("</w:body></w:document>")

        val contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
            "<Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>" +
            "<Override PartName=\"/word/footer1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml\"/>" +
            "</Types>"
        val rootRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
            "</Relationships>"
        val docRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer\" Target=\"footer1.xml\"/>" +
            "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"
        val styles = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:docDefaults><w:rPrDefault><w:rPr>" +
            "<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:cs=\"Calibri\"/>" +
            "<w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>" +
            "</w:rPr></w:rPrDefault></w:docDefaults>" +
            "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\">" +
            "<w:name w:val=\"Normal\"/><w:qFormat/></w:style></w:styles>"
        val footer = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:ftr xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:p><w:pPr><w:tabs><w:tab w:val=\"right\" w:pos=\"9900\"/></w:tabs>" +
            "<w:spacing w:after=\"0\"/></w:pPr>" +
            run("StudyPoint — Award List", size = 17, color = "64748B") +
            "<w:r><w:tab/></w:r>" +
            run("Page ", size = 17, color = "64748B") +
            "<w:fldSimple w:instr=\" PAGE \"><w:r><w:t>1</w:t></w:r></w:fldSimple>" +
            run(" of ", size = 17, color = "64748B") +
            "<w:fldSimple w:instr=\" NUMPAGES \"><w:r><w:t>1</w:t></w:r></w:fldSimple>" +
            "</w:p></w:ftr>"

        val out = File(exportDir(context), baseName(award, "docx"))
        ZipOutputStream(FileOutputStream(out)).use { z ->
            fun add(name: String, content: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(content.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            add("[Content_Types].xml", contentTypes)
            add("_rels/.rels", rootRels)
            add("word/document.xml", documentXml.toString())
            add("word/styles.xml", styles)
            add("word/footer1.xml", footer)
            add("word/_rels/document.xml.rels", docRels)
        }
        return out
    }
}
