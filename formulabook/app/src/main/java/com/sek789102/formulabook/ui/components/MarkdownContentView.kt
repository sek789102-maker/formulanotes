package com.sek789102.formulabook.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.domain.markdown.MarkdownBlock
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown

@Composable
fun MarkdownContentView(
    content: String,
    onToggleChecklistLine: ((Int) -> Unit)? = null,
    recallMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { SimpleMarkdown.parseBlocks(content) }
    val revealedIndices = remember(content, recallMode) { mutableStateMapOf<Int, Boolean>() }
    val highlightBg = Color(0xFFFEF08A)
    val highlightFg = Color(0xFF1E293B)
    val maskedBg = MaterialTheme.colorScheme.primaryContainer
    val maskedFg = MaterialTheme.colorScheme.onPrimaryContainer
    val maskedLabel = " [${stringResource(R.string.detail_recall_tap_reveal)}] "

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEachIndexed { blockIdx, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 21.sp
                        2 -> 18.sp
                        else -> 16.sp
                    }
                    val hasMask = recallMode && SimpleMarkdown.hasHighlights(block.text)
                    val isMasked = hasMask && revealedIndices[blockIdx] != true
                    Text(
                        text = SimpleMarkdown.buildInlineAnnotatedString(
                            text = block.text,
                            highlightBg = highlightBg,
                            highlightFg = highlightFg,
                            maskHighlights = isMasked,
                            maskedBg = maskedBg,
                            maskedFg = maskedFg,
                            maskedPlaceholder = maskedLabel
                        ),
                        fontSize = fontSize,
                        lineHeight = (fontSize.value + 7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .then(
                                if (hasMask) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            revealedIndices[blockIdx] = !(revealedIndices[blockIdx] ?: false)
                                        }
                                } else Modifier
                            )
                            .padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.Bullet -> {
                    val hasMask = recallMode && SimpleMarkdown.hasHighlights(block.text)
                    val isMasked = hasMask && revealedIndices[blockIdx] != true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (hasMask) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            revealedIndices[blockIdx] = !(revealedIndices[blockIdx] ?: false)
                                        }
                                } else Modifier
                            )
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                        )
                        Text(
                            text = SimpleMarkdown.buildInlineAnnotatedString(
                                text = block.text,
                                highlightBg = highlightBg,
                                highlightFg = highlightFg,
                                maskHighlights = isMasked,
                                maskedBg = maskedBg,
                                maskedFg = maskedFg,
                                maskedPlaceholder = maskedLabel
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Checklist -> {
                    val hasMask = recallMode && SimpleMarkdown.hasHighlights(block.text)
                    val isMasked = hasMask && revealedIndices[blockIdx] != true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (hasMask) {
                                    Modifier.clickable {
                                        revealedIndices[blockIdx] = !(revealedIndices[blockIdx] ?: false)
                                    }
                                } else if (onToggleChecklistLine != null) {
                                    Modifier.clickable { onToggleChecklistLine(block.lineIndex) }
                                } else Modifier
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = block.checked,
                            onCheckedChange = if (onToggleChecklistLine != null) {
                                { onToggleChecklistLine(block.lineIndex) }
                            } else null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = SimpleMarkdown.buildInlineAnnotatedString(
                                text = block.text,
                                highlightBg = highlightBg,
                                highlightFg = highlightFg,
                                strikethrough = block.checked,
                                maskHighlights = isMasked,
                                maskedBg = maskedBg,
                                maskedFg = maskedFg,
                                maskedPlaceholder = maskedLabel
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = if (block.checked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    if (block.text.isEmpty()) {
                        Spacer(Modifier.height(6.dp))
                    } else {
                        val hasMask = recallMode && SimpleMarkdown.hasHighlights(block.text)
                        val isMasked = hasMask && revealedIndices[blockIdx] != true
                        Text(
                            text = SimpleMarkdown.buildInlineAnnotatedString(
                                text = block.text,
                                highlightBg = highlightBg,
                                highlightFg = highlightFg,
                                maskHighlights = isMasked,
                                maskedBg = maskedBg,
                                maskedFg = maskedFg,
                                maskedPlaceholder = maskedLabel
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 27.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = if (hasMask) {
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        revealedIndices[blockIdx] = !(revealedIndices[blockIdx] ?: false)
                                    }
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }
}
