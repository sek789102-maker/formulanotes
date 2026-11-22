package com.sek789102.changkhamnuan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.sek789102.changkhamnuan.R
import com.sek789102.changkhamnuan.data.BusinessProfile
import com.sek789102.changkhamnuan.data.ItemEntity
import com.sek789102.changkhamnuan.data.ProjectEntity
import com.sek789102.changkhamnuan.domain.Contractor
import com.sek789102.changkhamnuan.domain.DocType
import com.sek789102.changkhamnuan.domain.Fmt
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.QuoteLine
import com.sek789102.changkhamnuan.domain.QuoteMath
import com.sek789102.changkhamnuan.domain.QuoteOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Draws a Thai quotation (ใบเสนอราคา). The same drawing code makes an A4 PDF (with page
 * breaks) and one tall PNG for sending in LINE. Only the contractor's details are printed.
 */
/** A single payment stage printed as its own invoice / receipt. */
data class StageDoc(val no: Int, val title: String, val amount: Double, val percent: Double, val contractTotal: Double)

class QuoteRenderer(private val context: Context) {
    private val regular: Typeface = ResourcesCompat.getFont(context, R.font.ibm_plex_sans_thai_regular) ?: Typeface.DEFAULT
    private val bold: Typeface = ResourcesCompat.getFont(context, R.font.ibm_plex_sans_thai_bold) ?: Typeface.DEFAULT_BOLD
    private val thai = Locale.forLanguageTag("th-TH")

    private val w = 595f // A4 width in points
    private val margin = 36f
    private val ink = Color.rgb(28, 32, 40)
    private val muted = Color.rgb(100, 108, 120)
    private val accent = Color.rgb(36, 36, 36)        // charcoal: table header, totals
    private val gold = Color.rgb(138, 97, 0)          // dark gold: section labels (readable on white)
    private val yellow = Color.rgb(255, 194, 26)      // brand yellow: bars
    private val line = Color.rgb(210, 216, 224)

    private fun paint(size: Float, b: Boolean = false, color: Int = ink) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (b) bold else regular
        textSize = size
        this.color = color
    }

    private fun layout(text: String, p: TextPaint, width: Float, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt().coerceAtLeast(1)).setAlignment(align).build()

    fun quoteNumber(p: ProjectEntity): String = docNumber(p, DocType.QUOTE)

    fun docNumber(p: ProjectEntity, type: DocType, stage: Int = 0): String =
        Contractor.docNumber(type, SimpleDateFormat("yyyy", Locale.US).format(Date(p.createdAt)), p.quoteNo, stage)

    /** Draws everything; [ensure] is asked before each block so the PDF can start a new page. */
    private fun draw(
        project: ProjectEntity,
        items: List<ItemEntity>,
        profile: BusinessProfile,
        doc: DocType,
        stage: StageDoc?,
        canvasProvider: () -> Canvas,
        ensure: (needed: Float, y: Float) -> Float
    ): Float {
        var c = canvasProvider()
        var y = margin
        val contentW = w - margin * 2

        // While the item table is being drawn, a new PDF page starts with the table header again.
        var repeatHeader: ((Canvas, Float) -> Float)? = null
        fun block(height: Float, drawIt: (Canvas, Float) -> Unit) {
            val ny = ensure(height, y)
            if (ny != y) {
                c = canvasProvider()
                y = ny
                repeatHeader?.let { y = it(c, y) }
            }
            drawIt(c, y)
            y += height
        }

        // Header: contractor (left) and document title (right)
        val shop = layout(listOf(profile.shopName, profile.ownerName).filter { it.isNotBlank() }.joinToString("\n").ifBlank { "—" }, paint(15f, true), contentW * 0.6f)
        val contact = layout(
            listOf(
                profile.address,
                listOfNotNull(profile.phone.takeIf { it.isNotBlank() }?.let { "โทร $it" }, profile.lineId.takeIf { it.isNotBlank() }?.let { "LINE $it" }).joinToString("  "),
                profile.taxId.takeIf { it.isNotBlank() }?.let { "เลขประจำตัวผู้เสียภาษี $it" }.orEmpty()
            ).filter { it.isNotBlank() }.joinToString("\n"),
            paint(9.5f, color = muted), contentW * 0.6f
        )
        val title = layout("${doc.th}\n${doc.en}", paint(if (doc == DocType.BOQ) 13f else 17f, true, accent), contentW * 0.4f, Layout.Alignment.ALIGN_OPPOSITE)
        val issued = Date(if (doc == DocType.QUOTE) project.createdAt else System.currentTimeMillis())
        val dateFmt = SimpleDateFormat("d MMMM yyyy", thai)
        val third = when (doc) {
            DocType.QUOTE -> "ยืนราคา ${project.validDays} วัน"
            DocType.BOQ -> ""
            else -> "อ้างอิงใบเสนอราคา ${quoteNumber(project)}"
        }
        val meta = layout(
            listOf("เลขที่ ${docNumber(project, doc, stage?.no ?: 0)}", "วันที่ ${dateFmt.format(issued)}", third).filter { it.isNotBlank() }.joinToString("\n"),
            paint(9.5f, color = muted), contentW * 0.4f, Layout.Alignment.ALIGN_OPPOSITE
        )
        val headH = maxOf(shop.height + contact.height + 4f, title.height + meta.height + 4f) + 10f
        block(headH) { cv, top ->
            cv.save(); cv.translate(margin, top); shop.draw(cv); cv.translate(0f, shop.height + 4f); contact.draw(cv); cv.restore()
            cv.save(); cv.translate(margin + contentW * 0.6f, top); title.draw(cv); cv.translate(0f, title.height + 4f); meta.draw(cv); cv.restore()
        }
        block(4f) { cv, top -> cv.drawRect(margin, top, w - margin, top + 3f, Paint().apply { color = yellow }) }

        // Customer
        val cust = layout(
            listOf(
                "เรียน " + project.customerName.ifBlank { "ลูกค้า" } + (if (project.customerPhone.isNotBlank()) "   โทร ${project.customerPhone}" else ""),
                "งาน: ${project.name}" + (if (project.siteAddress.isNotBlank()) "   สถานที่: ${project.siteAddress}" else "")
            ).joinToString("\n"),
            paint(10.5f), contentW
        )
        block(cust.height + 14f) { cv, top -> cv.save(); cv.translate(margin, top + 8f); cust.draw(cv); cv.restore() }

        // Table
        val colNo = 24f
        val colQty = 58f
        val colUnit = 46f
        val colPrice = 72f
        val colAmt = 84f
        val colName = contentW - colNo - colQty - colUnit - colPrice - colAmt
        val headP = paint(9.5f, true, Color.WHITE)
        fun row(cv: Canvas, top: Float, cells: List<Pair<StaticLayout, Float>>) {
            var x = margin
            cells.forEach { (l, cw) -> cv.save(); cv.translate(x + 4f, top + 4f); l.draw(cv); cv.restore(); x += cw }
        }
        val headCells = listOf(
            layout("#", headP, colNo - 8f) to colNo,
            layout("รายการ", headP, colName - 8f) to colName,
            layout("จำนวน", headP, colQty - 8f, Layout.Alignment.ALIGN_OPPOSITE) to colQty,
            layout("หน่วย", headP, colUnit - 8f) to colUnit,
            layout("ราคา/หน่วย", headP, colPrice - 8f, Layout.Alignment.ALIGN_OPPOSITE) to colPrice,
            layout("จำนวนเงิน", headP, colAmt - 8f, Layout.Alignment.ALIGN_OPPOSITE) to colAmt
        )
        val headRowH = headCells.maxOf { it.first.height } + 8f
        fun drawHeader(cv: Canvas, top: Float): Float {
            cv.drawRect(margin, top, w - margin, top + headRowH, Paint().apply { color = accent })
            cv.drawRect(margin, top + headRowH - 2f, w - margin, top + headRowH, Paint().apply { color = yellow })
            row(cv, top, headCells)
            return top + headRowH
        }
        fun tableHeader() = block(headRowH) { cv, top -> drawHeader(cv, top) }

        val sections = listOf(ItemKind.MATERIAL to "ค่าวัสดุ", ItemKind.LABOR to "ค่าแรง", ItemKind.OTHER to "ค่าใช้จ่ายอื่น")
        var n = 0
        val bodyP = paint(9.5f)
        val secP = paint(10f, true, gold)
        tableHeader()
        repeatHeader = { cv, top -> drawHeader(cv, top) }
        fun itemRow(name: String, qty: Double, unit: String, price: Double) {
            n++
            val cells = listOf(
                layout("$n", bodyP, colNo - 8f) to colNo,
                layout(name, bodyP, colName - 8f) to colName,
                layout(Fmt.n(qty), bodyP, colQty - 8f, Layout.Alignment.ALIGN_OPPOSITE) to colQty,
                layout(unit, bodyP, colUnit - 8f) to colUnit,
                layout(Fmt.money(price), bodyP, colPrice - 8f, Layout.Alignment.ALIGN_OPPOSITE) to colPrice,
                layout(Fmt.money(qty * price), bodyP, colAmt - 8f, Layout.Alignment.ALIGN_OPPOSITE) to colAmt
            )
            val h = cells.maxOf { c2 -> c2.first.height } + 8f
            block(h) { cv, top ->
                row(cv, top, cells)
                cv.drawLine(margin, top + h, w - margin, top + h, Paint().apply { color = line; strokeWidth = 0.6f })
            }
        }
        fun sectionLabel(label: String) {
            val sec = layout(label, secP, contentW - 8f)
            block(sec.height + 8f) { cv, top -> cv.save(); cv.translate(margin + 4f, top + 4f); sec.draw(cv); cv.restore() }
        }
        fun subtotalRow(label: String, v: Double) {
            val l = layout(label, paint(9.5f, true), colName + colQty + colUnit + colPrice - 8f, Layout.Alignment.ALIGN_OPPOSITE)
            val vl = layout(Fmt.money(v), paint(9.5f, true), colAmt - 8f, Layout.Alignment.ALIGN_OPPOSITE)
            block(l.height + 8f) { cv, top ->
                cv.drawRect(margin, top, w - margin, top + l.height + 8f, Paint().apply { color = Color.rgb(250, 244, 226) })
                cv.save(); cv.translate(margin + colNo + 4f, top + 4f); l.draw(cv); cv.restore()
                cv.save(); cv.translate(w - margin - colAmt + 4f, top + 4f); vl.draw(cv); cv.restore()
            }
        }
        when {
            stage != null -> itemRow(
                "ค่างานงวดที่ ${stage.no}: ${stage.title}" +
                    (if (stage.percent > 0) " (${Fmt.n(stage.percent)}% ของมูลค่างาน ${Fmt.money(stage.contractTotal)} บาท)" else ""),
                1.0, "งวด", stage.amount
            )
            doc == DocType.BOQ -> items.groupBy { it.group.ifBlank { "รายการอื่น ๆ" } }.forEach { (group, rows) ->
                sectionLabel(group)
                rows.forEach { itemRow(it.name, it.qty, it.unit, it.unitPrice) }
                subtotalRow("รวม $group", rows.sumOf { it.qty * it.unitPrice })
            }
            else -> sections.forEach { (kind, label) ->
                val rows = items.filter { it.kind == kind }
                if (rows.isEmpty()) return@forEach
                sectionLabel(label)
                rows.forEach { itemRow(it.name, it.qty, it.unit, it.unitPrice) }
            }
        }

        repeatHeader = null

        // Totals
        val full = QuoteMath.totals(
            items.map { QuoteLine(it.kind, it.qty, it.unitPrice) },
            QuoteOptions(project.overheadPercent, project.discount, project.vat, project.withholding)
        )
        val st = stage?.let { Contractor.stageTotals(it.amount, project.vat, project.withholding) }
        val t = if (st == null) full else full.copy(beforeVat = st.beforeVat, vat = st.vat, total = st.total, withholding = st.withholding, toPay = st.toPay)
        val totalRows = if (st != null) buildList {
            if (project.vat) {
                add("มูลค่าก่อนภาษี" to st.beforeVat)
                add("ภาษีมูลค่าเพิ่ม 7%" to st.vat)
            }
        } else buildList {
            add("รวมค่าวัสดุ" to t.materials)
            add("รวมค่าแรง" to t.labor)
            if (t.other > 0) add("รวมค่าใช้จ่ายอื่น" to t.other)
            if (t.overhead > 0) add("ค่าดำเนินการ ${Fmt.n(project.overheadPercent)}%" to t.overhead)
            if (t.discount > 0) add("ส่วนลด" to -t.discount)
            if (project.vat) {
                add("ยอดก่อนภาษี" to t.beforeVat)
                add("ภาษีมูลค่าเพิ่ม 7%" to t.vat)
            }
        }
        val labelP = paint(10f, color = muted)
        val valP = paint(10f)
        totalRows.forEach { (label, v) ->
            val l = layout(label, labelP, 200f, Layout.Alignment.ALIGN_OPPOSITE)
            val vl = layout(Fmt.money(v), valP, colAmt - 8f, Layout.Alignment.ALIGN_OPPOSITE)
            block(l.height + 4f) { cv, top ->
                cv.save(); cv.translate(w - margin - colAmt - 208f, top + 2f); l.draw(cv); cv.restore()
                cv.save(); cv.translate(w - margin - colAmt + 4f, top + 2f); vl.draw(cv); cv.restore()
            }
        }
        val grandL = layout("ยอดรวมทั้งสิ้น", paint(12f, true), 200f, Layout.Alignment.ALIGN_OPPOSITE)
        val grandV = layout(Fmt.money(t.total), paint(12f, true, accent), colAmt + 40f, Layout.Alignment.ALIGN_OPPOSITE)
        val words = layout("(" + QuoteMath.bahtText(t.total) + ")", paint(9.5f, color = muted), contentW, Layout.Alignment.ALIGN_OPPOSITE)
        block(grandL.height + words.height + 14f) { cv, top ->
            cv.drawRect(w - margin - 260f, top + 1f, w - margin, top + 3.5f, Paint().apply { color = yellow })
            cv.save(); cv.translate(w - margin - colAmt - 208f, top + 6f); grandL.draw(cv); cv.restore()
            cv.save(); cv.translate(w - margin - 4f - (colAmt + 40f), top + 6f); grandV.draw(cv); cv.restore()
            cv.save(); cv.translate(margin, top + 8f + grandL.height); words.draw(cv); cv.restore()
        }
        if (project.withholding) {
            val wl = layout(
                "หัก ณ ที่จ่าย 3% (ของยอดก่อนภาษี) ${Fmt.money(t.withholding)} บาท · ยอดชำระสุทธิ ${Fmt.money(t.toPay)} บาท",
                paint(9.5f, color = muted), contentW, Layout.Alignment.ALIGN_OPPOSITE
            )
            block(wl.height + 6f) { cv, top -> cv.save(); cv.translate(margin, top + 2f); wl.draw(cv); cv.restore() }
        }

        if (doc == DocType.RECEIPT) {
            val rl = layout("ได้รับเงินจำนวนข้างต้นไว้ถูกต้องแล้ว", paint(11f, true, accent), contentW)
            block(rl.height + 12f) { cv, top -> cv.save(); cv.translate(margin, top + 8f); rl.draw(cv); cv.restore() }
        }

        // Notes & terms
        val notes = listOf(
            project.note.takeIf { it.isNotBlank() && doc != DocType.RECEIPT }?.let { "หมายเหตุ: $it" },
            profile.terms.takeIf { it.isNotBlank() && doc == DocType.QUOTE }?.let { "เงื่อนไข: $it" },
            "ปริมาณวัสดุเป็นค่าประมาณจากการคำนวณ อาจปรับตามหน้างานจริง".takeIf { doc == DocType.QUOTE || doc == DocType.BOQ },
            "เอกสารนี้ไม่ใช่ใบกำกับภาษี".takeIf { (doc == DocType.INVOICE || doc == DocType.RECEIPT) && project.vat }
        ).filterNotNull().joinToString("\n")
        val nl = layout(notes, paint(9f, color = muted), contentW)
        block(nl.height + 18f) { cv, top -> cv.save(); cv.translate(margin, top + 12f); nl.draw(cv); cv.restore() }

        // Signatures
        val sigP = paint(9.5f, color = muted)
        val (sigA, sigB) = when (doc) {
            DocType.QUOTE -> "ผู้เสนอราคา" to "ผู้อนุมัติ / ลูกค้า"
            DocType.INVOICE -> "ผู้วางบิล" to "ผู้รับวางบิล"
            DocType.RECEIPT -> "ผู้รับเงิน" to "ผู้จ่ายเงิน"
            DocType.BOQ -> "ผู้จัดทำ" to "ผู้ตรวจสอบ"
        }
        val s1 = layout("$sigA\n\n\n( ${profile.ownerName.ifBlank { " ".repeat(30) }} )", sigP, 200f, Layout.Alignment.ALIGN_CENTER)
        val s2 = layout("$sigB\n\n\n( ${" ".repeat(30)} )", sigP, 200f, Layout.Alignment.ALIGN_CENTER)
        block(s1.height + 24f) { cv, top ->
            cv.save(); cv.translate(margin + 10f, top + 18f); s1.draw(cv); cv.restore()
            cv.save(); cv.translate(w - margin - 210f, top + 18f); s2.draw(cv); cv.restore()
        }
        return y + margin
    }

    fun writePdf(project: ProjectEntity, items: List<ItemEntity>, profile: BusinessProfile, out: File, doc: DocType = DocType.QUOTE, stage: StageDoc? = null) {
        val doc = PdfDocument()
        val pageH = 842f
        var pageNo = 0
        var page: PdfDocument.Page? = null
        fun newPage(): Canvas {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(w.toInt(), pageH.toInt(), pageNo).create())
            return page!!.canvas
        }
        var current: Canvas? = null
        try {
            draw(
                project, items, profile, doc, stage,
                canvasProvider = { current ?: newPage().also { current = it } },
                ensure = { needed, y ->
                    if (y + needed > pageH - margin && y > margin + 1f) {
                        current = newPage()
                        margin
                    } else y
                }
            )
            page?.let { doc.finishPage(it) }
            FileOutputStream(out).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    /** One tall image (good for LINE). Rendered at 2x for sharp text. */
    fun writePng(project: ProjectEntity, items: List<ItemEntity>, profile: BusinessProfile, out: File, doc: DocType = DocType.QUOTE, stage: StageDoc? = null) {
        val scale = 2f
        // First pass measures the height.
        val probe = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val height = draw(project, items, profile, doc, stage, { Canvas(probe) }, { _, y -> y })
        probe.recycle()
        val bmp = Bitmap.createBitmap((w * scale).toInt(), (height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawColor(Color.WHITE)
        cv.scale(scale, scale)
        draw(project, items, profile, doc, stage, { cv }, { _, y -> y })
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }
}

/** BOQ as a spreadsheet file (UTF-8 with BOM so Excel shows Thai correctly). */
object BoqCsv {
    private fun q(v: String) = "\"" + v.replace("\"", "\"\"") + "\""

    fun build(project: ProjectEntity, items: List<ItemEntity>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append(listOf("หมวดงาน", "ลำดับ", "รายการ", "ประเภท", "จำนวน", "หน่วย", "ราคาต่อหน่วย", "จำนวนเงิน").joinToString(",") { q(it) }).append("\r\n")
        var n = 0
        var grand = 0.0
        items.groupBy { it.group.ifBlank { "รายการอื่น ๆ" } }.forEach { (group, rows) ->
            rows.forEach { it2 ->
                n++
                val kind = when (it2.kind) { ItemKind.MATERIAL -> "วัสดุ"; ItemKind.LABOR -> "ค่าแรง"; ItemKind.OTHER -> "อื่น ๆ" }
                sb.append(listOf(q(group), "$n", q(it2.name), q(kind), plain(it2.qty), q(it2.unit), plain(it2.unitPrice), plain(it2.qty * it2.unitPrice)).joinToString(",")).append("\r\n")
            }
            val sub = rows.sumOf { it.qty * it.unitPrice }
            grand += sub
            sb.append(listOf(q(group), "", q("รวม $group"), "", "", "", "", plain(sub)).joinToString(",")).append("\r\n")
        }
        sb.append(listOf(q(""), "", q("รวมทั้งหมด (ก่อนค่าดำเนินการ ส่วนลด และภาษี)"), "", "", "", "", plain(grand)).joinToString(",")).append("\r\n")
        sb.append(q("งาน: ${project.name}")).append("\r\n")
        return sb.toString()
    }

    private fun plain(v: Double) = String.format(Locale.US, "%.2f", v)
}
