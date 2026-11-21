package com.sek789102.jotwai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.util.Dates
import com.sek789102.jotwai.util.NoteImageStorage
import kotlinx.coroutines.runBlocking

/** Debug builds only (adb broadcast): creates sample notes in Thai for screenshots. */
class DebugSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as JotwaiApp
        runBlocking {
            val repo = app.repository
            if (repo.getAllNotes().isNotEmpty()) return@runBlocking
            val work = repo.createFolder("งาน")!!
            repo.createFolder("ส่วนตัว")
            val lHome = repo.createLabel("บ้าน")!!
            val lWork = repo.createLabel("งาน")!!
            val lFood = repo.createLabel("อาหาร")!!
            val lTravel = repo.createLabel("เที่ยว")!!

            fun items(vararg pairs: Pair<String, Boolean>) = pairs.map { ChecklistItem(text = it.first, checked = it.second) }

            // 1
            repo.createNote(
                NoteEntity(
                    title = "รายการซื้อของ", type = NoteType.CHECKLIST, color = 3, isPinned = true,
                    checklist = items("นมสด 2 ขวด" to false, "ขนมปังโฮลวีต" to false, "กาแฟคั่วกลาง" to false, "ไข่ไก่ 1 แผง" to true, "ผลไม้" to true)
                ),
                listOf(lHome)
            )
            // 2
            repo.createNote(
                NoteEntity(
                    title = "ประชุมทีม วันจันทร์",
                    content = "# วาระ\n- สรุปยอดขาย **ไตรมาส 3**\n- แผนการตลาดปีหน้า\n- ==ส่งรายงานก่อนวันศุกร์==\n\n[x] จองห้องประชุม\n[ ] เตรียมสไลด์",
                    folderId = work, isPinned = true, reminderAt = Dates.at(1, 9)
                ),
                listOf(lWork)
            )
            // 3
            repo.createNote(
                NoteEntity(
                    title = "ไอเดียทริปเชียงใหม่", color = 4,
                    content = "- ดอยอินทนนท์ ดูทะเลหมอก\n- คาเฟ่ย่านนิมมาน\n- ถนนคนเดินวันอาทิตย์\n- ข้าวซอย *ร้านดัง*"
                ),
                listOf(lTravel)
            )
            // 4
            repo.createNote(
                NoteEntity(
                    title = "สูตรต้มยำกุ้ง (Tom Yum)", color = 2,
                    content = "1. ต้มน้ำ ใส่ข่า ตะไคร้ ใบมะกรูด\n2. ใส่กุ้ง เห็ด\n3. ปรุงน้ำปลา มะนาว พริก\n4. โรยผักชี"
                ),
                listOf(lFood)
            )
            // 5
            repo.createNote(
                NoteEntity(
                    title = "ออกกำลังกาย", type = NoteType.CHECKLIST, color = 5,
                    checklist = items("วิ่ง 30 นาที" to true, "ยืดเหยียด" to false, "ดื่มน้ำ 2 ลิตร" to false),
                    reminderAt = Dates.at(1, 6), reminderRepeat = RepeatRule.DAILY
                )
            )
            // 6
            repo.createNote(NoteEntity(title = "รหัส Wi-Fi บ้าน", content = "ไม่บอก", isLocked = true))
            // 7 - a note with a picture
            val picture = runCatching { makePicture(context) }.getOrNull()
            repo.createNote(
                NoteEntity(
                    title = "แบบห้องนั่งเล่น", content = "โทนสีอบอุ่น ไม้ธรรมชาติ", color = 10,
                    imagePaths = listOfNotNull(picture)
                )
            )
            // 8
            repo.createNote(
                NoteEntity(
                    title = "หนังสือที่อยากอ่าน", color = 8,
                    content = "- Atomic Habits\n- จิตวิทยาสายดาร์ก\n- The Psychology of Money"
                )
            )
            // 9
            repo.createNote(NoteEntity(content = "โทรหาช่างแอร์ พรุ่งนี้บ่ายสอง 📞"))
            Log.i("DebugSeed", "sample notes created")
        }
    }

    private fun makePicture(context: Context): String? {
        val bmp = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(250, 236, 214))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.rgb(196, 140, 92); c.drawRect(0f, 430f, 900f, 600f, p)
        p.color = Color.rgb(120, 156, 120); c.drawRoundRect(RectF(120f, 250f, 520f, 440f), 40f, 40f, p)
        p.color = Color.rgb(98, 132, 98); c.drawRoundRect(RectF(140f, 200f, 500f, 300f), 40f, 40f, p)
        p.color = Color.rgb(232, 135, 30); c.drawCircle(700f, 150f, 70f, p)
        p.color = Color.rgb(90, 70, 50); c.drawRect(650f, 260f, 670f, 430f, p)
        p.color = Color.rgb(110, 160, 90); c.drawCircle(660f, 240f, 60f, p)
        return NoteImageStorage.saveDrawing(context, bmp).also { bmp.recycle() }
    }
}
