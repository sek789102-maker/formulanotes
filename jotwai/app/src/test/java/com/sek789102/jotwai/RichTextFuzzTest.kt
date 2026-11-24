package com.sek789102.jotwai

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.sek789102.jotwai.domain.RichText
import com.sek789102.jotwai.domain.TextEditing
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Random typing, deleting, keyboard word replacement, pasting and toolbar presses, run through the
 * same steps as the editor. After every step the note must show no code characters (⟦ ⟧).
 */
class RichTextFuzzTest {

    /** What [com.sek789102.jotwai.ui.viewmodel.EditorViewModel.onBodyChange] does with a keyboard edit. */
    private fun edit(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val afterDelete = RichText.fixDeletion(old, new)
        val safe = if (afterDelete === new) RichText.fixEdit(old, new) else afterDelete
        return RichText.sanitize(TextEditing.continueList(old, safe))
    }

    private fun format(v: TextFieldValue, f: (TextFieldValue) -> TextFieldValue) = RichText.sanitize(f(v))

    private val words = listOf("ก", "ปกติ", "สวัสดี", "a", "hello", " ", "\n", "- ", "1. ", "[ ] ", "> ", "# ", "**", "*", "~~", "==", "`", "ไม้", "่", "ำ", "5", "---")
    private val tags = listOf(
        "c" to "E53935", "c" to "00ACC1", "c" to null, "h" to "FFEB3B", "h" to null, "z" to "1.25", "z" to "0.625", "z" to null,
        "f" to "serif", "f" to "hand", "f" to null, "u" to "", "sup" to "", "sub" to ""
    )

    private fun checkClean(v: TextFieldValue, step: String) {
        val t = v.text
        assertTrue("selection out of range at $step: $t ${v.selection}", v.selection.start in 0..t.length && v.selection.end in 0..t.length)
        val d = RichText.parse(t).display
        assertFalse("code shown at $step:\nraw=$t\ndisplay=$d", d.contains('⟦') || d.contains('⟧'))
        // Every displayed cursor position must map back into the text.
        val p = RichText.parse(t)
        for (i in 0..p.display.length) assertTrue(p.d2o[i] in 0..t.length)
        for (i in 0..t.length) assertTrue(p.o2d[i] in 0..p.display.length)
    }

    private fun runFuzz(seed: Int, steps: Int) {
        val r = Random(seed)
        var v = TextFieldValue("")
        var clipboard = ""
        repeat(steps) { step ->
            val t = v.text
            val c = v.selection.start.coerceIn(0, t.length)
            val op = r.nextInt(100)
            val label = "seed=$seed step=$step op=$op"
            v = when {
                op < 30 -> { // type at the cursor (or at a random raw position, like a keyboard out of sync)
                    val at = if (r.nextInt(4) == 0) r.nextInt(t.length + 1) else c
                    val w = words[r.nextInt(words.size)]
                    edit(v, TextFieldValue(t.substring(0, at) + w + t.substring(at), TextRange(at + w.length)))
                }
                op < 45 && t.isNotEmpty() -> { // backspace
                    val at = if (r.nextInt(4) == 0) r.nextInt(t.length) + 1 else c
                    if (at == 0) v else edit(v.copy(selection = TextRange(at)), TextFieldValue(t.substring(0, at - 1) + t.substring(at), TextRange(at - 1)))
                }
                op < 55 && t.isNotEmpty() -> { // keyboard replaces a raw range (word suggestion / autocorrect)
                    val s = r.nextInt(t.length + 1)
                    val e = (s + r.nextInt(8)).coerceAtMost(t.length)
                    val w = words[r.nextInt(words.size)]
                    edit(v, TextFieldValue(t.substring(0, s) + w + t.substring(e), TextRange(s + w.length)))
                }
                op < 62 && t.isNotEmpty() -> { // copy a raw range (may cut a code in half)
                    val s = r.nextInt(t.length + 1)
                    val e = (s + r.nextInt(12)).coerceAtMost(t.length)
                    clipboard = t.substring(s, e)
                    v
                }
                op < 70 -> { // paste
                    edit(v, TextFieldValue(t.substring(0, c) + clipboard + t.substring(c), TextRange(c + clipboard.length)))
                }
                op < 76 && t.isNotEmpty() -> { // select (cut) and delete a range
                    val s = r.nextInt(t.length + 1)
                    val e = (s + r.nextInt(10)).coerceAtMost(t.length)
                    edit(v.copy(selection = TextRange(s, e)), TextFieldValue(t.substring(0, s) + t.substring(e), TextRange(s)))
                }
                op < 88 -> { // toolbar: a tag, on a selection or at the cursor
                    val (name, arg) = tags[r.nextInt(tags.size)]
                    val sel = if (r.nextBoolean() && t.isNotEmpty()) {
                        val s = r.nextInt(t.length + 1); TextRange(s, (s + r.nextInt(6)).coerceAtMost(t.length))
                    } else TextRange(c)
                    format(v.copy(selection = sel)) { RichText.applyTag(it, name, arg) }
                }
                op < 94 -> { // toolbar: Markdown marker
                    val m = listOf("**", "*", "~~", "==", "`")[r.nextInt(5)]
                    format(v) { RichText.toggleMarker(it, m) }
                }
                op < 97 -> format(v) { RichText.setAlign(it, listOf(androidx.compose.ui.text.style.TextAlign.Center, androidx.compose.ui.text.style.TextAlign.End, androidx.compose.ui.text.style.TextAlign.Start)[r.nextInt(3)]) }
                else -> format(v) { RichText.clearFormatting(it) }
            }
            if (v.text.length > 400) v = RichText.sanitize(TextFieldValue(v.text.take(200), TextRange(0)))
            checkClean(v, label)
        }
    }

    @Test fun randomEditingNeverShowsCodes() {
        for (seed in 1..60) runFuzz(seed, 400)
    }

    @Test fun brokenStoredNotesOpenClean() {
        val r = Random(7)
        val alphabet = "⟦⟧/:czfhualrsupbi0123456789ABCDEF.ปกติ \n*#-"
        repeat(3000) {
            val raw = String(CharArray(r.nextInt(40)) { alphabet[r.nextInt(alphabet.length)] })
            val repaired = RichText.repairTags(raw)
            val d = RichText.parse(repaired).display
            assertFalse("raw=$raw repaired=$repaired display=$d", d.contains('⟦') || d.contains('⟧'))
            assertFalse(RichText.stripTags(raw).contains('⟦') || RichText.stripTags(raw).contains('⟧'))
        }
    }
}
