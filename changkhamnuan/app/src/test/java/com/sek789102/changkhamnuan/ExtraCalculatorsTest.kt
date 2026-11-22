package com.sek789102.changkhamnuan

import com.sek789102.changkhamnuan.domain.Calculators
import com.sek789102.changkhamnuan.domain.Catalog
import com.sek789102.changkhamnuan.domain.ChoiceField
import com.sek789102.changkhamnuan.domain.ExtraCalculators
import com.sek789102.changkhamnuan.domain.Rates
import com.sek789102.changkhamnuan.ui.screens.calcGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraCalculatorsTest {
    private val r = Rates()
    private fun buy(res: com.sek789102.changkhamnuan.domain.CalcResult, key: String) = res.materials.first { it.priceKey == key }.buyQty

    @Test fun `every option of every calculator gives finite results with priced materials`() {
        for (calc in Calculators.all) {
            val variants = listOf(emptyMap<String, String>()) + calc.fields.filterIsInstance<ChoiceField>()
                .flatMap { f -> f.options.map { mapOf(f.key to it.first) } }
            for (v in variants) {
                val res = calc.run(v, r)
                if (v.isEmpty()) assertTrue("${calc.id} has no materials", res.materials.isNotEmpty())
                for (m in res.materials) {
                    assertTrue("${calc.id} $v ${m.priceKey} not in catalog", Catalog.byKey.containsKey(m.priceKey))
                    assertTrue("${calc.id} ${m.name} qty", m.qty.isFinite() && m.qty >= 0)
                    assertTrue("${calc.id} ${m.name} buy", m.buyQty.isFinite() && m.buyQty > 0)
                }
                assertTrue(res.laborBase.isFinite())
            }
        }
    }

    @Test fun `every calculator is on the home screen once`() {
        val ids = calcGroups.flatMap { it.second }
        assertEquals(Calculators.all.map { it.id }.sorted(), ids.sorted())
        assertEquals(Catalog.items.size, Catalog.byKey.size) // no duplicate price keys
    }

    @Test fun `gable tile roof`() {
        // 9.2 × 11.2 m with eaves, slope 4.6 / cos30° = 5.3116 m, area 118.98 m²
        val res = ExtraCalculators.roof.run(emptyMap(), r)
        assertEquals(118.98, res.laborBase, 0.01)
        assertEquals(1250.0, buy(res, "roof_tile"), 0.0) // 118.98 × 10 × 1.05 = 1249.3
        assertEquals(34.0, buy(res, "roof_ridge"), 0.0) // 11.2 m × 3
    }

    @Test fun `metal roof sheets per side`() {
        val res = ExtraCalculators.roof.run(mapOf("mat" to "metal", "shape" to "shed", "w" to "4", "l" to "6", "overhang" to "0", "pitch" to "10"), r)
        // 6 / 0.76 → 8 sheets; slope 4 / cos10° = 4.0617 → cut 4.1 m
        assertEquals(8 * 4.1, buy(res, "metal_sheet"), 1e-9)
    }

    @Test fun `earth fill and excavation trucks`() {
        val fill = ExtraCalculators.fill.run(emptyMap(), r) // 10 × 20 × 0.5 = 100 m³ +30%
        assertEquals(130.0, buy(fill, "fill_soil"), 1e-9)
        assertTrue(fill.facts.any { it.second == "11 เที่ยว" })
        val wa = ExtraCalculators.fill.run(mapOf("mode" to "wa", "wa" to "100", "depth" to "1", "mat" to "sand"), r)
        assertEquals(480.0, buy(wa, "fill_sand"), 1e-9) // 400 m² × 1 m × 1.2
        val dig = ExtraCalculators.excavation.run(emptyMap(), r) // 1 × 1 × 1.2 × 6 = 7.2 m³ +25%
        assertEquals(7.2, dig.laborBase, 1e-9)
        assertEquals(9.0, buy(dig, "haul_soil"), 1e-9)
        assertTrue(ExtraCalculators.excavation.run(mapOf("haul" to "keep"), r).materials.isEmpty())
    }

    @Test fun `gypsum ceiling and drywall`() {
        val c = ExtraCalculators.ceiling.run(emptyMap(), r) // 4 × 5 = 20 m²
        assertEquals(8.0, buy(c, "gypsum_board"), 0.0)
        assertEquals(13.0, buy(c, "c_line"), 0.0)
        assertEquals(6.0, buy(c, "wall_angle"), 0.0)
        val d = ExtraCalculators.drywall.run(mapOf("length" to "6", "height" to "2.8"), r)
        assertEquals(13.0, buy(d, "gypsum_board"), 0.0) // 16.8 × 2 × 1.05 / 2.88 = 12.25 → 13
        assertEquals(11.0, buy(d, "c_stud"), 0.0) // 6 / 0.6 + 1 = 11 studs, each fits in one 3 m length
    }

    @Test fun `pipes count sticks fittings and glue`() {
        val p = ExtraCalculators.pipes.run(emptyMap(), r) // 20 m 1/2"
        assertEquals(6.0, buy(p, "pvc_s12"), 0.0) // 22 m / 4 → 6
        assertEquals(4.0, buy(p, "socket_s12"), 0.0)
        assertEquals(1.0, buy(p, "pvc_glue"), 0.0) // 8 + 12 + 6 = 26 joints
        assertEquals(20.0, buy(p, "pipe_clip"), 0.0)
    }
}
