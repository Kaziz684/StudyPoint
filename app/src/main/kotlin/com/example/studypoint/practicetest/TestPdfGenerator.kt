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
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a printable A4 MCQs test paper PDF from real stored [Question]s.
 * Layout is computed in a placement pass first (so footers can show
 * "Page X of Y"), then rendered with [PdfDocument]. Questions are kept
 * together on one page whenever they fit; only an unusually long question
 * is split line-by-line as a fallback.
 */
object TestPdfGenerator {

    data class TestPaperData(
        val testTitle: String,
        val classLabel: String,
        val subject: String,
        val chaptersLabel: String,
        val timeAllowed: String,
        val questions: List<Question>,
        val withAnswers: Boolean
    )

    private const val PAGE_W = 595 // A4 at 72 dpi
    private const val PAGE_H = 842
    private const val MARGIN = 42
    private const val CONTENT_W = PAGE_W - 2 * MARGIN
    private const val MINI_HEADER_H = 22
    private const val FOOTER_Y = PAGE_H - MARGIN + 16

    private val NAVY = Color.rgb(15, 23, 42)
    private val GREEN = Color.rgb(16, 185, 129)
    private val TEXT = Color.rgb(30, 41, 59)
    private val MUTED = Color.rgb(100, 116, 139)

    private fun paint(size: Float, bold: Boolean = false, color: Int = TEXT): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        return p
    }

    private fun layoutOf(text: String, p: TextPaint, width: Int = CONTENT_W): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width).build()

    /** A drawable block with a fixed height, placed by the pagination pass. */
    private class Block(
        val height: Int,
        val forceNewPage: Boolean = false,
        val draw: (Canvas, Int, Int) -> Unit
    )

    private class Placement(val unit: Block, val page: Int, val y: Int)

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
    private fun simulateEnd(units: List<Block>): Pair<Int, Int> {
        var page = 0
        var y = topFor(0)
        var has = false
        for (b in units) {
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

    /** Classic stacked layout, kept as the fallback for MCQs that cannot be tabled. */
    private fun addStackedFallback(units: MutableList<Block>, q: Question, index: Int) {
        val n = index + 1
        val qLayout = layoutOf("$n.  ${q.questionText}", paint(11.5f))
        val optLayouts = q.options.mapIndexed { oi, opt ->
            layoutOf("        ${'A' + oi})   $opt", paint(11f))
        }
        val all = listOf(qLayout) + optLayouts
        val totalH = all.sumOf { it.height } + 10
        val maxBlockH = pageContentHeight()
        if (totalH <= maxBlockH) {
            units += Block(totalH, draw = stackDraw(all))
            return
        }
        var cur = mutableListOf<Triple<StaticLayout, Int, Int>>()
        var curH = 0
        fun flush() {
            if (cur.isEmpty()) return
            val chunk = cur.toList()
            val h = curH
            units += Block(h) { c, x, y ->
                var yy = y
                for ((l, s, e) in chunk) {
                    lineRangeDraw(l, s, e)(c, x, yy)
                    yy += l.getLineBottom(e - 1) - l.getLineTop(s)
                }
            }
            cur = mutableListOf()
            curH = 0
        }
        for (l in all) {
            var s = 0
            while (s < l.lineCount) {
                var e = s
                var h = 0
                while (e < l.lineCount) {
                    val lh = l.getLineBottom(e) - l.getLineTop(e)
                    if (curH + h + lh > maxBlockH && (cur.isNotEmpty() || h > 0)) break
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
        units += Block(10) { _, _, _ -> }
    }

    /**
     * Appends the MCQs as a table: one row per MCQ, options in A/B/C/D columns.
     * The header row repeats at the top of every page the table flows onto
     * (header and its first row are fused into one block so the header is
     * never left alone at a page bottom).
     */
    private fun addMcqTable(units: MutableList<Block>, questions: List<Question>) {
        if (questions.isEmpty()) return
        var end = simulateEnd(units)
        var page = end.first
        var y = end.second
        var chunkStart = true
        questions.forEachIndexed { idx, q ->
            val row = mcqRow(q, idx)
            if (q.options.size != 4 || row.height > pageContentHeight() - TBL_HEAD_H) {
                addStackedFallback(units, q, idx)
                end = simulateEnd(units)
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
                units += Block(TBL_HEAD_H + r.height) { c, x, yy ->
                    drawTableHeader(c, x, yy)
                    drawTableRow(c, x, yy + TBL_HEAD_H, r)
                }
                y += TBL_HEAD_H + row.height
                chunkStart = false
            } else {
                val r = row
                units += Block(r.height) { c, x, yy -> drawTableRow(c, x, yy, r) }
                y += row.height
            }
        }
    }

    fun generate(context: Context, data: TestPaperData, outFile: File): File {
        val units = mutableListOf<Block>()

        // ---------- Header (page 1) ----------
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
        val appSubLayout = layoutOf("MCQs Test Generator", paint(11f, color = MUTED), CONTENT_W - 70)
        val titleLayout = layoutOf(
            data.testTitle.uppercase(), paint(16f, bold = true, color = NAVY), CONTENT_W
        )
        units += Block(104) { c, x, y ->
            if (iconBmp != null) {
                c.drawBitmap(iconBmp, x.toFloat(), y.toFloat(), null)
                c.save(); c.translate((x + 66).toFloat(), y.toFloat()); appNameLayout.draw(c); c.restore()
                c.save(); c.translate((x + 66).toFloat(), (y + 30).toFloat()); appSubLayout.draw(c); c.restore()
            } else {
                c.save(); c.translate(x.toFloat(), y.toFloat()); appNameLayout.draw(c); c.restore()
                c.save(); c.translate(x.toFloat(), (y + 30).toFloat()); appSubLayout.draw(c); c.restore()
            }
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2.5f }
            c.drawLine(x.toFloat(), (y + 58).toFloat(), (x + CONTENT_W).toFloat(), (y + 58).toFloat(), rulePaint)
            // Centered test title
            val titleX = x + (CONTENT_W - titleLayout.width) / 2
            c.save(); c.translate(titleX.toFloat(), (y + 68).toFloat()); titleLayout.draw(c); c.restore()
        }

        // ---------- Info block ----------
        val infoPaint = paint(10.5f)
        val infoBold = paint(10.5f, bold = true)
        val chapLayout = layoutOf("Chapter(s):  ${data.chaptersLabel}", infoPaint)
        val infoHeight = 18 + chapLayout.height + 4 + 18 + 18 + 10
        units += Block(infoHeight) { c, x, y ->
            var yy = y
            c.drawText("Class:  ${data.classLabel}", x.toFloat(), (yy + 11).toFloat(), infoBold)
            c.drawText("Subject:  ${data.subject}", (x + CONTENT_W / 2).toFloat(), (yy + 11).toFloat(), infoBold)
            yy += 18
            c.save(); c.translate(x.toFloat(), yy.toFloat()); chapLayout.draw(c); c.restore()
            yy += chapLayout.height + 4
            c.drawText("Total MCQs:  ${data.questions.size}", x.toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Total Marks:  ${data.questions.size}", (x + 170).toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Time Allowed:  ${data.timeAllowed}", (x + 340).toFloat(), (yy + 11).toFloat(), infoPaint)
            yy += 18
            c.drawText("Student Name: ____________________", x.toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Roll Number: ____________", (x + 250).toFloat(), (yy + 11).toFloat(), infoPaint)
            c.drawText("Date: ____/____/________", (x + 410).toFloat(), (yy + 11).toFloat(), infoPaint)
        }

        // ---------- Instructions ----------
        val instrLayout = layoutOf(
            "Instructions: Choose the correct answer for each question. Each question carries 1 mark.",
            paint(10.5f, color = MUTED)
        )
        units += Block(instrLayout.height + 12) { c, x, y ->
            c.save(); c.translate(x.toFloat(), y.toFloat()); instrLayout.draw(c); c.restore()
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225); strokeWidth = 1f }
            val ly = y + instrLayout.height + 6
            c.drawLine(x.toFloat(), ly.toFloat(), (x + CONTENT_W).toFloat(), ly.toFloat(), linePaint)
        }

        // ---------- Questions (table layout: one row per MCQ, options in A/B/C/D columns) ----------
        addMcqTable(units, data.questions)

        // ---------- Answer key ----------
        var missingAnswers = 0
        if (data.withAnswers && data.questions.isNotEmpty()) {
            val keyTitle = layoutOf("ANSWER KEY", paint(15f, bold = true, color = NAVY), CONTENT_W)
            units += Block(40, forceNewPage = true) { c, x, y ->
                val tx = x + (CONTENT_W - keyTitle.width) / 2
                c.save(); c.translate(tx.toFloat(), y.toFloat()); keyTitle.draw(c); c.restore()
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN; strokeWidth = 2f }
                c.drawLine(x.toFloat(), (y + 30).toFloat(), (x + CONTENT_W).toFloat(), (y + 30).toFloat(), rulePaint)
            }
            val keyPaint = paint(11.5f)
            val colW = CONTENT_W / 4
            val entries = data.questions.mapIndexed { idx, q ->
                val letter = if (q.correctAnswerIndex in q.options.indices) {
                    ('A' + q.correctAnswerIndex).toString()
                } else {
                    missingAnswers++
                    "?"
                }
                "${idx + 1}.  $letter"
            }
            entries.chunked(4).forEach { row ->
                units += Block(19) { c, x, y ->
                    row.forEachIndexed { col, text ->
                        c.drawText(text, (x + col * colW).toFloat(), (y + 13).toFloat(), keyPaint)
                    }
                }
            }
            if (missingAnswers > 0) {
                val note = layoutOf(
                    "Note: Answers marked ? were not available in the source data.",
                    paint(10f, color = MUTED)
                )
                units += Block(note.height + 8, draw = stackDraw(listOf(note)))
            }
        }

        // ---------- Pagination pass ----------
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = topFor(0)
        var pageHasContent = false
        for (unit in units) {
            if (unit.forceNewPage) {
                if (pageHasContent) {
                    page++
                    y = topFor(page)
                    pageHasContent = false
                }
                placements += Placement(unit, page, y)
                y += unit.height
                pageHasContent = true
                continue
            }
            if (y + unit.height > PAGE_H - MARGIN && pageHasContent) {
                page++
                y = topFor(page)
                pageHasContent = false
            }
            placements += Placement(unit, page, y)
            y += unit.height
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
            placements.filter { it.page == p }.forEach { it.unit.draw(c, MARGIN, it.y) }
            // Footer
            c.drawLine(
                MARGIN.toFloat(), (FOOTER_Y - 10).toFloat(),
                (MARGIN + CONTENT_W).toFloat(), (FOOTER_Y - 10).toFloat(), linePaint
            )
            c.drawText("StudyPoint — MCQs Test Paper", MARGIN.toFloat(), FOOTER_Y.toFloat(), footerPaint)
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
