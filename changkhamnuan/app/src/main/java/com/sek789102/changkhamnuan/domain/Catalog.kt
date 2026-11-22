package com.sek789102.changkhamnuan.domain

/** Materials that can have a saved price. Prices are the user's own (no defaults: they vary by area and shop). */
data class CatalogItem(val key: String, val name: String, val unit: String, val group: String = "")

object Catalog {
    private const val G_MASON = "อิฐ ปูน ทราย หิน"
    private const val G_CONC = "คอนกรีต เหล็ก ไม้แบบ"
    private const val G_FINISH = "กระเบื้อง สี พื้น"
    private const val G_ROOF = "หลังคา ฝ้า ผนังเบา"
    private const val G_PIPE = "ท่อ PVC"
    private const val G_OUT = "งานดิน ลาน รั้ว"

    val items: List<CatalogItem> = listOf(
        CatalogItem("brick_mon", "อิฐมอญ", "ก้อน", G_MASON),
        CatalogItem("brick_aac", "อิฐมวลเบา 20×60 ซม.", "ก้อน", G_MASON),
        CatalogItem("brick_block", "คอนกรีตบล็อก", "ก้อน", G_MASON),
        CatalogItem("cement_bag", "ปูนซีเมนต์", "ถุง", G_MASON),
        CatalogItem("mortar_aac", "ปูนก่ออิฐมวลเบา", "ถุง", G_MASON),
        CatalogItem("plaster_bag", "ปูนฉาบสำเร็จรูป", "ถุง", G_MASON),
        CatalogItem("sand", "ทรายหยาบ", "คิว", G_MASON),
        CatalogItem("stone", "หิน", "คิว", G_MASON),
        CatalogItem("ready_mix", "คอนกรีตผสมเสร็จ", "คิว", G_CONC),
        CatalogItem("lean_concrete", "คอนกรีตหยาบ (ลีน)", "คิว", G_CONC),
        CatalogItem("plywood", "ไม้อัดทำแบบ 4×8 ฟุต", "แผ่น", G_CONC),
        CatalogItem("batten", "ไม้เคร่า", "ท่อน", G_CONC),
        CatalogItem("prop", "ค้ำยัน", "ต้น", G_CONC),
        CatalogItem("nails", "ตะปู", "กก.", G_CONC),
        CatalogItem("pile_driven", "เสาเข็มตอก", "ต้น", G_CONC)
    ) + Calculators.rebarSizes.map { (n, _) -> CatalogItem("rebar_$n", "เหล็ก $n", "เส้น", G_CONC) } + listOf(
        CatalogItem("tie_wire", "ลวดผูกเหล็ก", "กก.", G_CONC),
        CatalogItem("steel_kg", "เหล็กรูปพรรณ (ราคาต่อ กก.)", "กก.", G_CONC),
        CatalogItem("tile_box", "กระเบื้อง", "กล่อง", G_FINISH),
        CatalogItem("tile_adhesive", "กาวซีเมนต์", "ถุง", G_FINISH),
        CatalogItem("grout", "ยาแนว", "ถุง", G_FINISH),
        CatalogItem("paint_bucket", "สีจริง", "ถัง", G_FINISH),
        CatalogItem("paint_gal", "สีจริง", "แกลลอน", G_FINISH),
        CatalogItem("primer_bucket", "สีรองพื้น", "ถัง", G_FINISH),
        CatalogItem("primer_gal", "สีรองพื้น", "แกลลอน", G_FINISH),
        CatalogItem("floor_spc", "พื้น SPC", "กล่อง", G_FINISH),
        CatalogItem("floor_laminate", "พื้นลามิเนต", "กล่อง", G_FINISH),
        CatalogItem("floor_vinyl", "กระเบื้องยาง / ไวนิล", "กล่อง", G_FINISH),
        CatalogItem("floor_foam", "โฟมรองพื้น", "ม้วน", G_FINISH),
        CatalogItem("skirting", "บัวพื้น", "เส้น", G_FINISH),
        CatalogItem("roof_tile", "กระเบื้องหลังคาคอนกรีต", "แผ่น", G_ROOF),
        CatalogItem("roof_ridge", "ครอบสันหลังคา", "แผ่น", G_ROOF),
        CatalogItem("metal_sheet", "เมทัลชีท", "เมตร", G_ROOF),
        CatalogItem("metal_ridge", "ครอบสันเมทัลชีท", "เมตร", G_ROOF),
        CatalogItem("purlin", "แป (ท่อนละ 6 ม.)", "ท่อน", G_ROOF),
        CatalogItem("gypsum_board", "แผ่นยิปซัม 120×240 ซม.", "แผ่น", G_ROOF),
        CatalogItem("c_line", "โครงซีลาย", "เส้น", G_ROOF),
        CatalogItem("wall_angle", "ฉากริม", "เส้น", G_ROOF),
        CatalogItem("hanger", "ชุดแขวนฝ้า", "ชุด", G_ROOF),
        CatalogItem("ceiling_tile", "แผ่นฝ้าทีบาร์ 60×60 ซม.", "แผ่น", G_ROOF),
        CatalogItem("main_tee", "ทีบาร์ ตัวหลัก", "เส้น", G_ROOF),
        CatalogItem("cross_tee_120", "ทีบาร์ ตัวซอย 120 ซม.", "เส้น", G_ROOF),
        CatalogItem("cross_tee_60", "ทีบาร์ ตัวซอย 60 ซม.", "เส้น", G_ROOF),
        CatalogItem("c_stud", "โครงผนัง C-stud", "เส้น", G_ROOF),
        CatalogItem("u_track", "รางผนัง U-track", "เส้น", G_ROOF)
    ) + ExtraCalculators.pipeSizes.flatMap { (id, label) ->
        listOf(
            CatalogItem("pvc_$id", "ท่อ PVC $label", "เส้น", G_PIPE),
            CatalogItem("elbow_$id", "ข้องอ $label", "ตัว", G_PIPE),
            CatalogItem("tee_$id", "สามทาง $label", "ตัว", G_PIPE),
            CatalogItem("socket_$id", "ข้อต่อตรง $label", "ตัว", G_PIPE)
        )
    } + listOf(
        CatalogItem("pvc_glue", "น้ำยาประสานท่อ", "กระป๋อง", G_PIPE),
        CatalogItem("pipe_clip", "กิ๊บจับท่อ", "ตัว", G_PIPE),
        CatalogItem("fill_soil", "ดินถม", "คิว", G_OUT),
        CatalogItem("fill_sand", "ทรายถม", "คิว", G_OUT),
        CatalogItem("laterite", "ลูกรัง", "คิว", G_OUT),
        CatalogItem("haul_soil", "ขนดินทิ้ง", "คิว", G_OUT),
        CatalogItem("wire_mesh", "ไวร์เมช (แผ่น 2×6 ม.)", "แผ่น", G_OUT),
        CatalogItem("paver_worm", "บล็อกตัวหนอน", "ก้อน", G_OUT),
        CatalogItem("paver_rect", "บล็อกปูพื้น 10×20 ซม.", "ก้อน", G_OUT),
        CatalogItem("fence_mesh", "ตาข่ายรั้ว", "ม้วน", G_OUT),
        CatalogItem("fence_post", "เสาเหล็กรั้ว", "ต้น", G_OUT),
        CatalogItem("crushed_rock", "หินคลุก", "คิว", G_OUT),
        CatalogItem("dowel", "เหล็กเดือย", "ท่อน", G_OUT),
        CatalogItem("joint_sealant", "วัสดุยาแนวรอยต่อถนน", "ม.", G_OUT),
        CatalogItem("manhole", "บ่อพัก", "บ่อ", G_OUT)
    ) + CivilCalculators.drainSizes.map { (id, d) -> CatalogItem("drain_pipe_$id", "ท่อระบายน้ำ คสล. Ø ${d.toInt()} ซม.", "ท่อน", G_OUT) } + listOf(
        CatalogItem("septic_tank", "ถังบำบัดสำเร็จรูป", "ถัง", G_PIPE),
        CatalogItem("water_tank", "ถังเก็บน้ำ", "ถัง", G_PIPE)
    )

    val byKey = items.associateBy { it.key }
}
