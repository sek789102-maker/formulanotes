package com.sek789102.changkhamnuan.domain

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Civil-works and contractor calculators (1.3): bar schedule by spacing, structural steel,
 * piles, concrete road, drainage, slope, stairs, formwork, cut & fill and tanks.
 * Quantity take-off only — never structural design.
 */
object CivilCalculators {

    private fun pos(v: Double) = max(0.0, v)

    private val mixField = ChoiceField(
        "mix", "คอนกรีต",
        listOf("ready" to "คอนกรีตผสมเสร็จ (สั่งเป็นคิว)", "124" to "ผสมเอง 1 : 2 : 4"),
        "ready"
    )

    // ------------------------------------------------------------------ bar cutting
    /**
     * Number of stock bars needed for [count] pieces of [pieceLen] m. Short pieces are cut from
     * one bar; long pieces are joined with a lap of 40 bar diameters.
     */
    fun stockBars(pieceLen: Double, count: Double, stock: Double, diaMm: Double): Double {
        if (pieceLen <= 0 || count <= 0 || stock <= 0) return 0.0
        if (pieceLen <= stock + 1e-9) {
            val perBar = floor(stock / pieceLen + 1e-9)
            return ceilTo(count / perBar)
        }
        val lap = 40 * diaMm / 1000
        val joins = ceilTo((pieceLen - stock) / (stock - lap))
        val total = count * (pieceLen + joins * lap)
        return ceilTo(total / stock)
    }

    private val mainSizes = listOf("DB12" to 12.0, "DB16" to 16.0, "DB20" to 20.0, "DB25" to 25.0, "DB10" to 10.0, "RB9" to 9.0)
    private val stirrupSizes = listOf("RB6" to 6.0, "RB9" to 9.0, "DB10" to 10.0)

    private data class BarSet(val name: String, val d: Double, val pieceLen: Double, val count: Double)

    // ---- 1. เหล็กเสริมตามระยะ ----
    val rebarGrid = Calculator(
        id = "rebar_grid",
        title = "เหล็กเสริมตามระยะ",
        subtitle = "พื้น · คาน · เสา · เหล็กปลอก · ลวดผูก",
        fields = listOf(
            ChoiceField("member", "ชิ้นส่วน", listOf("slab" to "พื้น / ฐานแผ่ (ตะแกรง)", "beam" to "คาน", "column" to "เสา"), "slab"),
            NumberField("w", "กว้าง", "ม.", 4.0, showIf = "member" to "slab"),
            NumberField("l", "ยาว", "ม.", 5.0, showIf = "member" to "slab"),
            ChoiceField("layers", "ชั้นเหล็ก (พื้น)", listOf("1" to "ชั้นเดียว", "2" to "สองชั้น (บน + ล่าง)"), "1"),
            NumberField("len", "ความยาว / ความสูง (ต่อชิ้น)", "ม.", 4.0, help = "คาน: ความยาวคาน · เสา: ความสูงเสา"),
            NumberField("count", "จำนวนชิ้น (คาน/เสา)", "ชิ้น", 4.0),
            NumberField("b", "หน้าตัด กว้าง (คาน/เสา)", "ซม.", 20.0),
            NumberField("h", "หน้าตัด ลึก (คาน/เสา)", "ซม.", 40.0),
            NumberField("bars", "เหล็กแกนรวม (คาน/เสา)", "เส้น", 4.0, "คาน: บน + ล่าง รวมกัน"),
            ChoiceField("size", "ขนาดเหล็กแกน / ตะแกรง", mainSizes.map { it.first to it.first }, "DB12"),
            NumberField("spacing", "ระยะห่างตะแกรง (พื้น)", "ซม.", 20.0),
            ChoiceField("stirrup", "เหล็กปลอก (คาน/เสา)", stirrupSizes.map { it.first to it.first }, "RB6"),
            NumberField("stirrup_spacing", "ระยะเหล็กปลอก", "ซม.", 15.0)
        ),
        compute = { i, r ->
            val member = i.choice("member")
            val (sizeName, d) = mainSizes.first { it.first == i.choice("size") }
            val stock = r["rebar_length"]
            val cover = r["rebar_cover_cm"] / 100
            val sets = mutableListOf<BarSet>()
            val facts = mutableListOf<Pair<String, String>>()
            if (member == "slab") {
                val w = pos(i.num("w")); val l = pos(i.num("l"))
                val s = i.num("spacing").takeIf { it > 0 }?.div(100) ?: 0.2
                val layers = if (i.choice("layers") == "2") 2.0 else 1.0
                val alongL = (floor(pos(w - 2 * cover) / s + 1e-9) + 1) * layers
                val alongW = (floor(pos(l - 2 * cover) / s + 1e-9) + 1) * layers
                if (w > 0 && l > 0) {
                    sets += BarSet(sizeName, d, pos(l - 2 * cover), alongL)
                    sets += BarSet(sizeName, d, pos(w - 2 * cover), alongW)
                }
                facts += "เหล็กแนวยาว" to "${Fmt.n(alongL)} ท่อน × ${Fmt.n(pos(l - 2 * cover))} ม."
                facts += "เหล็กแนวขวาง" to "${Fmt.n(alongW)} ท่อน × ${Fmt.n(pos(w - 2 * cover))} ม."
            } else {
                val len = pos(i.num("len")); val n = pos(i.num("count"))
                val b = pos(i.num("b")) / 100; val h = pos(i.num("h")) / 100
                val anchor = r["rebar_anchor_m"]
                val mainPiece = len + 2 * anchor
                val mainCount = pos(i.num("bars")) * n
                if (len > 0 && mainCount > 0) sets += BarSet(sizeName, d, mainPiece, mainCount)
                val (stName, sd) = stirrupSizes.first { it.first == i.choice("stirrup") }
                val ss = i.num("stirrup_spacing").takeIf { it > 0 }?.div(100) ?: 0.15
                val perMember = if (len > 0) floor(len / ss + 1e-9) + 1 else 0.0
                val stirrupLen = 2 * (pos(b - 2 * cover) + pos(h - 2 * cover)) + 2 * 10 * sd / 1000
                if (perMember > 0 && n > 0 && stirrupLen > 0) sets += BarSet(stName, sd, stirrupLen, perMember * n)
                facts += "เหล็กแกน" to "${Fmt.n(mainCount)} ท่อน × ${Fmt.n(mainPiece)} ม. (รวมระยะฝัง/ทาบปลาย ${Fmt.n(anchor)} ม. × 2)"
                facts += "เหล็กปลอก" to "${Fmt.n(perMember * n)} ตัว × ${Fmt.n(stirrupLen)} ม."
            }
            var totalKg = 0.0
            val mats = sets.groupBy { it.name }.map { (name, list) ->
                val dd = list.first().d
                val bars = list.sumOf { stockBars(it.pieceLen, it.count, stock, dd) }
                val meters = list.sumOf { it.pieceLen * it.count }
                val kg = meters * Calculators.rebarKgPerM(dd)
                totalKg += kg
                MaterialLine("rebar_$name", "เหล็ก $name (เส้นละ ${Fmt.n(stock)} ม.)", meters, "ม.", bars, "เส้น", "ใช้จริง ${Fmt.n(meters)} ม. ≈ ${Fmt.n(kg)} กก.")
            }.toMutableList()
            val wire = totalKg / 1000 * r["tie_wire_kg_per_ton"]
            if (wire > 0) mats += MaterialLine("tie_wire", "ลวดผูกเหล็ก", wire, "กก.", ceilTo(wire), "กก.", "${Fmt.n(r["tie_wire_kg_per_ton"])} กก. ต่อเหล็ก 1 ตัน")
            facts += "น้ำหนักเหล็กรวม" to "${Fmt.n(totalKg)} กก."
            CalcResult(
                facts, mats, laborBase = totalKg, laborUnit = "กก.",
                warnings = listOf("ขนาดและระยะเหล็กให้ใช้ตามแบบวิศวกร แอปช่วยถอดจำนวนเหล็กเท่านั้น")
            )
        }
    )

    // ---- 2. เหล็กรูปพรรณ ----
    /** kg per metre from the cross-section (steel 7,850 kg/m³, corner radii ignored). */
    fun sectionKgPerM(shape: String, a: Double, b: Double, c: Double, t: Double, t2: Double): Double {
        val areaMm2 = when (shape) {
            "h" -> 2 * b * t2 + max(0.0, a - 2 * t2) * t          // h=a, flange b, web t, flange t2
            "c" -> t * (max(0.0, a - t) + 2 * max(0.0, b - t) + 2 * max(0.0, c - t / 2)) // lipped channel
            "box" -> 2 * t * (a + b) - 4 * t * t
            "pipe" -> PI * t * (a - t)                            // a = outside diameter
            else -> a * t                                          // flat bar / plate: width × thickness
        }
        return max(0.0, areaMm2) * 7850 / 1_000_000
    }

    val steelSection = Calculator(
        id = "steel_section",
        title = "เหล็กรูปพรรณ",
        subtitle = "H-beam · ตัวซี · เหล็กกล่อง · ท่อ · แผ่น",
        fields = listOf(
            ChoiceField("shape", "ชนิดเหล็ก", listOf("c" to "เหล็กตัวซี (C ริมพับ)", "box" to "เหล็กกล่อง", "h" to "H-beam", "pipe" to "ท่อกลม", "flat" to "เหล็กแบน / แผ่น"), "c"),
            NumberField("c_h", "ความสูง", "มม.", 100.0, showIf = "shape" to "c"),
            NumberField("c_b", "ปีก", "มม.", 50.0, showIf = "shape" to "c"),
            NumberField("c_c", "ริมพับ", "มม.", 20.0, showIf = "shape" to "c"),
            NumberField("c_t", "ความหนา", "มม.", 2.3, showIf = "shape" to "c"),
            NumberField("box_a", "ด้านกว้าง", "มม.", 50.0, showIf = "shape" to "box"),
            NumberField("box_b", "ด้านลึก", "มม.", 50.0, showIf = "shape" to "box"),
            NumberField("box_t", "ความหนา", "มม.", 2.3, showIf = "shape" to "box"),
            NumberField("h_h", "ความสูง", "มม.", 200.0, showIf = "shape" to "h"),
            NumberField("h_b", "ปีกกว้าง", "มม.", 200.0, showIf = "shape" to "h"),
            NumberField("h_tw", "เอวหนา", "มม.", 8.0, showIf = "shape" to "h"),
            NumberField("h_tf", "ปีกหนา", "มม.", 12.0, showIf = "shape" to "h"),
            NumberField("p_d", "เส้นผ่านศูนย์กลางนอก", "มม.", 60.5, showIf = "shape" to "pipe"),
            NumberField("p_t", "ความหนา", "มม.", 2.3, showIf = "shape" to "pipe"),
            NumberField("f_w", "กว้าง", "มม.", 50.0, showIf = "shape" to "flat"),
            NumberField("f_t", "หนา", "มม.", 6.0, showIf = "shape" to "flat"),
            NumberField("piece_len", "ความยาวที่ใช้ต่อท่อน", "ม.", 6.0),
            NumberField("pieces", "จำนวนท่อน", "ท่อน", 10.0)
        ),
        compute = { i, r ->
            val shape = i.choice("shape")
            val kgm = when (shape) {
                "c" -> sectionKgPerM("c", i.num("c_h"), i.num("c_b"), i.num("c_c"), i.num("c_t"), 0.0)
                "box" -> sectionKgPerM("box", i.num("box_a"), i.num("box_b"), 0.0, i.num("box_t"), 0.0)
                "h" -> sectionKgPerM("h", i.num("h_h"), i.num("h_b"), 0.0, i.num("h_tw"), i.num("h_tf"))
                "pipe" -> sectionKgPerM("pipe", i.num("p_d"), 0.0, 0.0, i.num("p_t"), 0.0)
                else -> sectionKgPerM("flat", i.num("f_w"), 0.0, 0.0, i.num("f_t"), 0.0)
            }
            val label = when (shape) {
                "c" -> "C ${Fmt.n(i.num("c_h"))}×${Fmt.n(i.num("c_b"))}×${Fmt.n(i.num("c_c"))}×${Fmt.n(i.num("c_t"))}"
                "box" -> "กล่อง ${Fmt.n(i.num("box_a"))}×${Fmt.n(i.num("box_b"))}×${Fmt.n(i.num("box_t"))}"
                "h" -> "H ${Fmt.n(i.num("h_h"))}×${Fmt.n(i.num("h_b"))}×${Fmt.n(i.num("h_tw"))}×${Fmt.n(i.num("h_tf"))}"
                "pipe" -> "ท่อ Ø${Fmt.n(i.num("p_d"))}×${Fmt.n(i.num("p_t"))}"
                else -> "แบน ${Fmt.n(i.num("f_w"))}×${Fmt.n(i.num("f_t"))}"
            }
            val piece = pos(i.num("piece_len")); val n = pos(i.num("pieces"))
            val stock = r["steel_stock_length"]
            val bars = if (piece <= stock + 1e-9) stockBars(piece, n, stock, 0.0) else ceilTo(piece * n / stock)
            val kgUsed = piece * n * kgm
            val kgBought = bars * stock * kgm
            CalcResult(
                facts = listOf(
                    "น้ำหนักต่อเมตร (ประมาณ)" to "${Fmt.n(kgm, 2)} กก./ม.",
                    "ความยาวที่ใช้" to "${Fmt.n(piece * n)} ม. (${Fmt.n(kgUsed)} กก.)",
                    "ต้องซื้อ" to "${Fmt.n(bars)} เส้น × ${Fmt.n(stock)} ม."
                ),
                materials = if (bars > 0) listOf(MaterialLine("steel_kg", "เหล็กรูปพรรณ $label มม.", kgBought, "กก.", ceilTo(kgBought), "กก.", "${Fmt.n(bars)} เส้น × ${Fmt.n(stock)} ม. · ราคาต่อ กก.")) else emptyList(),
                laborBase = kgUsed, laborUnit = "กก.",
                warnings = listOf("น้ำหนักคำนวณจากหน้าตัด (ไม่รวมมุมโค้ง) อาจต่างจากตารางผู้ผลิต ±3%")
            )
        }
    )

    // ---- 3. เสาเข็ม ----
    val piles = Calculator(
        id = "piles",
        title = "เสาเข็ม",
        subtitle = "เข็มตอก · เข็มเจาะ · คอนกรีต · เหล็ก",
        fields = listOf(
            ChoiceField("type", "ชนิดเข็ม", listOf("driven" to "เข็มตอก (สำเร็จรูป)", "bored" to "เข็มเจาะ (หล่อในที่)"), "driven"),
            NumberField("footings", "จำนวนฐาน", "ฐาน", 6.0),
            NumberField("per", "เข็มต่อฐาน", "ต้น", 2.0),
            NumberField("length", "ความยาวเข็ม", "ม.", 6.0),
            NumberField("dia", "เส้นผ่านศูนย์กลาง (เข็มเจาะ)", "ซม.", 35.0, showIf = "type" to "bored")
        ),
        compute = { i, r ->
            val n = ceilTo(pos(i.num("footings")) * pos(i.num("per")))
            val len = pos(i.num("length"))
            if (i.choice("type") == "driven") {
                CalcResult(
                    facts = listOf("จำนวนเข็ม" to "${Fmt.n(n)} ต้น", "ความยาวรวม" to "${Fmt.n(n * len)} ม."),
                    materials = if (n > 0) listOf(MaterialLine("pile_driven", "เสาเข็มตอก ยาว ${Fmt.n(len)} ม.", n, "ต้น", n, "ต้น", "หน้าตัดตามแบบ · สั่งเผื่อตัดหัวเข็มตามหน้างาน")) else emptyList(),
                    laborBase = n, laborUnit = "ต้น",
                    warnings = listOf("จำนวน ขนาด และความยาวเข็มต้องใช้ตามแบบวิศวกร / ผลเจาะสำรวจดิน")
                )
            } else {
                val d = pos(i.num("dia")) / 100
                val vol = PI * d * d / 4 * len * n
                val withWaste = vol * (1 + r["bored_waste"] / 100)
                val steel = vol * r["bored_steel_kg_m3"]
                CalcResult(
                    facts = listOf(
                        "จำนวนเข็ม" to "${Fmt.n(n)} ต้น · ความยาวรวม ${Fmt.n(n * len)} ม.",
                        "คอนกรีต" to "${Fmt.n(vol, 3)} ลบ.ม. (รวมเผื่อ ${Fmt.n(r["bored_waste"])}% = ${Fmt.n(withWaste, 3)})",
                        "เหล็กเสริม (ประมาณ)" to "${Fmt.n(steel)} กก."
                    ),
                    materials = Calculators.concreteLines(withWaste, "ready", r) +
                        listOfNotNull(if (steel > 0) Calculators.rebarFromKg(steel, r, "≈ ${Fmt.n(steel)} กก. (${Fmt.n(r["bored_steel_kg_m3"])} กก./คิว) ขนาดจริงตามแบบ") else null),
                    laborBase = n * len, laborUnit = "ม.",
                    warnings = listOf("จำนวน ขนาด และความยาวเข็มต้องใช้ตามแบบวิศวกร / ผลเจาะสำรวจดิน")
                )
            }
        }
    )

    // ---- 4. ถนนคอนกรีต ----
    val road = Calculator(
        id = "road",
        title = "ถนนคอนกรีต",
        subtitle = "คอนกรีต · หินคลุก · ไวร์เมช · เหล็กเดือย",
        fields = listOf(
            NumberField("w", "ความกว้างถนน", "ม.", 4.0),
            NumberField("l", "ความยาวถนน", "ม.", 50.0),
            NumberField("t", "คอนกรีตหนา", "ซม.", 15.0),
            NumberField("base", "หินคลุกรองพื้นทาง หนา", "ซม.", 10.0),
            NumberField("sand", "ทรายรองใต้คอนกรีต หนา", "ซม.", 5.0),
            NumberField("joint", "ระยะรอยต่อ", "ม.", 5.0),
            ChoiceField("mesh", "เหล็กเสริมผิว", listOf("mesh" to "ไวร์เมช", "none" to "ไม่ใส่"), "mesh"),
            ChoiceField("dowel", "เหล็กเดือยที่รอยต่อ", listOf("yes" to "ใส่ (ระยะ 30 ซม.)", "no" to "ไม่ใส่"), "yes")
        ),
        compute = { i, r ->
            val w = pos(i.num("w")); val l = pos(i.num("l")); val area = w * l
            val conc = area * pos(i.num("t")) / 100 * (1 + r["concrete_waste"] / 100)
            val base = area * pos(i.num("base")) / 100 * (1 + r["base_compaction"] / 100)
            val sand = area * pos(i.num("sand")) / 100 * (1 + r["fill_shrink_sand"] / 100)
            val js = i.num("joint").takeIf { it > 0 } ?: 5.0
            val joints = if (l > 0) max(0.0, ceilTo(l / js) - 1) else 0.0
            val jointLen = joints * w
            val dowels = if (i.choice("dowel") == "yes" && w > 0) joints * (floor(w / 0.3 + 1e-9) + 1) else 0.0
            val sheets = area * (1 + r["wire_mesh_lap"] / 100) / r["wire_mesh_sheet_m2"]
            CalcResult(
                facts = listOf(
                    "พื้นที่ผิวถนน" to "${Fmt.n(area)} ตร.ม.",
                    "คอนกรีต (รวมเผื่อ)" to "${Fmt.n(conc, 3)} ลบ.ม.",
                    "รอยต่อ" to "${Fmt.n(joints)} แนว · ยาวรวม ${Fmt.n(jointLen)} ม."
                ),
                materials = Calculators.concreteLines(conc, "ready", r) + listOfNotNull(
                    if (base > 0) MaterialLine("crushed_rock", "หินคลุก", base, "ลบ.ม.", ceilTo(base, 0.5), "คิว", "รวมเผื่อบดอัด ${Fmt.n(r["base_compaction"])}%") else null,
                    if (sand > 0) MaterialLine("fill_sand", "ทรายรองพื้น", sand, "ลบ.ม.", ceilTo(sand, 0.5), "คิว") else null,
                    if (i.choice("mesh") == "mesh" && area > 0) MaterialLine("wire_mesh", "ไวร์เมช (แผ่น 2×6 ม.)", sheets, "แผ่น", ceilTo(sheets), "แผ่น") else null,
                    if (dowels > 0) MaterialLine("dowel", "เหล็กเดือย ยาว 50 ซม.", dowels, "ท่อน", dowels, "ท่อน", "${Fmt.n(joints)} แนว × ${Fmt.n(dowels / max(1.0, joints))} ท่อน") else null,
                    if (jointLen > 0) MaterialLine("joint_sealant", "วัสดุยาแนวรอยต่อ", jointLen, "ม.", ceilTo(jointLen), "ม.") else null
                ),
                laborBase = area, laborUnit = "ตร.ม."
            )
        }
    )

    // ---- 5. ท่อระบายน้ำ + บ่อพัก ----
    val drainSizes = listOf("d30" to 30.0, "d40" to 40.0, "d60" to 60.0, "d80" to 80.0, "d100" to 100.0)

    val drain = Calculator(
        id = "drain",
        title = "ท่อระบายน้ำ",
        subtitle = "ท่อ คสล. · บ่อพัก · ทรายรอง · ดินขุด",
        fields = listOf(
            NumberField("length", "ความยาวแนวท่อ", "ม.", 40.0),
            ChoiceField("size", "ขนาดท่อ", drainSizes.map { it.first to "Ø ${Fmt.n(it.second)} ซม." }, "d40"),
            NumberField("spacing", "บ่อพักทุก ๆ", "ม.", 10.0, "ใส่ 0 ถ้าไม่มีบ่อพัก"),
            NumberField("depth", "ความลึกร่องเฉลี่ย", "ม.", 1.0),
            NumberField("bedding", "ทรายรองท่อ หนา", "ซม.", 10.0)
        ),
        compute = { i, r ->
            val len = pos(i.num("length"))
            val (id, dcm) = drainSizes.first { it.first == i.choice("size") }
            val pipes = ceilTo(len / r["drain_pipe_length"])
            val sp = i.num("spacing")
            val holes = if (sp > 0 && len > 0) ceilTo(len / sp) + 1 else 0.0
            val trenchW = dcm / 100 + 0.5
            val dig = len * trenchW * pos(i.num("depth"))
            val sand = len * trenchW * pos(i.num("bedding")) / 100 * (1 + r["fill_shrink_sand"] / 100)
            val joints = max(0.0, pipes - 1)
            val mortarKg = joints * r["drain_joint_cement_kg"]
            CalcResult(
                facts = listOf(
                    "จำนวนท่อ" to "${Fmt.n(pipes)} ท่อน (ท่อนละ ${Fmt.n(r["drain_pipe_length"])} ม.)",
                    "บ่อพัก" to "${Fmt.n(holes)} บ่อ",
                    "ดินขุดร่อง (กว้าง ${Fmt.n(trenchW)} ม.)" to "${Fmt.n(dig)} ลบ.ม."
                ),
                materials = listOfNotNull(
                    if (pipes > 0) MaterialLine("drain_pipe_$id", "ท่อระบายน้ำ คสล. Ø ${Fmt.n(dcm)} ซม.", pipes, "ท่อน", pipes, "ท่อน") else null,
                    if (holes > 0) MaterialLine("manhole", "บ่อพัก", holes, "บ่อ", holes, "บ่อ", "สำเร็จรูป หรือหล่อในที่") else null,
                    if (sand > 0) MaterialLine("fill_sand", "ทรายรองท่อ", sand, "ลบ.ม.", ceilTo(sand, 0.5), "คิว") else null,
                    if (mortarKg > 0) Calculators.cementBags(mortarKg, r).copy(name = "ปูนซีเมนต์ ยาแนวรอยต่อท่อ")
                ),
                laborBase = len, laborUnit = "ม."
            )
        }
    )

    // ---- 6. ความลาดเอียง / ระดับ (tool) ----
    val slope = Calculator(
        id = "slope",
        title = "ความลาดเอียง",
        subtitle = "% · องศา · 1:n · ระดับปลายทาง",
        isTool = true,
        fields = listOf(
            ChoiceField("mode", "รู้ค่าอะไร", listOf("percent" to "ความลาด (%)", "degree" to "องศา", "ratio" to "อัตราส่วน 1 : n", "rise" to "ระยะตั้ง + ระยะราบ"), "percent"),
            NumberField("percent", "ความลาด", "%", 1.0, showIf = "mode" to "percent"),
            NumberField("degree", "มุม", "องศา", 5.0, showIf = "mode" to "degree"),
            NumberField("ratio", "1 :", "", 100.0, showIf = "mode" to "ratio"),
            NumberField("rise", "ระยะตั้ง (ต่างระดับ)", "ม.", 0.5, showIf = "mode" to "rise"),
            NumberField("run", "ระยะราบ", "ม.", 50.0),
            NumberField("start", "ระดับจุดเริ่ม", "ม.", 0.0, "เช่น +0.00 หรือระดับจากกล้อง")
        ),
        compute = { i, _ ->
            val run = pos(i.num("run"))
            val grade = when (i.choice("mode")) {          // rise / run
                "percent" -> i.num("percent") / 100
                "degree" -> tan(i.num("degree").coerceIn(-89.0, 89.0) * PI / 180)
                "ratio" -> i.num("ratio").takeIf { it != 0.0 }?.let { 1 / it } ?: 0.0
                else -> if (run > 0) i.num("rise") / run else 0.0
            }
            val rise = grade * run
            val deg = atan(grade) * 180 / PI
            val facts = listOf(
                "ความลาด" to "${Fmt.n(grade * 100, 3)} %",
                "มุม" to "${Fmt.n(deg, 3)} องศา",
                "อัตราส่วน" to if (grade != 0.0) "1 : ${Fmt.n(1 / kotlin.math.abs(grade), 1)}" else "ราบ",
                "ต่างระดับในระยะ ${Fmt.n(run)} ม." to "${Fmt.n(rise * 100, 1)} ซม.",
                "ความยาวตามลาด" to "${Fmt.n(sqrt(run * run + rise * rise), 3)} ม.",
                "ระดับปลายทาง (ลาดลง)" to Fmt.n(i.num("start") - rise, 3),
                "ระดับปลายทาง (ลาดขึ้น)" to Fmt.n(i.num("start") + rise, 3)
            )
            val warnings = buildList {
                if (kotlin.math.abs(grade) in 1e-9..0.005) add("ท่อระบายน้ำทั่วไปควรลาดไม่น้อยกว่าประมาณ 0.5–1% ตรวจสอบตามแบบ")
            }
            CalcResult(facts, emptyList(), warnings = warnings)
        }
    )

    // ---- 7. บันได ----
    val stairs = Calculator(
        id = "stairs",
        title = "บันได",
        subtitle = "ลูกตั้ง · ลูกนอน · คอนกรีต · ไม้แบบ",
        fields = listOf(
            NumberField("height", "ความสูงจากพื้นถึงพื้น", "ม.", 3.0),
            NumberField("width", "ความกว้างบันได", "ม.", 1.0),
            NumberField("riser", "ลูกตั้งที่ต้องการ (ไม่เกิน)", "ซม.", 18.0),
            NumberField("tread", "ลูกนอน", "ซม.", 25.0),
            NumberField("waist", "ท้องบันไดหนา", "ซม.", 15.0),
            ChoiceField("flights", "รูปแบบ", listOf("1" to "ช่วงเดียว (ตรง)", "2" to "สองช่วง มีชานพัก"), "2"),
            mixField
        ),
        compute = { i, r ->
            val h = pos(i.num("height")); val w = pos(i.num("width"))
            val target = i.num("riser").takeIf { it > 0 } ?: 18.0
            val n = if (h > 0) ceilTo(h * 100 / target) else 0.0
            val riser = if (n > 0) h / n else 0.0
            val tread = pos(i.num("tread")) / 100
            val treads = max(0.0, n - 1)
            val run = treads * tread
            val slopeLen = sqrt(run * run + h * h)
            val t = pos(i.num("waist")) / 100
            val twoFlights = i.choice("flights") == "2"
            val landing = if (twoFlights) w * (2 * w) * t else 0.0
            val waist = slopeLen * w * t
            val steps = n * (riser * tread / 2) * w
            val vol = waist + steps + landing
            val withWaste = vol * (1 + r["concrete_waste"] / 100)
            val steel = vol * r["stair_steel_kg_m3"]
            val form = slopeLen * w + n * riser * w + 2 * (run * h / 2 + slopeLen * t) + (if (twoFlights) 2 * w * w else 0.0)
            val warnings = buildList {
                if (riser * 100 > 20 + 1e-9) add("ลูกตั้งสูงเกิน 20 ซม. (เกณฑ์ทั่วไปของบ้านพักอาศัย) ควรเพิ่มจำนวนขั้น")
                if (tread * 100 < 22 - 1e-9) add("ลูกนอนแคบกว่า 22 ซม. (เกณฑ์ทั่วไปของบ้านพักอาศัย)")
                if (w < 0.8) add("บันไดแคบกว่า 80 ซม. (เกณฑ์ทั่วไปของบ้านพักอาศัย)")
                val step = 2 * riser * 100 + tread * 100
                if (n > 0 && (step < 58 || step > 66)) add("2 × ลูกตั้ง + ลูกนอน = ${Fmt.n(step, 1)} ซม. (เดินสบายประมาณ 60–64 ซม.)")
                add("ตรวจสอบกับกฎหมายควบคุมอาคารและแบบวิศวกรก่อนก่อสร้าง")
            }
            CalcResult(
                facts = listOf(
                    "จำนวนขั้น (ลูกตั้ง)" to "${Fmt.n(n)} ขั้น สูงขั้นละ ${Fmt.n(riser * 100, 1)} ซม.",
                    "ลูกนอน" to "${Fmt.n(treads)} ลูก × ${Fmt.n(tread * 100)} ซม. = ระยะราบ ${Fmt.n(run)} ม.",
                    "ความยาวตามลาด" to "${Fmt.n(slopeLen)} ม.",
                    "คอนกรีต (รวมเผื่อ)" to "${Fmt.n(withWaste, 3)} ลบ.ม.",
                    "พื้นที่ไม้แบบ" to "${Fmt.n(form)} ตร.ม."
                ),
                materials = Calculators.concreteLines(withWaste, i.choice("mix"), r) + listOfNotNull(
                    if (steel > 0) Calculators.rebarFromKg(steel, r, "≈ ${Fmt.n(steel)} กก. (${Fmt.n(r["stair_steel_kg_m3"])} กก./คิว) ขนาดจริงตามแบบ") else null,
                    if (form > 0) MaterialLine("plywood", "ไม้อัดทำแบบ 4×8 ฟุต", form / 2.88, "แผ่น", ceilTo(form / 2.88), "แผ่น") else null
                ),
                laborBase = n, laborUnit = "ขั้น", warnings = warnings
            )
        }
    )

    // ---- 8. ไม้แบบ ----
    val formwork = Calculator(
        id = "formwork",
        title = "ไม้แบบ",
        subtitle = "ไม้อัด · ไม้เคร่า · ค้ำยัน · ตะปู",
        fields = listOf(
            ChoiceField("member", "ชิ้นส่วน", listOf("column" to "เสา", "beam" to "คาน", "slab" to "พื้น"), "column"),
            NumberField("b", "หน้าตัด กว้าง (เสา/คาน)", "ซม.", 20.0),
            NumberField("h", "หน้าตัด ลึก (เสา/คาน)", "ซม.", 20.0),
            NumberField("len", "ความสูงเสา / ความยาวคาน", "ม.", 3.0),
            NumberField("count", "จำนวน (เสา/คาน)", "ชิ้น", 8.0),
            NumberField("area", "พื้นที่พื้น", "ตร.ม.", 20.0),
            NumberField("reuse", "ใช้ไม้แบบซ้ำ", "ครั้ง", 3.0, "ไม้อัดใช้ซ้ำได้ประมาณ 3–5 ครั้ง")
        ),
        compute = { i, r ->
            val member = i.choice("member")
            val b = pos(i.num("b")) / 100; val h = pos(i.num("h")) / 100
            val len = pos(i.num("len")); val n = pos(i.num("count"))
            val area = when (member) {
                "column" -> 2 * (b + h) * len * n
                "beam" -> (2 * h + b) * len * n
                else -> pos(i.num("area"))
            }
            val reuse = max(1.0, i.num("reuse"))
            val sheets = area * (1 + r["formwork_waste"] / 100) / 2.88 / reuse
            val battenM = area * r["formwork_batten_m_per_m2"] / reuse
            val props = when (member) {
                "beam" -> ceilTo(len * n / r["prop_spacing_beam"])
                "slab" -> ceilTo(area / r["prop_area_slab"])
                else -> 0.0
            }
            val nails = area * r["formwork_nail_kg_per_m2"]
            CalcResult(
                facts = listOf("พื้นที่ผิวแบบ" to "${Fmt.n(area)} ตร.ม.", "ใช้ซ้ำ" to "${Fmt.n(reuse)} ครั้ง"),
                materials = listOfNotNull(
                    if (sheets > 0) MaterialLine("plywood", "ไม้อัดทำแบบ 4×8 ฟุต", sheets, "แผ่น", ceilTo(sheets), "แผ่น", "รวมเผื่อ ${Fmt.n(r["formwork_waste"])}% · หารใช้ซ้ำแล้ว") else null,
                    if (battenM > 0) MaterialLine("batten", "ไม้เคร่า (ท่อนละ ${Fmt.n(r["batten_length"])} ม.)", battenM, "ม.", ceilTo(battenM / r["batten_length"]), "ท่อน") else null,
                    if (props > 0) MaterialLine("prop", "ค้ำยัน", props, "ต้น", props, "ต้น", if (member == "beam") "ระยะ ${Fmt.n(r["prop_spacing_beam"])} ม." else "1 ต้น / ${Fmt.n(r["prop_area_slab"])} ตร.ม.") else null,
                    if (nails > 0) MaterialLine("nails", "ตะปู", nails, "กก.", ceilTo(nails), "กก.") else null
                ),
                laborBase = area, laborUnit = "ตร.ม."
            )
        }
    )

    // ---- 9. ดินตัด-ถม จากระดับ ----
    val cutFill = Calculator(
        id = "cutfill",
        title = "ดินตัด-ถม",
        subtitle = "จากระดับ 4 มุม + กลาง · ตัด / ถม แยกกัน",
        fields = listOf(
            NumberField("w", "กว้าง", "ม.", 20.0),
            NumberField("l", "ยาว", "ม.", 40.0),
            NumberField("design", "ระดับที่ต้องการ", "ม.", 0.5, "ใช้หมุดอ้างอิงเดียวกับระดับที่ส่องได้"),
            NumberField("h1", "ระดับดินเดิม มุมที่ 1", "ม.", 0.0),
            NumberField("h2", "ระดับดินเดิม มุมที่ 2", "ม.", 0.2),
            NumberField("h3", "ระดับดินเดิม มุมที่ 3", "ม.", 0.8),
            NumberField("h4", "ระดับดินเดิม มุมที่ 4", "ม.", 0.4),
            NumberField("hc", "ระดับดินเดิม ตรงกลาง", "ม.", 0.3),
            ChoiceField("reuse", "ดินที่ตัดออก", listOf("reuse" to "ใช้ถมคืนในพื้นที่", "haul" to "ขนทิ้งทั้งหมด"), "reuse")
        ),
        compute = { i, r ->
            val w = pos(i.num("w")); val l = pos(i.num("l"))
            val d = i.num("design")
            val c = listOf(i.num("h1"), i.num("h2"), i.num("h3"), i.num("h4"))
            val hc = i.num("hc")
            // Four quarter cells: corner, two edge midpoints (average of adjacent corners), centre.
            var fill = 0.0; var cut = 0.0
            val cellArea = w * l / 4
            for (k in 0..3) {
                val a = c[k]; val prev = c[(k + 3) % 4]; val next = c[(k + 1) % 4]
                val pts = listOf(a, (a + prev) / 2, (a + next) / 2, hc)
                val diff = pts.sumOf { d - it } / 4
                if (diff > 0) fill += diff * cellArea else cut += -diff * cellArea
            }
            val shrink = r["fill_shrink_soil"]; val swell = r["dig_swell"]
            val reuse = i.choice("reuse") == "reuse"
            val usableCut = if (reuse) cut else 0.0
            // Cut soil re-compacted on site covers part of the fill.
            val fillNeeded = max(0.0, fill - usableCut)
            val buy = fillNeeded * (1 + shrink / 100)
            val haul = if (reuse) max(0.0, cut - fill) * (1 + swell / 100) else cut * (1 + swell / 100)
            CalcResult(
                facts = listOf(
                    "พื้นที่" to "${Fmt.n(w * l)} ตร.ม. (${Fmt.n(w * l / 4)} ตร.วา)",
                    "ปริมาตรดินตัด" to "${Fmt.n(cut)} ลบ.ม.",
                    "ปริมาตรดินถม (บดอัดแล้ว)" to "${Fmt.n(fill)} ลบ.ม.",
                    "ต้องซื้อดินถมเพิ่ม" to "${Fmt.n(buy)} ลบ.ม. (เผื่อยุบ ${Fmt.n(shrink)}%)",
                    "ต้องขนดินทิ้ง" to "${Fmt.n(haul)} ลบ.ม. (ดินฟู ${Fmt.n(swell)}%)"
                ),
                materials = listOfNotNull(
                    if (buy > 0) MaterialLine("fill_soil", "ดินถม", buy, "ลบ.ม.", ceilTo(buy), "คิว") else null,
                    if (haul > 0) MaterialLine("haul_soil", "ขนดินทิ้ง", haul, "ลบ.ม.", ceilTo(haul), "คิว") else null
                ),
                laborBase = cut + fill, laborUnit = "ลบ.ม.",
                warnings = listOf("คิดจากระดับ 5 จุด เหมาะกับพื้นที่ที่ลาดสม่ำเสมอ ถ้าพื้นที่ขรุขระควรส่องระดับเป็นตาราง")
            )
        }
    )

    // ---- 10. ถังบำบัด / ถังเก็บน้ำ ----
    private val septicSizes = listOf(600.0, 800.0, 1000.0, 1200.0, 1600.0, 2000.0, 2500.0, 3000.0, 4000.0, 5000.0, 6000.0)
    private val waterSizes = listOf(500.0, 700.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 4000.0, 5000.0, 6000.0)

    /** Smallest standard size that holds [litres]; above the largest size, several of the largest. */
    fun pickTank(litres: Double, sizes: List<Double>): Pair<Double, Double> {
        if (litres <= 0) return 0.0 to 0.0
        sizes.firstOrNull { it >= litres - 1e-9 }?.let { return it to 1.0 }
        val big = sizes.last()
        return big to ceilTo(litres / big)
    }

    val tanks = Calculator(
        id = "tanks",
        title = "ถังบำบัด / ถังน้ำ",
        subtitle = "ขนาดถังตามจำนวนคน",
        fields = listOf(
            NumberField("people", "จำนวนผู้อยู่อาศัย", "คน", 5.0),
            NumberField("days", "สำรองน้ำใช้", "วัน", 1.0),
            ChoiceField("what", "คำนวณ", listOf("both" to "ถังบำบัด + ถังเก็บน้ำ", "septic" to "ถังบำบัดอย่างเดียว", "water" to "ถังเก็บน้ำอย่างเดียว"), "both")
        ),
        compute = { i, r ->
            val people = pos(i.num("people"))
            val what = i.choice("what")
            val septicL = people * r["septic_l_per_person"]
            val waterL = people * r["water_l_per_person_day"] * pos(i.num("days"))
            val (sSize, sCount) = pickTank(septicL, septicSizes)
            val (wSize, wCount) = pickTank(waterL, waterSizes)
            val facts = mutableListOf<Pair<String, String>>()
            val mats = mutableListOf<MaterialLine>()
            if (what != "water") {
                facts += "ถังบำบัด (${Fmt.n(r["septic_l_per_person"])} ล./คน)" to "${Fmt.n(septicL)} ลิตร"
                if (sCount > 0) mats += MaterialLine("septic_tank", "ถังบำบัดสำเร็จรูป ${Fmt.n(sSize)} ลิตร", sCount, "ถัง", sCount, "ถัง")
            }
            if (what != "septic") {
                facts += "น้ำใช้ (${Fmt.n(r["water_l_per_person_day"])} ล./คน/วัน)" to "${Fmt.n(waterL)} ลิตร"
                if (wCount > 0) mats += MaterialLine("water_tank", "ถังเก็บน้ำ ${Fmt.n(wSize)} ลิตร", wCount, "ถัง", wCount, "ถัง")
            }
            CalcResult(
                facts, mats, laborBase = 1.0, laborUnit = "ชุด",
                warnings = listOf("ตรวจสอบตารางเลือกขนาดของผู้ผลิต และข้อกำหนดท้องถิ่น (เช่น อาคารพาณิชย์/หอพักใช้เกณฑ์ต่างจากบ้าน)")
            )
        }
    )

    val all = listOf(rebarGrid, steelSection, piles, road, drain, slope, stairs, formwork, cutFill, tanks)
}
