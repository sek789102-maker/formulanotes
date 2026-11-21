package com.sek789102.changkhamnuan.domain

/** Materials that can have a saved price. Prices are the user's own (no defaults: they vary by area and shop). */
data class CatalogItem(val key: String, val name: String, val unit: String)

object Catalog {
    val items: List<CatalogItem> = listOf(
        CatalogItem("brick_mon", "อิฐมอญ", "ก้อน"),
        CatalogItem("brick_aac", "อิฐมวลเบา 20×60 ซม.", "ก้อน"),
        CatalogItem("brick_block", "คอนกรีตบล็อก", "ก้อน"),
        CatalogItem("cement_bag", "ปูนซีเมนต์", "ถุง"),
        CatalogItem("mortar_aac", "ปูนก่ออิฐมวลเบา", "ถุง"),
        CatalogItem("plaster_bag", "ปูนฉาบสำเร็จรูป", "ถุง"),
        CatalogItem("sand", "ทรายหยาบ", "คิว"),
        CatalogItem("stone", "หิน", "คิว"),
        CatalogItem("ready_mix", "คอนกรีตผสมเสร็จ", "คิว"),
        CatalogItem("tile_box", "กระเบื้อง", "กล่อง"),
        CatalogItem("tile_adhesive", "กาวซีเมนต์", "ถุง"),
        CatalogItem("grout", "ยาแนว", "ถุง"),
        CatalogItem("paint_bucket", "สีจริง", "ถัง"),
        CatalogItem("paint_gal", "สีจริง", "แกลลอน"),
        CatalogItem("primer_bucket", "สีรองพื้น", "ถัง"),
        CatalogItem("primer_gal", "สีรองพื้น", "แกลลอน")
    ) + Calculators.rebarSizes.map { (n, _) -> CatalogItem("rebar_$n", "เหล็ก $n", "เส้น") }

    val byKey = items.associateBy { it.key }
}
