package com.sek789102.jotwai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sek789102.jotwai.data.local.AppDatabase
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.data.repository.NotesRepository
import com.sek789102.jotwai.data.repository.ReminderSink
import com.sek789102.jotwai.util.NoteImageStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var repo: NotesRepository
    private val scheduled = mutableMapOf<Long, Long?>()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = NotesRepository(ctx, db, object : ReminderSink {
            override fun schedule(note: NoteEntity) { scheduled[note.id] = note.reminderAt }
            override fun cancel(noteId: Long) { scheduled[noteId] = null }
        })
        NoteImageStorage.imagesDir(ctx).listFiles()?.forEach { it.delete() }
    }

    @After fun tearDown() = db.close()

    private suspend fun note(id: Long) = repo.getNote(id)!!.note

    @Test fun `create, edit content, pin, archive, trash, restore, delete forever`() = runBlocking {
        val id = repo.createNote(NoteEntity(title = "ร่าง"))
        repo.saveContent(id, "ซื้อของ", "นม\nไข่", NoteType.TEXT, emptyList(), emptyList())
        assertEquals("ซื้อของ", note(id).title)

        repo.setPinned(listOf(id), true)
        assertTrue(note(id).isPinned)
        repo.setArchived(listOf(id), true)
        assertTrue(note(id).isArchived)
        assertFalse("archiving unpins", note(id).isPinned)

        repo.moveToTrash(listOf(id))
        assertNotNull(note(id).deletedAt)
        repo.restoreFromTrash(listOf(id))
        assertNull(note(id).deletedAt)

        repo.deleteForever(listOf(id))
        assertNull(repo.getNote(id))
    }

    @Test fun `saving content never overwrites a reminder set meanwhile`() = runBlocking {
        val id = repo.createNote(NoteEntity(title = "a"))
        val at = System.currentTimeMillis() + 3_600_000
        repo.setReminder(id, at, RepeatRule.DAILY)
        repo.saveContent(id, "a2", "", NoteType.TEXT, emptyList(), emptyList())
        assertEquals(at, note(id).reminderAt)
        assertEquals(RepeatRule.DAILY, note(id).reminderRepeat)
        assertEquals(at, scheduled[id])
        repo.moveToTrash(listOf(id))
        assertNull("trashing cancels the alarm", scheduled[id])
    }

    @Test fun `one-off reminder clears after firing, repeating one moves forward`() = runBlocking {
        val now = System.currentTimeMillis()
        val once = repo.createNote(NoteEntity(title = "once", reminderAt = now - 1000))
        repo.advanceReminder(once, now)
        assertNull(note(once).reminderAt)

        val daily = repo.createNote(NoteEntity(title = "daily", reminderAt = now - 1000, reminderRepeat = RepeatRule.DAILY))
        repo.advanceReminder(daily, now)
        assertEquals(now - 1000 + TimeUnit.DAYS.toMillis(1), note(daily).reminderAt)
    }

    @Test fun `labels and folders`() = runBlocking {
        val id = repo.createNote(NoteEntity(title = "x"))
        val work = repo.createLabel("งาน")!!
        assertEquals(work, repo.createLabel(" งาน "))
        repo.setNoteLabel(id, work, true)
        assertEquals(listOf("งาน"), repo.getNote(id)!!.labels.map { it.name })
        val other = repo.createLabel("ส่วนตัว")!!
        assertFalse("duplicate name refused", repo.renameLabel(other, "งาน"))
        repo.deleteLabel(work)
        assertTrue(repo.getNote(id)!!.labels.isEmpty())

        val folder = repo.createFolder("บ้าน")!!
        repo.moveToFolder(listOf(id), folder)
        repo.deleteFolder(folder)
        val n = note(id)
        assertNull("deleting a folder keeps its notes", n.folderId)
        assertTrue(repo.folders.first().isEmpty())
    }

    @Test fun `trash older than 30 days is purged`() = runBlocking {
        val old = repo.createNote(NoteEntity(title = "old"))
        val recent = repo.createNote(NoteEntity(title = "recent"))
        repo.moveToTrash(listOf(old, recent))
        db.noteDao().updateNote(note(old).copy(deletedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31)))
        assertEquals(1, repo.purgeTrash())
        assertNull(repo.getNote(old))
        assertNotNull(repo.getNote(recent))
    }

    @Test fun `versions are deduplicated, capped and restorable`() = runBlocking {
        val id = repo.createNote(NoteEntity(title = "v", content = "หนึ่ง"))
        repo.snapshotVersion(note(id))
        repo.snapshotVersion(note(id)) // same content: not stored twice
        assertEquals(1, db.noteDao().getVersions(id).size)

        repo.saveContent(id, "v", "สอง", NoteType.TEXT, emptyList(), emptyList())
        val first = db.noteDao().getVersions(id).single()
        assertTrue(repo.restoreVersion(first.id))
        assertEquals("หนึ่ง", note(id).content)
        assertTrue("restoring keeps the replaced text as a version", db.noteDao().getVersions(id).any { it.content == "สอง" })

        repeat(NotesRepository.MAX_VERSIONS + 5) { i ->
            repo.saveContent(id, "v", "แก้ $i", NoteType.TEXT, emptyList(), emptyList())
            repo.snapshotVersion(note(id))
        }
        assertEquals(NotesRepository.MAX_VERSIONS, db.noteDao().getVersions(id).size)
    }

    @Test fun `blank notes are discarded, others kept`() = runBlocking {
        val blank = repo.createNote(NoteEntity(type = NoteType.CHECKLIST, checklist = listOf(ChecklistItem())))
        assertTrue(repo.deleteIfBlank(blank))
        val kept = repo.createNote(NoteEntity(title = "มีชื่อ"))
        assertFalse(repo.deleteIfBlank(kept))
    }

    @Test fun `duplicate copies text, labels and image files but not the reminder`() = runBlocking {
        NoteImageStorage.resolveImageFile(ctx, "dup.jpg").writeBytes(byteArrayOf(1, 2, 3))
        val label = repo.createLabel("L")!!
        val id = repo.createNote(NoteEntity(title = "t", content = "c", imagePaths = listOf("dup.jpg"), reminderAt = System.currentTimeMillis() + 99999), listOf(label))
        val copy = repo.duplicate(id)!!
        val c = repo.getNote(copy)!!
        assertEquals("c", c.note.content)
        assertNull(c.note.reminderAt)
        assertEquals(listOf("L"), c.labels.map { it.name })
        assertEquals(1, c.note.imagePaths.size)
        assertTrue(c.note.imagePaths[0] != "dup.jpg")
        assertTrue(NoteImageStorage.resolveImageFile(ctx, c.note.imagePaths[0]).isFile)
    }

    @Test fun `orphaned images are purged, used ones kept (also by versions)`() = runBlocking {
        val oldTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        fun file(name: String) = NoteImageStorage.resolveImageFile(ctx, name).apply { writeBytes(byteArrayOf(1)); setLastModified(oldTime) }
        val used = file("used.jpg")
        val inVersion = file("version.jpg")
        val orphan = file("orphan.jpg")
        val fresh = NoteImageStorage.resolveImageFile(ctx, "fresh.jpg").apply { writeBytes(byteArrayOf(1)) }

        val id = repo.createNote(NoteEntity(title = "p", imagePaths = listOf("version.jpg")))
        repo.snapshotVersion(note(id))
        repo.saveContent(id, "p", "", NoteType.TEXT, emptyList(), listOf("used.jpg"))

        repo.purgeOrphanedImages()
        assertTrue(used.exists())
        assertTrue("an old version still shows this photo", inVersion.exists())
        assertTrue("just-added photo is kept", fresh.exists())
        assertFalse(orphan.exists())
    }

    // ---- 1.2.0: folder colours, past reminders, text export/import, list sections ----

    @Test fun `folders keep their colour`() = runBlocking {
        val id = repo.createFolder("เรียน", 1)!!
        assertEquals(1, repo.folders.first().single { it.id == id }.color)
        repo.setFolderColor(repo.folders.first().single { it.id == id }, 3)
        val f = repo.folders.first().single { it.id == id }
        assertEquals(3, f.color)
        assertEquals("เรียน", f.name)
        repo.setFolderColor(f, 9)
        assertEquals(3, repo.folders.first().single { it.id == id }.color)
    }

    @Test fun `clearing past reminders keeps upcoming and repeating ones`() = runBlocking {
        val now = System.currentTimeMillis()
        val past = repo.createNote(NoteEntity(title = "past"))
        val future = repo.createNote(NoteEntity(title = "future"))
        val repeat = repo.createNote(NoteEntity(title = "repeat"))
        repo.setReminder(past, now - 60_000)
        repo.setReminder(future, now + 3_600_000)
        repo.setReminder(repeat, now - 60_000, RepeatRule.DAILY)
        assertEquals(1, repo.clearPastReminders(now))
        assertNull(note(past).reminderAt)
        assertNotNull(note(future).reminderAt)
        assertNotNull(note(repeat).reminderAt)
    }

    @Test fun `notes export to text and text files import as notes`() = runBlocking {
        repo.createNote(NoteEntity(title = "ซื้อของ", content = "⟦c:E53935⟧นม⟧ ไข่".replace("⟧ ไข่", "⟦/c⟧ ไข่")))
        val trashed = repo.createNote(NoteEntity(title = "ทิ้ง"))
        repo.moveToTrash(listOf(trashed))
        val out = java.io.ByteArrayOutputStream()
        assertEquals(1, repo.exportText(out))
        val text = out.toString("UTF-8")
        assertTrue(text.contains("ซื้อของ"))
        assertTrue(text.contains("นม ไข่"))
        assertFalse(text.contains("⟦"))
        assertFalse(text.contains("ทิ้ง"))

        val id = repo.importTextNote("plan.txt", "แผนงาน\nข้อ 1\nข้อ 2")!!
        assertEquals("แผนงาน", note(id).title)
        assertEquals("ข้อ 1\nข้อ 2", note(id).content)
        val one = repo.importTextNote("memo.txt", "บรรทัดเดียว")!!
        assertEquals("memo", note(one).title)
        assertNull(repo.importTextNote("empty.txt", "   "))
    }

    @Test fun `list sections survive encoding`() {
        val all = listOf(
            com.sek789102.jotwai.domain.Section.Notes, com.sek789102.jotwai.domain.Section.Favorites,
            com.sek789102.jotwai.domain.Section.Reminders, com.sek789102.jotwai.domain.Section.Archive,
            com.sek789102.jotwai.domain.Section.Trash, com.sek789102.jotwai.domain.Section.Folder(7),
            com.sek789102.jotwai.domain.Section.Label(9)
        )
        all.forEach { assertEquals(it, com.sek789102.jotwai.domain.Section.decode(it.encode())) }
    }
}
