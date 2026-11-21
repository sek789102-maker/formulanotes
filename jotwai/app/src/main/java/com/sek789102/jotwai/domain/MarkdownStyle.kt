package com.sek789102.jotwai.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

/**
 * Styles Markdown while typing without changing the text: **bold**, *italic*, ~~strike~~,
 * ==highlight==, headings, list markers and ticked [x] lines. Markers stay visible but faded,
 * so the cursor always matches what is stored.
 */
class MarkdownVisualTransformation(
    private val markerColor: Color,
    private val highlightColor: Color,
    private val doneColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(MarkdownStyle.style(text.text, markerColor, highlightColor, doneColor), OffsetMapping.Identity)
}

object MarkdownStyle {
    private val inline = listOf(
        Triple("**", SpanStyle(fontWeight = FontWeight.Bold), 2),
        Triple("~~", SpanStyle(textDecoration = TextDecoration.LineThrough), 2),
        Triple("==", SpanStyle(), 2)
    )

    fun style(text: String, marker: Color, highlight: Color, done: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        val markerStyle = SpanStyle(color = marker)
        var lineStart = 0
        for (line in text.split('\n')) {
            val end = lineStart + line.length
            Regex("""^(#{1,3})\s""").find(line)?.let { m ->
                val size = when (m.groupValues[1].length) { 1 -> 1.45.em; 2 -> 1.25.em; else -> 1.1.em }
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), lineStart, end)
                addStyle(markerStyle, lineStart, lineStart + m.value.length)
            }
            Regex("""^\s*([-*•]|\d{1,4}[.)])\s""").find(line)?.let { m ->
                addStyle(SpanStyle(color = marker, fontWeight = FontWeight.Bold), lineStart + m.groups[1]!!.range.first, lineStart + m.groups[1]!!.range.last + 1)
            }
            Regex("""^\s*\[( |x|X)\]\s""").find(line)?.let { m ->
                addStyle(SpanStyle(color = marker, fontWeight = FontWeight.Bold), lineStart, lineStart + m.value.length)
                if (m.groupValues[1].isNotBlank()) {
                    addStyle(SpanStyle(color = done, textDecoration = TextDecoration.LineThrough), lineStart + m.value.length, end)
                }
            }
            lineStart = end + 1
        }
        for ((token, style, len) in inline) {
            var from = 0
            while (true) {
                val s = text.indexOf(token, from)
                if (s < 0) break
                val e = text.indexOf(token, s + len)
                if (e < 0 || text.substring(s + len, e).contains('\n') || e == s + len) {
                    from = s + len
                    continue
                }
                val st = if (token == "==") SpanStyle(background = highlight) else style
                addStyle(st, s + len, e)
                addStyle(markerStyle, s, s + len)
                addStyle(markerStyle, e, e + len)
                from = e + len
            }
        }
        // *italic* (single asterisks not part of **)
        Regex("""(?<![*\w])\*(?!\*)([^*\n]+?)\*(?!\*)""").findAll(text).forEach { m ->
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), m.range.first + 1, m.range.last)
            addStyle(markerStyle, m.range.first, m.range.first + 1)
            addStyle(markerStyle, m.range.last, m.range.last + 1)
        }
    }
}
