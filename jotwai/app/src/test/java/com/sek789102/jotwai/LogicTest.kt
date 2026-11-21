package com.sek789102.jotwai

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.data.model.SortOrder
import com.sek789102.jotwai.data.repository.ReminderMath
import com.sek789102.jotwai.domain.NoteQuery
import com.sek789102.jotwai.domain.SearchFilter
import com.sek789102.jotwai.domain.Section
import com.sek789102.jotwai.domain.TextEditing
import com.sek789102.jotwai.domain.UndoHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LogicTest {

    private fun typeEnter(text: String): TextFieldValue {
        val old = TextFieldValue(text, TextRange(text.length))
        val new = TextFieldValue("$text\n", TextRange(text.length + 1))
        return TextEditing.continueList(old, new)
    }

    @Test fun `enter continues bullet, number and checkbox lists`() {
        assertEquals("- นม\n- ", typeEnter("- นม").text)
        assertEquals("1. ไข่\n2. ", typeEnter("1. ไข่").text)
        assertEquals("[x] ขนมปัง\n[ ] ", typeEnter("[x] ขนมปัง").text)
        assertEquals("  - ย่อย\n  - ", typeEnter("  - ย่อย").text)
        assertEquals("ข้อความปกติ\n", typeEnter("ข้อความปกติ").text)
    }

    @Test fun `enter on an empty list item ends the list`() {
        val r = typeEnter("- นม\n- ")
        assertEquals("- นม\n", r.text)
        assertEquals(r.text.length, r.selection.start)
    }

    @Test fun `bold wraps and unwraps the selection`() {
        val v = TextFieldValue("สวัสดีครับ", TextRange(0, 6))
        val b = TextEditing.toggleWrap(v, "**")
        assertEquals("**สวัสดี**ครับ", b.text)
        assertEquals(TextRange(2, 8), b.selection)
        assertEquals("สวัสดีครับ", TextEditing.toggleWrap(b, "**").text)
    }

    @Test fun `line prefix toggles on selected lines and replaces other list markers`() {
        val v = TextFieldValue("หนึ่ง\nสอง", TextRange(0, 9))
        val bullets = TextEditing.toggleLinePrefix(v, "- ")
        assertEquals("- หนึ่ง\n- สอง", bullets.text)
        assertEquals("หนึ่ง\nสอง", TextEditing.toggleLinePrefix(bullets, "- ").text)
        val boxes = TextEditing.toggleLinePrefix(TextFieldValue("- นม", TextRange(2)), "[ ] ")
        assertEquals("[ ] นม", boxes.text)
    }

    @Test fun `checkbox line toggles and plain text strips markers`() {
        assertEquals("a\n[x] b", TextEditing.toggleBoxAtLine("a\n[ ] b", 1))
        assertEquals("☑ ทำแล้ว\nหัวข้อ\nหนา", TextEditing.toPlainText("[x] ทำแล้ว\n# หัวข้อ\n**หนา**"))
    }

    @Test fun `undo groups fast typing and redo restores`() {
        val h = UndoHistory("", groupWindowMillis = 500)
        h.record("ก", now = 1000)
        h.record("กข", now = 1100) // grouped with previous step
        h.record("กขค", now = 2000) // new step
        assertEquals("กข", h.undo())
        assertEquals("", h.undo())
        assertNull(h.undo())
        assertEquals("กข", h.redo())
        h.record("ใหม่", now = 5000)
        assertFalse(h.canRedo)
    }

    private fun n(
        id: Long,
        title: String = "",
        content: String = "",
        labels: List<String> = emptyList(),
        locked: Boolean = false,
        pinned: Boolean = false,
        archived: Boolean = false,
        trashed: Boolean = false,
        checklist: List<ChecklistItem> = emptyList(),
        updated: Long = id
    ) = NoteWithLabels(
        NoteEntity(
            id = id, title = title, content = content, isLocked = locked, isPinned = pinned, isArchived = archived,
            deletedAt = if (trashed) 1L else null, checklist = checklist,
            type = if (checklist.isEmpty()) NoteType.TEXT else NoteType.CHECKLIST, updatedAt = updated, createdAt = id
        ),
        labels.mapIndexed { i, l -> LabelEntity(i + 1L, l) }
    )

    @Test fun `search looks in body, checklist items and labels, all words must match`() {
        val shop = n(1, title = "ไปตลาด", checklist = listOf(ChecklistItem(text = "ซื้อนมสด")))
        val work = n(2, content = "ประชุม Budget ปีหน้า", labels = listOf("งาน"))
        assertTrue(NoteQuery.matches(shop, SearchFilter("นม")))
        assertTrue(NoteQuery.matches(work, SearchFilter("budget")))
        assertTrue(NoteQuery.matches(work, SearchFilter("งาน ประชุม")))
        assertFalse(NoteQuery.matches(work, SearchFilter("งาน ตลาด")))
    }

    @Test fun `locked notes are found by title only`() {
        val secret = n(3, title = "บัญชีธนาคาร", content = "รหัส 1234", locked = true)
        assertTrue(NoteQuery.matches(secret, SearchFilter("บัญชี")))
        assertFalse(NoteQuery.matches(secret, SearchFilter("1234")))
    }

    @Test fun `sections and pinned-first sorting`() {
        val list = listOf(n(1, "a", updated = 10), n(2, "b", pinned = true, updated = 5), n(3, "c", archived = true), n(4, "d", trashed = true))
        val notes = list.filter { NoteQuery.inSection(it, Section.Notes) }
        assertEquals(listOf(2L, 1L), NoteQuery.sort(notes, SortOrder.UPDATED, Section.Notes).map { it.note.id })
        assertEquals(listOf(3L), list.filter { NoteQuery.inSection(it, Section.Archive) }.map { it.note.id })
        assertEquals(listOf(4L), list.filter { NoteQuery.inSection(it, Section.Trash) }.map { it.note.id })
    }

    @Test fun `repeating reminders move to the next future time`() {
        val base = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 10, 8, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val now = base + 1000
        val daily = ReminderMath.nextOccurrence(base, RepeatRule.DAILY, now)!!
        assertEquals(base + 24 * 3600 * 1000L, daily)
        val weekly = ReminderMath.nextOccurrence(base, RepeatRule.WEEKLY, now + 10L * 24 * 3600 * 1000)!!
        assertEquals(base + 14L * 24 * 3600 * 1000, weekly)
        assertNull(ReminderMath.nextOccurrence(base, RepeatRule.NONE, now))
    }
}
