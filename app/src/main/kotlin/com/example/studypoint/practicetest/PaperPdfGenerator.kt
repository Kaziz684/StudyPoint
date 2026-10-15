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
import java.io.File
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
        val includeAnswers: Boolean
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
        val sectionCount = listOf(data.mcqs, data.shorts, data.longs).count { it.isNotEmpty() }

        // ---------- Header ----------
        val iconBmp: Bitmap? = try {
            val d: Drawable = context.packageManager.getApplicationIcon(context.packageName)
            val bmp = Bitmap.createBitmap(54, 54, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            d.setBounds(0, 0, 54, 54)
            d.draw(cv)
            bmp
        } catch (e: Exception) {
            null
        }
        val appNameLayout = layoutOf("StudyPoint", paint(21f, bold = true, color = NAVY), CONTENT_W - 70)
        val appSubLayout = layoutOf("Paper Generator", paint(11f, color = MUTED), CONTENT_W - 70)
        val titleLayout = layoutOf(data.testTitle.uppercase(), paint(16f, bold = true, color = NAVY))
        blocks += Block(104) { c, x, y ->
            if (iconBmp != null) {
                c.drawBitmap(iconBmp, x.toFloat(), y.toFloat(), null)
            }
            val tx = if (iconBmp != null) x + 66 else x
            c.save(); c.translate(tx.toFloat(), y.toFloat()); appNameLayout.draw(c); c.restore()
            c.save(); c.translate(tx.toFloat(), (y + 30).toFloat()); appSubLayout.draw(c); c.restore()
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2.5f }
            c.drawLine(x.toFloat(), (y + 58).toFloat(), (x + CONTENT_W).toFloat(), (y + 58).toFloat(), rulePaint)
            val titleX = x + (CONTENT_W - titleLayout.width) / 2
            c.save(); c.translate(titleX.toFloat(), (y + 68).toFloat()); titleLayout.draw(c); c.restore()
        }

        // ---------- Info ----------
        val infoPaint = paint(10.5f)
        val infoBold = paint(10.5f, bold = true)
        val chapLayout = layoutOf("Chapter(s):  ${data.chaptersLabel}", infoPaint)
        val infoHeight = 18 + chapLayout.height + 4 + 18 + 18 + 10
        blocks += Block(infoHeight) { c, x, y ->
            var yy = y
            c.drawText("Class:  ${data.classLabel}", x.toFloat(), (yy + 11).toFloat(), infoBold)
            c.drawText("Subject:  ${data.subject}", (x + CONTENT_W / 2).toFloat(), (yy + 11).toFloat(), infoBold)
            yy += 18
            c.save(); c.translate(x.toFloat(), yy.toFloat()); chapLayout.draw(c); c.restore()
            yy += chapLayout.height + 4
            c.drawText("Total Questions:  $totalQuestions", x.toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Total Marks:  $totalMarks", (x + 170).toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Time Allowed:  ${data.timeAllowed}", (x + 340).toFloat(), (yy + 11).toFloat(), infoPaint)
            yy += 18
            c.drawText("Student Name: ____________________", x.toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Roll Number: ____________", (x + 250).toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Date: ____/____/________", (x + 410).toFloat(), (yy + 11).toFloat(), infoPaint)
        }

        // ---------- Instructions ----------
        val instrText = if (data.includeAnswers) {
            "Solved paper: correct answers are provided with the questions (MCQ answer key is at the end)."
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

        // ---------- Sections ----------
        var sectionIndex = 0
        fun sectionHeading(name: String) {
            sectionIndex++
            if (sectionCount <= 1) return
            val letter = 'A' + (sectionIndex - 1)
            val h = layoutOf("SECTION $letter — $name", paint(13f, bold = true, color = NAVY))
            blocks += Block(h.height + 10, draw = stackDraw(listOf(h)))
        }

        // MCQs section — table layout (one row per MCQ, options in A/B/C/D columns)
        if (data.mcqs.isNotEmpty()) {
            sectionHeading("MCQs")
            addMcqTable(blocks, data.mcqs)
        }

        // Written sections (Short, then Long)
        val qPaintW = paint(11.5f)
        val ansPaint = paint(11f, color = ANS_COLOR)
        val missPaint = paint(10.5f, color = MUTED)
        fun writtenSection(questions: List<WrittenQuestion>, name: String) {
            if (questions.isEmpty()) return
            sectionHeading(name)
            questions.forEachIndexed { idx, wq ->
                val marksSuffix = if (wq.marks.isNotBlank()) "   (Marks: ${wq.marks})" else ""
                val qLayout = layoutOf(boldPrefix("Q${idx + 1}.  ", wq.question + marksSuffix), qPaintW)
                val layouts = mutableListOf(qLayout)
                if (data.includeAnswers) {
                    if (wq.answer.isNotBlank()) {
                        layouts += layoutOf(boldPrefix("Answer:  ", wq.answer), ansPaint)
                    } else {
                        layouts += layoutOf("(Answer not available in the source data)", missPaint)
                    }
                }
                blocks += blocksFor(layouts)
            }
        }
        writtenSection(data.shorts, "Short Questions")
        writtenSection(data.longs, "Long Questions")

        // ---------- MCQ answer key ----------
        if (data.includeAnswers && data.mcqs.isNotEmpty()) {
            val keyTitle = layoutOf("ANSWER KEY (MCQs)", paint(15f, bold = true, color = NAVY))
            blocks += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
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
        val includeAnswers: Boolean
    )

    private val ROMAN = listOf("(i)", "(ii)", "(iii)", "(iv)", "(v)", "(vi)", "(vii)", "(viii)")

    private fun roman(i: Int): String = if (i in ROMAN.indices) ROMAN[i] else "(${i + 1})"

    fun generateBoardPattern(context: Context, data: BoardPaperData, outFile: File): File {
        val blocks = mutableListOf<Block>()

        // ---------- Header ----------
        val iconBmp: Bitmap? = try {
            val d: Drawable = context.packageManager.getApplicationIcon(context.packageName)
            val bmp = Bitmap.createBitmap(54, 54, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            d.setBounds(0, 0, 54, 54)
            d.draw(cv)
            bmp
        } catch (e: Exception) {
            null
        }
        val appNameLayout = layoutOf("StudyPoint", paint(21f, bold = true, color = NAVY), CONTENT_W - 70)
        val appSubLayout = layoutOf("Board Pattern Paper", paint(11f, color = MUTED), CONTENT_W - 70)
        val titleLayout = layoutOf(data.testTitle.uppercase(), paint(16f, bold = true, color = NAVY))
        blocks += Block(104) { c, x, y ->
            if (iconBmp != null) {
                c.drawBitmap(iconBmp, x.toFloat(), y.toFloat(), null)
            }
            val tx = if (iconBmp != null) x + 66 else x
            c.save(); c.translate(tx.toFloat(), y.toFloat()); appNameLayout.draw(c); c.restore()
            c.save(); c.translate(tx.toFloat(), (y + 30).toFloat()); appSubLayout.draw(c); c.restore()
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2.5f }
            c.drawLine(x.toFloat(), (y + 58).toFloat(), (x + CONTENT_W).toFloat(), (y + 58).toFloat(), rulePaint)
            val titleX = x + (CONTENT_W - titleLayout.width) / 2
            c.save(); c.translate(titleX.toFloat(), (y + 68).toFloat()); titleLayout.draw(c); c.restore()
        }

        // ---------- Info ----------
        val infoPaint = paint(10.5f)
        val infoBold = paint(10.5f, bold = true)
        blocks += Block(64) { c, x, y ->
            var yy = y
            c.drawText("Class:  ${data.classLabel}", x.toFloat(), (yy + 11).toFloat(), infoBold)
            c.drawText("Subject:  ${data.subject}", (x + CONTENT_W / 2).toFloat(), (yy + 11).toFloat(), infoBold)
            yy += 18
            c.drawText("Total Marks:  75", x.toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Time Allowed:  2 Hours 30 Minutes", (x + 170).toFloat(), (yy + 11).toFloat(), infoPaint)
            yy += 18
            c.drawText("Student Name: ____________________", x.toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Roll Number: ____________", (x + 250).toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Date: ____/____/________", (x + 410).toFloat(), (yy + 11).toFloat(), infoPaint)
        }

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
}
