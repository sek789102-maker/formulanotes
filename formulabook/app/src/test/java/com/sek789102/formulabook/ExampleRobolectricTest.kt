package com.sek789102.formulabook

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.core.content.ContextCompat
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.local.UserPreferencesRepository
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.ThemeMode
import com.sek789102.formulabook.data.repository.NotesRepository
import com.sek789102.formulabook.domain.study.GpaCalculator
import com.sek789102.formulabook.domain.study.GpaCourseEntry
import com.sek789102.formulabook.domain.study.PlannerHelper
import com.sek789102.formulabook.domain.study.Sm2Rating
import com.sek789102.formulabook.domain.study.TimetableEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import android.app.AlarmManager
import android.content.Intent
import com.sek789102.formulabook.alarm.ExamAlarmReceiver
import com.sek789102.formulabook.alarm.ExamNotificationScheduler
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import com.sek789102.formulabook.data.repository.BackupTooLargeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    private fun buildInMemoryDb(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(appContext(), AppDatabase::class.java)
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

    @Test
    fun `app name matches localized title`() {
        val appName = appContext().getString(R.string.app_name)
        assertEquals("สมุดโน้ตสูตร", appName)
    }

    @Test
    fun `fresh database starts completely empty across notes, tags, exams, and focus sessions`() = runBlocking {
        val db = buildInMemoryDb()
        try {
            val repo = NotesRepository(appContext(), db)
            assertTrue(repo.allNotesWithTags.first().isEmpty())
            assertTrue(repo.trashedNotesWithTags.first().isEmpty())
            assertTrue(repo.allTags.first().isEmpty())
            assertTrue(repo.allExams.first().isEmpty())
            assertTrue(repo.allStudySessions.first().isEmpty())

            val tagId = repo.createOrGetTag("สอบกลางภาค", 1)
            val noteId = repo.saveNote(
                existingId = 0L,
                title = "พลังงานจลน์",
                subjectId = SubjectCategory.PHYSICS.name,
                content = "ทบทวนบทที่ 4",
                formulas = listOf(FormulaBlock(title = "Ek", expression = "E = 0.5 × m × v²")),
                imagePaths = emptyList(),
                tagIds = listOf(tagId),
                isPinned = true,
                needsReview = false,
                paperStyle = 0
            )
            repo.addExam("สอบกลางภาคฟิสิกส์", SubjectCategory.PHYSICS.name, System.currentTimeMillis() + 86400000L, "บทที่ 1-4")
            repo.recordStudySession(SubjectCategory.PHYSICS.name, 25)

            val savedNotes = repo.allNotesWithTags.first()
            assertEquals(1, savedNotes.size)
            assertEquals(noteId, savedNotes.first().note.id)
            assertEquals(1, savedNotes.first().tags.size)
            assertEquals("สอบกลางภาค", savedNotes.first().tags.first().name)
            assertEquals(1, repo.allExams.first().size)
            assertEquals(25, repo.allStudySessions.first().first().durationMinutes)
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup export v3 includes custom subjects, exams, sessions, SM-2 data and skips duplicates on re-import`() = runBlocking {
        val sourceDb = buildInMemoryDb()
        val targetDb = buildInMemoryDb()
        try {
            val sourceRepo = NotesRepository(appContext(), sourceDb)
            val customSubject = sourceRepo.createCustomSubject("ดาราศาสตร์", 0xFF0284C7)
            val tagId = sourceRepo.createOrGetTag("ปลายภาค", 2)
            val noteId = sourceRepo.saveNote(
                existingId = 0L,
                title = "กฎของเคปเลอร์",
                subjectId = customSubject.id,
                content = "- [ ] ท่องสมการ\n**สำคัญมาก**",
                formulas = listOf(FormulaBlock(id = "f1", title = "คาบโคจร", expression = "T² = a³")),
                imagePaths = emptyList(),
                tagIds = listOf(tagId),
                isPinned = false,
                needsReview = true,
                paperStyle = 1,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L
            )
            sourceRepo.rateFormulaSm2(noteId, "f1", Sm2Rating.EASY)
            sourceRepo.addExam("สอบดาราศาสตร์", customSubject.id, 1750000000000L, "บทวงโคจร")
            sourceRepo.recordStudySession(customSubject.id, 25)

            val backupJson = sourceRepo.exportBackupJson()
            val root = JSONObject(backupJson)
            assertEquals(NotesRepository.BACKUP_VERSION, root.getInt("version"))
            assertTrue(root.getJSONArray("subjects").length() >= 1)
            assertEquals(1, root.getJSONArray("exam_countdowns").length())
            assertEquals(1, root.getJSONArray("study_sessions").length())

            val targetRepo = NotesRepository(appContext(), targetDb)
            val firstImport = targetRepo.importBackupJson(backupJson).getOrThrow()
            assertEquals(1, firstImport.importedNotes)
            assertEquals(0, firstImport.skippedNotes)
            assertEquals(1, firstImport.importedExams)
            assertEquals(1, firstImport.importedSessions)
            assertTrue(firstImport.importedSubjects >= 1)

            val importedNote = targetRepo.allNotesWithTags.first().first().note
            assertEquals(customSubject.id, importedNote.subject)
            assertEquals(4, importedNote.formulas.first().intervalDays)

            val secondImport = targetRepo.importBackupJson(backupJson).getOrThrow()
            assertEquals(0, secondImport.importedNotes)
            assertEquals(1, secondImport.skippedNotes)
            assertEquals(0, secondImport.importedExams)
            assertEquals(1, secondImport.skippedExams)
            assertEquals(0, secondImport.importedSessions)
            assertEquals(1, secondImport.skippedSessions)
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }

    @Test
    fun `legacy v1 backup without version or exams imports cleanly and mid-import error rolls back`() = runBlocking {
        val db = buildInMemoryDb()
        try {
            val repo = NotesRepository(appContext(), db)

            val legacyV1Json = """
                {
                  "notes": [
                    {
                      "title": "สมการกำลังสอง",
                      "subject": "MATH",
                      "content": "สูตรหลัก",
                      "createdAt": 1690000000000,
                      "formulas": [
                        { "title": "Quadratic", "expression": "x = (-b ± √(b² - 4ac)) / (2a)" }
                      ],
                      "tags": [
                        { "name": "พีชคณิต", "colorIndex": 0 }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val v1Result = repo.importBackupJson(legacyV1Json).getOrThrow()
            assertEquals(1, v1Result.importedNotes)
            assertEquals(0, v1Result.importedExams)
            assertEquals(0, v1Result.importedSessions)

            val brokenMidImportJson = """
                {
                  "version": 2,
                  "notes": [
                    {
                      "title": "โน้ตที่ไม่ควรถูกบันทึก",
                      "subject": "CHEMISTRY",
                      "content": "จะถูก rollback",
                      "createdAt": 1710000000000
                    },
                    "NOT_AN_OBJECT_CAUSES_JSON_EXCEPTION"
                  ]
                }
            """.trimIndent()

            val brokenResult = repo.importBackupJson(brokenMidImportJson)
            assertTrue(brokenResult.isFailure)
            val notesAfterRollback = repo.allNotesWithTags.first()
            assertEquals(1, notesAfterRollback.size)
            assertEquals("สมการกำลังสอง", notesAfterRollback.first().note.title)
        } finally {
            db.close()
        }
    }

    @Test
    fun `soft delete moves note to trash, restore brings it back, and 30 day purge removes expired notes`() = runBlocking {
        val db = buildInMemoryDb()
        try {
            val repo = NotesRepository(appContext(), db)
            val tagId = repo.createOrGetTag("เรขาคณิต", 3)
            val noteId = repo.saveNote(
                existingId = 0L,
                title = "ทฤษฎีบทพีทาโกรัส",
                subjectId = SubjectCategory.MATH.name,
                content = "[ ] ทบทวน",
                formulas = listOf(FormulaBlock(title = "พีทาโกรัส", expression = "c² = a² + b²")),
                imagePaths = emptyList(),
                tagIds = listOf(tagId),
                isPinned = true,
                needsReview = false,
                paperStyle = 2
            )

            repo.softDeleteNote(noteId)
            assertTrue(repo.allNotesWithTags.first().isEmpty())
            assertEquals(1, repo.trashedNotesWithTags.first().size)

            repo.restoreNoteFromTrash(noteId)
            assertEquals(1, repo.allNotesWithTags.first().size)
            assertTrue(repo.trashedNotesWithTags.first().isEmpty())

            // Toggle checklist line in DB
            repo.toggleChecklistLine(noteId, 0)
            assertEquals("[x] ทบทวน", repo.getNoteSnapshot(noteId)?.note?.content)

            // Save an expired trashed note (31 days old) and verify purge
            val now = System.currentTimeMillis()
            repo.saveNote(
                existingId = 0L,
                title = "โน้ตหมดอายุ",
                subjectId = SubjectCategory.GENERAL.name,
                content = "เก่าเกิน 30 วัน",
                formulas = emptyList(),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0,
                deletedAt = now - TimeUnit.DAYS.toMillis(31)
            )
            assertEquals(1, repo.trashedNotesWithTags.first().size)
            val purged = repo.purgeExpiredTrash(retentionDays = 30L, nowMillis = now)
            assertEquals(1, purged)
            assertTrue(repo.trashedNotesWithTags.first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `deleting custom subject reassigns its notes to GENERAL without data loss`() = runBlocking {
        val db = buildInMemoryDb()
        try {
            val repo = NotesRepository(appContext(), db)
            val custom = repo.createCustomSubject("แคลคูลัส 2", 0xFF7C3AED)
            val noteId = repo.saveNote(
                existingId = 0L,
                title = "อินทิกรัลหลายชั้น",
                subjectId = custom.id,
                content = "เนื้อหา",
                formulas = emptyList(),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = false,
                paperStyle = 0
            )
            repo.deleteCustomSubject(custom.id)
            val updatedNote = repo.getNoteSnapshot(noteId)?.note
            assertNotNull(updatedNote)
            assertEquals(SubjectCategory.GENERAL.name, updatedNote!!.subject)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migration 2 to 3 preserves existing notes, exams, sessions and seeds default subjects`() = runBlocking {
        val dbName = "migration_2_to_3_test.db"
        appContext().deleteDatabase(dbName)

        // Create a real v2 SQLite schema with pre-existing user data
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(appContext())
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `subject` TEXT NOT NULL, `content` TEXT NOT NULL, `formulas` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `needsReview` INTEGER NOT NULL, `paperStyle` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                    )
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (`noteId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`noteId`, `tagId`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`)")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `exam_countdowns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `subject` TEXT NOT NULL, `examDateMillis` INTEGER NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `study_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "INSERT INTO notes (id, title, subject, content, formulas, isPinned, needsReview, paperStyle, createdAt, updatedAt) " +
                            "VALUES (10, 'โน้ตเดิม v2', 'MATH', 'ยังอยู่ครบ', '[]', 1, 0, 0, 1700000000000, 1700000001000)"
                    )
                    db.execSQL(
                        "INSERT INTO exam_countdowns (id, title, subject, examDateMillis, note, createdAt) " +
                            "VALUES (5, 'สอบ v2', 'PHYSICS', 1760000000000, 'อ่านบท 1', 1700000000000)"
                    )
                    db.execSQL(
                        "INSERT INTO study_sessions (id, subject, durationMinutes, completedAt) " +
                            "VALUES (7, 'CHEMISTRY', 30, 1700000000000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(helperConfig).writableDatabase.close()

        val migratedDb = Room.databaseBuilder(appContext(), AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = NotesRepository(appContext(), migratedDb)
            val notes = repo.allNotesWithTags.first()
            assertEquals(1, notes.size)
            assertEquals("โน้ตเดิม v2", notes.first().note.title)
            assertTrue(notes.first().note.imagePaths.isEmpty())
            assertEquals(null, notes.first().note.deletedAt)
            assertFalse(notes.first().note.isPinned.not())

            assertEquals(1, repo.allExams.first().size)
            assertEquals(1, repo.allStudySessions.first().size)
            assertTrue(repo.allSubjects.first().size >= SubjectCategory.entries.size)
        } finally {
            migratedDb.close()
            appContext().deleteDatabase(dbName)
        }
    }

    @Test
    fun `user preferences repository defaults to system theme, dynamic color off, and updates cleanly`() = runBlocking {
        val repo = UserPreferencesRepository(appContext())

        val initial = repo.preferencesFlow.first()
        assertEquals(ThemeMode.SYSTEM, initial.themeMode)
        assertFalse(initial.dynamicColor)

        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.preferencesFlow.first().themeMode)

        repo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo.preferencesFlow.first().themeMode)

        repo.setDynamicColor(true)
        assertTrue(repo.preferencesFlow.first().dynamicColor)

        repo.setDarkMode(true)
        assertEquals(ThemeMode.DARK, repo.preferencesFlow.first().themeMode)

        repo.setDarkMode(false)
        assertEquals(ThemeMode.LIGHT, repo.preferencesFlow.first().themeMode)

        repo.setThemeMode(ThemeMode.SYSTEM)
        repo.setDynamicColor(false)
        assertEquals(ThemeMode.SYSTEM, repo.preferencesFlow.first().themeMode)
        assertFalse(repo.preferencesFlow.first().dynamicColor)
    }

    @Test
    fun `phase 1 theme strings, splash icon, cartoon mascots, and empty state drawables resolve cleanly`() {
        val ctx = appContext()
        assertEquals("สว่าง", ctx.getString(R.string.theme_mode_light))
        assertEquals("มืด", ctx.getString(R.string.theme_mode_dark))
        assertEquals("ตามระบบ", ctx.getString(R.string.theme_mode_system))
        assertEquals("ถังขยะ", ctx.getString(R.string.settings_trash_title))
        assertTrue(ctx.getString(R.string.settings_dynamic_color_title).isNotBlank())
        assertTrue(ctx.getString(R.string.settings_exam_notif_permission_denied).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_speech_no_formula, 2).contains("เยี่ยมเลย!"))
        assertTrue(ctx.getString(R.string.mascot_study_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_focus_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_math_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_tags_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_science_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_humanities_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_english_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.mascot_trophy_desc).isNotBlank())
        assertTrue(ctx.getString(R.string.detail_recall_mode).isNotBlank())
        assertTrue(ctx.getString(R.string.editor_note_templates_btn).isNotBlank())
        assertTrue(ctx.getString(R.string.math_tool_gpa).isNotBlank())
        assertTrue(ctx.getString(R.string.study_mode_match).isNotBlank())
        assertTrue(ctx.getString(R.string.match_win_title).isNotBlank())
        assertTrue(ctx.getString(R.string.nav_planner).isNotBlank())
        assertTrue(ctx.getString(R.string.nav_vault).isNotBlank())
        assertTrue(ctx.getString(R.string.vault_header_title).isNotBlank())
        assertTrue(ctx.getString(R.string.vault_save_to_note).isNotBlank())
        assertTrue(ctx.getString(R.string.settings_vault_summary, 3, 5).contains("3"))
        assertTrue(ctx.getString(R.string.planner_timetable_title).isNotBlank())
        assertTrue(ctx.getString(R.string.planner_tasks_title, 0).isNotBlank())

        assertEquals(11, SubjectCategory.entries.size)

        val drawables = listOf(
            R.drawable.ic_splash_book,
            R.drawable.img_student_mascot,
            R.drawable.img_mascot_study,
            R.drawable.img_mascot_focus,
            R.drawable.img_mascot_math,
            R.drawable.img_mascot_tags,
            R.drawable.img_mascot_science,
            R.drawable.img_mascot_humanities,
            R.drawable.img_mascot_english,
            R.drawable.img_mascot_trophy,
            R.drawable.ill_empty_notes,
            R.drawable.ill_empty_search,
            R.drawable.ill_empty_tags,
            R.drawable.ill_empty_study,
            R.drawable.ill_empty_done,
            R.drawable.ill_empty_exams,
            R.drawable.ill_empty_trash
        )
        drawables.forEach { resId ->
            assertNotNull("Drawable $resId failed to inflate", ContextCompat.getDrawable(ctx, resId))
        }
    }

    @Test
    fun `expanded subjects include thai, social, history, art and flashcard blockType round-trips in DB and backup`() = runBlocking {
        val db = buildInMemoryDb()
        val targetDb = buildInMemoryDb()
        try {
            val repo = NotesRepository(appContext(), db)
            val subjects = repo.allSubjects.first()
            val subjectIds = subjects.map { it.id }.toSet()
            assertTrue("THAI" in subjectIds)
            assertTrue("ENGLISH" in subjectIds)
            assertTrue("SOCIAL" in subjectIds)
            assertTrue("HISTORY" in subjectIds)
            assertTrue("ART" in subjectIds)
            assertEquals(SubjectCategory.GENERAL.ordinal, subjects.first { it.id == "GENERAL" }.sortOrder)

            val flashcard = FormulaBlock(
                id = "fc_eng_1",
                title = "Resilient",
                expression = "ฟื้นตัวเร็ว ปรับตัวเก่ง",
                variablesNote = "She is remarkably resilient.",
                blockType = FormulaBlock.BLOCK_TYPE_FLASHCARD
            )
            repo.saveNote(
                existingId = 0L,
                title = "คำศัพท์อังกฤษ บท 1",
                subjectId = SubjectCategory.ENGLISH.name,
                content = "==Resilient== แปลว่า ฟื้นตัวเร็ว",
                formulas = listOf(flashcard),
                imagePaths = emptyList(),
                tagIds = emptyList(),
                isPinned = false,
                needsReview = true,
                paperStyle = 0
            )

            val saved = repo.allNotesWithTags.first().first().note
            assertEquals(SubjectCategory.ENGLISH.name, saved.subject)
            assertTrue(saved.formulas.first().isFlashcard)
            assertEquals(FormulaBlock.BLOCK_TYPE_FLASHCARD, saved.formulas.first().blockType)

            val backupJson = repo.exportBackupJson()
            val targetRepo = NotesRepository(appContext(), targetDb)
            targetRepo.importBackupJson(backupJson).getOrThrow()
            val imported = targetRepo.allNotesWithTags.first().first().note
            assertEquals(FormulaBlock.BLOCK_TYPE_FLASHCARD, imported.formulas.first().blockType)
            assertTrue(imported.formulas.first().isFlashcard)
        } finally {
            db.close()
            targetDb.close()
        }
    }

    @Test
    fun `gpa calculator and planner timetable preferences work cleanly`() = runBlocking {
        val courses = listOf(
            GpaCourseEntry(name = "ภาษาไทย", credits = 1.5, gradePoint = 4.0),
            GpaCourseEntry(name = "คณิตศาสตร์", credits = 2.5, gradePoint = 3.0)
        )
        val summary = GpaCalculator.computeGpa(courses)
        assertEquals(4.0, summary.totalCredits, 0.001)
        assertEquals(3.375, summary.gpa, 0.001)
        assertEquals(2, summary.courseCount)

        val encoded = GpaCalculator.toJson(courses)
        val decoded = GpaCalculator.fromJson(encoded)
        assertEquals(2, decoded.size)
        assertEquals("ภาษาไทย", decoded[0].name)

        val schedule = listOf(
            TimetableEntry(dayOfWeek = 1, startTime = "08:30", endTime = "09:30", subjectId = "MATH", room = "402"),
            TimetableEntry(dayOfWeek = 3, startTime = "10:00", endTime = "11:00", subjectId = "ENGLISH", room = "205")
        )
        val roundTripped = PlannerHelper.fromJson(PlannerHelper.toJson(schedule))
        assertEquals(2, roundTripped.size)
        assertEquals("402", roundTripped[0].room)

        val prefsRepo = UserPreferencesRepository(appContext())
        prefsRepo.setGpaCourses(courses)
        prefsRepo.setTimetableEntries(schedule)

        val prefs = prefsRepo.preferencesFlow.first()
        assertEquals(2, prefs.gpaCourses.size)
        assertEquals(2, prefs.timetableEntries.size)
        assertEquals("MATH", prefs.timetableEntries.first().subjectId)
    }

    @Test
    fun `exam notification scheduler schedules future slots, skips past or disabled, and cancels cleanly`() {
        val ctx = appContext()
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val now = 1_750_000_000_000L
        val tenDaysLater = now + TimeUnit.DAYS.toMillis(10)
        val futureExam = ExamCountdownEntity(
            id = 42L,
            title = "สอบปลายภาค",
            subject = SubjectCategory.MATH.name,
            examDateMillis = tenDaysLater
        )
        val pastExam = ExamCountdownEntity(
            id = 43L,
            title = "สอบที่ผ่านมาแล้ว",
            subject = SubjectCategory.PHYSICS.name,
            examDateMillis = now - TimeUnit.DAYS.toMillis(5)
        )

        ExamNotificationScheduler.rescheduleAll(ctx, listOf(futureExam, pastExam), enabled = true, nowMillis = now)
        assertEquals(4, shadowAlarmManager.scheduledAlarms.size)

        ExamNotificationScheduler.cancelForExam(ctx, futureExam.id)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        ExamNotificationScheduler.rescheduleAll(ctx, listOf(futureExam), enabled = false, nowMillis = now)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        val triggerUtc = ExamNotificationScheduler.computeTriggerTimeMillis(
            examDateMillis = tenDaysLater,
            daysBefore = 0,
            timeZone = TimeZone.getTimeZone("Asia/Bangkok")
        )
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok")).apply {
            timeInMillis = triggerUtc
        }
        assertEquals(7, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun `exam alarm receiver ignores missing extras and deleted exams without crashing`() {
        val ctx = appContext()
        val receiver = ExamAlarmReceiver()
        receiver.onReceive(ctx, null)
        receiver.onReceive(
            ctx,
            Intent(ctx, ExamAlarmReceiver::class.java).apply {
                putExtra(ExamNotificationScheduler.EXTRA_EXAM_ID, 99999L)
                putExtra(ExamNotificationScheduler.EXTRA_EXAM_TITLE, "สอบที่ถูกลบ")
            }
        )
    }

    @Test
    fun `backup import rejects oversized payload and keeps untitled note when it has formulas`() = runBlocking {
        val db = buildInMemoryDb()
        try {
            val repo = NotesRepository(appContext(), db)

            val hugePayload = "a".repeat(NotesRepository.MAX_BACKUP_CHARS + 1)
            val hugeRes = repo.importBackupJson(hugePayload)
            assertTrue(hugeRes.isFailure)
            assertTrue(hugeRes.exceptionOrNull() is BackupTooLargeException)

            val untitledWithFormulaJson = """
                {
                  "version": 3,
                  "notes": [
                    {
                      "title": "",
                      "subject": "PHYSICS",
                      "content": "",
                      "createdAt": 1720000000000,
                      "formulas": [
                        { "title": "กฎข้อที่ 2", "expression": "F = m * a" }
                      ]
                    }
                  ]
                }
            """.trimIndent()
            val summary = repo.importBackupJson(untitledWithFormulaJson).getOrThrow()
            assertEquals(1, summary.importedNotes)
            val notes = repo.allNotesWithTags.first()
            assertEquals(1, notes.size)
            assertTrue(notes.first().note.title.isNotBlank())
            assertEquals(1, notes.first().note.formulas.size)
        } finally {
            db.close()
        }
    }
}
