package com.sek789102.changkhamnuan.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/**
 * Calculators added in 1.1: retaining wall, flooring, roof, ceiling, drywall, pipes, earth fill,
 * excavation, yard and fence. Quantities are estimates for budgeting and ordering; every
 * assumption is shown in the result or editable under "อัตราการใช้วัสดุ".
 */
object ExtraCalculators {

    private val mixField = ChoiceField(
        "mix", "คอนกรีต",
        listOf("ready" to "คอนกรีตผสมเสร็จ (สั่งเป็นคิว)", "124" to "ผสมเอง 1 : 2 : 4"),
        "ready"
    )

    private fun pos(v: Double) = max(0.0, v)
    private fun trucks(loose: Double, cap: Double) = if (cap > 0) ceilTo(loose / cap) else 0.0

    // ---- กำแพงกันดิน (คสล. รูปตัว L / ตัวที คว่ำ) ----
    val retaining = Calculator(
        id = "retaining",
        title = "กำแพงกันดิน",
        subtitle = "คอนกรีต · เหล็ก · ไม้แบบ",
        fields = listOf(
            NumberField("length", "ความยาวกำแพง", "ม.", 10.0),
            NumberField("height", "ความสูงผนัง (เหนือฐาน)", "ม.", 1.5),
            NumberField("wall_t", "ความหนาผนัง", "ซม.", 20.0),
            NumberField("base_w", "ความกว้างฐาน", "ม.", 1.2),
            NumberField("base_t", "ความหนาฐาน", "ซม.", 30.0),
            mixField
        ),
        compute = { i, r ->
            val l = pos(i.num("length")); val h = pos(i.num("height"))
            val stem = l * h * pos(i.num("wall_t")) / 100
            val base = l * pos(i.num("base_w")) * pos(i.num("base_t")) / 100
            val vol = stem + base
            val withWaste = vol * (1 + r["concrete_waste"] / 100)
            val lean = l * pos(i.num("base_w")) * r["lean_thick"] / 100
            val steelKg = vol * r["retain_steel_kg_m3"]
            val form = 2 * l * h + 2 * l * pos(i.num("base_t")) / 100
            val warnings = buildList {
                add("ค่าประมาณเพื่อตั้งงบ ขนาดผนัง ฐาน และเหล็กเสริมจริงต้องใช้ตามแบบวิศวกร")
                if (h > 1.5) add("ผนังสูงเกิน 1.5 ม. ควรให้วิศวกรออกแบบและคำนวณแรงดันดิน")
                if (h > 0 && i.num("base_w") < h * 0.5) add("ฐานกว้างน้อยกว่าครึ่งหนึ่งของความสูง เสี่ยงล้ม ควรตรวจสอบกับวิศวกร")
            }
            CalcResult(
                facts = listOf(
                    "คอนกรีตผนัง" to "${Fmt.n(stem, 3)} ลบ.ม.",
                    "คอนกรีตฐาน" to "${Fmt.n(base, 3)} ลบ.ม.",
                    "รวมเผื่อ ${Fmt.n(r["concrete_waste"])}%" to "${Fmt.n(withWaste, 3)} ลบ.ม.",
                    "พื้นที่ไม้แบบ" to "${Fmt.n(form)} ตร.ม.",
                    "เหล็กเสริม (ประมาณ)" to "${Fmt.n(steelKg)} กก."
                ),
                materials = Calculators.concreteLines(withWaste, i.choice("mix"), r) + listOfNotNull(
                    if (lean > 0) MaterialLine("lean_concrete", "คอนกรีตหยาบรองพื้น หนา ${Fmt.n(r["lean_thick"])} ซม.", lean, "ลบ.ม.", ceilTo(lean, 0.5), "คิว") else null,
                    if (steelKg > 0) Calculators.rebarFromKg(steelKg, r, "≈ ${Fmt.n(steelKg)} กก. (${Fmt.n(r["retain_steel_kg_m3"])} กก./คิว) ขนาดจริงตามแบบ") else null,
                    if (form > 0) MaterialLine("plywood", "ไม้อัดทำแบบ 4×8 ฟุต", form / 2.88, "แผ่น", ceilTo(form / 2.88), "แผ่น", "ถ้าไม่ใช้ซ้ำ · ใช้ซ้ำได้จะลดลง") else null
                ),
                laborBase = l, laborUnit = "ม."
            )
        }
    )

    // ---- ฐานราก + ตอม่อ ----
    val footing = Calculator(
        id = "footing",
        title = "ฐานราก",
        subtitle = "ฐานแผ่ · ตอม่อ · คอนกรีตหยาบ",
        fields = listOf(
            NumberField("count", "จำนวนฐาน", "ฐาน", 6.0),
            NumberField("w", "ฐานกว้าง", "ม.", 1.0),
            NumberField("l", "ฐานยาว", "ม.", 1.0),
            NumberField("t", "ฐานหนา", "ซม.", 30.0),
            NumberField("pw", "ตอม่อ กว้าง", "ซม.", 20.0, "ใส่ 0 ถ้าไม่มีตอม่อ"),
            NumberField("pd", "ตอม่อ ลึก", "ซม.", 20.0),
            NumberField("ph", "ตอม่อ สูง", "ม.", 1.0),
            mixField
        ),
        compute = { i, r ->
            val n = pos(i.num("count")); val w = pos(i.num("w")); val l = pos(i.num("l")); val t = pos(i.num("t")) / 100
            val pw = pos(i.num("pw")) / 100; val pd = pos(i.num("pd")) / 100; val ph = pos(i.num("ph"))
            val pad = n * w * l * t
            val ped = n * pw * pd * ph
            val vol = pad + ped
            val withWaste = vol * (1 + r["concrete_waste"] / 100)
            val lean = n * (w + 0.1) * (l + 0.1) * r["lean_thick"] / 100
            val steelKg = vol * r["footing_steel_kg_m3"]
            val form = n * (2 * (w + l) * t + 2 * (pw + pd) * ph)
            CalcResult(
                facts = listOf(
                    "คอนกรีตฐาน" to "${Fmt.n(pad, 3)} ลบ.ม.",
                    "คอนกรีตตอม่อ" to "${Fmt.n(ped, 3)} ลบ.ม.",
                    "รวมเผื่อ ${Fmt.n(r["concrete_waste"])}%" to "${Fmt.n(withWaste, 3)} ลบ.ม.",
                    "พื้นที่ไม้แบบ" to "${Fmt.n(form)} ตร.ม.",
                    "เหล็กเสริม (ประมาณ)" to "${Fmt.n(steelKg)} กก."
                ),
                materials = Calculators.concreteLines(withWaste, i.choice("mix"), r) + listOfNotNull(
                    if (lean > 0) MaterialLine("lean_concrete", "คอนกรีตหยาบรองพื้น หนา ${Fmt.n(r["lean_thick"])} ซม.", lean, "ลบ.ม.", ceilTo(lean, 0.5), "คิว") else null,
                    if (steelKg > 0) Calculators.rebarFromKg(steelKg, r, "≈ ${Fmt.n(steelKg)} กก. (${Fmt.n(r["footing_steel_kg_m3"])} กก./คิว) ขนาดจริงตามแบบ") else null,
                    if (form > 0) MaterialLine("plywood", "ไม้อัดทำแบบ 4×8 ฟุต", form / 2.88, "แผ่น", ceilTo(form / 2.88), "แผ่น", "ถ้าไม่ใช้ซ้ำ · ใช้ซ้ำได้จะลดลง") else null
                ),
                laborBase = n, laborUnit = "ฐาน",
                warnings = listOf("ขนาดฐาน ตอม่อ และเหล็กเสริมต้องใช้ตามแบบวิศวกร ตัวเลขนี้ใช้ตั้งงบและสั่งของ")
            )
        }
    )

    // ---- ปูพื้น SPC / ลามิเนต / ไวนิล ----
    val flooring = Calculator(
        id = "flooring",
        title = "ปูพื้น",
        subtitle = "SPC · ลามิเนต · กระเบื้องยาง · บัว",
        fields = listOf(
            NumberField("w", "กว้าง", "ม.", 4.0),
            NumberField("l", "ยาว", "ม.", 5.0),
            NumberField("minus", "หักพื้นที่ (ตู้ติดผนัง ฯลฯ)", "ตร.ม.", 0.0),
            ChoiceField("type", "วัสดุปูพื้น", listOf("spc" to "พื้น SPC", "laminate" to "พื้นลามิเนต", "vinyl" to "กระเบื้องยาง / ไวนิล"), "spc"),
            NumberField("box_m2", "ปูได้ต่อกล่อง (0 = ค่าทั่วไป)", "ตร.ม.", 0.0, help = "ดูข้างกล่อง"),
            ChoiceField("foam", "โฟมรองพื้น", listOf("no" to "ไม่ใช้ (SPC ส่วนใหญ่มีโฟมติดมา)", "yes" to "ใช้โฟมรองพื้น"), "no"),
            ChoiceField("skirting", "บัวพื้น", listOf("yes" to "คิดบัวรอบห้อง", "no" to "ไม่คิดบัว"), "yes"),
            NumberField("doors", "ประตู (หักบัว)", "ช่อง", 1.0, "ช่องละ 0.9 ม.")
        ),
        compute = { i, r ->
            val w = pos(i.num("w")); val l = pos(i.num("l"))
            val area = pos(w * l - i.num("minus"))
            val type = i.choice("type")
            val perBox = i.num("box_m2").takeIf { it > 0 } ?: r["floor_box_$type"]
            val need = area * (1 + r["floor_waste"] / 100)
            val boxes = ceilTo(need / perBox)
            val name = mapOf("spc" to "พื้น SPC", "laminate" to "พื้นลามิเนต", "vinyl" to "กระเบื้องยาง / ไวนิล")[type] ?: type
            val skirtLen = pos(2 * (w + l) - pos(i.num("doors")) * 0.9)
            val mats = buildList {
                add(MaterialLine("floor_$type", name, need, "ตร.ม.", boxes, "กล่อง", "กล่องละ ${Fmt.n(perBox)} ตร.ม. · รวมเผื่อ ${Fmt.n(r["floor_waste"])}%"))
                if (i.choice("foam") == "yes" && area > 0) add(MaterialLine("floor_foam", "โฟมรองพื้น", area * 1.05, "ตร.ม.", ceilTo(area * 1.05 / r["floor_foam_roll"]), "ม้วน", "ม้วนละ ${Fmt.n(r["floor_foam_roll"])} ตร.ม."))
                if (i.choice("skirting") == "yes" && skirtLen > 0) add(MaterialLine("skirting", "บัวพื้น", skirtLen, "ม.", ceilTo(skirtLen * 1.05 / r["skirting_len"]), "เส้น", "เส้นละ ${Fmt.n(r["skirting_len"])} ม."))
            }
            CalcResult(
                facts = listOf("พื้นที่ปู" to "${Fmt.n(area)} ตร.ม.", "ความยาวบัว" to "${Fmt.n(skirtLen)} ม."),
                materials = mats, laborBase = area, laborUnit = "ตร.ม."
            )
        }
    )

    // ---- หลังคา ----
    val roof = Calculator(
        id = "roof",
        title = "หลังคา",
        subtitle = "กระเบื้องคอนกรีต · เมทัลชีท · แป",
        fields = listOf(
            ChoiceField("shape", "ทรงหลังคา", listOf("gable" to "จั่ว (ลาดสองด้าน)", "shed" to "เพิงหมาแหงน (ลาดด้านเดียว)"), "gable"),
            NumberField("w", "ความกว้างอาคาร (ด้านลาด)", "ม.", 8.0),
            NumberField("l", "ความยาวอาคาร", "ม.", 10.0),
            NumberField("overhang", "ชายคายื่น", "ม.", 0.6),
            NumberField("pitch", "องศาความชัน", "องศา", 30.0, "กระเบื้องคอนกรีตควร ≥ 17° · เมทัลชีท ≥ 5°"),
            ChoiceField("mat", "วัสดุมุง", listOf("tile" to "กระเบื้องหลังคาคอนกรีต", "metal" to "เมทัลชีท"), "tile")
        ),
        compute = { i, r ->
            val ov = pos(i.num("overhang"))
            val wp = pos(i.num("w")) + 2 * ov
            val lp = pos(i.num("l")) + 2 * ov
            val pitch = i.num("pitch").coerceIn(0.0, 75.0)
            val gable = i.choice("shape") == "gable"
            val sides = if (gable) 2 else 1
            val slope = (if (gable) wp / 2 else wp) / cos(pitch * PI / 180)
            val area = slope * lp * sides
            val warnings = mutableListOf<String>()
            val mats = if (i.choice("mat") == "tile") {
                if (pitch < 17) warnings += "กระเบื้องคอนกรีตส่วนใหญ่ต้องการความชันอย่างน้อยประมาณ 17° ดูคู่มือผู้ผลิต"
                val tiles = area * r["roof_tile_pcs"] * (1 + r["roof_waste"] / 100)
                val rows = (ceilTo(slope / r["roof_batten_spacing"]) + 1) * sides
                val battenM = rows * lp
                listOfNotNull(
                    MaterialLine("roof_tile", "กระเบื้องหลังคาคอนกรีต", tiles, "แผ่น", ceilTo(tiles), "แผ่น", "รวมเผื่อ ${Fmt.n(r["roof_waste"])}%"),
                    if (gable) MaterialLine("roof_ridge", "ครอบสันหลังคา", lp * r["roof_ridge_per_m"], "แผ่น", ceilTo(lp * r["roof_ridge_per_m"]), "แผ่น") else null,
                    MaterialLine("purlin", "แป (ระยะ ${Fmt.n(r["roof_batten_spacing"] * 100)} ซม.)", battenM, "ม.", ceilTo(battenM * 1.05 / r["purlin_length"]), "ท่อน", "ท่อนละ ${Fmt.n(r["purlin_length"])} ม.")
                )
            } else {
                if (pitch < 5) warnings += "เมทัลชีทควรมีความชันอย่างน้อยประมาณ 5° เพื่อไม่ให้น้ำย้อน"
                val sheets = ceilTo(lp / r["metal_cover_width"]) * sides
                val sheetLen = ceilTo(slope, 0.1)
                val rows = (ceilTo(slope / r["metal_purlin_spacing"]) + 1) * sides
                val purlinM = rows * lp
                listOfNotNull(
                    MaterialLine("metal_sheet", "เมทัลชีท (ตัดยาวแผ่นละ ${Fmt.n(sheetLen)} ม.)", sheets * sheetLen, "ม.", sheets * sheetLen, "เมตร", "${Fmt.n(sheets)} แผ่น · หน้ากว้างใช้งาน ${Fmt.n(r["metal_cover_width"])} ม."),
                    if (gable) MaterialLine("metal_ridge", "ครอบสันเมทัลชีท", lp, "ม.", ceilTo(lp), "เมตร") else null,
                    MaterialLine("purlin", "แป (ระยะ ${Fmt.n(r["metal_purlin_spacing"])} ม.)", purlinM, "ม.", ceilTo(purlinM * 1.05 / r["purlin_length"]), "ท่อน", "ท่อนละ ${Fmt.n(r["purlin_length"])} ม.")
                )
            }
            CalcResult(
                facts = listOf(
                    "ขนาดรวมชายคา" to "${Fmt.n(wp)} × ${Fmt.n(lp)} ม.",
                    "ความยาวลาด (ต่อด้าน)" to "${Fmt.n(slope)} ม.",
                    "พื้นที่หลังคา" to "${Fmt.n(area)} ตร.ม."
                ),
                materials = mats, laborBase = area, laborUnit = "ตร.ม.", warnings = warnings
            )
        }
    )

    // ---- ฝ้าเพดาน ----
    val ceiling = Calculator(
        id = "ceiling",
        title = "ฝ้าเพดาน",
        subtitle = "ยิปซัมฉาบเรียบ · ฝ้าทีบาร์",
        fields = listOf(
            NumberField("w", "กว้าง", "ม.", 4.0),
            NumberField("l", "ยาว", "ม.", 5.0),
            ChoiceField("type", "ชนิดฝ้า", listOf("gypsum" to "ยิปซัมฉาบเรียบ (โครงซีลาย)", "tbar" to "ฝ้าทีบาร์ 60×60 ซม."), "gypsum")
        ),
        compute = { i, r ->
            val w = pos(i.num("w")); val l = pos(i.num("l"))
            val area = w * l
            val perim = 2 * (w + l)
            val waste = 1 + r["gypsum_waste"] / 100
            val angle = MaterialLine("wall_angle", "ฉากริม", perim, "ม.", ceilTo(perim / r["wall_angle_length"]), "เส้น", "เส้นละ ${Fmt.n(r["wall_angle_length"])} ม.")
            val hangers = MaterialLine("hanger", "ชุดแขวนฝ้า", area / 1.44, "ชุด", ceilTo(area / 1.44), "ชุด", "ประมาณ 1 ชุดต่อ 1.2 × 1.2 ม.")
            val mats = if (i.choice("type") == "gypsum") {
                val cl = area * r["ceiling_cline_m_per_m2"]
                listOf(
                    MaterialLine("gypsum_board", "แผ่นยิปซัม 120×240 ซม.", area * waste / 2.88, "แผ่น", ceilTo(area * waste / 2.88), "แผ่น", "รวมเผื่อ ${Fmt.n(r["gypsum_waste"])}%"),
                    MaterialLine("c_line", "โครงซีลาย", cl, "ม.", ceilTo(cl / r["c_line_length"]), "เส้น", "เส้นละ ${Fmt.n(r["c_line_length"])} ม."),
                    angle, hangers
                )
            } else {
                val mainM = area / 1.2
                listOf(
                    MaterialLine("ceiling_tile", "แผ่นฝ้าทีบาร์ 60×60 ซม.", area * waste / 0.36, "แผ่น", ceilTo(area * waste / 0.36), "แผ่น", "รวมเผื่อ ${Fmt.n(r["gypsum_waste"])}%"),
                    MaterialLine("main_tee", "ทีบาร์ ตัวหลัก 3.66 ม.", mainM, "ม.", ceilTo(mainM / 3.66), "เส้น", "ระยะ 1.2 ม."),
                    MaterialLine("cross_tee_120", "ทีบาร์ ตัวซอย 120 ซม.", area / 0.72, "เส้น", ceilTo(area / 0.72), "เส้น"),
                    MaterialLine("cross_tee_60", "ทีบาร์ ตัวซอย 60 ซม.", area / 0.72, "เส้น", ceilTo(area / 0.72), "เส้น"),
                    angle, hangers
                )
            }
            CalcResult(
                facts = listOf("พื้นที่ฝ้า" to "${Fmt.n(area)} ตร.ม.", "ความยาวรอบห้อง" to "${Fmt.n(perim)} ม."),
                materials = mats, laborBase = area, laborUnit = "ตร.ม."
            )
        }
    )

    // ---- ผนังเบา ----
    val drywall = Calculator(
        id = "drywall",
        title = "ผนังเบา",
        subtitle = "ยิปซัม · โครง C-stud · ราง",
        fields = Calculators.wallFields + listOf(NumberField("faces", "ปิดแผ่นกี่ด้าน", "ด้าน", 2.0)),
        compute = { i, r ->
            val area = Calculators.wallArea(i)
            val length = pos(i.num("length")); val h = pos(i.num("height"))
            val faces = pos(i.num("faces"))
            val boards = area * faces * (1 + r["gypsum_waste"] / 100) / 2.88
            val openings = pos(i.num("doors")) + pos(i.num("windows"))
            val studs = if (length > 0) ceilTo(length / r["stud_spacing"]) + 1 + 2 * openings else 0.0
            val piecesPerStud = ceilTo(h / r["stud_length"]).coerceAtLeast(1.0)
            val trackM = 2 * length
            val warnings = buildList { if (h > r["stud_length"]) add("ผนังสูงกว่าโครง 1 เส้น (${Fmt.n(r["stud_length"])} ม.) ต้องต่อโครงหรือสั่งยาวพิเศษ") }
            CalcResult(
                facts = listOf(
                    "พื้นที่ผนังสุทธิ" to "${Fmt.n(area)} ตร.ม.",
                    "จำนวนโครงตั้ง" to "${Fmt.n(studs)} ต้น (ระยะ ${Fmt.n(r["stud_spacing"])} ม. + ข้างช่องเปิด)"
                ),
                materials = listOf(
                    MaterialLine("gypsum_board", "แผ่นยิปซัม 120×240 ซม.", boards, "แผ่น", ceilTo(boards), "แผ่น", "${Fmt.n(faces)} ด้าน · รวมเผื่อ ${Fmt.n(r["gypsum_waste"])}%"),
                    MaterialLine("c_stud", "โครงผนัง C-stud", studs * h, "ม.", studs * piecesPerStud, "เส้น", "เส้นละ ${Fmt.n(r["stud_length"])} ม."),
                    MaterialLine("u_track", "รางบน-ล่าง U-track", trackM, "ม.", ceilTo(trackM * 1.05 / r["stud_length"]), "เส้น", "เส้นละ ${Fmt.n(r["stud_length"])} ม.")
                ),
                laborBase = area, laborUnit = "ตร.ม.", warnings = warnings
            )
        }
    )

    // ---- ท่อ PVC ----
    val pipeSizes = listOf("s12" to "1/2 นิ้ว", "s34" to "3/4 นิ้ว", "s1" to "1 นิ้ว", "s2" to "2 นิ้ว", "s4" to "4 นิ้ว")

    val pipes = Calculator(
        id = "pipes",
        title = "งานท่อ",
        subtitle = "ท่อ PVC · ข้อต่อ · น้ำยา · กิ๊บ",
        fields = listOf(
            ChoiceField("size", "ขนาดท่อ", pipeSizes, "s12"),
            NumberField("length", "ความยาวท่อรวม", "ม.", 20.0),
            NumberField("elbows", "ข้องอ", "ตัว", 6.0),
            NumberField("tees", "สามทาง", "ตัว", 2.0)
        ),
        compute = { i, r ->
            val id = i.choice("size")
            val label = pipeSizes.first { it.first == id }.second
            val len = pos(i.num("length"))
            val sticks = ceilTo(len * (1 + r["pipe_waste"] / 100) / r["pipe_length"])
            val sockets = max(0.0, ceilTo(len / r["pipe_length"]) - 1)
            val elbows = ceilTo(pos(i.num("elbows"))); val tees = ceilTo(pos(i.num("tees")))
            val joints = 2 * sockets + 2 * elbows + 3 * tees
            val glue = if (joints > 0) ceilTo(joints / r["glue_joints_per_can"]) else 0.0
            val clips = ceilTo(len / r["clip_spacing"])
            CalcResult(
                facts = listOf("ความยาวท่อ" to "${Fmt.n(len)} ม.", "จุดทาน้ำยา" to "${Fmt.n(joints)} จุด"),
                materials = listOfNotNull(
                    if (sticks > 0) MaterialLine("pvc_$id", "ท่อ PVC $label (เส้นละ ${Fmt.n(r["pipe_length"])} ม.)", len, "ม.", sticks, "เส้น", "รวมเผื่อตัด ${Fmt.n(r["pipe_waste"])}%") else null,
                    if (elbows > 0) MaterialLine("elbow_$id", "ข้องอ $label", elbows, "ตัว", elbows, "ตัว") else null,
                    if (tees > 0) MaterialLine("tee_$id", "สามทาง $label", tees, "ตัว", tees, "ตัว") else null,
                    if (sockets > 0) MaterialLine("socket_$id", "ข้อต่อตรง $label", sockets, "ตัว", sockets, "ตัว", "ต่อท่อแนวยาว (ประมาณ)") else null,
                    if (glue > 0) MaterialLine("pvc_glue", "น้ำยาประสานท่อ", joints, "จุด", glue, "กระป๋อง") else null,
                    if (clips > 0) MaterialLine("pipe_clip", "กิ๊บจับท่อ $label", clips, "ตัว", clips, "ตัว", "ระยะ ${Fmt.n(r["clip_spacing"])} ม.") else null
                ),
                laborBase = len, laborUnit = "ม."
            )
        }
    )

    // ---- ถมดิน ----
    private val fillMats = listOf("soil" to "ดินถม", "sand" to "ทรายถม", "laterite" to "ลูกรัง")

    val fill = Calculator(
        id = "fill",
        title = "ถมดิน",
        subtitle = "ดินถม · ทรายถม · ลูกรัง · จำนวนเที่ยวรถ",
        fields = listOf(
            ChoiceField("mode", "ใส่พื้นที่เป็น", listOf("wl" to "กว้าง × ยาว (ม.)", "wa" to "ตารางวา"), "wl"),
            NumberField("w", "กว้าง", "ม.", 10.0, showIf = "mode" to "wl"),
            NumberField("l", "ยาว", "ม.", 20.0, showIf = "mode" to "wl"),
            NumberField("wa", "พื้นที่", "ตร.วา", 50.0, showIf = "mode" to "wa"),
            NumberField("depth", "ถมสูง (หลังบดอัด)", "ม.", 0.5),
            ChoiceField("mat", "วัสดุถม", fillMats, "soil"),
            NumberField("truck", "ความจุรถ 1 เที่ยว", "คิว", 12.0, "6 ล้อ ≈ 4–5 คิว · 10 ล้อ ≈ 12–16 คิว")
        ),
        compute = { i, r ->
            val area = if (i.choice("mode") == "wl") pos(i.num("w")) * pos(i.num("l")) else pos(i.num("wa")) * 4
            val v = area * pos(i.num("depth"))
            val mat = i.choice("mat")
            val shrink = r["fill_shrink_$mat"]
            val loose = v * (1 + shrink / 100)
            val t = trucks(loose, i.num("truck"))
            val key = mapOf("soil" to "fill_soil", "sand" to "fill_sand", "laterite" to "laterite")[mat] ?: "fill_soil"
            CalcResult(
                facts = listOf(
                    "พื้นที่" to "${Fmt.n(area)} ตร.ม. (${Fmt.n(area / 4)} ตร.วา)",
                    "ปริมาตรหลังบดอัด" to "${Fmt.n(v)} ลบ.ม.",
                    "ต้องสั่ง (เผื่อยุบ ${Fmt.n(shrink)}%)" to "${Fmt.n(loose)} ลบ.ม.",
                    "จำนวนเที่ยวรถ" to "${Fmt.n(t)} เที่ยว"
                ),
                materials = if (loose > 0) listOf(MaterialLine(key, fillMats.first { it.first == mat }.second, loose, "ลบ.ม.", ceilTo(loose), "คิว", "≈ ${Fmt.n(t)} เที่ยว (รถ ${Fmt.n(i.num("truck"))} คิว)")) else emptyList(),
                laborBase = v, laborUnit = "ลบ.ม."
            )
        }
    )

    // ---- ขุดดิน ----
    val excavation = Calculator(
        id = "excavation",
        title = "ขุดดิน",
        subtitle = "หลุมฐานราก · ร่อง · ขนดินทิ้ง",
        fields = listOf(
            NumberField("w", "กว้าง", "ม.", 1.0),
            NumberField("l", "ยาว", "ม.", 1.0, "ขุดร่อง: ใส่ความยาวร่องทั้งหมด"),
            NumberField("depth", "ลึก", "ม.", 1.2),
            NumberField("count", "จำนวนหลุม", "หลุม", 6.0),
            ChoiceField("haul", "ดินที่ขุด", listOf("haul" to "ขนทิ้ง", "keep" to "เก็บไว้ถมกลับ"), "haul"),
            NumberField("truck", "ความจุรถ 1 เที่ยว", "คิว", 5.0, "6 ล้อ ≈ 4–5 คิว · 10 ล้อ ≈ 12–16 คิว")
        ),
        compute = { i, r ->
            val v = pos(i.num("w")) * pos(i.num("l")) * pos(i.num("depth")) * pos(i.num("count"))
            val loose = v * (1 + r["dig_swell"] / 100)
            val t = trucks(loose, i.num("truck"))
            val warnings = buildList { if (i.num("depth") > 1.5) add("ขุดลึกเกิน 1.5 ม. ควรทำค้ำยันหรือลาดดิน ป้องกันดินพัง") }
            CalcResult(
                facts = listOf(
                    "ปริมาตรดินขุด" to "${Fmt.n(v)} ลบ.ม.",
                    "ปริมาตรตอนขน (ฟู ${Fmt.n(r["dig_swell"])}%)" to "${Fmt.n(loose)} ลบ.ม.",
                    "จำนวนเที่ยวรถ" to "${Fmt.n(t)} เที่ยว"
                ),
                materials = if (i.choice("haul") == "haul" && loose > 0)
                    listOf(MaterialLine("haul_soil", "ขนดินทิ้ง", loose, "ลบ.ม.", ceilTo(loose), "คิว", "≈ ${Fmt.n(t)} เที่ยว (รถ ${Fmt.n(i.num("truck"))} คิว)"))
                else emptyList(),
                laborBase = v, laborUnit = "ลบ.ม.", warnings = warnings
            )
        }
    )

    // ---- งานลาน ----
    val yard = Calculator(
        id = "yard",
        title = "งานลาน",
        subtitle = "ลานคอนกรีต · บล็อกปูพื้น · ไวร์เมช",
        fields = listOf(
            NumberField("w", "กว้าง", "ม.", 5.0),
            NumberField("l", "ยาว", "ม.", 8.0),
            ChoiceField("type", "ชนิดลาน", listOf("concrete" to "ลานคอนกรีต", "worm" to "บล็อกตัวหนอน", "rect" to "บล็อกปูพื้น 10×20 ซม."), "concrete"),
            NumberField("thick", "คอนกรีตหนา", "ซม.", 10.0, showIf = "type" to "concrete"),
            ChoiceField("mesh", "ไวร์เมช (ลานคอนกรีต)", listOf("yes" to "ใส่ไวร์เมช", "no" to "ไม่ใส่"), "yes"),
            NumberField("sand", "ทรายรองพื้น หนา", "ซม.", 5.0)
        ),
        compute = { i, r ->
            val area = pos(i.num("w")) * pos(i.num("l"))
            val sandM3 = area * pos(i.num("sand")) / 100 * (1 + r["fill_shrink_sand"] / 100)
            val sandLine = if (sandM3 > 0) MaterialLine("fill_sand", "ทรายรองพื้น", sandM3, "ลบ.ม.", ceilTo(sandM3, 0.5), "คิว", "รวมเผื่อยุบ ${Fmt.n(r["fill_shrink_sand"])}%") else null
            val type = i.choice("type")
            val facts = mutableListOf("พื้นที่ลาน" to "${Fmt.n(area)} ตร.ม.")
            val mats = if (type == "concrete") {
                val vol = area * pos(i.num("thick")) / 100
                val withWaste = vol * (1 + r["concrete_waste"] / 100)
                facts += "คอนกรีต (รวมเผื่อ ${Fmt.n(r["concrete_waste"])}%)" to "${Fmt.n(withWaste, 3)} ลบ.ม."
                val sheets = area * (1 + r["wire_mesh_lap"] / 100) / r["wire_mesh_sheet_m2"]
                Calculators.concreteLines(withWaste, "ready", r) + listOfNotNull(
                    if (i.choice("mesh") == "yes" && area > 0) MaterialLine("wire_mesh", "ไวร์เมช (แผ่น 2×6 ม.)", sheets, "แผ่น", ceilTo(sheets), "แผ่น", "รวมเผื่อทาบ ${Fmt.n(r["wire_mesh_lap"])}%") else null,
                    sandLine
                )
            } else {
                val rate = r[if (type == "worm") "paver_worm_pcs" else "paver_rect_pcs"]
                val pcs = area * rate * (1 + r["paver_waste"] / 100)
                listOfNotNull(
                    MaterialLine("paver_$type", if (type == "worm") "บล็อกตัวหนอน" else "บล็อกปูพื้น 10×20 ซม.", pcs, "ก้อน", ceilTo(pcs), "ก้อน", "${Fmt.n(rate)} ก้อน/ตร.ม. · รวมเผื่อ ${Fmt.n(r["paver_waste"])}%"),
                    sandLine
                )
            }
            CalcResult(facts, mats, laborBase = area, laborUnit = "ตร.ม.")
        }
    )

    // ---- รั้ว ----
    val fence = Calculator(
        id = "fence",
        title = "รั้ว",
        subtitle = "รั้วบล็อก · รั้วตาข่าย · เสา ฐาน คาน",
        fields = listOf(
            ChoiceField("type", "ชนิดรั้ว", listOf("block" to "รั้วก่อบล็อก เสา-คาน คสล.", "mesh" to "รั้วตาข่าย เสาเหล็ก"), "block"),
            NumberField("length", "ความยาวรั้ว", "ม.", 20.0),
            NumberField("height", "ความสูงรั้ว", "ม.", 2.0),
            NumberField("spacing", "ระยะห่างเสา", "ม.", 3.0),
            mixField
        ),
        compute = { i, r ->
            val l = pos(i.num("length")); val h = pos(i.num("height"))
            val spacing = i.num("spacing").takeIf { it > 0 } ?: 3.0
            val posts = if (l > 0) ceilTo(l / spacing) + 1 else 0.0
            val facts = mutableListOf("จำนวนเสา" to "${Fmt.n(posts)} ต้น")
            val warnings = mutableListOf<String>()
            val mats = if (i.choice("type") == "block") {
                // posts 15×15 ซม. (ฝังลงฐาน 0.5 ม.), footings 0.6×0.6×0.3 ม., ground beam 15×30 ซม. + top beam 15×15 ซม.
                val vol = posts * 0.15 * 0.15 * (h + 0.5) + posts * 0.6 * 0.6 * 0.3 + l * (0.15 * 0.30 + 0.15 * 0.15)
                val withWaste = vol * (1 + r["concrete_waste"] / 100)
                val wall = pos(l * h - posts * 0.15 * h - l * 0.15)
                val blocks = wall * r["block_pcs"] * (1 + r["block_waste"] / 100)
                val steel = vol * r["fence_steel_kg_m3"]
                facts += "พื้นที่ก่อบล็อก" to "${Fmt.n(wall)} ตร.ม."
                facts += "คอนกรีต เสา ฐาน คาน (รวมเผื่อ)" to "${Fmt.n(withWaste, 3)} ลบ.ม."
                warnings += "คิดเสา 15×15 ซม. ฐาน 60×60×30 ซม. คานล่าง 15×30 ซม. คานบน 15×15 ซม. — ถ้าแบบต่างจากนี้ให้ปรับตามแบบ"
                if (h > 2.5) warnings += "รั้วสูงเกิน 2.5 ม. ควรให้วิศวกรตรวจสอบขนาดเสาและฐาน"
                Calculators.concreteLines(withWaste, i.choice("mix"), r) + listOf(
                    MaterialLine("brick_block", "คอนกรีตบล็อก", blocks, "ก้อน", ceilTo(blocks), "ก้อน", "รวมเผื่อ ${Fmt.n(r["block_waste"])}%"),
                    Calculators.cementBags(wall * r["block_cement"], r).copy(name = "ปูนซีเมนต์ (ก่อบล็อก)"),
                    Calculators.sand(wall * r["block_sand"]).copy(name = "ทรายหยาบ (ก่อบล็อก)"),
                    Calculators.rebarFromKg(steel, r, "≈ ${Fmt.n(steel)} กก. (${Fmt.n(r["fence_steel_kg_m3"])} กก./คิว) ขนาดจริงตามแบบ")
                )
            } else {
                // steel posts set in 30×30×50 ซม. concrete footings
                val vol = posts * 0.3 * 0.3 * 0.5 * (1 + r["concrete_waste"] / 100)
                val rolls = ceilTo(l / r["fence_mesh_roll"])
                facts += "คอนกรีตหล่อโคนเสา (รวมเผื่อ)" to "${Fmt.n(vol, 3)} ลบ.ม."
                listOf(
                    MaterialLine("fence_post", "เสาเหล็กรั้ว ยาว ${Fmt.n(h + 0.5)} ม.", posts, "ต้น", posts, "ต้น", "ฝังลงดิน 0.5 ม."),
                    MaterialLine("fence_mesh", "ตาข่ายรั้ว สูง ${Fmt.n(h)} ม.", l, "ม.", rolls, "ม้วน", "ม้วนละ ${Fmt.n(r["fence_mesh_roll"])} ม.")
                ) + Calculators.concreteLines(vol, i.choice("mix"), r)
            }
            CalcResult(facts, mats, laborBase = l, laborUnit = "ม.", warnings = warnings)
        }
    )

    val all = listOf(retaining, footing, flooring, roof, ceiling, drywall, pipes, fill, excavation, yard, fence)
}
