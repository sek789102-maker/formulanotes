package com.sek789102.formulabook

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.repository.InvalidBackupFormatException
import com.sek789102.formulabook.data.repository.NotesRepository
import com.sek789102.formulabook.util.NoteImageStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupZipAndImageCleanupTest {

    private fun ctx(): Context = ApplicationProvider.getApplicationContext()

    private fun db(): AppDatabase =
        Room.inMemoryDatabaseBuilder(ctx(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    AppDatabase.seedDefaultSubjects(db)
                }
            })
            .build()

    private suspend fun saveNoteWithImages(repo: NotesRepository, title: String, images: List<String>): Long =
        repo.saveNote(
            existingId = 0L,
            title = title,
            subjectId = SubjectCategory.PHYSICS.name,
            content = "เนื้อหา",
            formulas = emptyList(),
            imagePaths = images,
            tagIds = emptyList(),
            isPinned = false,
            needsReview = false,
            paperStyle = 0,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L
        )

    @Test
    fun `zip backup stores images as separate entries and restores them`() = runBlocking {
        val source = db()
        val target = db()
        try {
            val bytes = ByteArray(300_000) { (it % 251).toByte() }
            val img = NoteImageStorage.resolveImageFile(ctx(), "zip_test_photo.jpg")
            img.writeBytes(bytes)
            val srcRepo = NotesRepository(ctx(), source)
            saveNoteWithImages(srcRepo, "โน้ตมีรูป", listOf("zip_test_photo.jpg"))

            val out = ByteArrayOutputStream()
            srcRepo.exportBackupZip(out)

            val names = mutableListOf<String>()
            ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z ->
                while (true) names += (z.nextEntry ?: break).name
            }
            assertTrue("backup.json" in names)
            assertTrue("images/zip_test_photo.jpg" in names)

            // Simulate a new phone: the image file is gone before restoring.
            img.delete()
            val result = NotesRepository(ctx(), target)
                .importBackupStream(ByteArrayInputStream(out.toByteArray()))
                .getOrThrow()
            assertEquals(1, result.importedNotes)
            assertTrue(img.isFile)
            assertArrayEquals(bytes, img.readBytes())
            val restored = NotesRepository(ctx(), target).allNotesWithTags.first().single().note
            assertEquals(listOf("zip_test_photo.jpg"), restored.imagePaths)
        } finally {
            source.close()
            target.close()
            NoteImageStorage.resolveImageFile(ctx(), "zip_test_photo.jpg").delete()
        }
    }

    @Test
    fun `legacy json backup still imports through the stream entry point`() = runBlocking {
        val source = db()
        val target = db()
        try {
            val srcRepo = NotesRepository(ctx(), source)
            saveNoteWithImages(srcRepo, "โน้ตเก่า", emptyList())
            val json = srcRepo.exportBackupJson()
            val result = NotesRepository(ctx(), target)
                .importBackupStream(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
                .getOrThrow()
            assertEquals(1, result.importedNotes)
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun `zip entries with path traversal are ignored and zip without data is rejected`() = runBlocking {
        val target = db()
        try {
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { z ->
                z.putNextEntry(ZipEntry("images/../../evil.txt"))
                z.write("x".toByteArray())
                z.closeEntry()
            }
            val result = NotesRepository(ctx(), target).importBackupStream(ByteArrayInputStream(out.toByteArray()))
            assertTrue(result.exceptionOrNull() is InvalidBackupFormatException)
            assertFalse(File(ctx().cacheDir, "evil.txt").exists())
            assertFalse(File(ctx().filesDir, "evil.txt").exists())
        } finally {
            target.close()
        }
    }

    @Test
    fun `orphaned image files are purged but referenced and fresh files are kept`() = runBlocking {
        val database = db()
        try {
            val repo = NotesRepository(ctx(), database)
            val now = System.currentTimeMillis()
            val old = now - TimeUnit.DAYS.toMillis(3)
            val used = NoteImageStorage.resolveImageFile(ctx(), "orphan_used.jpg").apply { writeBytes(byteArrayOf(1)); setLastModified(old) }
            val trashedUse = NoteImageStorage.resolveImageFile(ctx(), "orphan_trashed.jpg").apply { writeBytes(byteArrayOf(2)); setLastModified(old) }
            val orphan = NoteImageStorage.resolveImageFile(ctx(), "orphan_old.jpg").apply { writeBytes(byteArrayOf(3)); setLastModified(old) }
            val fresh = NoteImageStorage.resolveImageFile(ctx(), "orphan_fresh.jpg").apply { writeBytes(byteArrayOf(4)) }

            saveNoteWithImages(repo, "ใช้รูป", listOf("orphan_used.jpg"))
            val trashedId = saveNoteWithImages(repo, "อยู่ในถังขยะ", listOf("orphan_trashed.jpg"))
            repo.softDeleteNote(trashedId)

            repo.purgeOrphanedImages(nowMillis = now)

            assertTrue(used.exists())
            assertTrue(trashedUse.exists())
            assertTrue(fresh.exists())
            assertFalse(orphan.exists())
        } finally {
            database.close()
            listOf("orphan_used.jpg", "orphan_trashed.jpg", "orphan_old.jpg", "orphan_fresh.jpg")
                .forEach { NoteImageStorage.resolveImageFile(ctx(), it).delete() }
        }
    }
}
