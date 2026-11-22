package com.sek789102.changkhamnuan.domain

/** Documents a job can produce. Numbers share the job's running number: QT-2026-0007, IV-2026-0007-2 … */
enum class DocType(val th: String, val en: String, val prefix: String) {
    QUOTE("ใบเสนอราคา", "QUOTATION", "QT"),
    INVOICE("ใบแจ้งหนี้", "INVOICE", "IV"),
    RECEIPT("ใบเสร็จรับเงิน", "RECEIPT", "RC"),
    BOQ("ใบแสดงปริมาณงานและราคา", "BILL OF QUANTITIES", "BQ")
}

data class PaymentSummary(val contract: Double, val scheduled: Double, val paid: Double) {
    val outstanding: Double get() = (contract - paid).coerceAtLeast(0.0)
    val unscheduled: Double get() = contract - scheduled
}

data class ProfitSummary(val revenue: Double, val labor: Double, val materials: Double, val other: Double) {
    val cost: Double get() = labor + materials + other
    val profit: Double get() = revenue - cost
    val marginPercent: Double get() = if (revenue > 0) profit / revenue * 100 else 0.0
}

data class OrderLine(val name: String, val unit: String, val qty: Double, val unitPrice: Double)

/** One stage's money split into its parts (stage amounts include VAT when the job has VAT). */
data class StageTotals(val beforeVat: Double, val vat: Double, val total: Double, val withholding: Double) {
    val toPay: Double get() = r2(total - withholding)
}

private fun r2(v: Double) = Math.round(v * 100.0) / 100.0

object Contractor {
    fun docNumber(type: DocType, year: String, no: Int, stage: Int = 0): String =
        "${type.prefix}-$year-${no.toString().padStart(4, '0')}" + (if (stage > 0) "-$stage" else "")

    fun stageAmount(percent: Double, amount: Double, contractTotal: Double): Double =
        if (percent > 0) r2(contractTotal * percent / 100) else r2(amount.coerceAtLeast(0.0))

    fun stageTotals(amount: Double, vat: Boolean, withholding: Boolean): StageTotals {
        val before = if (vat) r2(amount / (1 + QuoteMath.VAT_RATE)) else r2(amount)
        val v = r2(amount - before)
        val wht = if (withholding) r2(before * QuoteMath.WHT_RATE) else 0.0
        return StageTotals(before, v, r2(amount), wht)
    }

    /** [stages]: (percent, amount, paid). */
    fun payments(contractTotal: Double, stages: List<Triple<Double, Double, Boolean>>): PaymentSummary {
        val amounts = stages.map { stageAmount(it.first, it.second, contractTotal) to it.third }
        return PaymentSummary(r2(contractTotal), r2(amounts.sumOf { it.first }), r2(amounts.filter { it.second }.sumOf { it.first }))
    }

    /** Revenue is the job value before VAT (VAT is passed on to the tax office, not income). */
    fun profit(revenueBeforeVat: Double, costs: List<Pair<ItemKind, Double>>): ProfitSummary = ProfitSummary(
        r2(revenueBeforeVat),
        r2(costs.filter { it.first == ItemKind.LABOR }.sumOf { it.second }),
        r2(costs.filter { it.first == ItemKind.MATERIAL }.sumOf { it.second }),
        r2(costs.filter { it.first == ItemKind.OTHER }.sumOf { it.second })
    )

    /** Same material from several calculations (same name and unit) becomes one line to order. */
    fun orderList(materials: List<OrderLine>): List<OrderLine> =
        materials.filter { it.qty > 0 }
            .groupBy { it.name.trim() to it.unit.trim() }
            .map { (k, list) -> OrderLine(k.first, k.second, list.sumOf { it.qty }, list.maxOf { it.unitPrice }) }

    /** Ready-made stage splits. */
    val presets: List<Pair<String, List<Pair<String, Double>>>> = listOf(
        "มัดจำ 50% / ส่งงาน 50%" to listOf("มัดจำก่อนเริ่มงาน" to 50.0, "ส่งมอบงาน" to 50.0),
        "30 / 40 / 30" to listOf("มัดจำก่อนเริ่มงาน" to 30.0, "งานโครงสร้างเสร็จ" to 40.0, "ส่งมอบงาน" to 30.0),
        "4 งวด 25%" to listOf("มัดจำก่อนเริ่มงาน" to 25.0, "งานฐานรากและโครงสร้าง" to 25.0, "งานผนังและหลังคา" to 25.0, "ส่งมอบงาน" to 25.0)
    )
}
