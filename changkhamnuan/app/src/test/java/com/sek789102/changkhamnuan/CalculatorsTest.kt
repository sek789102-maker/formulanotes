package com.sek789102.changkhamnuan

import com.sek789102.changkhamnuan.domain.Calculators
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.PaintPack
import com.sek789102.changkhamnuan.domain.QuoteLine
import com.sek789102.changkhamnuan.domain.QuoteMath
import com.sek789102.changkhamnuan.domain.QuoteOptions
import com.sek789102.changkhamnuan.domain.Rates
import com.sek789102.changkhamnuan.domain.Units
import com.sek789102.changkhamnuan.domain.ceilTo
import com.sek789102.changkhamnuan.domain.isVisible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {
    private val r = Rates()
    private fun buy(res: com.sek789102.changkhamnuan.domain.CalcResult, key: String) = res.materials.first { it.priceKey == key }.buyQty

    @Test fun `half-brick wall uses the standard table`() {
        // 4 × 3 m = 12 m²: 138 bricks, 16.01 kg cement, 0.05 m³ sand per m²
        val res = Calculators.bricks.run(mapOf("type" to "mon_half", "length" to "4", "height" to "3"), r)
        assertEquals(1656.0, buy(res, "brick_mon"), 0.0)
        assertEquals(4.0, buy(res, "cement_bag"), 0.0) // 192.12 kg = 3.84 bags → 4
        assertEquals(1.0, buy(res, "sand"), 0.0) // 0.6 m³ → sold by half cubic metre
        assertEquals(12.0, res.laborBase, 1e-9)
    }

    @Test fun `doors and windows are subtracted from the wall`() {
        val res = Calculators.bricks.run(mapOf("type" to "mon_half", "length" to "4", "height" to "3", "doors" to "1", "windows" to "1"), r)
        assertEquals(12 - 1.8 - 1.2, res.laborBase, 1e-9)
        assertEquals(ceilTo(9.0 * 138), buy(res, "brick_mon"), 0.0)
    }

    @Test fun `aac and block walls add waste`() {
        val aac = Calculators.bricks.run(mapOf("type" to "aac", "length" to "5", "height" to "2"), r)
        assertEquals(ceilTo(10 * 8.33 * 1.05), buy(aac, "brick_aac"), 0.0) // 87.465 → 88
        assertEquals(2.0, buy(aac, "mortar_aac"), 0.0) // 10 m² / 7 per bag → 2
        val block = Calculators.bricks.run(mapOf("type" to "block", "length" to "5", "height" to "2"), r)
        assertEquals(ceilTo(10 * 12.5 * 1.05), buy(block, "brick_block"), 0.0) // 131.25 → 132
    }

    @Test fun `concrete 1-2-4 matches about 6_3 bags per cubic metre`() {
        val noWaste = Rates(mapOf("concrete_waste" to 0.0))
        val res = Calculators.concrete.run(mapOf("shape" to "slab", "slab_w" to "10", "slab_l" to "1", "slab_t" to "10", "mix" to "124"), noWaste) // 1 m³
        val cement = res.materials.first { it.priceKey == "cement_bag" }
        assertEquals(6.3, cement.qty / 50, 0.05)
        assertEquals(7.0, cement.buyQty, 0.0)
        assertEquals(0.44, res.materials.first { it.priceKey == "sand" }.qty, 0.001)
        assertEquals(0.88, res.materials.first { it.priceKey == "stone" }.qty, 0.001)
    }

    @Test fun `ready-mix is ordered by half cubic metre with waste`() {
        val res = Calculators.concrete.run(mapOf("shape" to "slab", "slab_w" to "4", "slab_l" to "5", "slab_t" to "10", "mix" to "ready"), r)
        assertEquals(2.0, res.laborBase, 1e-9) // 4 × 5 × 0.10
        assertEquals(2.5, buy(res, "ready_mix"), 0.0) // 2.1 → 2.5
        val beams = Calculators.concrete.run(mapOf("shape" to "member", "mem_w" to "20", "mem_d" to "40", "mem_len" to "5", "count" to "3", "mix" to "ready"), r)
        assertEquals(0.2 * 0.4 * 5 * 3, beams.laborBase, 1e-9)
    }

    @Test fun `tiles round to whole boxes`() {
        val res = Calculators.tiles.run(mapOf("w" to "3", "l" to "4", "size" to "60x60"), r)
        // 12 m² / 0.36 = 33.3 × 1.10 = 36.7 → 37 pieces → 4 per box → 10 boxes
        assertEquals(10.0, buy(res, "tile_box"), 0.0)
        assertEquals(4.0, buy(res, "tile_adhesive"), 0.0) // 63 kg / 20
        assertEquals(3.0, buy(res, "grout"), 0.0) // 2.4 kg / 1
        val custom = Calculators.tiles.run(mapOf("w" to "1", "l" to "1", "size" to "custom", "tw" to "10", "th" to "10", "per_box" to "50"), r)
        assertEquals(3.0, buy(custom, "tile_box"), 0.0) // 110 pieces / 50
    }

    @Test fun `paint is bought as buckets and gallons`() {
        assertEquals(0 to 1, PaintPack.split(0.8, 2.5))
        assertEquals(2 to 0, PaintPack.split(4.5, 2.5))
        assertEquals(2 to 1, PaintPack.split(6.0, 2.5))
        assertEquals(3 to 0, PaintPack.split(7.2, 2.5)) // 2 buckets + 2.2 gal → 3 gallons ≈ a bucket → 3 buckets
        val res = Calculators.paint.run(mapOf("length" to "10", "height" to "3", "sides" to "1", "coats" to "2", "primer_coats" to "1"), r)
        // 30 m² × 2 / 30 = 2 gal → 1 bucket? 2 ≥ 2.0 → one bucket
        assertTrue(res.materials.any { it.priceKey == "paint_bucket" && it.buyQty == 1.0 })
        assertTrue(res.materials.any { it.priceKey == "primer_gal" && it.buyQty == 1.0 }) // 0.86 gal
    }

    @Test fun `rebar weight follows d squared over 162`() {
        assertEquals(0.888, Calculators.rebarKgPerM(12.0), 0.002)
        val res = Calculators.rebar.run(mapOf("size" to "DB12", "piece_len" to "4", "pieces" to "8"), r)
        assertEquals(4.0, buy(res, "rebar_DB12"), 0.0) // 32 m × 1.1 = 35.2 m → 4 bars of 10 m
    }

    @Test fun `empty or bad boxes count as zero, untouched fields use defaults`() {
        val res = Calculators.tiles.run(mapOf("w" to "abc", "l" to "", "size" to "nope"), r)
        assertEquals(0.0, res.laborBase, 1e-9)
        assertEquals(0.0, buy(res, "tile_box"), 0.0)
        val untouched = Calculators.tiles.run(emptyMap(), r)
        assertEquals(12.0, untouched.laborBase, 1e-9) // defaults 3 × 4
    }

    @Test fun `fields show only for the matching choice`() {
        val c = Calculators.concrete
        val slab = c.fields.filter { it.isVisible(mapOf("shape" to "slab"), c) }.map { it.key }
        assertTrue("slab_t" in slab && "mem_w" !in slab)
        val member = c.fields.filter { it.isVisible(mapOf("shape" to "member"), c) }.map { it.key }
        assertTrue("mem_w" in member && "slab_t" !in member)
        val t = Calculators.tiles
        assertTrue(t.fields.none { it.key == "tw" && it.isVisible(mapOf("size" to "60x60"), t) })
        assertTrue(t.fields.any { it.key == "tw" && it.isVisible(mapOf("size" to "custom"), t) })
    }

    @Test fun `quote totals with overhead, discount, VAT and withholding`() {
        val t = QuoteMath.totals(
            listOf(QuoteLine(ItemKind.MATERIAL, 10.0, 100.0), QuoteLine(ItemKind.LABOR, 1.0, 500.0)),
            QuoteOptions(overheadPercent = 10.0, discount = 50.0, vat = true, withholding = true)
        )
        assertEquals(1500.0, t.subtotal, 0.0)
        assertEquals(150.0, t.overhead, 0.0)
        assertEquals(1600.0, t.beforeVat, 0.0)
        assertEquals(112.0, t.vat, 0.0)
        assertEquals(1712.0, t.total, 0.0)
        assertEquals(48.0, t.withholding, 0.0)
        assertEquals(1664.0, t.toPay, 0.0)
    }

    @Test fun `thai baht text`() {
        assertEquals("หนึ่งพันเจ็ดร้อยสิบสองบาทถ้วน", QuoteMath.bahtText(1712.0))
        assertEquals("ยี่สิบเอ็ดบาทถ้วน", QuoteMath.bahtText(21.0))
        assertEquals("สิบเอ็ดบาทถ้วน", QuoteMath.bahtText(11.0))
        assertEquals("หนึ่งบาทถ้วน", QuoteMath.bahtText(1.0))
        assertEquals("หนึ่งร้อยเอ็ดบาทห้าสิบสตางค์", QuoteMath.bahtText(101.5))
        assertEquals("หนึ่งล้านบาทถ้วน", QuoteMath.bahtText(1_000_000.0))
        assertEquals("สองล้านห้าแสนบาทถ้วน", QuoteMath.bahtText(2_500_000.0))
    }

    @Test fun `thai land units`() {
        val rai = Units.groups.first { it.id == "area" }.units.first { it.id == "rai" }
        val m2 = Units.groups.first { it.id == "area" }.units.first { it.id == "m2" }
        assertEquals(1600.0, Units.convert(1.0, rai, m2), 1e-9)
        assertEquals("1 ไร่ 0 งาน 62.5 ตร.วา", Units.thaiLand(1850.0))
    }
}
