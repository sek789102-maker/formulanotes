package com.sek789102.changkhamnuan.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

/** One material to buy. [qty] is the exact need; [buyQty] is rounded up to what shops sell. */
data class MaterialLine(
    val priceKey: String,
    val name: String,
    val qty: Double,
    val unit: String,
    val buyQty: Double,
    val buyUnit: String,
    val note: String? = null
)

data class CalcResult(
    /** Short facts about the job, e.g. "พื้นที่สุทธิ" to "12.40 ตร.ม.". */
    val facts: List<Pair<String, String>>,
    val materials: List<MaterialLine>,
    /** Quantity used for labour pricing (ตร.ม. / ลบ.ม. / กก.) and its unit. */
    val laborBase: Double = 0.0,
    val laborUnit: String = "ตร.ม.",
    val warnings: List<String> = emptyList()
)

sealed interface Field {
    val key: String
    val label: String
}

/** Whether a field applies to the current choices (e.g. custom tile size only for "กำหนดเอง"). */
fun Field.isVisible(values: Map<String, String>, calc: Calculator): Boolean {
    val rule = (this as? NumberField)?.showIf ?: return true
    val choice = calc.fields.firstOrNull { it.key == rule.first } as? ChoiceField ?: return true
    return (values[rule.first]?.takeIf { v -> choice.options.any { it.first == v } } ?: choice.default) == rule.second
}

data class NumberField(
    override val key: String,
    override val label: String,
    val unit: String,
    val default: Double,
    val help: String? = null,
    /** Shown only when choice field `first` has value `second`. */
    val showIf: Pair<String, String>? = null
) : Field

data class ChoiceField(
    override val key: String,
    override val label: String,
    val options: List<Pair<String, String>>,
    val default: String
) : Field

/** Values typed on the form. Missing or unparsable numbers fall back to the field default. */
class Inputs(private val calc: Calculator, private val raw: Map<String, String>) {
    /** A value typed by the user; an empty box counts as 0. Keys never typed use the field default. */
    fun num(key: String): Double {
        val f = calc.fields.first { it.key == key } as NumberField
        val r = raw[key] ?: return f.default
        return r.replace(",", "").trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
    }

    fun choice(key: String): String {
        val f = calc.fields.first { it.key == key } as ChoiceField
        val v = raw[key]
        return if (v != null && f.options.any { it.first == v }) v else f.default
    }
}

data class Calculator(
    val id: String,
    val title: String,
    val subtitle: String,
    val fields: List<Field>,
    val compute: (Inputs, Rates) -> CalcResult
) {
    fun run(raw: Map<String, String>, rates: Rates): CalcResult = compute(Inputs(this, raw), rates)
}

object Fmt {
    /** "1,234.5" style, trimming useless zeros. */
    fun n(v: Double, decimals: Int = 2): String {
        val s = String.format(java.util.Locale.US, "%,.${decimals}f", v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    fun money(v: Double): String = String.format(java.util.Locale.US, "%,.2f", v)
}

/** Rounds up, ignoring floating-point noise (2.0000000001 → 2). */
fun ceilTo(v: Double, step: Double = 1.0): Double {
    if (v <= 0) return 0.0
    val units = v / step
    val r = round(units)
    return (if (kotlin.math.abs(units - r) < 1e-9) r else ceil(units)) * step
}

object Calculators {

    // ---- shared wall area helper ----
    private val wallFields = listOf(
        NumberField("length", "ความยาวผนัง", "ม.", 4.0),
        NumberField("height", "ความสูงผนัง", "ม.", 3.0),
        NumberField("doors", "ประตู (หักออก)", "ช่อง", 0.0, "ช่องละ 0.9 × 2.0 ม."),
        NumberField("windows", "หน้าต่าง (หักออก)", "ช่อง", 0.0, "ช่องละ 1.2 × 1.0 ม."),
        NumberField("extra_area", "หักพื้นที่อื่นเพิ่ม", "ตร.ม.", 0.0)
    )

    const val DOOR_AREA = 0.9 * 2.0
    const val WINDOW_AREA = 1.2 * 1.0

    fun wallArea(i: Inputs): Double =
        max(0.0, i.num("length") * i.num("height") - i.num("doors") * DOOR_AREA - i.num("windows") * WINDOW_AREA - i.num("extra_area"))

    private fun areaFacts(i: Inputs, area: Double) = listOf(
        "พื้นที่ผนังรวม" to "${Fmt.n(i.num("length") * i.num("height"))} ตร.ม.",
        "พื้นที่สุทธิ (หักช่องเปิด)" to "${Fmt.n(area)} ตร.ม."
    )

    private fun cementBags(kg: Double, r: Rates) = MaterialLine(
        "cement_bag", "ปูนซีเมนต์", kg, "กก.", ceilTo(kg / r["cement_bag_kg"]), "ถุง",
        "≈ ${Fmt.n(kg / r["cement_bag_kg"])} ถุง (ถุงละ ${Fmt.n(r["cement_bag_kg"])} กก.)"
    )

    private fun sand(m3: Double) = MaterialLine("sand", "ทรายหยาบ", m3, "ลบ.ม.", ceilTo(m3, 0.5), "คิว", "ร้านมักขายทีละครึ่งคิว")

    // ---- 1. ก่ออิฐ ----
    val bricks = Calculator(
        id = "bricks",
        title = "ก่ออิฐ",
        subtitle = "อิฐมอญ · อิฐมวลเบา · คอนกรีตบล็อก",
        fields = listOf(
            ChoiceField(
                "type", "ชนิดผนัง",
                listOf(
                    "mon_half" to "อิฐมอญ ครึ่งแผ่น",
                    "mon_full" to "อิฐมอญ เต็มแผ่น",
                    "aac" to "อิฐมวลเบา 20×60 ซม.",
                    "block" to "คอนกรีตบล็อก 19×39 ซม."
                ),
                "mon_half"
            )
        ) + wallFields,
        compute = { i, r ->
            val area = wallArea(i)
            val type = i.choice("type")
            val mats = when (type) {
                "mon_half", "mon_full" -> {
                    val pcs = area * r["${type}_pcs"]
                    listOf(
                        MaterialLine("brick_mon", "อิฐมอญ", pcs, "ก้อน", ceilTo(pcs), "ก้อน"),
                        cementBags(area * r["${type}_cement"], r),
                        sand(area * r["${type}_sand"])
                    )
                }
                "aac" -> {
                    val pcs = area * r["aac_pcs"] * (1 + r["aac_waste"] / 100)
                    val bags = area / r["aac_mortar_m2_per_bag"]
                    listOf(
                        MaterialLine("brick_aac", "อิฐมวลเบา 20×60 ซม.", pcs, "ก้อน", ceilTo(pcs), "ก้อน", "รวมเผื่อ ${Fmt.n(r["aac_waste"])}%"),
                        MaterialLine("mortar_aac", "ปูนก่ออิฐมวลเบา", bags, "ถุง", ceilTo(bags), "ถุง")
                    )
                }
                else -> {
                    val pcs = area * r["block_pcs"] * (1 + r["block_waste"] / 100)
                    listOf(
                        MaterialLine("brick_block", "คอนกรีตบล็อก", pcs, "ก้อน", ceilTo(pcs), "ก้อน", "รวมเผื่อ ${Fmt.n(r["block_waste"])}%"),
                        cementBags(area * r["block_cement"], r),
                        sand(area * r["block_sand"])
                    )
                }
            }
            CalcResult(areaFacts(i, area), mats, laborBase = area, laborUnit = "ตร.ม.")
        }
    )

    // ---- 2. ฉาบปูน ----
    val plaster = Calculator(
        id = "plaster",
        title = "ฉาบปูน",
        subtitle = "ปูนฉาบสำเร็จรูป ตามความหนา",
        fields = wallFields + listOf(
            NumberField("sides", "ฉาบกี่ด้าน", "ด้าน", 2.0),
            NumberField("thickness", "ความหนาปูนฉาบ", "ซม.", 1.5)
        ),
        compute = { i, r ->
            val area = wallArea(i) * max(0.0, i.num("sides"))
            val t = max(0.0, i.num("thickness"))
            val bags = area * t / r["plaster_m2_per_bag_1cm"] * (1 + r["plaster_waste"] / 100)
            val warnings = buildList {
                if (t > 3) add("ปูนฉาบหนาเกิน 3 ซม. ควรฉาบเป็นชั้นหรือเสริมตะแกรง")
            }
            CalcResult(
                facts = listOf("พื้นที่ฉาบ (${Fmt.n(i.num("sides"))} ด้าน)" to "${Fmt.n(area)} ตร.ม.", "ความหนา" to "${Fmt.n(t)} ซม."),
                materials = listOf(MaterialLine("plaster_bag", "ปูนฉาบสำเร็จรูป", bags, "ถุง", ceilTo(bags), "ถุง", "รวมเผื่อ ${Fmt.n(r["plaster_waste"])}%")),
                laborBase = area, laborUnit = "ตร.ม.", warnings = warnings
            )
        }
    )

    // ---- 3. คอนกรีต ----
    val mixes = listOf(
        "124" to Triple(1.0, 2.0, 4.0),
        "1153" to Triple(1.0, 1.5, 3.0),
        "136" to Triple(1.0, 3.0, 6.0)
    )

    val concrete = Calculator(
        id = "concrete",
        title = "เทคอนกรีต",
        subtitle = "พื้น · คาน · เสา · ฐานราก",
        fields = listOf(
            ChoiceField(
                "shape", "งาน",
                listOf("slab" to "พื้น / ลาน (กว้าง × ยาว × หนา)", "member" to "คาน / เสา / ฐานราก (หน้าตัด × ยาว × จำนวน)"),
                "slab"
            ),
            NumberField("slab_w", "กว้าง", "ม.", 4.0, showIf = "shape" to "slab"),
            NumberField("slab_l", "ยาว", "ม.", 5.0, showIf = "shape" to "slab"),
            NumberField("slab_t", "หนา", "ซม.", 10.0, showIf = "shape" to "slab"),
            NumberField("mem_w", "กว้าง (หน้าตัด)", "ซม.", 20.0, showIf = "shape" to "member"),
            NumberField("mem_d", "ลึก / สูง (หน้าตัด)", "ซม.", 40.0, showIf = "shape" to "member"),
            NumberField("mem_len", "ความยาว", "ม.", 4.0, showIf = "shape" to "member"),
            NumberField("count", "จำนวน", "ชิ้น", 1.0),
            ChoiceField(
                "mix", "ส่วนผสม",
                listOf(
                    "ready" to "คอนกรีตผสมเสร็จ (สั่งเป็นคิว)",
                    "124" to "ผสมเอง 1 : 2 : 4 (งานทั่วไป)",
                    "1153" to "ผสมเอง 1 : 1.5 : 3 (งานรับแรงมาก)",
                    "136" to "ผสมเอง 1 : 3 : 6 (งานหยาบ/ทับหน้า)"
                ),
                "ready"
            )
        ),
        compute = { i, r ->
            val count = max(0.0, i.num("count"))
            val volume = if (i.choice("shape") == "slab") {
                i.num("slab_w") * i.num("slab_l") * i.num("slab_t") / 100.0 * count
            } else {
                i.num("mem_w") / 100.0 * i.num("mem_d") / 100.0 * i.num("mem_len") * count
            }.coerceAtLeast(0.0)
            val withWaste = volume * (1 + r["concrete_waste"] / 100)
            val mix = i.choice("mix")
            val mats = if (mix == "ready") {
                listOf(MaterialLine("ready_mix", "คอนกรีตผสมเสร็จ", withWaste, "ลบ.ม.", ceilTo(withWaste, 0.5), "คิว", "รวมเผื่อ ${Fmt.n(r["concrete_waste"])}% · สั่งทีละครึ่งคิว"))
            } else {
                val (c, s, a) = mixes.first { it.first == mix }.second
                val parts = c + s + a
                val dry = withWaste * r["concrete_dry_factor"]
                val cementKg = dry * c / parts * r["cement_density"]
                listOf(
                    cementBags(cementKg, r),
                    sand(dry * s / parts),
                    MaterialLine("stone", "หิน", dry * a / parts, "ลบ.ม.", ceilTo(dry * a / parts, 0.5), "คิว", "ร้านมักขายทีละครึ่งคิว")
                )
            }
            CalcResult(
                facts = listOf("ปริมาตรคอนกรีต" to "${Fmt.n(volume, 3)} ลบ.ม.", "รวมเผื่อ ${Fmt.n(r["concrete_waste"])}%" to "${Fmt.n(withWaste, 3)} ลบ.ม."),
                materials = mats,
                laborBase = volume, laborUnit = "ลบ.ม."
            )
        }
    )

    // ---- 4. ปูกระเบื้อง ----
    /** Tile presets: id → (width cm, height cm, pieces per box). */
    val tileSizes = linkedMapOf(
        "20x20" to Triple(20.0, 20.0, 25.0),
        "30x30" to Triple(30.0, 30.0, 11.0),
        "40x40" to Triple(40.0, 40.0, 6.0),
        "60x60" to Triple(60.0, 60.0, 4.0),
        "30x60" to Triple(30.0, 60.0, 8.0),
        "60x120" to Triple(60.0, 120.0, 2.0),
        "20x120" to Triple(20.0, 120.0, 6.0)
    )

    val tiles = Calculator(
        id = "tiles",
        title = "ปูกระเบื้อง",
        subtitle = "พื้น · ผนัง · กาวซีเมนต์ · ยาแนว",
        fields = listOf(
            NumberField("w", "กว้าง", "ม.", 3.0),
            NumberField("l", "ยาว / สูง", "ม.", 4.0),
            NumberField("minus", "หักพื้นที่ (ช่องเปิด ฯลฯ)", "ตร.ม.", 0.0),
            ChoiceField(
                "size", "ขนาดกระเบื้อง",
                tileSizes.keys.map { it to it.replace("x", " × ") + " ซม." } + ("custom" to "กำหนดเอง"),
                "60x60"
            ),
            NumberField("tw", "กระเบื้องกว้าง", "ซม.", 30.0, showIf = "size" to "custom"),
            NumberField("th", "กระเบื้องยาว", "ซม.", 30.0, showIf = "size" to "custom"),
            NumberField("per_box", "จำนวนต่อกล่อง (0 = ค่าทั่วไปของขนาดนี้)", "แผ่น", 0.0, help = "ดูข้างกล่องกระเบื้อง")
        ),
        compute = { i, r ->
            val area = max(0.0, i.num("w") * i.num("l") - i.num("minus"))
            val size = i.choice("size")
            val preset = tileSizes[size]
            val tw = preset?.first ?: i.num("tw")
            val th = preset?.second ?: i.num("th")
            val perBox = i.num("per_box").takeIf { it > 0 } ?: preset?.third ?: 1.0
            val tileArea = tw / 100 * th / 100
            val warnings = mutableListOf<String>()
            val pcs = if (tileArea > 0) area / tileArea * (1 + r["tile_waste"] / 100) else 0.0
            if (tileArea <= 0) warnings += "ใส่ขนาดกระเบื้องให้ถูกต้อง"
            val boxes = ceilTo(ceilTo(pcs) / perBox)
            val adhesive = area * r["tile_adhesive"]
            val grout = area * r["tile_grout"]
            CalcResult(
                facts = listOf(
                    "พื้นที่ปู" to "${Fmt.n(area)} ตร.ม.",
                    "กระเบื้อง" to "${Fmt.n(tw)} × ${Fmt.n(th)} ซม. · ${Fmt.n(perBox)} แผ่น/กล่อง"
                ),
                materials = listOf(
                    MaterialLine("tile_box", "กระเบื้อง ${Fmt.n(tw)}×${Fmt.n(th)} ซม.", pcs, "แผ่น", boxes, "กล่อง",
                        "${Fmt.n(ceilTo(pcs))} แผ่น (รวมเผื่อ ${Fmt.n(r["tile_waste"])}%)"),
                    MaterialLine("tile_adhesive", "กาวซีเมนต์", adhesive, "กก.", ceilTo(adhesive / r["adhesive_bag_kg"]), "ถุง",
                        "ถุงละ ${Fmt.n(r["adhesive_bag_kg"])} กก."),
                    MaterialLine("grout", "ยาแนว", grout, "กก.", ceilTo(grout / r["grout_bag_kg"]), "ถุง", "ถุงละ ${Fmt.n(r["grout_bag_kg"])} กก.")
                ),
                laborBase = area, laborUnit = "ตร.ม.", warnings = warnings
            )
        }
    )

    // ---- 5. ทาสี ----
    val paint = Calculator(
        id = "paint",
        title = "ทาสี",
        subtitle = "สีจริง + สีรองพื้น · ถัง / แกลลอน",
        fields = wallFields + listOf(
            NumberField("sides", "ทากี่ด้าน", "ด้าน", 1.0),
            NumberField("coats", "สีจริง กี่รอบ", "รอบ", 2.0),
            NumberField("primer_coats", "สีรองพื้น กี่รอบ (0 = ไม่ทา)", "รอบ", 1.0)
        ),
        compute = { i, r ->
            val area = wallArea(i) * max(0.0, i.num("sides"))
            val gal = area * max(0.0, i.num("coats")) / r["paint_cover"]
            val primer = area * max(0.0, i.num("primer_coats")) / r["primer_cover"]
            fun lines(key: String, name: String, need: Double): List<MaterialLine> {
                if (need <= 0) return emptyList()
                val (buckets, gallons) = PaintPack.split(need, r["paint_bucket_gal"])
                return buildList {
                    if (buckets > 0) add(MaterialLine("${key}_bucket", "$name (ถัง ${Fmt.n(r["paint_bucket_gal"])} แกลลอน)", need, "แกลลอน", buckets.toDouble(), "ถัง"))
                    if (gallons > 0) add(MaterialLine("${key}_gal", "$name (แกลลอน)", if (buckets > 0) 0.0 else need, "แกลลอน", gallons.toDouble(), "แกลลอน"))
                }
            }
            CalcResult(
                facts = listOf(
                    "พื้นที่ทา (${Fmt.n(i.num("sides"))} ด้าน)" to "${Fmt.n(area)} ตร.ม.",
                    "สีจริงที่ต้องใช้" to "${Fmt.n(gal)} แกลลอน",
                    "สีรองพื้นที่ต้องใช้" to "${Fmt.n(primer)} แกลลอน"
                ),
                materials = lines("paint", "สีจริง", gal) + lines("primer", "สีรองพื้น", primer),
                laborBase = area, laborUnit = "ตร.ม."
            )
        }
    )

    // ---- 6. เหล็กเส้น ----
    val rebarSizes = listOf("RB6" to 6.0, "RB9" to 9.0, "DB10" to 10.0, "DB12" to 12.0, "DB16" to 16.0, "DB20" to 20.0, "DB25" to 25.0)

    /** kg per metre ≈ d² / 162 (d in mm); DB12 → 0.889 (มอก. 0.888). */
    fun rebarKgPerM(d: Double) = d * d / 162.0

    val rebar = Calculator(
        id = "rebar",
        title = "เหล็กเส้น",
        subtitle = "จำนวนเส้น · น้ำหนัก",
        fields = listOf(
            ChoiceField("size", "ขนาดเหล็ก", rebarSizes.map { it.first to it.first }, "DB12"),
            NumberField("piece_len", "ความยาวที่ใช้ต่อท่อน", "ม.", 4.0),
            NumberField("pieces", "จำนวนท่อน", "ท่อน", 8.0)
        ),
        compute = { i, r ->
            val (name, d) = rebarSizes.first { it.first == i.choice("size") }
            val total = max(0.0, i.num("piece_len")) * max(0.0, i.num("pieces"))
            val withLap = total * (1 + r["rebar_lap"] / 100)
            val bars = ceilTo(withLap / r["rebar_length"])
            val kg = withLap * rebarKgPerM(d)
            val warnings = buildList {
                if (i.num("piece_len") > r["rebar_length"]) add("ท่อนยาวกว่าเหล็ก 1 เส้น ต้องทาบต่อ")
            }
            CalcResult(
                facts = listOf(
                    "ความยาวรวม" to "${Fmt.n(total)} ม.",
                    "รวมเผื่อทาบ/ตัด ${Fmt.n(r["rebar_lap"])}%" to "${Fmt.n(withLap)} ม.",
                    "น้ำหนักต่อเมตร" to "${Fmt.n(rebarKgPerM(d), 3)} กก./ม.",
                    "น้ำหนักรวม" to "${Fmt.n(kg)} กก."
                ),
                materials = listOf(MaterialLine("rebar_$name", "เหล็ก $name (เส้นละ ${Fmt.n(r["rebar_length"])} ม.)", withLap, "ม.", bars, "เส้น", "≈ ${Fmt.n(kg)} กก.")),
                laborBase = kg, laborUnit = "กก.", warnings = warnings
            )
        }
    )

    val all = listOf(bricks, plaster, concrete, tiles, paint, rebar)
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

object PaintPack {
    /**
     * Usual cheapest buy: full buckets, then gallons for the rest. If the rest would need
     * almost a bucket's worth of gallons, one more bucket is bought instead.
     */
    fun split(needGallons: Double, bucketGallons: Double): Pair<Int, Int> {
        if (needGallons <= 0) return 0 to 0
        var buckets = floor(needGallons / bucketGallons + 1e-9).toInt()
        val rest = needGallons - buckets * bucketGallons
        var gallons = ceilTo(rest).toInt()
        if (gallons >= bucketGallons - 0.5) {
            buckets += 1
            gallons = 0
        }
        return buckets to gallons
    }
}
