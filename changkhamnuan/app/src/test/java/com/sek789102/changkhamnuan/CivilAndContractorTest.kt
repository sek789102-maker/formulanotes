package com.sek789102.changkhamnuan

import com.sek789102.changkhamnuan.domain.CalcResult
import com.sek789102.changkhamnuan.domain.CivilCalculators
import com.sek789102.changkhamnuan.domain.Contractor
import com.sek789102.changkhamnuan.domain.DocType
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.OrderLine
import com.sek789102.changkhamnuan.domain.Rates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CivilAndContractorTest {
    private val r = Rates()
    private fun buy(res: CalcResult, key: String) = res.materials.first { it.priceKey == key }.buyQty
    private fun fact(res: CalcResult, label: String) = res.facts.first { it.first.startsWith(label) }.second

    @Test fun `stock bars cut short pieces and lap long ones`() {
        assertEquals(11.0, CivilCalculators.stockBars(3.95, 21.0, 10.0, 12.0), 0.0) // 2 pieces per bar
        assertEquals(2.0, CivilCalculators.stockBars(12.0, 1.0, 10.0, 12.0), 0.0)   // 12 m + 0.48 m lap
        assertEquals(0.0, CivilCalculators.stockBars(0.0, 5.0, 10.0, 12.0), 0.0)
    }

    @Test fun `slab mesh by spacing`() {
        // 4 × 5 m, DB12 @ 20 cm, cover 2.5 cm: 20 bars × 4.95 m + 25 bars × 3.95 m
        val res = CivilCalculators.rebarGrid.run(emptyMap(), r)
        assertEquals(23.0, buy(res, "rebar_DB12"), 0.0) // 10 + 13 stock bars
        assertEquals(3.0, buy(res, "tie_wire"), 0.0)    // 175.8 kg × 15 kg/t = 2.6
    }

    @Test fun `beam bars and stirrups`() {
        // 4 beams 4 m, 20×40, 4-DB12 (4.6 m each), RB6 @ 15 cm: 27 stirrups × 1.12 m per beam
        val res = CivilCalculators.rebarGrid.run(mapOf("member" to "beam"), r)
        assertEquals(8.0, buy(res, "rebar_DB12"), 0.0)  // 16 pieces of 4.6 m, 2 per bar
        assertEquals(14.0, buy(res, "rebar_RB6"), 0.0)  // 108 pieces of 1.12 m, 8 per bar
    }

    @Test fun `structural steel weight from section`() {
        assertEquals(48.73, CivilCalculators.sectionKgPerM("h", 200.0, 200.0, 0.0, 8.0, 12.0), 0.01)
        assertEquals(3.445, CivilCalculators.sectionKgPerM("box", 50.0, 50.0, 0.0, 2.3, 0.0), 0.001)
        val res = CivilCalculators.steelSection.run(emptyMap(), r)
        assertTrue(fact(res, "ต้องซื้อ").startsWith("10 เส้น"))
    }

    @Test fun `bored piles concrete`() {
        val res = CivilCalculators.piles.run(mapOf("type" to "bored"), r) // 12 piles Ø35 × 6 m, +10%
        assertEquals(8.0, buy(res, "ready_mix"), 0.0) // 7.62 m³ → 8
        assertEquals(12.0, CivilCalculators.piles.run(emptyMap(), r).let { buy(it, "pile_driven") }, 0.0)
    }

    @Test fun `concrete road`() {
        val res = CivilCalculators.road.run(emptyMap(), r) // 4 × 50 m, 15 cm
        assertEquals(31.5, buy(res, "ready_mix"), 0.0)
        assertEquals(126.0, buy(res, "dowel"), 0.0) // 9 joints × 14 dowels
        assertEquals(36.0, buy(res, "joint_sealant"), 0.0)
    }

    @Test fun `drainage pipes and manholes`() {
        val res = CivilCalculators.drain.run(emptyMap(), r)
        assertEquals(40.0, buy(res, "drain_pipe_d40"), 0.0)
        assertEquals(5.0, buy(res, "manhole"), 0.0)
    }

    @Test fun `slope tool`() {
        val res = CivilCalculators.slope.run(emptyMap(), r) // 1% over 50 m
        assertTrue(res.materials.isEmpty())
        assertEquals("50 ซม.", fact(res, "ต่างระดับ"))
        assertEquals("-0.5", fact(res, "ระดับปลายทาง (ลาดลง)"))
        val deg = CivilCalculators.slope.run(mapOf("mode" to "ratio", "ratio" to "4", "run" to "8"), r)
        assertEquals("200 ซม.", fact(deg, "ต่างระดับ"))
    }

    @Test fun `stairs steps`() {
        val res = CivilCalculators.stairs.run(emptyMap(), r) // 3 m, riser ≤ 18 cm
        assertTrue(fact(res, "จำนวนขั้น").startsWith("17 ขั้น"))
        assertEquals(17.0, res.laborBase, 0.0)
    }

    @Test fun `formwork with reuse`() {
        val res = CivilCalculators.formwork.run(emptyMap(), r) // 8 columns 20×20 × 3 m = 19.2 m²
        assertEquals(19.2, res.laborBase, 1e-9)
        assertEquals(3.0, buy(res, "plywood"), 0.0) // 19.2 × 1.1 / 2.88 / 3 = 2.44
    }

    @Test fun `cut and fill from five levels`() {
        val res = CivilCalculators.cutFill.run(emptyMap(), r)
        assertEquals("10 ลบ.ม.", fact(res, "ปริมาตรดินตัด"))
        assertEquals("140 ลบ.ม.", fact(res, "ปริมาตรดินถม"))
        assertEquals(169.0, buy(res, "fill_soil"), 0.0) // (140 − 10) × 1.3
        assertTrue(res.materials.none { it.priceKey == "haul_soil" })
    }

    @Test fun `tank sizes`() {
        val res = CivilCalculators.tanks.run(emptyMap(), r) // 5 people
        assertTrue(res.materials.any { it.name.contains("ถังบำบัดสำเร็จรูป 1,000 ลิตร") })
        assertEquals(1000.0 to 1.0, CivilCalculators.pickTank(900.0, listOf(500.0, 1000.0)))
        assertEquals(1000.0 to 3.0, CivilCalculators.pickTank(2500.0, listOf(500.0, 1000.0)))
    }

    @Test fun `document numbers`() {
        assertEquals("QT-2026-0007", Contractor.docNumber(DocType.QUOTE, "2026", 7))
        assertEquals("IV-2026-0007-2", Contractor.docNumber(DocType.INVOICE, "2026", 7, 2))
    }

    @Test fun `stage money with vat and withholding`() {
        val t = Contractor.stageTotals(107.0, vat = true, withholding = true)
        assertEquals(100.0, t.beforeVat, 0.0); assertEquals(7.0, t.vat, 0.0); assertEquals(3.0, t.withholding, 0.0); assertEquals(104.0, t.toPay, 0.0)
        assertEquals(30000.0, Contractor.stageAmount(30.0, 0.0, 100000.0), 0.0)
        assertEquals(5000.0, Contractor.stageAmount(0.0, 5000.0, 100000.0), 0.0)
        val pay = Contractor.payments(100000.0, listOf(Triple(30.0, 0.0, true), Triple(40.0, 0.0, false), Triple(0.0, 10000.0, false)))
        assertEquals(80000.0, pay.scheduled, 0.0); assertEquals(30000.0, pay.paid, 0.0); assertEquals(70000.0, pay.outstanding, 0.0)
        Contractor.presets.forEach { (_, s) -> assertEquals(100.0, s.sumOf { it.second }, 1e-9) }
    }

    @Test fun `profit and order list`() {
        val p = Contractor.profit(100000.0, listOf(ItemKind.LABOR to 30000.0, ItemKind.MATERIAL to 40000.0, ItemKind.OTHER to 5000.0))
        assertEquals(25000.0, p.profit, 0.0); assertEquals(25.0, p.marginPercent, 1e-9)
        val o = Contractor.orderList(listOf(OrderLine("ปูนซีเมนต์", "ถุง", 4.0, 150.0), OrderLine("ปูนซีเมนต์ ", "ถุง", 6.0, 0.0), OrderLine("ทราย", "คิว", 1.0, 0.0)))
        assertEquals(2, o.size)
        assertEquals(10.0, o.first { it.name == "ปูนซีเมนต์" }.qty, 0.0)
    }
}
