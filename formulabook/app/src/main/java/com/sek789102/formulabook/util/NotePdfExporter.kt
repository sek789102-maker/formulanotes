package com.sek789102.formulabook.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.resolveDisplayName
import com.sek789102.formulabook.domain.markdown.MarkdownBlock
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotePdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 42f
    private const val MARGIN_TOP = 42f
    private const val MARGIN_BOTTOM = 44f
    private val CONTENT_WIDTH = (PAGE_WIDTH - MARGIN_X * 2).toInt()

    fun exportNoteToPdfUri(
        context: Context,
        noteWithTags: NoteWithTags,
        subject: SubjectEntity?
    ): Uri? {
        return try {
            val thaiTypeface = ResourcesCompat.getFont(context, R.font.noto_sans_thai) ?: Typeface.DEFAULT
            val monoTypeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: thaiTypeface
            val note = noteWithTags.note
            val subjectName = subject.resolveDisplayName(context)
            val dateText = SimpleDateFormat("d MMMM yyyy · HH:mm", Locale.forLanguageTag("th-TH"))
                .format(Date(note.updatedAt))

            val document = PdfDocument()
            try {
                var pageNumber = 1
                var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                var canvas = page.canvas
                var cursorY = MARGIN_TOP

            fun ensureSpace(neededHeight: Float) {
                if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    cursorY = MARGIN_TOP
                }
            }

            // Paint styles
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(thaiTypeface, Typeface.BOLD)
                textSize = 20f
                color = Color.parseColor("#0F172A")
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = thaiTypeface
                textSize = 11f
                color = Color.parseColor("#475569")
            }
            val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(thaiTypeface, Typeface.BOLD)
                textSize = 14f
                color = Color.parseColor("#1D4ED8")
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = thaiTypeface
                textSize = 12.5f
                color = Color.parseColor("#1E293B")
            }
            val formulaTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(thaiTypeface, Typeface.BOLD)
                textSize = 12f
                color = Color.parseColor("#1D4ED8")
            }
            val formulaExprPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = monoTypeface
                textSize = 14f
                color = Color.parseColor("#0F172A")
            }
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EFF6FF")
                style = Paint.Style.FILL
            }
            val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#BFDBFE")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            // Header Accent
            val headerBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subject?.colorHex?.toInt() ?: Color.parseColor("#2563EB")
            }
            canvas.drawRoundRect(MARGIN_X, cursorY, MARGIN_X + 6f, cursorY + 46f, 3f, 3f, headerBarPaint)

            val titleLayout = staticLayout(
                note.title.ifBlank { context.getString(R.string.note_untitled_paren) },
                titlePaint,
                CONTENT_WIDTH - 16
            )
            canvas.save()
            canvas.translate(MARGIN_X + 14f, cursorY)
            titleLayout.draw(canvas)
            canvas.restore()
            cursorY += maxOf(28f, titleLayout.height.toFloat()) + 4f

            val metaLine = buildString {
                append(subjectName)
                append("  •  ")
                append(dateText)
                if (noteWithTags.tags.isNotEmpty()) {
                    append("  •  ")
                    append(noteWithTags.tags.joinToString(" ") { "#${it.name}" })
                }
            }
            val metaLayout = staticLayout(metaLine, metaPaint, CONTENT_WIDTH - 16)
            canvas.save()
            canvas.translate(MARGIN_X + 14f, cursorY)
            metaLayout.draw(canvas)
            canvas.restore()
            cursorY += metaLayout.height + 14f

            canvas.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, dividerPaint)
            cursorY += 14f

            // Body Markdown lines
            if (note.content.isNotBlank()) {
                val blocks = SimpleMarkdown.parseBlocks(note.content)
                for (block in blocks) {
                    val (displayText, paintUsed) = when (block) {
                        is MarkdownBlock.Heading -> SimpleMarkdown.buildInlineAnnotatedString(block.text).text to headingPaint
                        is MarkdownBlock.Bullet -> ("• " + SimpleMarkdown.buildInlineAnnotatedString(block.text).text) to bodyPaint
                        is MarkdownBlock.Checklist -> {
                            val mark = if (block.checked) "☑ " else "☐ "
                            (mark + SimpleMarkdown.buildInlineAnnotatedString(block.text).text) to bodyPaint
                        }
                        is MarkdownBlock.Paragraph -> SimpleMarkdown.buildInlineAnnotatedString(block.text).text to bodyPaint
                    }
                    if (displayText.isEmpty()) {
                        cursorY += 8f
                        continue
                    }
                    val layout = staticLayout(displayText, paintUsed, CONTENT_WIDTH)
                    ensureSpace(layout.height.toFloat() + 6f)
                    canvas.save()
                    canvas.translate(MARGIN_X, cursorY)
                    layout.draw(canvas)
                    canvas.restore()
                    cursorY += layout.height + 5f
                }
                cursorY += 10f
            }

            // Formulas Section
            if (note.formulas.isNotEmpty()) {
                val sectionHeader = staticLayout(
                    context.getString(R.string.detail_formulas_section, note.formulas.size),
                    headingPaint,
                    CONTENT_WIDTH
                )
                ensureSpace(sectionHeader.height + 60f)
                canvas.save()
                canvas.translate(MARGIN_X, cursorY)
                sectionHeader.draw(canvas)
                canvas.restore()
                cursorY += sectionHeader.height + 10f

                for (formula in note.formulas) {
                    val title = formula.title.ifBlank { context.getString(R.string.share_formula_fallback) }
                    val tLayout = staticLayout(title, formulaTitlePaint, CONTENT_WIDTH - 24)
                    val eLayout = staticLayout(formula.expression, formulaExprPaint, CONTENT_WIDTH - 24)
                    val vLayout = if (formula.variablesNote.isNotBlank()) {
                        staticLayout(formula.variablesNote, metaPaint, CONTENT_WIDTH - 24)
                    } else null

                    val boxHeight = 12f + tLayout.height + 6f + eLayout.height +
                        (vLayout?.let { 6f + it.height } ?: 0f) + 12f
                    ensureSpace(boxHeight + 10f)

                    val rect = RectF(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + boxHeight)
                    canvas.drawRoundRect(rect, 10f, 10f, cardBgPaint)
                    canvas.drawRoundRect(rect, 10f, 10f, cardBorderPaint)

                    var innerY = cursorY + 12f
                    drawLayout(canvas, tLayout, MARGIN_X + 12f, innerY)
                    innerY += tLayout.height + 6f
                    drawLayout(canvas, eLayout, MARGIN_X + 12f, innerY)
                    innerY += eLayout.height
                    if (vLayout != null) {
                        innerY += 6f
                        drawLayout(canvas, vLayout, MARGIN_X + 12f, innerY)
                    }

                    cursorY += boxHeight + 10f
                }
            }

            // Attached Images
            if (note.imagePaths.isNotEmpty()) {
                val imgHeader = staticLayout(
                    context.getString(R.string.detail_images_section, note.imagePaths.size),
                    headingPaint,
                    CONTENT_WIDTH
                )
                ensureSpace(imgHeader.height + 140f)
                canvas.save()
                canvas.translate(MARGIN_X, cursorY)
                imgHeader.draw(canvas)
                canvas.restore()
                cursorY += imgHeader.height + 10f

                for (imgName in note.imagePaths) {
                    val file = NoteImageStorage.resolveImageFile(context, imgName)
                    val bitmap = NoteImageStorage.decodeSampledBitmap(file, 1200) ?: continue
                    val maxImgWidth = CONTENT_WIDTH.toFloat()
                    val maxImgHeight = 260f
                    val scale = minOf(maxImgWidth / bitmap.width.toFloat(), maxImgHeight / bitmap.height.toFloat(), 1f)
                    val drawW = bitmap.width * scale
                    val drawH = bitmap.height * scale
                    ensureSpace(drawH + 12f)
                    val dest = RectF(MARGIN_X, cursorY, MARGIN_X + drawW, cursorY + drawH)
                    canvas.drawBitmap(bitmap, null, dest, null)
                    bitmap.recycle()
                    cursorY += drawH + 12f
                }
            }

            document.finishPage(page)

            val outDir = File(context.cacheDir, "shared_pdfs").apply { if (!exists()) mkdirs() }
            pruneOldPdfs(outDir)
            val safeName = note.title
                .replace(Regex("[^a-zA-Z0-9ก-๙_-]"), "_")
                .take(40)
                .ifBlank { "note_${note.id}" }
            val outFile = File(outDir, "${safeName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            } finally {
                runCatching { document.close() }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun pruneOldPdfs(dir: File) {
        runCatching {
            val files = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: return
            val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            files.forEachIndexed { index, file ->
                if (index >= 8 || file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        }
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.15f)
            .setIncludePad(false)
            .build()
    }

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
