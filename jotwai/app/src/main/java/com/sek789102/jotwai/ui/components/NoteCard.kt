package com.sek789102.jotwai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.domain.MarkdownStyle
import com.sek789102.jotwai.ui.theme.NoteColors
import com.sek789102.jotwai.util.Dates

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    item: NoteWithLabels,
    folderName: String?,
    selected: Boolean,
    darkTheme: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val note = item.note
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val bg = if (note.color == 0) scheme.surface else NoteColors.of(note.color, darkTheme)
    val border = when {
        selected -> BorderStroke(2.5.dp, scheme.primary)
        note.color == 0 -> BorderStroke(1.dp, scheme.outlineVariant)
        else -> null
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("note_card_${note.id}")
    ) {
        Column {
            if (!note.isLocked && note.imagePaths.isNotEmpty()) {
                NoteImage(
                    fileName = note.imagePaths.first(),
                    maxPx = 480,
                    modifier = Modifier.fillMaxWidth().heightIn(max = if (compact) 120.dp else 180.dp)
                )
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (note.title.isNotBlank()) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    note.isLocked -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.locked_note_hidden), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    note.type == NoteType.CHECKLIST -> ChecklistPreview(item, compact)
                    note.content.isNotBlank() -> Text(
                        MarkdownStyle.style(
                            note.content.take(1200),
                            marker = scheme.onSurfaceVariant.copy(alpha = 0.45f),
                            highlight = Color(0x66FFD54F),
                            done = scheme.onSurfaceVariant
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (compact) 6 else 10,
                        overflow = TextOverflow.Ellipsis
                    )
                    note.title.isBlank() && note.imagePaths.isEmpty() -> Text(
                        stringResource(R.string.empty_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                val chips = buildList<Pair<ImageVector, String>> {
                    note.reminderAt?.let {
                        val icon = if (note.reminderRepeat == RepeatRule.NONE) Icons.Outlined.Alarm else Icons.Outlined.Repeat
                        add(icon to Dates.dayAndTime(context, it))
                    }
                    folderName?.let { add(Icons.Outlined.Folder to it) }
                    item.labels.take(3).forEach { add(Icons.AutoMirrored.Outlined.Label to it.name) }
                    if (item.labels.size > 3) add(Icons.AutoMirrored.Outlined.Label to "+${item.labels.size - 3}")
                }
                if (chips.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val overdue = note.reminderAt != null && note.reminderAt < System.currentTimeMillis()
                        chips.forEachIndexed { i, (icon, label) ->
                            MetaChip(icon, label, strike = i == 0 && overdue && note.reminderAt != null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistPreview(item: NoteWithLabels, compact: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val items = item.note.checklist.filter { it.text.isNotBlank() }
    val open = items.filter { !it.checked }
    val done = items.size - open.size
    val shown = open.take(if (compact) 5 else 8)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        shown.forEach {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckBoxOutlineBlank, null, Modifier.size(18.dp).padding(top = 2.dp), tint = scheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(it.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (open.size > shown.size) {
            Text("…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        if (done > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckBox, null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.checklist_done_count, done),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textDecoration = TextDecoration.None
                )
            }
        }
    }
}

@Composable
fun MetaChip(icon: ImageVector, label: String, strike: Boolean = false, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = scheme.onSurface.copy(alpha = 0.07f),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = scheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (strike) TextDecoration.LineThrough else null
            )
        }
    }
}
