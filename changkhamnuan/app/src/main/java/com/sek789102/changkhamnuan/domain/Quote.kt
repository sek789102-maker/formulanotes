package com.sek789102.changkhamnuan.domain

enum class ItemKind { MATERIAL, LABOR, OTHER }

data class QuoteLine(val kind: ItemKind, val qty: Double, val unitPrice: Double) {
    val amount: Double get() = qty * unitPrice
}

data class QuoteOptions(
    /** ค่าดำเนินการ/กำไร % on the subtotal. */
    val overheadPercent: Double = 0.0,
    val discount: Double = 0.0,
    val vat: Boolean = false,
    /** หัก ณ ที่จ่าย 3% (shown for company customers; deducted from the amount paid). */
    val withholding: Boolean = false
)

data class QuoteTotals(
    val materials: Double,
    val labor: Double,
    val other: Double,
    val subtotal: Double,
    val overhead: Double,
    val discount: Double,
    val beforeVat: Double,
    val vat: Double,
    val total: Double,
    val withholding: Double,
    val toPay: Double
)

object QuoteMath {
    const val VAT_RATE = 0.07
    const val WHT_RATE = 0.03

    private fun r2(v: Double) = Math.round(v * 100.0) / 100.0

    fun totals(lines: List<QuoteLine>, o: QuoteOptions): QuoteTotals {
        val m = r2(lines.filter { it.kind == ItemKind.MATERIAL }.sumOf { it.amount })
        val l = r2(lines.filter { it.kind == ItemKind.LABOR }.sumOf { it.amount })
        val x = r2(lines.filter { it.kind == ItemKind.OTHER }.sumOf { it.amount })
        val sub = r2(m + l + x)
        val overhead = r2(sub * o.overheadPercent.coerceAtLeast(0.0) / 100)
        val discount = r2(o.discount.coerceIn(0.0, sub + overhead))
        val before = r2(sub + overhead - discount)
        val vat = if (o.vat) r2(before * VAT_RATE) else 0.0
        val total = r2(before + vat)
        // Withholding is calculated on the amount before VAT (Thai practice for service work).
        val wht = if (o.withholding) r2(before * WHT_RATE) else 0.0
        return QuoteTotals(m, l, x, sub, overhead, discount, before, vat, total, wht, r2(total - wht))
    }

    /** "หนึ่งหมื่นสองพันบาทถ้วน" — Thai baht text used on Thai quotations. */
    fun bahtText(amount: Double): String {
        val satang = Math.round(amount * 100) % 100
        val baht = Math.round(amount * 100) / 100
        if (baht == 0L && satang == 0L) return "ศูนย์บาทถ้วน"
        val b = if (baht > 0) readNumber(baht) + "บาท" else ""
        val s = if (satang > 0) readNumber(satang) + "สตางค์" else "ถ้วน"
        return b + s
    }

    private val digits = listOf("", "หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก", "เจ็ด", "แปด", "เก้า")
    private val places = listOf("", "สิบ", "ร้อย", "พัน", "หมื่น", "แสน")

    private fun readNumber(n: Long): String {
        if (n >= 1_000_000) {
            val high = n / 1_000_000
            val low = n % 1_000_000
            return readNumber(high) + "ล้าน" + (if (low > 0) readUnderMillion(low, afterMillion = true) else "")
        }
        return readUnderMillion(n, afterMillion = false)
    }

    private fun readUnderMillion(n: Long, afterMillion: Boolean): String {
        val s = n.toString()
        val sb = StringBuilder()
        val len = s.length
        for ((idx, ch) in s.withIndex()) {
            val d = ch - '0'
            val pos = len - idx - 1
            if (d == 0) continue
            when {
                pos == 0 && d == 1 && (len > 1 || afterMillion) -> sb.append("เอ็ด")
                pos == 1 && d == 2 -> sb.append("ยี่")
                pos == 1 && d == 1 -> {}
                else -> sb.append(digits[d])
            }
            sb.append(places[pos])
        }
        return sb.toString()
    }
}
