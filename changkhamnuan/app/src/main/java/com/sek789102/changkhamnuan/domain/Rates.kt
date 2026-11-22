package com.sek789102.changkhamnuan.domain

/**
 * Material usage rates. Every value is a default the user can change in Settings,
 * because brands, local practice and site conditions differ. [source] says where the
 * default comes from so users can judge it.
 */
data class RateDef(
    val key: String,
    val group: String,
    val label: String,
    val unit: String,
    val default: Double,
    val source: String
)

object RateDefs {
    private const val SRC_YOTA = "ตารางปริมาณวัสดุ หลักเกณฑ์ราคากลางงานก่อสร้าง (yotathai.com) — รวมเผื่อเสียหายแล้ว"
    private const val SRC_EST = "ค่าประมาณทั่วไป — ควรปรับตามฉลากสินค้า/หน้างาน"
    private const val SRC_LABEL = "ดูฉลากปูนที่ใช้จริง แล้วปรับค่า"
    private const val SRC_LABEL_GEN = "ขนาดที่ขายทั่วไป — ดูฉลาก/สเปกสินค้าที่ใช้จริง แล้วปรับค่า"
    private const val SRC_STRUCT = "ค่าประมาณเพื่อตั้งงบ — ปริมาณจริงต้องใช้ตามแบบวิศวกร"

    val all: List<RateDef> = listOf(
        // ก่ออิฐมอญ (ขนาด 7x16x3.5 ซม.)
        RateDef("mon_half_pcs", "ก่ออิฐมอญครึ่งแผ่น", "อิฐมอญ", "ก้อน/ตร.ม.", 138.0, SRC_YOTA),
        RateDef("mon_half_cement", "ก่ออิฐมอญครึ่งแผ่น", "ปูนซีเมนต์", "กก./ตร.ม.", 16.01, SRC_YOTA),
        RateDef("mon_half_sand", "ก่ออิฐมอญครึ่งแผ่น", "ทรายหยาบ", "ลบ.ม./ตร.ม.", 0.05, SRC_YOTA),
        RateDef("mon_full_pcs", "ก่ออิฐมอญเต็มแผ่น", "อิฐมอญ", "ก้อน/ตร.ม.", 276.0, SRC_YOTA),
        RateDef("mon_full_cement", "ก่ออิฐมอญเต็มแผ่น", "ปูนซีเมนต์", "กก./ตร.ม.", 34.0, SRC_YOTA),
        RateDef("mon_full_sand", "ก่ออิฐมอญเต็มแผ่น", "ทรายหยาบ", "ลบ.ม./ตร.ม.", 0.12, SRC_YOTA),
        // อิฐมวลเบา 20x60 ซม.
        RateDef("aac_pcs", "ก่ออิฐมวลเบา (20×60 ซม.)", "อิฐมวลเบา", "ก้อน/ตร.ม.", 8.33, "1 ÷ (0.20 × 0.60) ม. (เอกสาร ม.มหิดล)"),
        RateDef("aac_mortar_m2_per_bag", "ก่ออิฐมวลเบา (20×60 ซม.)", "ปูนก่ออิฐมวลเบา ก่อได้", "ตร.ม./ถุง", 7.0, SRC_LABEL),
        RateDef("aac_waste", "ก่ออิฐมวลเบา (20×60 ซม.)", "เผื่อเสียหาย", "%", 5.0, SRC_EST),
        // คอนกรีตบล็อก 19x39 ซม.
        RateDef("block_pcs", "ก่อคอนกรีตบล็อก (19×39 ซม.)", "คอนกรีตบล็อก", "ก้อน/ตร.ม.", 12.5, "1 ÷ (0.20 × 0.40) ม. รวมแนวปูน"),
        RateDef("block_cement", "ก่อคอนกรีตบล็อก (19×39 ซม.)", "ปูนซีเมนต์", "กก./ตร.ม.", 10.0, SRC_EST),
        RateDef("block_sand", "ก่อคอนกรีตบล็อก (19×39 ซม.)", "ทรายหยาบ", "ลบ.ม./ตร.ม.", 0.035, SRC_EST),
        RateDef("block_waste", "ก่อคอนกรีตบล็อก (19×39 ซม.)", "เผื่อเสียหาย", "%", 5.0, SRC_EST),
        // ฉาบ
        RateDef("plaster_m2_per_bag_1cm", "ฉาบปูน", "ปูนฉาบสำเร็จรูป ฉาบได้ (หนา 1 ซม.)", "ตร.ม./ถุง", 2.8, SRC_LABEL),
        RateDef("plaster_waste", "ฉาบปูน", "เผื่อเสียหาย", "%", 5.0, SRC_EST),
        // คอนกรีต
        RateDef("concrete_dry_factor", "คอนกรีต", "ตัวคูณปริมาตรแห้ง", "เท่า", 1.54, "มาตรฐานการคำนวณส่วนผสมโดยปริมาตร (1:2:4 ≈ ปูน 6.3 ถุง/คิว)"),
        RateDef("cement_density", "คอนกรีต", "ความหนาแน่นปูนซีเมนต์", "กก./ลบ.ม.", 1440.0, "ค่ามาตรฐานทั่วไป"),
        RateDef("concrete_waste", "คอนกรีต", "เผื่อเสียหาย", "%", 5.0, SRC_EST),
        // กระเบื้อง
        RateDef("tile_adhesive", "ปูกระเบื้อง", "กาวซีเมนต์ (หนา 5 มม.)", "กก./ตร.ม.", 5.25, SRC_YOTA),
        RateDef("tile_grout", "ปูกระเบื้อง", "ยาแนว", "กก./ตร.ม.", 0.2, SRC_YOTA),
        RateDef("tile_waste", "ปูกระเบื้อง", "เผื่อตัด/แตก", "%", 10.0, SRC_EST),
        // สี
        RateDef("paint_cover", "ทาสี", "สีจริง ทาได้ (ต่อรอบ)", "ตร.ม./แกลลอน", 30.0, "ผู้ผลิตสีทั่วไประบุ 23–37 ตร.ม./แกลลอน/รอบ — ดูฉลาก"),
        RateDef("primer_cover", "ทาสี", "สีรองพื้น ทาได้ (ต่อรอบ)", "ตร.ม./แกลลอน", 35.0, SRC_LABEL),
        // ขนาดบรรจุ
        RateDef("cement_bag_kg", "ขนาดบรรจุ", "ปูนซีเมนต์ 1 ถุง", "กก.", 50.0, "ขนาดถุงมาตรฐาน"),
        RateDef("adhesive_bag_kg", "ขนาดบรรจุ", "กาวซีเมนต์ 1 ถุง", "กก.", 20.0, "ขนาดถุงที่ขายทั่วไป"),
        RateDef("grout_bag_kg", "ขนาดบรรจุ", "ยาแนว 1 ถุง", "กก.", 1.0, "ขนาดถุงที่ขายทั่วไป"),
        RateDef("paint_bucket_gal", "ขนาดบรรจุ", "สี 1 ถัง", "แกลลอน", 2.5, "ถัง 9.46 ลิตร = 2.5 แกลลอน"),
        RateDef("rebar_length", "ขนาดบรรจุ", "เหล็กเส้น 1 เส้น", "ม.", 10.0, "ความยาวมาตรฐาน"),
        RateDef("rebar_lap", "ขนาดบรรจุ", "เผื่อทาบต่อ/ตัดเหล็ก", "%", 10.0, SRC_EST),
        // กำแพงกันดิน / รั้ว
        RateDef("retain_steel_kg_m3", "กำแพงกันดิน", "เหล็กเสริมต่อคอนกรีต 1 คิว", "กก./ลบ.ม.", 90.0, SRC_STRUCT),
        RateDef("footing_steel_kg_m3", "ฐานราก", "เหล็กเสริมต่อคอนกรีต 1 คิว", "กก./ลบ.ม.", 80.0, SRC_STRUCT),
        RateDef("lean_thick", "กำแพงกันดิน", "คอนกรีตหยาบรองพื้น หนา", "ซม.", 5.0, SRC_EST),
        RateDef("fence_steel_kg_m3", "รั้ว", "เหล็กเสริมต่อคอนกรีต 1 คิว (เสา/คาน/ฐาน)", "กก./ลบ.ม.", 100.0, SRC_STRUCT),
        RateDef("fence_mesh_roll", "รั้ว", "ตาข่ายรั้ว 1 ม้วน ยาว", "ม.", 10.0, SRC_LABEL_GEN),
        // ปูพื้น
        RateDef("floor_box_spc", "ปูพื้น", "SPC 1 กล่อง ปูได้", "ตร.ม./กล่อง", 2.2, SRC_LABEL_GEN),
        RateDef("floor_box_laminate", "ปูพื้น", "ลามิเนต 1 กล่อง ปูได้", "ตร.ม./กล่อง", 2.4, SRC_LABEL_GEN),
        RateDef("floor_box_vinyl", "ปูพื้น", "กระเบื้องยาง/ไวนิล 1 กล่อง ปูได้", "ตร.ม./กล่อง", 3.3, SRC_LABEL_GEN),
        RateDef("floor_waste", "ปูพื้น", "เผื่อตัด", "%", 7.0, SRC_EST),
        RateDef("floor_foam_roll", "ปูพื้น", "โฟมรองพื้น 1 ม้วน ปูได้", "ตร.ม./ม้วน", 20.0, SRC_LABEL_GEN),
        RateDef("skirting_len", "ปูพื้น", "บัวพื้น 1 เส้น ยาว", "ม.", 2.4, SRC_LABEL_GEN),
        // หลังคา
        RateDef("roof_tile_pcs", "หลังคา", "กระเบื้องหลังคาคอนกรีต", "แผ่น/ตร.ม.", 10.0, "กระเบื้องลอนคอนกรีตทั่วไป ≈ 10 แผ่น/ตร.ม. — ดูคู่มือผู้ผลิต"),
        RateDef("roof_ridge_per_m", "หลังคา", "ครอบสันหลังคา", "แผ่น/ม.", 3.0, SRC_LABEL_GEN),
        RateDef("roof_batten_spacing", "หลังคา", "ระยะแป (กระเบื้องคอนกรีต)", "ม.", 0.32, "ระยะแปตามคู่มือผู้ผลิตกระเบื้อง (ประมาณ 32–34 ซม.)"),
        RateDef("metal_cover_width", "หลังคา", "เมทัลชีท ความกว้างใช้งานต่อแผ่น", "ม.", 0.76, SRC_LABEL_GEN),
        RateDef("metal_purlin_spacing", "หลังคา", "ระยะแป (เมทัลชีท)", "ม.", 1.0, SRC_EST),
        RateDef("purlin_length", "หลังคา", "แป 1 ท่อน ยาว", "ม.", 6.0, "ความยาวมาตรฐานเหล็กรูปพรรณ"),
        RateDef("roof_waste", "หลังคา", "เผื่อตัด/แตก", "%", 5.0, SRC_EST),
        // ฝ้า / ผนังเบา
        RateDef("gypsum_waste", "ฝ้า / ผนังเบา", "เผื่อตัดแผ่นยิปซัม", "%", 5.0, SRC_EST),
        RateDef("ceiling_cline_m_per_m2", "ฝ้า / ผนังเบา", "โครงซีลาย (ฝ้าฉาบเรียบ)", "ม./ตร.ม.", 2.5, "โครงหลักระยะ 1.2 ม. + โครงซอยระยะ 0.6 ม."),
        RateDef("c_line_length", "ฝ้า / ผนังเบา", "โครงซีลาย 1 เส้น ยาว", "ม.", 4.0, SRC_LABEL_GEN),
        RateDef("wall_angle_length", "ฝ้า / ผนังเบา", "ฉากริม 1 เส้น ยาว", "ม.", 3.0, SRC_LABEL_GEN),
        RateDef("stud_spacing", "ฝ้า / ผนังเบา", "ระยะโครงผนัง (C-stud)", "ม.", 0.6, "ระยะทั่วไปสำหรับแผ่น 120 ซม."),
        RateDef("stud_length", "ฝ้า / ผนังเบา", "โครงผนัง/ราง 1 เส้น ยาว", "ม.", 3.0, SRC_LABEL_GEN),
        // ท่อ
        RateDef("pipe_length", "ท่อ", "ท่อ PVC 1 เส้น ยาว", "ม.", 4.0, "ความยาวมาตรฐาน"),
        RateDef("pipe_waste", "ท่อ", "เผื่อตัดท่อ", "%", 10.0, SRC_EST),
        RateDef("clip_spacing", "ท่อ", "ระยะกิ๊บจับท่อ", "ม.", 1.0, SRC_EST),
        RateDef("glue_joints_per_can", "ท่อ", "น้ำยาประสานท่อ 1 กระป๋อง ทาได้", "จุดต่อ", 40.0, SRC_LABEL_GEN),
        // งานดิน
        RateDef("fill_shrink_soil", "งานดิน", "ดินถม เผื่อยุบตัวเมื่อบดอัด", "%", 30.0, SRC_EST),
        RateDef("fill_shrink_sand", "งานดิน", "ทรายถม เผื่อยุบตัว", "%", 20.0, SRC_EST),
        RateDef("fill_shrink_laterite", "งานดิน", "ลูกรัง เผื่อยุบตัว", "%", 25.0, SRC_EST),
        RateDef("dig_swell", "งานดิน", "ดินขุดฟูขึ้น (ปริมาตรตอนขน)", "%", 25.0, SRC_EST),
        // งานลาน
        RateDef("wire_mesh_sheet_m2", "งานลาน", "ไวร์เมช 1 แผ่น (2 × 6 ม.)", "ตร.ม.", 12.0, SRC_LABEL_GEN),
        RateDef("wire_mesh_lap", "งานลาน", "เผื่อทาบไวร์เมช", "%", 10.0, SRC_EST),
        RateDef("paver_worm_pcs", "งานลาน", "บล็อกตัวหนอน", "ก้อน/ตร.ม.", 40.0, SRC_LABEL_GEN),
        RateDef("paver_rect_pcs", "งานลาน", "บล็อกปูพื้น 10×20 ซม.", "ก้อน/ตร.ม.", 50.0, "1 ÷ (0.10 × 0.20) ม."),
        RateDef("paver_waste", "งานลาน", "เผื่อตัด/แตก", "%", 5.0, SRC_EST),
        // เหล็กเสริมตามระยะ / เหล็กรูปพรรณ
        RateDef("rebar_cover_cm", "เหล็กเสริมตามระยะ", "ระยะหุ้มคอนกรีต", "ซม.", 2.5, "ค่าทั่วไปของพื้น/คาน — ฐานรากมักใช้ 5–7.5 ซม. ตามแบบ"),
        RateDef("rebar_anchor_m", "เหล็กเสริมตามระยะ", "ระยะฝัง/ทาบปลายเหล็กแกน (ต่อปลาย)", "ม.", 0.3, SRC_STRUCT),
        RateDef("tie_wire_kg_per_ton", "เหล็กเสริมตามระยะ", "ลวดผูกเหล็ก", "กก./ตัน", 15.0, SRC_EST),
        RateDef("steel_stock_length", "เหล็กรูปพรรณ", "เหล็กรูปพรรณ 1 เส้น ยาว", "ม.", 6.0, "ความยาวมาตรฐานที่ขายทั่วไป"),
        // เสาเข็ม / ถนน / ท่อระบายน้ำ
        RateDef("bored_waste", "เสาเข็ม", "เข็มเจาะ เผื่อคอนกรีต", "%", 10.0, SRC_EST),
        RateDef("bored_steel_kg_m3", "เสาเข็ม", "เข็มเจาะ เหล็กเสริมต่อคอนกรีต 1 คิว", "กก./ลบ.ม.", 60.0, SRC_STRUCT),
        RateDef("base_compaction", "ถนนคอนกรีต", "หินคลุก เผื่อบดอัด", "%", 25.0, SRC_EST),
        RateDef("drain_pipe_length", "ท่อระบายน้ำ", "ท่อ คสล. 1 ท่อน ยาว", "ม.", 1.0, SRC_LABEL_GEN),
        RateDef("drain_joint_cement_kg", "ท่อระบายน้ำ", "ปูนยาแนวต่อรอยต่อท่อ", "กก./รอยต่อ", 2.0, SRC_EST),
        // บันได / ไม้แบบ
        RateDef("stair_steel_kg_m3", "บันได", "เหล็กเสริมต่อคอนกรีต 1 คิว", "กก./ลบ.ม.", 100.0, SRC_STRUCT),
        RateDef("formwork_waste", "ไม้แบบ", "เผื่อตัดไม้อัด", "%", 10.0, SRC_EST),
        RateDef("formwork_batten_m_per_m2", "ไม้แบบ", "ไม้เคร่าต่อพื้นที่แบบ", "ม./ตร.ม.", 3.0, SRC_EST),
        RateDef("batten_length", "ไม้แบบ", "ไม้เคร่า 1 ท่อน ยาว", "ม.", 4.0, SRC_LABEL_GEN),
        RateDef("prop_spacing_beam", "ไม้แบบ", "ระยะค้ำยันใต้คาน", "ม.", 1.0, SRC_EST),
        RateDef("prop_area_slab", "ไม้แบบ", "ค้ำยันใต้พื้น 1 ต้นต่อ", "ตร.ม.", 1.0, SRC_EST),
        RateDef("formwork_nail_kg_per_m2", "ไม้แบบ", "ตะปู", "กก./ตร.ม.", 0.15, SRC_EST),
        // ถัง
        RateDef("septic_l_per_person", "ถังบำบัด / ถังน้ำ", "ถังบำบัด ต่อคน", "ลิตร/คน", 200.0, "ค่าประมาณจากตารางเลือกขนาดของผู้ผลิตทั่วไป — ตรวจสอบรุ่นที่ใช้"),
        RateDef("water_l_per_person_day", "ถังบำบัด / ถังน้ำ", "น้ำใช้ ต่อคนต่อวัน", "ลิตร", 200.0, "ค่าประมาณการใช้น้ำบ้านพักอาศัยทั่วไป")
    )

    val byKey: Map<String, RateDef> = all.associateBy { it.key }
    val groups: List<String> = all.map { it.group }.distinct()
}

/** Current rates: user overrides on top of the defaults. */
class Rates(private val overrides: Map<String, Double> = emptyMap()) {
    operator fun get(key: String): Double =
        overrides[key] ?: RateDefs.byKey[key]?.default ?: error("Unknown rate $key")

    fun isCustom(key: String): Boolean = overrides.containsKey(key)
}
