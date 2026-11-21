package com.sek789102.formulabook

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.sek789102.formulabook.alarm.BootCompletedReceiver
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.local.UserPreferencesRepository
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.repository.NotesRepository
import com.sek789102.formulabook.ui.viewmodel.NotesViewModel
import com.sek789102.formulabook.util.NoteImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SevenCriticalJourneysTest {

    private lateinit var context: Context
    private val testDbName = "seven_journeys_disk_test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
        NoteImageStorage.imagesDir(context).listFiles()?.forEach { it.delete() }
    }

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
        NoteImageStorage.imagesDir(context).listFiles()?.forEach { it.delete() }
    }

    private fun openDiskDb(name: String = testDbName): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    AppDatabase.seedDefaultSubjects(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    AppDatabase.seedDefaultSubjects(db)
                }
            })
            .build()
    }

    private fun createTempJpegFile(width: Int = 120, height: Int = 90): File {
        val temp = File(context.cacheDir, "test_input_${System.nanoTime()}.jpg")
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLUE)
        FileOutputStream(temp).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        bmp.recycle()
        return temp
    }

    private fun <T> awaitStateWithMainIdle(
        flow: StateFlow<T>,
        timeoutMs: Long = 5000L,
        predicate: (T) -> Boolean
    ): T {
        val collectorJob = CoroutineScope(Dispatchers.Unconfined).launch {
            flow.collect {}
        }
        try {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val current = flow.value
                if (predicate(current)) return current
                Thread.sleep(15)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val finalValue = flow.value
            assertTrue("Timed out waiting for StateFlow condition; last value=$finalValue", predicate(finalValue))
            return finalValue
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    fun `1_create_note_close_app_and_reopen_preserves_note_on_disk`() {
        runBlocking {
        val db1 = openDiskDb()
        val createdId: Long
        try {
            val repo1 = NotesRepository(context, db1)
            createdId = repo1.saveNote(
                existingId = 0L,
                title = "กฎข้อที่ 2 ของนิวตัน",
                subjectId = SubjectCategory.PHYSICS.name,
                content = "ความเร่งแปรผันตรงกับแรงลัพธ์",
                formulas = listOf(FormulaBlock(title = "แรง", expression = "F = m × a")),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = true,
                needsReview = true,
                paperStyle = 1
            )
            assertTrue(createdId > 0L)
        } finally {
            db1.close()
        }

        // Simulate full app restart by opening a fresh Room instance on the same SQLite file
        val db2 = openDiskDb()
        try {
            val repo2 = NotesRepository(context, db2)
            val saved = repo2.allNotesWithTags.first()
            assertEquals(1, saved.size)
            val note = saved.first().note
            assertEquals(createdId, note.id)
            assertEquals("กฎข้อที่ 2 ของนิวตัน", note.title)
            assertEquals(SubjectCategory.PHYSICS.name, note.subject)
            assertEquals("F = m × a", note.formulas.first().expression)
            assertTrue(note.isPinned)
            assertTrue(note.needsReview)
        } finally {
            db2.close()
        }
        }
    }

    @Test
    fun `2_autosave_edits_update_existing_note_without_duplication_across_reopen`() {
        runBlocking {
        val db1 = openDiskDb()
        val prefs = UserPreferencesRepository(context)
        try {
            val repo1 = NotesRepository(context, db1)
            val vm = NotesViewModel(context, repo1, prefs, SavedStateHandle())

            // Simulate first auto-save creating the note
            val autoSavedId = vm.saveNoteSuspend(
                existingId = 0L,
                title = "สรุปคณิต",
                subjectId = SubjectCategory.MATH.name,
                content = "ฉบับร่าง 1",
                formulas = emptyList(),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0
            )
            assertTrue(autoSavedId > 0L)

            // Simulate subsequent auto-save while editing the same note
            val updatedId = vm.saveNoteSuspend(
                existingId = autoSavedId,
                title = "สรุปคณิตศาสตร์ ม.ปลาย",
                subjectId = SubjectCategory.MATH.name,
                content = "ฉบับสมบูรณ์พร้อมสอบ",
                formulas = listOf(FormulaBlock(title = "กำลังสอง", expression = "x = (-b ± √(b² - 4ac)) / (2a)")),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0
            )
            assertEquals(autoSavedId, updatedId)
        } finally {
            db1.close()
        }

        val db2 = openDiskDb()
        try {
            val repo2 = NotesRepository(context, db2)
            val notes = repo2.allNotesWithTags.first()
            assertEquals(1, notes.size)
            assertEquals("สรุปคณิตศาสตร์ ม.ปลาย", notes.first().note.title)
            assertEquals("ฉบับสมบูรณ์พร้อมสอบ", notes.first().note.content)
            assertEquals(1, notes.first().note.formulas.size)
        } finally {
            db2.close()
        }
        }
    }

    @Test
    fun `3_attach_image_close_and_reopen_keeps_image_file_and_decodes_cleanly`() {
        runBlocking {
        val rawFile = createTempJpegFile(width = 640, height = 480)
        val storedFileName = NoteImageStorage.saveCompressedImageFromUri(context, Uri.fromFile(rawFile))
        rawFile.delete()
        assertNotNull(storedFileName)

        val db1 = openDiskDb()
        try {
            val repo1 = NotesRepository(context, db1)
            repo1.saveNote(
                existingId = 0L,
                title = "โน้ตมีภาพประกอบ",
                subjectId = SubjectCategory.BIOLOGY.name,
                content = "โครงสร้างเซลล์",
                formulas = emptyList(),
                imagePaths = listOf(storedFileName!!),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0
            )
        } finally {
            db1.close()
        }

        val db2 = openDiskDb()
        try {
            val repo2 = NotesRepository(context, db2)
            val note = repo2.allNotesWithTags.first().first().note
            assertEquals(listOf(storedFileName), note.imagePaths)

            val resolvedFile = NoteImageStorage.resolveImageFile(context, note.imagePaths.first())
            assertTrue(resolvedFile.exists())
            assertTrue(resolvedFile.length() > 0L)

            val thumbBitmap = NoteImageStorage.decodeSampledBitmap(resolvedFile, 480)
            assertNotNull(thumbBitmap)
            thumbBitmap?.recycle()
        } finally {
            db2.close()
        }
        }
    }

    @Test
    fun `4_soft_delete_moves_to_trash_and_restore_brings_note_and_image_back`() {
        runBlocking {
        val rawFile = createTempJpegFile()
        val storedFileName = NoteImageStorage.saveCompressedImageFromUri(context, Uri.fromFile(rawFile))!!
        rawFile.delete()

        val db = openDiskDb()
        try {
            val repo = NotesRepository(context, db)
            val noteId = repo.saveNote(
                existingId = 0L,
                title = "เคมีอินทรีย์",
                subjectId = SubjectCategory.CHEMISTRY.name,
                content = "หมู่ฟังก์ชัน",
                formulas = emptyList(),
                imagePaths = listOf(storedFileName),
                tagIds = emptyList(),
                isPinned = true,
                needsReview = false,
                paperStyle = 2
            )

            repo.softDeleteNote(noteId)
            assertTrue(repo.allNotesWithTags.first().isEmpty())
            assertEquals(1, repo.trashedNotesWithTags.first().size)
            assertTrue(NoteImageStorage.resolveImageFile(context, storedFileName).exists())

            repo.restoreNoteFromTrash(noteId)
            val active = repo.allNotesWithTags.first()
            assertEquals(1, active.size)
            assertTrue(repo.trashedNotesWithTags.first().isEmpty())
            assertEquals("เคมีอินทรีย์", active.first().note.title)
            assertEquals(listOf(storedFileName), active.first().note.imagePaths)
            assertTrue(NoteImageStorage.resolveImageFile(context, storedFileName).exists())
        } finally {
            db.close()
        }
        }
    }

    @Test
    fun `5_export_and_import_restores_notes_formulas_subjects_and_attached_images`() {
        runBlocking {
        val rawFile = createTempJpegFile(width = 300, height = 200)
        val storedFileName = NoteImageStorage.saveCompressedImageFromUri(context, Uri.fromFile(rawFile))!!
        rawFile.delete()

        val sourceDb = openDiskDb("export_source.db")
        val exportedJson: String
        try {
            val sourceRepo = NotesRepository(context, sourceDb)
            val customSubj = sourceRepo.createCustomSubject("สถิติประยุกต์", 0xFF0EA5E9)
            sourceRepo.saveNote(
                existingId = 0L,
                title = "ค่าเบี่ยงเบนมาตรฐาน",
                subjectId = customSubj.id,
                content = "ใช้กับข้อมูลประชากร",
                formulas = listOf(FormulaBlock(title = "SD", expression = "σ = √(Σ(x-μ)²/N)")),
                imagePaths = listOf(storedFileName),
                tagIds = emptyList(),
                isPinned = true,
                needsReview = true,
                paperStyle = 1,
                createdAt = 1_730_000_000_000L,
                updatedAt = 1_730_000_000_000L
            )
            exportedJson = sourceRepo.exportBackupJson()
        } finally {
            sourceDb.close()
            context.deleteDatabase("export_source.db")
        }

        // Wipe image on disk before importing into a completely fresh DB
        NoteImageStorage.deleteImageFiles(context, listOf(storedFileName))
        assertFalse(NoteImageStorage.resolveImageFile(context, storedFileName).exists())

        val targetDb = openDiskDb("import_target.db")
        try {
            val targetRepo = NotesRepository(context, targetDb)
            val summary = targetRepo.importBackupJson(exportedJson).getOrThrow()
            assertEquals(1, summary.importedNotes)
            assertTrue(summary.importedSubjects >= 1)

            val restored = targetRepo.allNotesWithTags.first()
            assertEquals(1, restored.size)
            assertEquals("ค่าเบี่ยงเบนมาตรฐาน", restored.first().note.title)
            assertEquals(listOf(storedFileName), restored.first().note.imagePaths)
            assertTrue(NoteImageStorage.resolveImageFile(context, storedFileName).exists())
            assertNotNull(NoteImageStorage.decodeSampledBitmap(NoteImageStorage.resolveImageFile(context, storedFileName), 480))
        } finally {
            targetDb.close()
            context.deleteDatabase("import_target.db")
        }
        }
    }

    @Test
    fun `6_search_pin_ordering_and_subject_change_work_accurately_in_viewmodel`() {
        runBlocking {
        val db = openDiskDb()
        val prefs = UserPreferencesRepository(context)
        try {
            val repo = NotesRepository(context, db)
            val vm = NotesViewModel(context, repo, prefs, SavedStateHandle())

            val mathId = vm.saveNoteSuspend(
                existingId = 0L,
                title = "ตรีโกณมิติพื้นฐาน",
                subjectId = SubjectCategory.MATH.name,
                content = "sin²θ + cos²θ = 1",
                formulas = listOf(FormulaBlock(title = "เอกลักษณ์", expression = "sin²θ + cos²θ = 1")),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0
            )
            val engId = vm.saveNoteSuspend(
                existingId = 0L,
                title = "คำศัพท์ TOEIC",
                subjectId = SubjectCategory.ENGLISH.name,
                content = "Resilient = ยืดหยุ่น ปรับตัวเก่ง",
                formulas = emptyList(),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0
            )

            // Wait until both notes appear in filteredNotes
            val initialList = awaitStateWithMainIdle(vm.filteredNotes) { it.size == 2 }
            assertEquals(2, initialList.size)

            // Pin the English note and verify it moves to top
            vm.togglePin(engId, currentlyPinned = false)
            val afterPin = awaitStateWithMainIdle(vm.filteredNotes) { list ->
                list.firstOrNull()?.note?.id == engId && list.first().note.isPinned
            }
            assertEquals(engId, afterPin.first().note.id)

            // Search by formula/content keyword
            vm.updateSearchQuery("sin²θ")
            val searchResult = awaitStateWithMainIdle(vm.filteredNotes) { it.size == 1 }
            assertEquals(mathId, searchResult.first().note.id)

            // Clear search, change subject of engId to HISTORY, and filter by HISTORY
            vm.updateSearchQuery("")
            vm.saveNoteSuspend(
                existingId = engId,
                title = "คำศัพท์ประวัติศาสตร์",
                subjectId = SubjectCategory.HISTORY.name,
                content = "Resilient = ยืดหยุ่น ปรับตัวเก่ง",
                formulas = emptyList(),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = true,
                needsReview = false,
                paperStyle = 0
            )
            vm.selectSubjectFilter(SubjectCategory.HISTORY.name)
            val historyOnly = awaitStateWithMainIdle(vm.filteredNotes) { list ->
                list.size == 1 && list.first().note.subject == SubjectCategory.HISTORY.name
            }
            assertEquals(engId, historyOnly.first().note.id)
        } finally {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            db.close()
        }
        }
    }

    @Test
    fun `7_simulated_device_reboot_preserves_disk_notes_and_runs_boot_receiver_safely`() {
        runBlocking {
        val db1 = openDiskDb()
        try {
            val repo1 = NotesRepository(context, db1)
            repo1.saveNote(
                existingId = 0L,
                title = "โน้ตก่อนรีสตาร์ตเครื่อง",
                subjectId = SubjectCategory.THAI.name,
                content = "คำราชาศัพท์สำคัญ",
                formulas = emptyList(),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = true,
                needsReview = false,
                paperStyle = 0
            )
            repo1.addExam(
                title = "สอบภาษาไทย",
                subjectId = SubjectCategory.THAI.name,
                examDateMillis = System.currentTimeMillis() + 86_400_000L * 5,
                note = "ทบทวนทั้งหมด"
            )
        } finally {
            db1.close()
        }

        // Simulate ACTION_BOOT_COMPLETED broadcast after phone restarts
        val bootReceiver = BootCompletedReceiver()
        bootReceiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Reopen database after reboot and verify all notes and exams remain intact
        val db2 = openDiskDb()
        try {
            val repo2 = NotesRepository(context, db2)
            val notes = repo2.allNotesWithTags.first()
            val exams = repo2.allExams.first()
            assertEquals(1, notes.size)
            assertEquals("โน้ตก่อนรีสตาร์ตเครื่อง", notes.first().note.title)
            assertEquals(1, exams.size)
            assertEquals("สอบภาษาไทย", exams.first().title)
        } finally {
            db2.close()
        }
        }
    }
}
