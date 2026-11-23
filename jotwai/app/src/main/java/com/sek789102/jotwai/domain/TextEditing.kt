package com.sek789102.jotwai.domain

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Plain-text editing helpers for the note body (light Markdown: **bold**, *italic*, ~~strike~~, ==mark==, # heading, - list, 1. list, [ ] box). */
object TextEditing {

    private val bulletPrefix = Regex("""^(\s*)([-*•])\s""")
    private val numberPrefix = Regex("""^(\s*)(\d{1,4})[.)]\s""")
    private val boxPrefix = Regex("""^(\s*)\[( |x|X)\]\s""")
    private val quotePrefix = Regex("""^(\s*)>\s""")
    private val alignTag = Regex("""^⟦al:[a-z]⟧""")

    /**
     * Called on every change. When the user pressed Enter at the end of a list line,
     * continues the list ("- ", "2. ", "[ ] "); Enter on an empty list line ends the list.
     */
    fun continueList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val inserted = new.text.length - old.text.length
        if (inserted != 1 || !new.selection.collapsed) return new
        val cursor = new.selection.start
        if (cursor == 0 || new.text[cursor - 1] != '\n') return new
        if (old.text.substring(0, cursor - 1) != new.text.substring(0, cursor - 1)) return new

        val lineStart = new.text.lastIndexOf('\n', cursor - 2) + 1
        val line = new.text.substring(lineStart, cursor - 1)

        val (prefix, body) = when {
            quotePrefix.containsMatchIn(line) -> {
                val m = quotePrefix.find(line)!!
                "${m.groupValues[1]}> " to line.substring(m.range.last + 1)
            }
            boxPrefix.containsMatchIn(line) -> {
                val m = boxPrefix.find(line)!!
                "${m.groupValues[1]}[ ] " to line.substring(m.range.last + 1)
            }
            numberPrefix.containsMatchIn(line) -> {
                val m = numberPrefix.find(line)!!
                val next = (m.groupValues[2].toIntOrNull() ?: 0) + 1
                val sep = line[m.groups[2]!!.range.last + 1]
                "${m.groupValues[1]}$next$sep " to line.substring(m.range.last + 1)
            }
            bulletPrefix.containsMatchIn(line) -> {
                val m = bulletPrefix.find(line)!!
                "${m.groupValues[1]}${m.groupValues[2]} " to line.substring(m.range.last + 1)
            }
            else -> return new
        }
        return if (body.isBlank()) {
            // Empty item: remove its marker and stop the list.
            val text = new.text.substring(0, lineStart) + new.text.substring(cursor)
            TextFieldValue(text, TextRange(lineStart))
        } else {
            val text = new.text.substring(0, cursor) + prefix + new.text.substring(cursor)
            TextFieldValue(text, TextRange(cursor + prefix.length))
        }
    }

    /** Wraps the selection in [marker] (or removes it when already wrapped). */
    fun toggleWrap(value: TextFieldValue, marker: String): TextFieldValue {
        val s = value.selection.min
        val e = value.selection.max
        val t = value.text
        val before = t.substring(0, s)
        val sel = t.substring(s, e)
        val after = t.substring(e)
        if (before.endsWith(marker) && after.startsWith(marker)) {
            val text = before.dropLast(marker.length) + sel + after.drop(marker.length)
            return TextFieldValue(text, TextRange(s - marker.length, e - marker.length))
        }
        if (sel.length >= marker.length * 2 && sel.startsWith(marker) && sel.endsWith(marker)) {
            val inner = sel.substring(marker.length, sel.length - marker.length)
            return TextFieldValue(before + inner + after, TextRange(s, s + inner.length))
        }
        val text = before + marker + sel + marker + after
        return TextFieldValue(text, TextRange(s + marker.length, e + marker.length))
    }

    /** Adds [prefix] to every selected line, or removes it if all lines already have it. */
    fun toggleLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
        val t = value.text
        val min = value.selection.min
        val start = if (min == 0) 0 else t.lastIndexOf('\n', min - 1) + 1
        var end = t.indexOf('\n', value.selection.max)
        if (end < 0) end = t.length
        val lines = t.substring(start, end).split('\n')
        // Keep a leading alignment tag (⟦al:c⟧) in front of the list/heading marker.
        fun head(line: String) = alignTag.find(line)?.value ?: ""
        val allHave = lines.all { it.removePrefix(head(it)).startsWith(prefix) }
        val changed = lines.joinToString("\n") { line ->
            val h = head(line)
            val rest = line.substring(h.length)
            h + if (allHave) rest.removePrefix(prefix) else prefix + stripKnownPrefix(rest)
        }
        val text = t.substring(0, start) + changed + t.substring(end)
        val selection = if (value.selection.collapsed && lines.size == 1) {
            TextRange((value.selection.start + changed.length - (end - start)).coerceIn(start, start + changed.length))
        } else {
            TextRange(start, start + changed.length)
        }
        return TextFieldValue(text, selection)
    }

    private fun stripKnownPrefix(line: String): String {
        listOf(boxPrefix, numberPrefix, bulletPrefix, quotePrefix, Regex("""^#{1,3}\s""")).forEach { r ->
            r.find(line)?.let { m -> if (m.range.first == 0) return line.substring(m.range.last + 1) }
        }
        return line
    }

    /** Text notes can have "[ ] item" lines; tapping one in read mode ticks it. */
    fun toggleBoxAtLine(text: String, lineIndex: Int): String {
        val lines = text.split('\n').toMutableList()
        if (lineIndex !in lines.indices) return text
        val m = boxPrefix.find(lines[lineIndex]) ?: return text
        val checked = m.groupValues[2].isNotBlank()
        val replacement = "${m.groupValues[1]}[${if (checked) " " else "x"}] "
        lines[lineIndex] = replacement + lines[lineIndex].substring(m.range.last + 1)
        return lines.joinToString("\n")
    }

    fun wordCount(text: String): Int {
        // Thai has no spaces between words, so count characters for it and words for the rest.
        return text.split(Regex("""\s+""")).count { it.isNotBlank() }
    }

    /** Strips Markdown markers for previews, sharing and the widget. */
    fun toPlainText(text: String): String = RichText.stripTags(text).lineSequence().joinToString("\n") { line ->
        var l = line
        if (l.trim() == "---") return@joinToString "──────────"
        quotePrefix.find(l)?.let { m -> l = m.groupValues[1] + "│ " + l.substring(m.range.last + 1) }
        boxPrefix.find(l)?.let { m -> l = (if (m.groupValues[2].isBlank()) "☐ " else "☑ ") + l.substring(m.range.last + 1) }
        Regex("""^#{1,3}\s""").find(l)?.let { l = l.substring(it.range.last + 1) }
        l.replace("**", "").replace("~~", "").replace("==", "").replace("`", "")
    }
}
