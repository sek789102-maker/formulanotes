package com.sek789102.formulabook.domain.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Checklist(val lineIndex: Int, val checked: Boolean, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

object SimpleMarkdown {

    fun parseBlocks(content: String): List<MarkdownBlock> {
        if (content.isEmpty()) return emptyList()
        val lines = content.split("\n")
        return lines.mapIndexed { index, rawLine ->
            val trimmedStart = rawLine.trimStart()
            when {
                trimmedStart.startsWith("- [x] ", ignoreCase = true) -> {
                    MarkdownBlock.Checklist(index, true, trimmedStart.drop(6))
                }
                trimmedStart.startsWith("- [ ] ") -> {
                    MarkdownBlock.Checklist(index, false, trimmedStart.drop(6))
                }
                trimmedStart.startsWith("[x] ", ignoreCase = true) || trimmedStart.equals("[x]", ignoreCase = true) -> {
                    MarkdownBlock.Checklist(index, true, trimmedStart.drop(3).trimStart())
                }
                trimmedStart.startsWith("[ ] ") || trimmedStart == "[ ]" -> {
                    MarkdownBlock.Checklist(index, false, trimmedStart.drop(3).trimStart())
                }
                trimmedStart.startsWith("### ") -> MarkdownBlock.Heading(3, trimmedStart.drop(4))
                trimmedStart.startsWith("## ") -> MarkdownBlock.Heading(2, trimmedStart.drop(3))
                trimmedStart.startsWith("# ") -> MarkdownBlock.Heading(1, trimmedStart.drop(2))
                trimmedStart.startsWith("- ") -> MarkdownBlock.Bullet(trimmedStart.drop(2))
                else -> MarkdownBlock.Paragraph(rawLine)
            }
        }
    }

    fun hasHighlights(text: String): Boolean {
        val start = text.indexOf("==")
        if (start < 0) return false
        return text.indexOf("==", start + 2) > start + 2
    }

    fun toggleChecklistLine(content: String, lineIndex: Int): String {
        val lines = content.split("\n").toMutableList()
        if (lineIndex !in lines.indices) return content
        val line = lines[lineIndex]
        val leadingSpaces = line.takeWhile { it == ' ' || it == '\t' }
        val rest = line.drop(leadingSpaces.length)
        val updatedRest = when {
            rest.startsWith("- [x] ", ignoreCase = true) -> "- [ ] " + rest.drop(6)
            rest.startsWith("- [ ] ") -> "- [x] " + rest.drop(6)
            rest.startsWith("[x] ", ignoreCase = true) -> "[ ] " + rest.drop(4)
            rest.equals("[x]", ignoreCase = true) -> "[ ]"
            rest.startsWith("[ ] ") -> "[x] " + rest.drop(4)
            rest == "[ ]" -> "[x]"
            else -> rest
        }
        lines[lineIndex] = leadingSpaces + updatedRest
        return lines.joinToString("\n")
    }

    fun buildInlineAnnotatedString(
        text: String,
        highlightBg: Color = Color(0xFFFEF08A),
        highlightFg: Color = Color(0xFF1E293B),
        strikethrough: Boolean = false,
        maskHighlights: Boolean = false,
        maskedBg: Color = Color(0xFFDCEBFF),
        maskedFg: Color = Color(0xFF0B3A75),
        maskedPlaceholder: String = "  ••••  "
    ): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("**", i) -> {
                        val end = text.indexOf("**", i + 2)
                        if (end > i + 2) {
                            val startIdx = length
                            append(text.substring(i + 2, end))
                            addStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (strikethrough) TextDecoration.LineThrough else null
                                ),
                                startIdx,
                                length
                            )
                            i = end + 2
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    text.startsWith("==", i) -> {
                        val end = text.indexOf("==", i + 2)
                        if (end > i + 2) {
                            val startIdx = length
                            if (maskHighlights) {
                                append(maskedPlaceholder)
                                addStyle(
                                    SpanStyle(
                                        background = maskedBg,
                                        color = maskedFg,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    startIdx,
                                    length
                                )
                            } else {
                                append(text.substring(i + 2, end))
                                addStyle(
                                    SpanStyle(
                                        background = highlightBg,
                                        color = highlightFg,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (strikethrough) TextDecoration.LineThrough else null
                                    ),
                                    startIdx,
                                    length
                                )
                            }
                            i = end + 2
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
            if (strikethrough) {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), 0, length)
            }
        }
    }

    fun toReadablePlainLines(content: String): List<String> {
        return parseBlocks(content).map { block ->
            val raw = when (block) {
                is MarkdownBlock.Heading -> block.text
                is MarkdownBlock.Bullet -> "• ${block.text}"
                is MarkdownBlock.Checklist -> if (block.checked) "☑ ${block.text}" else "☐ ${block.text}"
                is MarkdownBlock.Paragraph -> block.text
            }
            buildInlineAnnotatedString(raw).text
        }
    }
}
