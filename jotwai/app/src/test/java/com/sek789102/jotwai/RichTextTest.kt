package com.sek789102.jotwai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.sek789102.jotwai.domain.RichText
import com.sek789102.jotwai.domain.RichText.Kind
import com.sek789102.jotwai.domain.TextEditing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {
    private val pal = RichText.Palette(Color.Blue, Color.Yellow, Color.Gray, Color.LightGray, Color.Gray, Color.Gray)

    @Test fun markersAreHiddenAndCursorMapsInsideClosingMarker() {
        val p = RichText.parse("**หนา** ปกติ")
        assertEquals("หนา ปกติ", p.display)
        assertEquals(0, p.d2o[0])          // before the opening marker
        assertEquals(5, p.d2o[3])          // end of the bold word stays inside the bold run
        assertEquals(3, p.o2d[5])
        assertEquals(p.display.length, p.o2d[p.text.length])
    }

    @Test fun allMarkdownAndTagsAreHidden() {
        val text = "# หัวข้อ\n- จุด\n[x] เสร็จ\n> อ้าง\n---\n⟦al:c⟧⟦c:E53935⟧แดง⟦/c⟧ ⟦z:2⟧ใหญ่⟦/z⟧ ⟦u⟧ขีด⟦/u⟧ ~~ฆ่า~~ ==มาร์ค== `code` *เอียง*"
        val p = RichText.parse(text)
        assertFalse(p.display.contains("⟦"))
        assertFalse(p.display.contains("**"))
        assertTrue(p.display.replace("\u200B", "\n").startsWith("หัวข้อ\n• จุด\n☑ เสร็จ\n┃ อ้าง\n───"))
        assertTrue(p.display.endsWith("แดง ใหญ่ ขีด ฆ่า มาร์ค code เอียง"))
        val kinds = p.spans.map { it.kind }.toSet()
        listOf(Kind.H1, Kind.DONE, Kind.QUOTE, Kind.RULE, Kind.COLOR, Kind.SIZE, Kind.UNDERLINE, Kind.STRIKE, Kind.MARK, Kind.CODE, Kind.ITALIC)
            .forEach { assertTrue("missing $it", it in kinds) }
        val a = RichText.annotate(p, pal)
        assertEquals(p.display, a.text)
        assertTrue(a.paragraphStyles.any { (it.item as ParagraphStyle).textAlign == TextAlign.Center })
    }

    @Test fun mapsAreMonotonicAndInRange() {
        val text = "⟦al:r⟧## ⟦c:112233⟧a**b**⟦/c⟧\n[ ] c\n---\n- d *e*"
        val p = RichText.parse(text)
        for (i in 1 until p.o2d.size) assertTrue(p.o2d[i] >= p.o2d[i - 1])
        p.o2d.forEach { assertTrue(it in 0..p.display.length) }
        p.d2o.forEach { assertTrue(it in 0..text.length) }
        assertEquals(p.display.length + 1, p.d2o.size)
    }

    @Test fun headingCursorStartsAfterHiddenPrefix() {
        val p = RichText.parse("# หัว")
        assertEquals(2, p.d2o[0])
    }

    @Test fun backspaceOnHiddenMarkerDeletesVisibleText() {
        val old = TextFieldValue("**ab**", TextRange(6))
        val new = TextFieldValue("**ab*", TextRange(5))
        val fixed = RichText.fixDeletion(old, new)
        assertEquals("**a**", fixed.text)
        assertEquals(TextRange(3), fixed.selection)
    }

    @Test fun backspaceAtHeadingStartRemovesHeading() {
        val old = TextFieldValue("# หัว", TextRange(2))
        val fixed = RichText.fixDeletion(old, TextFieldValue("#หัว", TextRange(1)))
        assertEquals("หัว", fixed.text)
        assertEquals(TextRange(0), fixed.selection)
    }

    @Test fun backspaceAfterBoxRemovesBox() {
        val old = TextFieldValue("[ ] นม", TextRange(4))
        val fixed = RichText.fixDeletion(old, TextFieldValue("[ ]นม", TextRange(3)))
        assertEquals("นม", fixed.text)
    }

    @Test fun normalDeletionIsUntouched() {
        val old = TextFieldValue("**abc**", TextRange(4))
        val new = TextFieldValue("**ac**", TextRange(3))
        assertEquals(new, RichText.fixDeletion(old, new))
    }

    @Test fun colourTagAppliesReplacesAndRemoves() {
        val v = TextFieldValue("สวัสดีครับ", TextRange(0, 6))
        val red = RichText.applyTag(v, "c", "E53935")
        assertEquals("⟦c:E53935⟧สวัสดี⟦/c⟧ครับ", red.text)
        assertEquals("สวัสดี", red.text.substring(red.selection.min, red.selection.max))
        val blue = RichText.applyTag(red, "c", "1E88E5")
        assertEquals("⟦c:1E88E5⟧สวัสดี⟦/c⟧ครับ", blue.text)
        assertEquals("สวัสดีครับ", RichText.applyTag(blue, "c", "1E88E5").text)
        assertEquals("สวัสดีครับ", RichText.applyTag(blue, "c", null).text)
    }

    @Test fun boldToggleWorksWhenSelectionIncludesHiddenMarkers() {
        val v = TextFieldValue("**หนา**", TextRange(0, 5)) // what a drag-select may produce
        assertEquals("หนา", RichText.toggleMarker(v, "**").text)
    }

    @Test fun boldButtonLeavesTheRunAtItsEnd() {
        val v = TextFieldValue("**หนา**", TextRange(5))
        val out = RichText.toggleMarker(v, "**")
        assertEquals("**หนา**", out.text)
        assertEquals(TextRange(7), out.selection)
    }

    @Test fun clearFormattingRemovesAllMarkersAroundSelection() {
        val v = TextFieldValue("**⟦c:E53935⟧แดง⟦/c⟧** ปกติ", TextRange(14, 17))
        assertEquals("แดง ปกติ", RichText.clearFormatting(v).text)
    }

    @Test fun alignmentAndListsCombine() {
        val centered = RichText.setAlign(TextFieldValue("รายการ", TextRange(0)), TextAlign.Center)
        assertEquals("⟦al:c⟧รายการ", centered.text)
        val bullet = TextEditing.toggleLinePrefix(centered, "- ")
        assertEquals("⟦al:c⟧- รายการ", bullet.text)
        assertEquals("รายการ", RichText.setAlign(TextEditing.toggleLinePrefix(bullet, "- "), TextAlign.Start).text)
    }

    @Test fun indentAndDivider() {
        assertEquals("    ก", RichText.indent(TextFieldValue("ก"), 1).text)
        assertEquals("ก", RichText.indent(TextFieldValue("    ก"), -1).text)
        assertEquals("ก\n---\n", RichText.insertDivider(TextFieldValue("ก", TextRange(1))).text)
    }

    @Test fun plainTextStripsEverything() {
        assertEquals(
            "แดง ใหญ่\n│ อ้าง\n──────────\nโค้ด",
            TextEditing.toPlainText("⟦c:E53935⟧แดง⟦/c⟧ ⟦z:2⟧ใหญ่⟦/z⟧\n> อ้าง\n---\n`โค้ด`")
        )
    }

    @Test fun activeKindsAtCursor() {
        val v = TextFieldValue("**ab** ⟦c:E53935⟧cd⟦/c⟧", TextRange(3))
        assertTrue(Kind.BOLD in RichText.activeKinds(v))
        val c = RichText.activeKinds(v.copy(selection = TextRange(19)))
        assertEquals("E53935", c[Kind.COLOR])
    }

    // ---- keyboard edits must never break hidden codes (bug from phone testing) ----

    private fun typedInside(old: String, at: Int, typed: String): TextFieldValue {
        val o = TextFieldValue(old, TextRange(old.length))
        val n = TextFieldValue(old.substring(0, at) + typed + old.substring(at), TextRange(at + typed.length))
        return RichText.fixEdit(o, n)
    }

    @Test fun typingInsideASizeTagGoesAfterIt() {
        val old = "⟦z:1.25⟧⟦/z⟧"
        val out = typedInside(old, 6, "ต") // where the phone put it: "⟦z:1.2" + "ต" + "5⟧"
        assertEquals("⟦z:1.25⟧ต⟦/z⟧", out.text)
        assertEquals("ต", RichText.parse(out.text).display)
        assertEquals(TextRange(9), out.selection)
    }

    @Test fun keyboardWordReplacementInsideColourTagIsMovedOut() {
        val old = "⟦c:00ACC1⟧⟦/c⟧"
        val out = typedInside(old, 8, "ofkr")
        assertEquals("⟦c:00ACC1⟧ofkr⟦/c⟧", out.text)
        assertEquals("ofkr", RichText.parse(out.text).display)
    }

    @Test fun typingInsideAFontTagGoesAfterIt() {
        val out = typedInside("⟦f:serif⟧⟦/f⟧", 7, "นย")
        assertEquals("⟦f:serif⟧นย⟦/f⟧", out.text)
    }

    @Test fun typingInsideClosingTagOrBoldMarkerIsMovedOut() {
        assertEquals("⟦u⟧ab⟦/u⟧c", typedInside("⟦u⟧ab⟦/u⟧", 7, "c").text)
        assertEquals("**ab**c", typedInside("**ab**", 5, "c").text)
    }

    @Test fun normalTypingIsUntouched() {
        val o = TextFieldValue("⟦c:E53935⟧แดง⟦/c⟧", TextRange(13))
        val n = TextFieldValue("⟦c:E53935⟧แดงมาก⟦/c⟧", TextRange(16))
        assertTrue(RichText.fixEdit(o, n) === n)
    }

    @Test fun deletionThatCutsIntoATagKeepsTheTag() {
        val old = "x⟦c:E53935⟧แดง⟦/c⟧"
        val o = TextFieldValue(old, TextRange(0, 6))
        val n = TextFieldValue(old.substring(6), TextRange(0))
        val out = RichText.fixEdit(o, n)
        assertEquals("⟦c:E53935⟧แดง⟦/c⟧", out.text)
        assertEquals("แดง", RichText.parse(out.text).display)
    }

    @Test fun brokenTagsFromOlderVersionAreRepaired() {
        assertEquals("⟦z:1.25⟧ต⟦/z⟧", RichText.repairTags("⟦z:1.2ต5⟧⟦/z⟧"))
        assertEquals("⟦c:00ACC1⟧ofkr⟦/c⟧", RichText.repairTags("⟦c:00ACCofkr1⟧⟦/c⟧"))
        assertEquals("⟦f:serif⟧นย⟦/f⟧", RichText.repairTags("⟦f:seriนยf⟧⟦/f⟧"))
        val ok = "⟦al:c⟧⟦c:E53935⟧แดง⟦/c⟧ ⟦z:2⟧ใหญ่⟦/z⟧ ⟦u⟧ขีด⟦/u⟧"
        assertEquals(ok, RichText.repairTags(ok))
        assertFalse(RichText.parse(RichText.repairTags("⟦z:1.2ต5⟧⟦/z⟧")).display.contains("⟦"))
        assertEquals("ofkr", RichText.stripTags("⟦c:00ACCofkr1⟧⟦/c⟧"))
    }

    // ---- style buttons with no selection ----

    @Test fun sizeButtonsAtABareCursorChangeTheNextTextAndDoNotNest() {
        var v = TextFieldValue("ab ", TextRange(3))
        v = RichText.applyTag(v, "z", "1.15")
        assertEquals("ab ⟦z:1.15⟧⟦/z⟧", v.text)
        assertEquals("1.15", RichText.activeKinds(v)[Kind.SIZE])
        v = RichText.applyTag(v, "z", "1.323")                 // press A+ again
        assertEquals("ab ⟦z:1.323⟧⟦/z⟧", v.text)
        assertEquals("1.323", RichText.activeKinds(v)[Kind.SIZE])
        v = RichText.applyTag(v, "z", null)                     // back to normal size
        assertEquals("ab ", v.text)
    }

    @Test fun styleInsideAWordAppliesToTheWord() {
        val v = RichText.applyTag(TextFieldValue("hello world", TextRange(2)), "c", "E53935")
        assertEquals("⟦c:E53935⟧hello⟦/c⟧ world", v.text)
        assertEquals("hello world", RichText.parse(v.text).display)
    }

    @Test fun styleInsideARunChangesTheRun() {
        val t = "⟦c:E53935⟧hello⟦/c⟧"
        val blue = RichText.applyTag(TextFieldValue(t, TextRange(13)), "c", "1E88E5")
        assertEquals("⟦c:1E88E5⟧hello⟦/c⟧", blue.text)
        assertEquals("hello", RichText.applyTag(TextFieldValue(t, TextRange(13)), "c", null).text)
        val big = RichText.applyTag(TextFieldValue("⟦z:2⟧ใหญ่⟦/z⟧", TextRange(7)), "z", "3")
        assertEquals("⟦z:3⟧ใหญ่⟦/z⟧", big.text)
    }

    @Test fun newStyleAtTheEndOfARunStartsAfterIt() {
        val t = "⟦c:E53935⟧hello⟦/c⟧"
        val v = RichText.applyTag(TextFieldValue(t, TextRange(15)), "c", "1E88E5")
        assertEquals("⟦c:E53935⟧hello⟦/c⟧⟦c:1E88E5⟧⟦/c⟧", v.text)
        assertEquals(TextRange(t.length + 10), v.selection)
        val leave = RichText.applyTag(TextFieldValue(t, TextRange(15)), "c", "E53935")
        assertEquals(t, leave.text)
        assertEquals(TextRange(t.length), leave.selection)
    }

    @Test fun defaultAtAPlainCursorDoesNothing() {
        val v = TextFieldValue("ab", TextRange(2))
        assertEquals(v, RichText.applyTag(v, "z", null))
        assertEquals(v, RichText.applyTag(v, "f", null))
    }
}
