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
        RateDef("rebar_lap", "ขนาดบรรจุ", "เผื่อทาบต่อ/ตัดเหล็ก", "%", 10.0, SRC_EST)
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
