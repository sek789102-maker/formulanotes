package com.sek789102.jotwai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sek789102.jotwai.data.local.AppDatabase
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.repository.InvalidBackupException
import com.sek789102.jotwai.data.repository.NotesRepository
import com.sek789102.jotwai.util.NoteImageStorage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private fun db() = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()

    @Before fun setUp() {
        source = db()
        target = db()
        NoteImageStorage.imagesDir(ctx).listFiles()?.forEach { it.delete() }
    }

    @After fun tearDown() {
        source.close()
        target.close()
    }

    @Test fun `zip backup round-trips notes, checklist, folders, labels and photos`() = runBlocking {
        val src = NotesRepository(ctx, source)
        val photo = ByteArray(200_000) { (it % 251).toByte() }
        NoteImageStorage.resolveImageFile(ctx, "p1.jpg").writeBytes(photo)
        val folder = src.createFolder("งาน")!!
        val label = src.createLabel("ด่วน")!!
        val a = src.createNote(NoteEntity(title = "ประชุม", content = "**สรุป**", imagePaths = listOf("p1.jpg"), color = 3), listOf(label))
        src.moveToFolder(listOf(a), folder)
        val b = src.createNote(NoteEntity(title = "ซื้อของ", type = NoteType.CHECKLIST, checklist = listOf(ChecklistItem(text = "นม", checked = true), ChecklistItem(text = "ไข่"))))
        src.setArchived(listOf(b), true)

        val out = ByteArrayOutputStream()
        src.exportBackup(out)
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z -> while (true) names += (z.nextEntry ?: break).name }
        assertTrue("backup.json" in names)
        assertTrue("images/p1.jpg" in names)

        NoteImageStorage.resolveImageFile(ctx, "p1.jpg").delete() // as on a new phone
        val dst = NotesRepository(ctx, target)
        val r = dst.importBackup(ByteArrayInputStream(out.toByteArray())).getOrThrow()
        assertEquals(2, r.imported)

        val all = dst.getAllNotes().associateBy { it.note.title }
        val meeting = all.getValue("ประชุม")
        assertEquals("**สรุป**", meeting.note.content)
        assertEquals(3, meeting.note.color)
        assertEquals(listOf("ด่วน"), meeting.labels.map { it.name })
        assertEquals("งาน", target.noteDao().getFolders().single { it.id == meeting.note.folderId }.name)
        assertArrayEquals(photo, NoteImageStorage.resolveImageFile(ctx, "p1.jpg").readBytes())
        val shop = all.getValue("ซื้อของ").note
        assertTrue(shop.isArchived)
        assertEquals(listOf("นม" to true, "ไข่" to false), shop.checklist.map { it.text to it.checked })

        val again = dst.importBackup(ByteArrayInputStream(out.toByteArray())).getOrThrow()
        assertEquals(0, again.imported)
        assertEquals(2, again.skipped)
    }

    @Test fun `old FormulaNotes backup imports with formulas, tags and subjects kept`() = runBlocking {
        val legacy = """
            {"app":"SmutNoteFormula","version":3,
             "subjects":[{"id":"PHYSICS","name":"ฟิสิกส์"}],
             "notes":[{"title":"พลังงาน","subject":"PHYSICS","content":"บทที่ 4","createdAt":1700000000000,
                       "formulas":[{"title":"Ek","expression":"E = ½mv²","variablesNote":"m มวล"}],
                       "tags":[{"name":"สอบ","colorIndex":1}],"imagePaths":[]}]}
        """.trimIndent()
        val dst = NotesRepository(ctx, target)
        assertEquals(1, dst.importBackup(ByteArrayInputStream(legacy.toByteArray())).getOrThrow().imported)
        val n = dst.getAllNotes().single()
        assertTrue(n.note.content.startsWith("บทที่ 4"))
        assertTrue(n.note.content.contains("- Ek: E = ½mv² (m มวล)"))
        assertEquals(listOf("สอบ"), n.labels.map { it.name })
        assertEquals("ฟิสิกส์", target.noteDao().getFolders().single().name)
    }

    @Test fun `bad files are rejected and zip paths cannot escape`() = runBlocking {
        val dst = NotesRepository(ctx, target)
        assertTrue(dst.importBackup(ByteArrayInputStream("not json".toByteArray())).exceptionOrNull() is InvalidBackupException)

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("images/../../evil.txt")); z.write(1); z.closeEntry()
        }
        assertTrue(dst.importBackup(ByteArrayInputStream(out.toByteArray())).exceptionOrNull() is InvalidBackupException)
        assertFalse(File(ctx.cacheDir, "evil.txt").exists())
        assertFalse(File(ctx.filesDir, "evil.txt").exists())
        assertTrue(dst.getAllNotes().isEmpty())
    }
}
