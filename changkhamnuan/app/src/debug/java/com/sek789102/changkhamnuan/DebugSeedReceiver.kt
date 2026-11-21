package com.sek789102.changkhamnuan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sek789102.changkhamnuan.data.BusinessProfile
import com.sek789102.changkhamnuan.domain.Calculators
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.Rates
import kotlinx.coroutines.runBlocking

/** Debug builds only: sample prices, shop profile and one project, for screenshots. */
class DebugSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as App
        runBlocking {
            val repo = app.repo
            app.settingsRepo.setProfile(BusinessProfile("ช่างสมชาย รับเหมาก่อสร้าง", "สมชาย ใจดี", "081-234-5678", "somchai.build", "99/9 ต.บางพูด อ.ปากเกร็ด จ.นนทบุรี"))
            mapOf(
                "brick_mon" to 1.3, "cement_bag" to 145.0, "sand" to 650.0, "stone" to 700.0, "ready_mix" to 2100.0,
                "brick_aac" to 22.0, "mortar_aac" to 140.0, "plaster_bag" to 120.0, "tile_box" to 360.0,
                "tile_adhesive" to 160.0, "grout" to 45.0, "paint_bucket" to 1450.0, "paint_gal" to 620.0,
                "primer_gal" to 480.0, "rebar_DB12" to 260.0
            ).forEach { (k, v) -> repo.setPrice(k, v) }
            val id = repo.createProject("ต่อเติมครัวหลังบ้าน", "คุณวิภา")
            repo.getProject(id)?.let { repo.updateProject(it.copy(customerPhone = "089-111-2222", siteAddress = "หมู่บ้านสุขสันต์ ซอย 5", overheadPercent = 10.0)) }
            val r = Rates()
            repo.addCalcResult(id, "ผนังครัว", Calculators.bricks.run(mapOf("type" to "aac", "length" to "6", "height" to "2.8", "doors" to "1", "windows" to "1"), r), 200.0)
            repo.addCalcResult(id, "พื้นครัว", Calculators.concrete.run(mapOf("shape" to "slab", "slab_w" to "3", "slab_l" to "4", "slab_t" to "10", "mix" to "ready"), r), 900.0)
            repo.addCalcResult(id, "ปูพื้นครัว", Calculators.tiles.run(mapOf("w" to "3", "l" to "4", "size" to "60x60"), r), 250.0)
            repo.addItem(id, ItemKind.OTHER, "ค่าขนส่งวัสดุ", 1.0, "เที่ยว", 800.0)
        }
    }
}
