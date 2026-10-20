package com.example.studypoint.practicetest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.graphics.BitmapFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.FileOutputStream

/**
 * Builds a complete question-paper PDF (A4) that can mix three sections —
 * MCQs, Short Questions and Long Questions — in solved or unsolved form.
 * Same layout engine style as [TestPdfGenerator]: a placement pass computes
 * pages first (so footers can show "Page X of Y"), questions are kept
 * together where possible, and long answers split line-by-line across pages
 * instead of being cut off.
 */
object PaperPdfGenerator {

    data class PaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val chaptersLabel: String,
        val timeAllowed: String,
        val mcqs: List<Question>,
        val shorts: List<WrittenQuestion>,
        val longs: List<WrittenQuestion>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 42
    private const val CONTENT_W = PAGE_W - 2 * MARGIN
    private const val MINI_HEADER_H = 22
    private const val FOOTER_Y = PAGE_H - MARGIN + 16

    private val NAVY = Color.rgb(15, 23, 42)
    private val GREEN = Color.rgb(16, 185, 129)
    private val TEXT = Color.rgb(30, 41, 59)
    private val MUTED = Color.rgb(100, 116, 139)
    private val ANS_COLOR = Color.rgb(6, 95, 70)

    private fun paint(size: Float, bold: Boolean = false, color: Int = TEXT): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        return p
    }

    private fun layoutOf(text: CharSequence, p: TextPaint, width: Int = CONTENT_W): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width).build()

    /** Editable school header printed at the top of every generated paper. */
    data class SchoolHeader(
        val schoolName: String = "AL SAMAD ACADEMY BWP",
        val address: String = "",
        val phone: String = "0301 23456789",
        val paperCode: String = "",
        val passingMarks: String = "",
        val logoPath: String = ""
    )

    private fun serifPaint(size: Float, bold: Boolean = false, color: Int = NAVY): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.create(Typeface.SERIF, Typeface.BOLD)
        else Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        return p
    }

    private fun fitSerif(text: String, startSize: Float, maxWidth: Int): TextPaint {
        var size = startSize
        var p = serifPaint(size, bold = true)
        while (p.measureText(text) > maxWidth && size > 11f) {
            size -= 1f
            p = serifPaint(size, bold = true)
        }
        return p
    }

    private fun headerLogo(context: Context, header: SchoolHeader): Bitmap? {
        if (header.logoPath.isNotBlank()) {
            try {
                val raw = BitmapFactory.decodeFile(header.logoPath)
                if (raw != null) return Bitmap.createScaledBitmap(raw, 56, 56, true)
            } catch (e: Exception) { /* fall through to the app icon */ }
        }
        return try {
            val d: Drawable = context.packageManager.getApplicationIcon(context.packageName)
            val bmp = Bitmap.createBitmap(56, 56, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            d.setBounds(0, 0, 56, 56)
            d.draw(cv)
            bmp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The classic school paper header: a boxed banner (logo + school name +
     * address + phone), the paper title, then a boxed info grid —
     * Subject / Class / Roll No / Paper Code, Time / Total Marks / Passing
     * Marks / Date, and a full-width Student Name row. Shared by all three
     * paper generators so every paper carries the same header.
     */
    private fun schoolHeaderBlocks(
        context: Context,
        header: SchoolHeader,
        title: String,
        subject: String,
        classLabel: String,
        timeText: String,
        totalMarksText: String,
        chaptersLabel: String? = null
    ): List<Block> {
        val out = mutableListOf<Block>()
        val logo = headerLogo(context, header)
        val textW = CONTENT_W - 76
        val nameP = fitSerif(header.schoolName.uppercase(), 20f, textW)
        val nameL = centeredLayout(header.schoolName.uppercase(), nameP, textW)
        val addrL = if (header.address.isNotBlank()) centeredLayout(header.address, paint(10f, color = MUTED), textW) else null
        val phoneL = if (header.phone.isNotBlank()) centeredLayout(header.phone, paint(10f, bold = true, color = MUTED), textW) else null
        var bannerH = nameL.height + 4
        if (addrL != null) bannerH += addrL.height + 1
        if (phoneL != null) bannerH += phoneL.height + 1
        bannerH = maxOf(bannerH + 12, 68)
        out += Block(bannerH) { c, x, y ->
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = NAVY; style = Paint.Style.STROKE; strokeWidth = 1.6f
            }
            c.drawRect(x.toFloat(), y.toFloat(), (x + CONTENT_W).toFloat(), (y + bannerH - 4).toFloat(), border)
            if (logo != null) {
                val ly = y + (bannerH - 4 - 56) / 2
                c.drawBitmap(logo, (x + 10).toFloat(), ly.toFloat(), null)
                val div = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
                c.drawLine((x + 74).toFloat(), (y + 6).toFloat(), (x + 74).toFloat(), (y + bannerH - 10).toFloat(), div)
            }
            var ty = y + 6
            val tx = x + 76
            c.save(); c.translate(tx.toFloat(), ty.toFloat()); nameL.draw(c); c.restore(); ty += nameL.height + 4
            if (addrL != null) { c.save(); c.translate(tx.toFloat(), ty.toFloat()); addrL.draw(c); c.restore(); ty += addrL.height + 1 }
            if (phoneL != null) { c.save(); c.translate(tx.toFloat(), ty.toFloat()); phoneL.draw(c); c.restore() }
        }

        val titleL = layoutOf(title.uppercase(), paint(14f, bold = true, color = NAVY), CONTENT_W)
        out += Block(titleL.height + 8) { c, x, y ->
            val tx = x + (CONTENT_W - titleL.width) / 2
            c.save(); c.translate(tx.toFloat(), (y + 2).toFloat()); titleL.draw(c); c.restore()
        }

        val dateText = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val labelP = paint(8.5f, bold = true, color = NAVY)
        val valueP = paint(10.5f)
        val rowH = 22
        out += Block(rowH * 3 + 6) { c, x, y ->
            val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105); style = Paint.Style.STROKE; strokeWidth = 1f
            }
            val w1 = (CONTENT_W * 0.30f).toInt()
            val w2 = (CONTENT_W * 0.22f).toInt()
            val w3 = (CONTENT_W * 0.24f).toInt()
            val w4 = CONTENT_W - w1 - w2 - w3
            val cols = listOf(w1, w2, w3, w4)
            fun cell(cx: Int, cy: Int, cw: Int, label: String, value: String) {
                c.drawRect(cx.toFloat(), cy.toFloat(), (cx + cw).toFloat(), (cy + rowH).toFloat(), gridP)
                c.drawText(label, (cx + 6).toFloat(), (cy + 8).toFloat(), labelP)
                if (value.isNotBlank()) c.drawText(value, (cx + 6).toFloat(), (cy + 19).toFloat(), valueP)
            }
            var cx = x
            val r1 = listOf(
                "SUBJECT:" to subject, "CLASS:" to classLabel,
                "ROLL NO:" to "______________",
                "PAPER CODE:" to header.paperCode.ifBlank { "____________" }
            )
            for ((i, cellData) in r1.withIndex()) { cell(cx, y, cols[i], cellData.first, cellData.second); cx += cols[i] }
            cx = x
            val r2y = y + rowH
            val r2 = listOf(
                "TIME:" to timeText, "TOTAL MARKS:" to totalMarksText,
                "PASSING MARKS:" to header.passingMarks.ifBlank { "____________" },
                "DATE:" to dateText
            )
            for ((i, cellData) in r2.withIndex()) { cell(cx, r2y, cols[i], cellData.first, cellData.second); cx += cols[i] }
            val r3y = y + rowH * 2
            c.drawRect(x.toFloat(), r3y.toFloat(), (x + CONTENT_W).toFloat(), (r3y + rowH).toFloat(), gridP)
            c.drawText("STUDENT NAME:", (x + 6).toFloat(), (r3y + 8).toFloat(), labelP)
            c.drawText("____________________________________________________________", (x + 6).toFloat(), (r3y + 19).toFloat(), valueP)
        }

        if (!chaptersLabel.isNullOrBlank()) {
            val chapL = layoutOf("Chapter(s):  $chaptersLabel", paint(10.5f))
            out += Block(chapL.height + 8) { c, x, y ->
                c.save(); c.translate(x.toFloat(), (y + 2).toFloat()); chapL.draw(c); c.restore()
            }
        }
        return out
    }

    private class Block(
        val height: Int,
        val forceNewPage: Boolean = false,
        val draw: (Canvas, Int, Int) -> Unit
    )

    private class Placement(val block: Block, val page: Int, val y: Int)

    private fun topFor(page: Int): Int = if (page == 0) MARGIN else MARGIN + MINI_HEADER_H

    private fun pageContentHeight(): Int = PAGE_H - MARGIN - topFor(1)

    private fun stackDraw(layouts: List<StaticLayout>): (Canvas, Int, Int) -> Unit = { c, x, y ->
        var yy = y
        for (l in layouts) {
            c.save()
            c.translate(x.toFloat(), yy.toFloat())
            l.draw(c)
            c.restore()
            yy += l.height
        }
    }

    private fun lineRangeDraw(l: StaticLayout, startLine: Int, endLineExclusive: Int): (Canvas, Int, Int) -> Unit =
        { c, x, y ->
            val top = l.getLineTop(startLine)
            val bottom = l.getLineBottom(endLineExclusive - 1)
            c.save()
            c.translate(x.toFloat(), (y - top).toFloat())
            c.clipRect(0f, top.toFloat(), l.width.toFloat(), bottom.toFloat())
            l.draw(c)
            c.restore()
        }

    /** Turns a list of layouts into blocks; splits line-by-line only if taller than one page. */
    private fun blocksFor(layouts: List<StaticLayout>, gapAfter: Int = 10): List<Block> {
        val totalH = layouts.sumOf { it.height } + gapAfter
        if (totalH <= pageContentHeight()) {
            return listOf(Block(totalH, draw = stackDraw(layouts)))
        }
        val blocks = mutableListOf<Block>()
        var cur = mutableListOf<Triple<StaticLayout, Int, Int>>()
        var curH = 0
        fun flush() {
            if (cur.isEmpty()) return
            val chunk = cur.toList()
            val h = curH
            blocks += Block(h) { c, x, y ->
                var yy = y
                for ((l, s, e) in chunk) {
                    lineRangeDraw(l, s, e)(c, x, yy)
                    yy += l.getLineBottom(e - 1) - l.getLineTop(s)
                }
            }
            cur = mutableListOf()
            curH = 0
        }
        val maxH = pageContentHeight()
        for (l in layouts) {
            var s = 0
            while (s < l.lineCount) {
                var e = s
                var h = 0
                while (e < l.lineCount) {
                    val lh = l.getLineBottom(e) - l.getLineTop(e)
                    if (curH + h + lh > maxH && (cur.isNotEmpty() || h > 0)) break
                    h += lh
                    e++
                }
                if (e == s) {
                    e = s + 1
                    h = l.getLineBottom(s) - l.getLineTop(s)
                }
                cur += Triple(l, s, e)
                curH += h
                s = e
                if (s < l.lineCount) flush()
            }
        }
        flush()
        blocks += Block(gapAfter) { _, _, _ -> }
        return blocks
    }

    private fun boldPrefix(prefix: String, body: String): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        val start = sb.length
        sb.append(prefix)
        sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, 0)
        sb.append(body)
        return sb
    }

    // ---------- MCQ table (rows & columns, light-blue design) ----------
    private val TBL_HEAD_BG = Color.rgb(219, 234, 248)
    private val TBL_NUM_BG = Color.rgb(235, 243, 251)
    private val TBL_LINE = Color.rgb(120, 155, 195)
    private const val TBL_NUM_W = 36
    private const val TBL_OPT_W = 64
    private const val TBL_HEAD_H = 30
    private const val TBL_Q_W = CONTENT_W - TBL_NUM_W - 4 * TBL_OPT_W

    private fun centeredLayout(text: String, p: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()

    /** Replicates the pagination pass so MCQ table chunks know where pages break. */
    private fun simulateEnd(blocks: List<Block>): Pair<Int, Int> {
        var page = 0
        var y = topFor(0)
        var has = false
        for (b in blocks) {
            if (b.forceNewPage) {
                if (has) {
                    page++
                    y = topFor(page)
                    has = false
                }
                y += b.height
                has = true
                continue
            }
            if (y + b.height > PAGE_H - MARGIN && has) {
                page++
                y = topFor(page)
                has = false
            }
            y += b.height
            has = true
        }
        return page to y
    }

    private class McqRow(
        val numLayout: StaticLayout,
        val qLayout: StaticLayout,
        val optLayouts: List<StaticLayout>,
        val height: Int
    )

    private fun mcqRow(q: Question, index: Int): McqRow {
        val numLayout = centeredLayout("${index + 1}", paint(11.5f, bold = true, color = NAVY), TBL_NUM_W - 8)
        val qLayout = layoutOf(q.questionText, paint(11f), TBL_Q_W - 12)
        val optLayouts = q.options.map { layoutOf(it, paint(10.5f), TBL_OPT_W - 24) }
        val h = maxOf(numLayout.height, qLayout.height, optLayouts.maxOfOrNull { it.height } ?: 0) + 14
        return McqRow(numLayout, qLayout, optLayouts, h)
    }

    private fun drawTableHeader(c: Canvas, x: Int, y: Int) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TBL_HEAD_BG; style = Paint.Style.FILL }
        c.drawRect(x.toFloat(), y.toFloat(), (x + CONTENT_W).toFloat(), (y + TBL_HEAD_H).toFloat(), bg)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TBL_LINE; style = Paint.Style.STROKE; strokeWidth = 1f
        }
        c.drawRect(x.toFloat(), y.toFloat(), (x + CONTENT_W).toFloat(), (y + TBL_HEAD_H).toFloat(), border)
        var vx = x + TBL_NUM_W
        for (w in listOf(TBL_Q_W, TBL_OPT_W, TBL_OPT_W, TBL_OPT_W)) {
            c.drawLine(vx.toFloat(), y.toFloat(), vx.toFloat(), (y + TBL_HEAD_H).toFloat(), border)
            vx += w
        }
        val hp = paint(11f, bold = true, color = NAVY)
        val labels = listOf("Q. No", "Question", "A", "B", "C", "D")
        val widths = listOf(TBL_NUM_W, TBL_Q_W, TBL_OPT_W, TBL_OPT_W, TBL_OPT_W, TBL_OPT_W)
        var lx = x
        for (i in labels.indices) {
            val l = centeredLayout(labels[i], hp, widths[i] - 4)
            c.save()
            c.translate((lx + 2).toFloat(), (y + (TBL_HEAD_H - l.height) / 2).toFloat())
            l.draw(c)
            c.restore()
            lx += widths[i]
        }
    }

    private fun drawTableRow(c: Canvas, x: Int, y: Int, row: McqRow) {
        val h = row.height
        val numBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TBL_NUM_BG; style = Paint.Style.FILL }
        c.drawRect(x.toFloat(), y.toFloat(), (x + TBL_NUM_W).toFloat(), (y + h).toFloat(), numBg)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TBL_LINE; style = Paint.Style.STROKE; strokeWidth = 1f
        }
        c.drawRect(x.toFloat(), y.toFloat(), (x + CONTENT_W).toFloat(), (y + h).toFloat(), border)
        var vx = x + TBL_NUM_W
        for (w in listOf(TBL_Q_W, TBL_OPT_W, TBL_OPT_W, TBL_OPT_W)) {
            c.drawLine(vx.toFloat(), y.toFloat(), vx.toFloat(), (y + h).toFloat(), border)
            vx += w
        }
        c.save()
        c.translate((x + 4).toFloat(), (y + (h - row.numLayout.height) / 2).toFloat())
        row.numLayout.draw(c)
        c.restore()
        c.save()
        c.translate((x + TBL_NUM_W + 6).toFloat(), (y + (h - row.qLayout.height) / 2).toFloat())
        row.qLayout.draw(c)
        c.restore()
        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105); style = Paint.Style.STROKE; strokeWidth = 1.1f
        }
        var ox = x + TBL_NUM_W + TBL_Q_W
        for (ol in row.optLayouts) {
            c.drawCircle((ox + 11).toFloat(), (y + h / 2).toFloat(), 4.2f, circle)
            c.save()
            c.translate((ox + 20).toFloat(), (y + (h - ol.height) / 2).toFloat())
            ol.draw(c)
            c.restore()
            ox += TBL_OPT_W
        }
    }

    /**
     * Appends the MCQs as a table: one row per MCQ, options in A/B/C/D columns.
     * The header row repeats at the top of every page the table flows onto
     * (header and its first row are fused into one block so the header is
     * never left alone at a page bottom). MCQs that cannot be tabled
     * (option count other than 4, or a row taller than a page) fall back to
     * the classic stacked layout.
     */
    private fun addMcqTable(blocks: MutableList<Block>, mcqs: List<Question>) {
        if (mcqs.isEmpty()) return
        var end = simulateEnd(blocks)
        var page = end.first
        var y = end.second
        var chunkStart = true
        mcqs.forEachIndexed { idx, q ->
            val row = mcqRow(q, idx)
            if (q.options.size != 4 || row.height > pageContentHeight() - TBL_HEAD_H) {
                val qL = layoutOf("${idx + 1}.  ${q.questionText}", paint(11.5f))
                val oL = q.options.mapIndexed { oi, opt ->
                    layoutOf("        ${'A' + oi})   $opt", paint(11f))
                }
                blocks += blocksFor(listOf(qL) + oL)
                end = simulateEnd(blocks)
                page = end.first
                y = end.second
                chunkStart = true
                return@forEachIndexed
            }
            val need = row.height + if (chunkStart) TBL_HEAD_H else 0
            if (chunkStart || y + need > PAGE_H - MARGIN) {
                if (y + TBL_HEAD_H + row.height > PAGE_H - MARGIN) {
                    page += 1
                    y = topFor(page)
                }
                val r = row
                blocks += Block(TBL_HEAD_H + r.height) { c, x, yy ->
                    drawTableHeader(c, x, yy)
                    drawTableRow(c, x, yy + TBL_HEAD_H, r)
                }
                y += TBL_HEAD_H + row.height
                chunkStart = false
            } else {
                val r = row
                blocks += Block(r.height) { c, x, yy -> drawTableRow(c, x, yy, r) }
                y += row.height
            }
        }
    }

    fun generate(context: Context, data: PaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()
        val totalQuestions = data.mcqs.size + data.shorts.size + data.longs.size
        val totalMarks = data.mcqs.size +
            data.shorts.sumOf { it.marksValue } + data.longs.sumOf { it.marksValue }

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            data.timeAllowed, totalMarks.toString(), data.chaptersLabel
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key and solutions are given at the end."
        } else {
            "Instructions: Attempt all questions. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        // ---------- Sections (board style: Q.1, Q.2, ... ; MCQs numbered, written parts in roman) ----------
        var qCounter = 0
        var mcqQ = 0
        var shortQ = 0
        var longQ = 0
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 12, draw = stackDraw(listOf(h)))
        }

        // MCQs \u2014 Q.1 with a numbered table (1, 2, 3, ...)
        if (data.mcqs.isNotEmpty()) {
            qCounter++; mcqQ = qCounter
            qHeading("Q.$mcqQ  Choose the correct option:   (${data.mcqs.size} x 1 = ${data.mcqs.size})")
            addMcqTable(blocks, data.mcqs)
        }

        // Written sections (Short, then Long) \u2014 one question number each, parts (i), (ii), (iii), ...
        val qPaintW = paint(11.5f)
        val ansPaint = paint(11f, color = ANS_COLOR)
        val missPaint = paint(10.5f, color = MUTED)
        fun writtenSection(questions: List<WrittenQuestion>, heading: String, isShort: Boolean) {
            if (questions.isEmpty()) return
            qCounter++
            if (isShort) shortQ = qCounter else longQ = qCounter
            qHeading("Q.$qCounter  $heading")
            questions.forEachIndexed { idx, wq ->
                val marksSuffix = if (wq.marks.isNotBlank()) "   (Marks: ${wq.marks})" else ""
                val qLayout = layoutOf(boldPrefix("${roman(idx)}  ", wq.question + marksSuffix), qPaintW)
                blocks += blocksFor(listOf(qLayout))
            }
        }
        writtenSection(data.shorts, "Write short answers to the following questions:", true)
        writtenSection(data.longs, "Write detailed answers to the following questions:", false)

        // ---------- Answer key & solutions (at the end) ----------
        if (data.includeAnswers && totalQuestions > 0) {
            val keyTitle = layoutOf("ANSWER KEY & SOLUTIONS", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            fun solHeading(title: String) {
                val h = layoutOf(title, paint(13f, bold = true, color = NAVY))
                blocks += Block(h.height + 12) { c, x, y ->
                    c.save(); c.translate(x.toFloat(), y.toFloat()); h.draw(c); c.restore()
                }
            }
            if (data.mcqs.isNotEmpty()) solHeading("Q.$mcqQ  MCQs \u2014 Answer Key")
            val keyPaint = paint(11.5f)
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            fun solutionSection(questions: List<WrittenQuestion>, title: String) {
                if (questions.isEmpty()) return
                solHeading(title)
                questions.forEachIndexed { idx, wq ->
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("${roman(idx)}  ", wq.answer), ansPaint)))
                    } else {
                        blocks += blocksFor(listOf(layoutOf("${roman(idx)}  (Answer not available in the source data)", missPaint)))
                    }
                }
            }
            solutionSection(data.shorts, "Q.$shortQ  Short Questions \u2014 Answers")
            solutionSection(data.longs, "Q.$longQ  Long Questions \u2014 Solutions")
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    // ================= Board Pattern (Class 10 English) =================

    data class NarrItem(val sentence: String, val indirect: String)

    data class BoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val shorts: List<WrittenQuestion>,
        val q3: BoardPatternPack.Q3Para,
        val poemSummary: String,
        val paraphrase: BoardPatternPack.ParaSet,
        val essayTopics: List<String>,
        val paragraphTopics: List<String>,
        val narration: List<NarrItem>,
        val pairs: List<BoardPatternPack.PairWords>,
        val q8: BoardPatternPack.Q8Para,
        val tenSentenceTopic: String,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    private val ROMAN = listOf("(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)", "(ix)", "(x)", "(xi)", "(xii)", "(xiii)", "(xiv)", "(xv)", "(xvi)", "(xvii)", "(xviii)", "(xix)", "(xx)", "(xxi)", "(xxii)", "(xxiii)", "(xxiv)", "(xxv)", "(xxvi)", "(xxvii)", "(xxviii)", "(xxix)", "(xxx)", "(xxxi)", "(xxxii)", "(xxxiii)", "(xxxiv)", "(xxxv)", "(xxxvi)", "(xxxvii)", "(xxxviii)", "(xxxix)", "(xl)")

    private fun roman(i: Int): String = if (i in ROMAN.indices) ROMAN[i] else "(${i + 1})"

    data class CsBoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val shortSections: List<List<WrittenQuestion>>,
        val longs: List<WrittenQuestion>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    data class TqBoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val shorts: List<WrittenQuestion>,
        val meanings: List<BoardPatternPack.WordMeaning>,
        val ayat: List<WrittenQuestion>,
        val longs: List<WrittenQuestion>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    /** Board Pattern paper (Class 9 Computer Science): 50 marks —
     *  Q.1 10 MCQs; Q.2-Q.4 shorts (any 4 of 6 each); Q.5-Q.7 longs (any 2 of 3). */
    fun generateBoardPatternCs(context: Context, data: CsBoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours", "50"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (10 x 1 = 10)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        body("Time Allowed: 1 Hour 45 Minutes   •   Total Marks: 40", paint(11f, bold = true, color = NAVY))

        data.shortSections.forEachIndexed { sIdx, section ->
            qHeading("Q.${sIdx + 2}  Write short answers to any FOUR (4) questions.   (2 x 4 = 8)")
            section.forEachIndexed { i, wq ->
                body("${roman(i)}  ${wq.question}")
            }
        }

        qHeading("Write detailed answers to any TWO (2) questions.   (2 x 8 = 16)")
        data.longs.forEachIndexed { i, wq ->
            qHeading("Q.${i + 5}  ${wq.question}")
        }

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            data.shortSections.forEachIndexed { sIdx, section ->
                qHeading("Q.${sIdx + 2}  (Short Answers)")
                section.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }

            qHeading("Long Questions  (Answers)")
            data.longs.forEachIndexed { i, wq ->
                val label = "Q.${i + 5}  "
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix(label, wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$label(Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    /** Board Pattern paper (60 marks, 2 hours — Physics / Biology Class 9 & 10,
     *  Pak Studies Class 10, Chemistry Class 9): Q.1 12 MCQs spread round-robin
     *  across chapters; Q.2-Q.4 shorts (any 5 of 8 each) from three chapter
     *  groups; Q.5-Q.7 longs (any 2 of 3). */
    data class SixtyBoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val shortSections: List<List<WrittenQuestion>>,
        val longs: List<WrittenQuestion>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    fun generateBoardPattern60(context: Context, data: SixtyBoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours", "60"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (12 x 1 = 12)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        body("Time Allowed: 1 Hour 45 Minutes   •   Total Marks: 48", paint(11f, bold = true, color = NAVY))

        data.shortSections.forEachIndexed { sIdx, section ->
            qHeading("Q.${sIdx + 2}  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)")
            section.forEachIndexed { i, wq ->
                body("${roman(i)}  ${wq.question}")
            }
        }

        qHeading("Write detailed answers to any TWO (2) questions.   (2 x 9 = 18)")
        data.longs.forEachIndexed { i, wq ->
            qHeading("Q.${i + 5}  ${wq.question}")
        }

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            data.shortSections.forEachIndexed { sIdx, section ->
                qHeading("Q.${sIdx + 2}  (Short Answers)")
                section.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }

            qHeading("Long Questions  (Answers)")
            data.longs.forEachIndexed { i, wq ->
                val label = "Q.${i + 5}  "
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix(label, wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$label(Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    /** Board Pattern paper (Class 10 Tarjuma-tul-Quran): 50 marks —
     *  Q.1 10 MCQs; Q.2 shorts (any 5 of 8); Q.3 word meanings (any 5 of 8);
     *  Q.4 ayat translation (any 3 of 5); Q.5 detailed (any 1 of 2). */
    fun generateBoardPatternTq(context: Context, data: TqBoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours", "50"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (10 x 1 = 10)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        body("Time Allowed: 1 Hour 45 Minutes   •   Total Marks: 40", paint(11f, bold = true, color = NAVY))

        qHeading("Q.2  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)")
        data.shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        qHeading("Q.3  Write the meanings of any FIVE (5) of the following words.   (1 x 5 = 5)")
        val colW = CONTENT_W / 2
        val wordPaint = paint(11.5f)
        val meanWords = data.meanings.mapIndexed { idx, m -> "${roman(idx)}  ${m.word}" }
        meanWords.chunked(2).forEach { row ->
            blocks += Block(19) { c, x, y ->
                row.forEachIndexed { col, text ->
                    c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), wordPaint)
                }
            }
        }
        blocks += Block(8) { _, _, _ -> }

        qHeading("Q.4  Translate the following verses into idiomatic Urdu. Attempt any THREE (3).   (3 x 5 = 15)")
        data.ayat.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        qHeading("Q.5  Write a detailed answer to any ONE (1) of the following questions.   (1 x 10 = 10)")
        data.longs.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val kcolW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * kcolW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            qHeading("Q.2  (Short Answers)")
            data.shorts.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }

            qHeading("Q.3  (Word Meanings)")
            data.meanings.forEachIndexed { i, m ->
                val r = roman(i)
                blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", "${m.word} — ${m.meaning}"), ansPaint)), gapAfter = 6)
            }

            qHeading("Q.4  (Translations)")
            data.ayat.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }

            qHeading("Q.5  (Detailed Answer)")
            data.longs.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun generateBoardPattern(context: Context, data: BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours 30 Minutes", "75"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }
        fun orLine() {
            val h = layoutOf("OR", paint(11.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 6) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); h.draw(c); c.restore()
            }
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (19 x 1 = 19)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        val sec1 = layoutOf("SECTION-I", paint(12.5f, bold = true, color = NAVY))
        blocks += Block(sec1.height + 8, draw = stackDraw(listOf(sec1)))

        qHeading("Q.2  Answer the following questions in your own words. (Attempt any FIVE)   (Marks: 10)")
        data.shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        val sec2 = layoutOf("SECTION-II", paint(12.5f, bold = true, color = NAVY))
        blocks += Block(sec2.height + 8, draw = stackDraw(listOf(sec2)))

        qHeading("Q.3  Translate the following paragraph into Urdu:   (Marks: 08)")
        body(data.q3.text)
        orLine()
        body("Rewrite the above paragraph in simple English.")

        qHeading("Q.4  Write the summary of the poem \"${data.poemSummary}\":   (Marks: 05)")
        orLine()
        body("Paraphrase the following lines of the poem \"${data.paraphrase.poem}\":")
        data.paraphrase.lines.split("\n").filter { it.isNotBlank() }.forEach { line ->
            blocks += blocksFor(listOf(layoutOf("        $line", paint(11.5f))), gapAfter = 2)
        }
        blocks += Block(8) { _, _, _ -> }

        qHeading("Q.5  Write an essay of 150-200 words on any ONE of the following topics:   (Marks: 15)")
        data.essayTopics.forEachIndexed { i, t ->
            body("${roman(i)}  $t", paint(11.5f))
        }
        orLine()
        body("Write a paragraph of 100-150 words on any ONE of the following topics:", paint(11.5f, bold = true))
        data.paragraphTopics.forEachIndexed { i, t ->
            body("${roman(i)}  $t", paint(11.5f))
        }

        qHeading("Q.6  Change the following sentences into indirect speech. (Attempt any FIVE)   (Marks: 05)")
        data.narration.forEachIndexed { i, n ->
            body("${roman(i)}  ${n.sentence}")
        }

        qHeading("Q.7  Use the following pairs of words in your own sentences. (Attempt any FIVE)   (Marks: 05)")
        data.pairs.chunked(4).forEach { rowPairs ->
            val text = rowPairs.joinToString(" ,   ") { "${it.first} : ${it.second}" }
            blocks += blocksFor(listOf(layoutOf(text, paint(11.5f))), gapAfter = 6)
        }
        blocks += Block(6) { _, _, _ -> }

        qHeading("Q.8  Translate the following paragraph into English:   (Marks: 08)")
        body(data.q8.urdu)
        orLine()
        body("Write TEN sentences on the topic: \"${data.tenSentenceTopic}\".")

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            qHeading("Q.2  (Short Answers)")
            data.shorts.forEachIndexed { i, wq ->
                val r = roman(i)
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }

            qHeading("Q.3  (Urdu Translation)")
            if (data.q3.urdu.isNotBlank()) {
                blocks += blocksFor(listOf(layoutOf(data.q3.urdu, ansPaint)), gapAfter = 8)
            } else {
                blocks += blocksFor(listOf(layoutOf("(Model translation not available for this paragraph)", missPaint)), gapAfter = 8)
            }

            qHeading("Q.6  (Indirect Speech)")
            data.narration.forEachIndexed { i, n ->
                val r = roman(i)
                blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", n.indirect), ansPaint)), gapAfter = 6)
            }

            qHeading("Q.7  (Pair of Words - model sentences)")
            data.pairs.forEach { p ->
                blocks += blocksFor(
                    listOf(layoutOf(boldPrefix("${p.first}:  ", p.exampleFirst), ansPaint),
                           layoutOf(boldPrefix("${p.second}:  ", p.exampleSecond), ansPaint)),
                    gapAfter = 6
                )
            }

            qHeading("Q.8  (English Translation)")
            blocks += blocksFor(listOf(layoutOf(data.q8.english, ansPaint)), gapAfter = 8)
            blocks += blocksFor(listOf(layoutOf("Note: Q.4 and Q.5 answers will vary from student to student.", missPaint)), gapAfter = 8)
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }
    data class Eng11BoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val proseShorts: List<WrittenQuestion>,
        val poemShorts: List<WrittenQuestion>,
        val playShorts: List<WrittenQuestion>,
        val letterTopic: String,
        val applicationTopic: String,
        val storyA: String,
        val storyB: String,
        val refLines: String,
        val punctuation: String,
        val pairs: List<BoardPatternPack.PairWords>,
        val translation: BoardPatternPack.Q3Para,
        val essayTopic: String,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    /** Board Pattern paper (Class 11 English, PECTAA model paper): 100 marks —
     *  Q.1 20 MCQs; Section-I: Q.2 prose shorts (any 7 of 10), Q.3 poem shorts
     *  (any 5 of 8), Q.4 play shorts (any 3 of 5); Section-II: Q.5 letter OR
     *  application, Q.6 story, Q.7 reference / punctuation / pairs, Q.8
     *  translation into Urdu (essay as the alternate question). */
    fun generateBoardPatternEng11(context: Context, data: Eng11BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "3 Hours", "100"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }
        fun orLine() {
            val h = layoutOf("OR", paint(11.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 6) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); h.draw(c); c.restore()
            }
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (20 x 1 = 20)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        val sec1 = layoutOf("SECTION-I", paint(12.5f, bold = true, color = NAVY))
        blocks += Block(sec1.height + 8, draw = stackDraw(listOf(sec1)))

        qHeading("Q.2  Write short answers (in 3-5 lines/sentences) of any SEVEN questions of the following:   (2 x 7 = 14)")
        data.proseShorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        qHeading("Q.3  Write short answers (in 3-5 lines/sentences) of any FIVE questions of the following:   (2 x 5 = 10)")
        data.poemShorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        qHeading("Q.4  Write short answers (in 3-5 lines/sentences) of any THREE questions of the following:   (2 x 3 = 6)")
        data.playShorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        val sec2 = layoutOf("SECTION-II", paint(12.5f, bold = true, color = NAVY))
        blocks += Block(sec2.height + 8, draw = stackDraw(listOf(sec2)))

        qHeading("Q.5  ${data.letterTopic}   (10)")
        orLine()
        body(data.applicationTopic)

        qHeading("Q.6  Write a story on the moral lesson:   (10)")
        body("\"${data.storyA}\"")
        orLine()
        body("\"${data.storyB}\"")

        qHeading("Q.7  (a) Explain the following lines with reference to the context:   (5)")
        data.refLines.split("\n").filter { it.isNotBlank() }.forEach { line ->
            blocks += blocksFor(listOf(layoutOf("        $line", paint(11.5f))), gapAfter = 2)
        }
        blocks += Block(8) { _, _, _ -> }
        body("(b) Punctuate the following lines:   (5)", paint(11.5f, bold = true))
        body(data.punctuation)
        body("(c) Use any five of the following pairs of words in your own sentences.   (1 x 5 = 5)", paint(11.5f, bold = true))
        data.pairs.chunked(4).forEach { rowPairs ->
            val text = rowPairs.joinToString(" ,   ") { "${it.first} : ${it.second}" }
            blocks += blocksFor(listOf(layoutOf(text, paint(11.5f))), gapAfter = 6)
        }
        blocks += Block(6) { _, _, _ -> }

        qHeading("Q.8  Translate the following passage into Urdu.   (15)")
        body(data.translation.text)
        body(
            "Alternate Question for Foreign / English Medium Candidates: instead of translation, write an essay on the given topic: \"${data.essayTopic}\"   (15)",
            paint(10.5f, color = MUTED)
        )

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                qHeading(heading)
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }
            keyShorts("Q.2  (Prose Short Answers)", data.proseShorts)
            keyShorts("Q.3  (Poem Short Answers)", data.poemShorts)
            keyShorts("Q.4  (Play Short Answers)", data.playShorts)

            qHeading("Q.7(c)  (Pair of Words - model sentences)")
            data.pairs.forEach { p ->
                val layouts = mutableListOf<StaticLayout>()
                if (p.exampleFirst.isNotBlank()) {
                    layouts += layoutOf(boldPrefix("${p.first}:  ", p.exampleFirst), ansPaint)
                } else {
                    layouts += layoutOf("${p.first}:  (Model sentence not available)", missPaint)
                }
                if (p.exampleSecond.isNotBlank()) {
                    layouts += layoutOf(boldPrefix("${p.second}:  ", p.exampleSecond), ansPaint)
                } else {
                    layouts += layoutOf("${p.second}:  (Model sentence not available)", missPaint)
                }
                blocks += blocksFor(layouts, gapAfter = 6)
            }

            qHeading("Q.8  (Urdu Translation)")
            if (data.translation.urdu.isNotBlank()) {
                blocks += blocksFor(listOf(layoutOf(data.translation.urdu, ansPaint)), gapAfter = 8)
            } else {
                blocks += blocksFor(listOf(layoutOf("(Model translation not available for this passage)", missPaint)), gapAfter = 8)
            }
            blocks += blocksFor(listOf(layoutOf("Note: Q.5, Q.6, Q.7(a), Q.7(b) and the Q.8 alternate essay answers will vary from student to student.", missPaint)), gapAfter = 8)
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }
    data class Eng9BoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val verbs: List<Question>,
        val spelling: List<Question>,
        val meanings: List<Question>,
        val grammar: List<Question>,
        val voice: List<Question>,
        val q2Shorts: List<WrittenQuestion>,
        val q3Translations: List<BoardPatternPack.Eng9Trans>,
        val q4Summary: BoardPatternPack.Eng9Summary,
        val q5Words: List<BoardPatternPack.Eng9Word>,
        val q6Prompts: List<BoardPatternPack.Eng9Prompt>,
        val q7Passage: BoardPatternPack.Eng9Trans,
        val q7Questions: List<WrittenQuestion>,
        val q8Options: List<BoardPatternPack.Eng9Trans>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    /** Board Pattern paper (Class 9 English, Pairing Scheme 2027): 75 marks,
     *  2 hours 30 minutes — Q.1 19 MCQs (verbs 5, spelling 4, meanings 5,
     *  grammar 5); Q.2 short answers (attempt 3 of 5 + 1 of 2); Q.3 translate
     *  into Urdu (any 2 of 3); Q.4 poem summary; Q.5 sentences (any 5 of 8);
     *  Q.6 letter / story / dialogue (any 1 of 3); Q.7 comprehension;
     *  Q.8 translate into English (any 5 of 8); Q.9 active/passive voice (5). */
    fun generateBoardPatternEng9(context: Context, data: Eng9BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours 30 Minutes", "75"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }
        fun subHead(text: String) {
            val h = layoutOf(text, paint(11.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 6, draw = stackDraw(listOf(h)))
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (19 x 1 = 19)")
        subHead("(a)  Form of Verbs   (5)")
        addMcqTable(blocks, data.verbs)
        subHead("(b)  Correct Spelling   (4)")
        addMcqTable(blocks, data.spelling)
        subHead("(c)  Correct Meanings   (5)")
        addMcqTable(blocks, data.meanings)
        subHead("(d)  Grammar   (5)")
        addMcqTable(blocks, data.grammar)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        body("Time Allowed: 2 Hours 10 Minutes   •   Total Marks: 56", paint(11f, bold = true, color = NAVY))

        qHeading("Q.2  Write short answers to any FOUR questions — any THREE from (i) to (v) and any ONE from (vi) to (vii).   (3 x 2 + 1 x 4 = 10)")
        data.q2Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        qHeading("Q.3  Translate any TWO of the following paragraphs into Urdu.   (2 x 4 = 8)")
        data.q3Translations.forEachIndexed { i, t ->
            body("${roman(i)}  ${t.english}")
        }

        qHeading("Q.4  Write the summary of the following poem.   (5)")
        body("Poem: \"${data.q4Summary.poem}\"")

        qHeading("Q.5  Use any FIVE of the following words in your own sentences.   (5 x 1 = 5)")
        data.q5Words.forEachIndexed { i, w ->
            body("${roman(i)}  ${w.word}")
        }

        qHeading("Q.6  Attempt any ONE of the following.   (8)")
        data.q6Prompts.forEachIndexed { i, p ->
            body("${roman(i)}  ${p.prompt}")
        }

        qHeading("Q.7  Read the following passage carefully and answer the questions given at the end.   (10)")
        body(data.q7Passage.english)
        data.q7Questions.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        qHeading("Q.8  Translate any FIVE of the following paragraphs into English.   (5 x 1 = 5)")
        data.q8Options.forEachIndexed { i, t ->
            body("${roman(i)}  ${t.urdu}")
        }

        qHeading("Q.9  Choose the correct option (Active / Passive Voice).   (5 x 1 = 5)")
        addMcqTable(blocks, data.voice)

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            fun keyMcqGrid(heading: String, mcqs: List<Question>) {
                qHeading(heading)
                val colW = CONTENT_W / 4
                val entries = mcqs.mapIndexed { idx, q ->
                    val letter = if (q.correctAnswerIndex in q.options.indices) {
                        ('A' + q.correctAnswerIndex).toString()
                    } else "?"
                    "${idx + 1}.  $letter"
                }
                entries.chunked(4).forEach { row ->
                    blocks += Block(19) { c, x, y ->
                        row.forEachIndexed { col, text ->
                            c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                        }
                    }
                }
                blocks += Block(10) { _, _, _ -> }
            }
            keyMcqGrid("Q.1  (MCQs — Verbs / Spelling / Meanings / Grammar)",
                data.verbs + data.spelling + data.meanings + data.grammar)
            keyMcqGrid("Q.9  (Active / Passive Voice)", data.voice)

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                qHeading(heading)
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.7  (Comprehension Questions)", data.q7Questions)

            qHeading("Q.3  (Urdu Translations — model)")
            data.q3Translations.forEach { t ->
                if (t.urdu.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(t.urdu, ansPaint)), gapAfter = 8)
                } else {
                    blocks += blocksFor(listOf(layoutOf("(Model translation not available)", missPaint)), gapAfter = 8)
                }
            }

            qHeading("Q.4  (Poem Summary — model)")
            blocks += blocksFor(listOf(layoutOf(data.q4Summary.summary, ansPaint)), gapAfter = 8)

            qHeading("Q.5  (Model Sentences)")
            data.q5Words.forEach { w ->
                if (w.sentence.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix("${w.word}:  ", w.sentence), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("${w.word}:  (Model sentence not available)", missPaint)), gapAfter = 6)
                }
            }

            qHeading("Q.8  (English Translations — model)")
            data.q8Options.forEach { t ->
                if (t.english.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(t.english, ansPaint)), gapAfter = 8)
                } else {
                    blocks += blocksFor(listOf(layoutOf("(Model translation not available)", missPaint)), gapAfter = 8)
                }
            }
            blocks += blocksFor(listOf(layoutOf("Note: Q.6 answers will vary from student to student.", missPaint)), gapAfter = 8)
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    data class Phy12BoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val q2Shorts: List<WrittenQuestion>,
        val q3Shorts: List<WrittenQuestion>,
        val q4Shorts: List<WrittenQuestion>,
        val detailed: List<WrittenQuestion>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    /** Board Pattern paper (Class 12 Physics, official pairing scheme): 85 marks,
     *  3 hours — Q.1 17 MCQs from Chapters 13-21; Part-II: Q.2 shorts from
     *  Chapters 13-15 (any 8 of 12), Q.3 from Chapters 16-18 (any 8 of 12),
     *  Q.4 from Chapters 19-21 (any 6 of 9); Part-III: Q.5-Q.9 detailed
     *  questions / numericals, attempt any THREE. */
    fun generateBoardPatternPhy12(context: Context, data: Phy12BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "3 Hours", "85"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (17 x 1 = 17)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        body("PART-II: SHORT ANSWER QUESTIONS", paint(11.5f, bold = true, color = NAVY))

        qHeading("Q.2  Write short answers to any EIGHT (8) questions.   (2 x 8 = 16)      [Chapters 13, 14, 15]")
        data.q2Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }
        qHeading("Q.3  Write short answers to any EIGHT (8) questions.   (2 x 8 = 16)      [Chapters 16, 17, 18]")
        data.q3Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }
        qHeading("Q.4  Write short answers to any SIX (6) questions.   (2 x 6 = 12)      [Chapters 19, 20, 21]")
        data.q4Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        body("PART-III: DETAILED QUESTIONS / NUMERICALS", paint(11.5f, bold = true, color = NAVY))
        body("Attempt any THREE (3) questions. Each question carries 8 marks — a long / theoretical part (5 marks) and a numerical part (3 marks).")
        data.detailed.forEachIndexed { i, wq ->
            qHeading("Q.${i + 5}  ${wq.question}")
        }

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                qHeading(heading)
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.3  (Short Answers)", data.q3Shorts)
            keyShorts("Q.4  (Short Answers)", data.q4Shorts)

            qHeading("Detailed Questions  (Answers)")
            data.detailed.forEachIndexed { i, wq ->
                val label = "Q.${i + 5}  "
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix(label, wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$label(Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    data class Chem10BoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val q2Shorts: List<WrittenQuestion>,
        val q3Shorts: List<WrittenQuestion>,
        val q4Shorts: List<WrittenQuestion>,
        val detailed: List<WrittenQuestion>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    /** Board Pattern paper (Class 10 Chemistry, official pairing scheme from the
     *  new book): 60 marks, 2 hours - Q.1 12 MCQs, one each from Chapters 14-24
     *  and 26; Part-I: Q.2 shorts from Chapters 14-18 (any 5 of 8), Q.3 from
     *  Chapters 19-22 (any 5 of 8), Q.4 from Chapters 23-26 (any 5 of 8);
     *  Part-II: Q.5-Q.7 detailed questions (a = 5, b = 4), attempt any TWO;
     *  pairings Q.5 = Ch 14+15, Q.6 = Ch 20+22, Q.7 = Ch 23+25. */
    fun generateBoardPatternChem10(context: Context, data: Chem10BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header (school banner + info grid) ----------
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours", "60"
        )

        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts."
        } else {
            "Note: Q.1 is compulsory. In each subjective question, attempt only the required number of parts. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }

        // ---------- OBJECTIVE PART ----------
        partBanner("OBJECTIVE PART")
        qHeading("Q.1  Choose the correct option:   (12 x 1 = 12)")
        addMcqTable(blocks, data.mcqs)

        // ---------- SUBJECTIVE PART ----------
        partBanner("SUBJECTIVE PART")
        body("PART-II: SHORT ANSWER QUESTIONS", paint(11.5f, bold = true, color = NAVY))

        qHeading("Q.2  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)      [Chapters 14, 15, 16, 17, 18]")
        data.q2Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }
        qHeading("Q.3  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)      [Chapters 19, 20, 21, 22]")
        data.q3Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }
        qHeading("Q.4  Write short answers to any FIVE (5) questions.   (2 x 5 = 10)      [Chapters 23, 24, 25, 26]")
        data.q4Shorts.forEachIndexed { i, wq ->
            body("${roman(i)}  ${wq.question}")
        }

        body("PART-III: DETAILED QUESTIONS", paint(11.5f, bold = true, color = NAVY))
        body("Attempt any TWO (2) questions. Each question carries 9 marks: part (a) 5 marks and part (b) 4 marks, from the paired chapters — Q.5: Chapters 14 and 15; Q.6: Chapters 20 and 22; Q.7: Chapters 23 and 25.")
        data.detailed.forEachIndexed { i, wq ->
            qHeading("Q.${i + 5}  ${wq.question}")
        }

        // ---------- Answer key ----------
        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }

            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                qHeading(heading)
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.3  (Short Answers)", data.q3Shorts)
            keyShorts("Q.4  (Short Answers)", data.q4Shorts)

            qHeading("Detailed Questions  (Answers)")
            data.detailed.forEachIndexed { i, wq ->
                val label = "Q.${i + 5}  "
                if (wq.answer.isNotBlank()) {
                    blocks += blocksFor(listOf(layoutOf(boldPrefix(label, wq.answer), ansPaint)), gapAfter = 6)
                } else {
                    blocks += blocksFor(listOf(layoutOf("$label(Answer not available in the source data)", missPaint)), gapAfter = 6)
                }
            }
        }

        // ---------- Pagination ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        // ---------- Render ----------
        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, p + 1).create()
            val pg = doc.startPage(pageInfo)
            val c = pg.canvas
            if (p > 0) {
                c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == p }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${p + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pg)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    data class Math10BoardPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val mcqs: List<Question>,
        val q2Shorts: List<WrittenQuestion>,
        val q3Shorts: List<WrittenQuestion>,
        val q4Shorts: List<WrittenQuestion>,
        val partII: List<Pair<WrittenQuestion, WrittenQuestion>>,
        val partIII: List<Pair<WrittenQuestion, WrittenQuestion>>,
        val includeAnswers: Boolean,
        val header: SchoolHeader = SchoolHeader()
    )

    /** Board Pattern paper (Class 10 Mathematics, official PCTB pairing scheme):
     *  75 marks, 2:30 hours - Objective (20 min, 15 marks): Q.1 15 MCQs
     *  (1 each from Chapters 1-8 and 10, 2 each from Chapters 9, 11, 12).
     *  Subjective (2:10 hrs, 60 marks): Part-I Q.2 shorts from Chapters 1-4,
     *  Q.3 from Chapters 5-8, Q.4 from Chapters 9-12 (any 6 of 9 each, 2x6=12);
     *  Part-II (any 2 of 3, parts of 4 marks): Q.5 = Ch 1+2, Q.6 = Ch 3+4,
     *  Q.7 = Ch 5+7; Part-III (any 1 of 2): Q.8 = Ch 6 + (8 or 11),
     *  Q.9 = Ch 10 + (9 or 12). */
    fun generateBoardPatternMath10(context: Context, data: Math10BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()
        blocks += schoolHeaderBlocks(
            context, data.header, data.testTitle, data.subject, data.classLabel,
            "2 Hours 30 Minutes", "75"
        )
        val instrText = if (data.includeAnswers) {
            "Solved paper: the answer key is given at the end. Note: Q.1 is compulsory. Attempt only the required number of questions in each part."
        } else {
            "Note: Q.1 is compulsory. Attempt only the required number of questions in each part. Write your answers neatly in the answer copy."
        }
        val instrLayout = layoutOf(instrText, paint(10.5f, color = MUTED))
        blocks += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }
        fun partBanner(text: String) {
            val h = layoutOf(text, paint(14f, bold = true, color = NAVY))
            blocks += Block(h.height + 16) { c, x, y ->
                val tx = x + (CONTENT_W - h.width) / 2
                c.save(); c.translate(tx.toFloat(), (y + 6).toFloat()); h.draw(c); c.restore()
                val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 1.6f }
                c.drawLine(x.toFloat(), (y + 6 + h.height + 4).toFloat(), (x + CONTENT_W).toFloat(), (y + 6 + h.height + 4).toFloat(), rp)
            }
        }
        fun qHeading(text: String) {
            val h = layoutOf(text, paint(12.5f, bold = true, color = NAVY))
            blocks += Block(h.height + 8, draw = stackDraw(listOf(h)))
        }
        fun body(text: String, p: TextPaint = paint(11.5f)) {
            blocks += blocksFor(listOf(layoutOf(text, p)), gapAfter = 8)
        }
        fun partQuestion(num: Int, pair: Pair<WrittenQuestion, WrittenQuestion>) {
            qHeading("Q.$num")
            blocks += blocksFor(listOf(layoutOf(boldPrefix("(a)  ", pair.first.question), paint(11.5f))), gapAfter = 6)
            blocks += blocksFor(listOf(layoutOf(boldPrefix("(b)  ", pair.second.question), paint(11.5f))), gapAfter = 8)
        }

        partBanner("OBJECTIVE TYPE")
        body("Time Allowed: 20 Minutes   ·   Max. Marks: 15", paint(11f, bold = true, color = NAVY))
        qHeading("Q.1  Choose the correct option:   (15 x 1 = 15)")
        addMcqTable(blocks, data.mcqs)

        partBanner("SUBJECTIVE TYPE")
        body("Time Allowed: 2 Hours 10 Minutes   ·   Max. Marks: 60", paint(11f, bold = true, color = NAVY))
        body("(Part-I)", paint(11.5f, bold = true, color = NAVY))
        qHeading("Q.2  Write short answers to any SIX (06) questions.   (2 x 6 = 12)      [Chapters 1, 2, 3, 4]")
        data.q2Shorts.forEachIndexed { i, wq -> body("${roman(i)}  ${wq.question}") }
        qHeading("Q.3  Write short answers to any SIX (06) questions.   (2 x 6 = 12)      [Chapters 5, 6, 7, 8]")
        data.q3Shorts.forEachIndexed { i, wq -> body("${roman(i)}  ${wq.question}") }
        qHeading("Q.4  Write short answers to any SIX (06) questions.   (2 x 6 = 12)      [Chapters 9, 10, 11, 12]")
        data.q4Shorts.forEachIndexed { i, wq -> body("${roman(i)}  ${wq.question}") }

        body("(Part-II)", paint(11.5f, bold = true, color = NAVY))
        body("Note: Attempt any TWO (02) questions. Each question has two parts (a) and (b), carrying 4 marks each.   (2 x 8 = 16)")
        data.partII.forEachIndexed { i, pair -> partQuestion(i + 5, pair) }

        body("(Part-III)", paint(11.5f, bold = true, color = NAVY))
        body("Note: Attempt any ONE (01) question. Each question has two parts (a) and (b), carrying 4 marks each.   (1 x 8 = 8)")
        data.partIII.forEachIndexed { i, pair -> partQuestion(i + 8, pair) }

        if (data.includeAnswers) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val ansPaint = paint(11f, color = ANS_COLOR)
            val missPaint = paint(10.5f, color = MUTED)
            val keyPaint = paint(11.5f)
            qHeading("Q.1  (MCQs)")
            val colW = CONTENT_W / 4
            val entries = data.mcqs.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else "?"
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                blocks += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            blocks += Block(10) { _, _, _ -> }
            fun keyShorts(heading: String, list: List<WrittenQuestion>) {
                qHeading(heading)
                list.forEachIndexed { i, wq ->
                    val r = roman(i)
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix("$r  ", wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$r  (Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }
            keyShorts("Q.2  (Short Answers)", data.q2Shorts)
            keyShorts("Q.3  (Short Answers)", data.q3Shorts)
            keyShorts("Q.4  (Short Answers)", data.q4Shorts)
            fun keyPair(heading: String, pair: Pair<WrittenQuestion, WrittenQuestion>) {
                qHeading(heading)
                listOf("(a)  " to pair.first, "(b)  " to pair.second).forEach { (label, wq) ->
                    if (wq.answer.isNotBlank()) {
                        blocks += blocksFor(listOf(layoutOf(boldPrefix(label, wq.answer), ansPaint)), gapAfter = 6)
                    } else {
                        blocks += blocksFor(listOf(layoutOf("$label(Answer not available in the source data)", missPaint)), gapAfter = 6)
                    }
                }
            }
            data.partII.forEachIndexed { i, pair -> keyPair("Q.${i + 5}  (Long Question)", pair) }
            data.partIII.forEachIndexed { i, pair -> keyPair("Q.${i + 8}  (Long Question)", pair) }
        }

        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (block in blocks) {
            if (block.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(block, page, y)
                y += block.height
                pageHasContent = true
                continue
            }
            if (y + block.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(block, page, y)
            y += block.height
            pageHasContent = true
        }
        val totalPages = page + 1

        val doc = PdfDocument()
        val miniPaint = paint(9f, color = MUTED)
        val footerPaint = paint(8.5f, color = MUTED)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
        for (pg in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pg + 1).create()
            val pageObj = doc.startPage(pageInfo)
            val c = pageObj.canvas
            if (pg > 0) {
                c.drawText("StudyPoint \u2014 ${data.testTitle}", MARGIN.toFloat(), (MARGIN + 9).toFloat(), miniPaint)
                c.drawLine(
                    MARGIN.toFloat(), (MARGIN + 14).toFloat(),
                    (MARGIN + CONTENT_W).toFloat(), (MARGIN + 14).toFloat(), linePaint
                )
            }
            placements.filter { it.page == pg }.forEach { it.block.draw(c, MARGIN, it.y) }
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint \u2014 ${data.testTitle}", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
            val pageText = "Page ${pg + 1} of $totalPages"
            val w = footerPaint.measureText(pageText)
            c.drawText(pageText, (PAGE_W - MARGIN - w), FOOTER_Y.toFloat(), footerPaint)
            doc.finishPage(pageObj)
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }
}
