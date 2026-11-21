package com.sek789102.formulabook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.badgeBgColor
import com.sek789102.formulabook.data.model.badgeFgColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import com.sek789102.formulabook.ui.components.EmptyStateIllustration
import com.sek789102.formulabook.ui.components.EmptyStateView
import com.sek789102.formulabook.ui.theme.Dimens
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrashScreen(
    trashedNotes: List<NoteWithTags>,
    subjects: List<SubjectEntity>,
    onBack: () -> Unit,
    onRestoreNote: (Long) -> Unit,
    onPermanentDeleteNote: (Long) -> Unit,
    onEmptyTrash: () -> Unit
) {
    BackHandler { onBack() }

    var showEmptyConfirm by remember { mutableStateOf(false) }
    var pendingDeleteNote by remember { mutableStateOf<NoteWithTags?>(null) }
    val nowMillis = remember(trashedNotes.size) { System.currentTimeMillis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.trash_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("trash_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (trashedNotes.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyConfirm = true },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.trash_empty_action),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .fillMaxSize()
            ) {
                Surface(
                    shape = RoundedCornerShape(Dimens.Radius12),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.Space16, vertical = Dimens.Space8)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.trash_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (trashedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            title = stringResource(R.string.trash_empty_title),
                            subtitle = stringResource(R.string.trash_empty_subtitle),
                            illustration = EmptyStateIllustration.TRASH
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = Dimens.Space16, vertical = Dimens.Space8),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                    ) {
                        items(trashedNotes, key = { it.note.id }) { item ->
                            val note = item.note
                            val subject = subjects.resolveSubject(note.subject)
                            val deletedAt = note.deletedAt ?: nowMillis
                            val daysElapsed = TimeUnit.MILLISECONDS.toDays((nowMillis - deletedAt).coerceAtLeast(0L)).toInt()
                            val remainingDays = (30 - daysElapsed).coerceIn(1, 30)
                            val previewText = remember(note.content) {
                                SimpleMarkdown.toReadablePlainLines(note.content)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                            }

                            Surface(
                                shape = RoundedCornerShape(Dimens.Radius16),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = Dimens.ElevationNone,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trash_note_card_${note.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(Dimens.Space16),
                                    verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = subject.badgeBgColor()
                                        ) {
                                            Text(
                                                text = subject.displayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = subject.badgeFgColor(),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.trash_days_remaining, remainingDays),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = note.title.ifBlank { stringResource(R.string.note_untitled_paren) },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (previewText.isNotBlank()) {
                                        Text(
                                            text = previewText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.height(2.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { pendingDeleteNote = item },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.trash_delete_forever))
                                        }
                                        Button(
                                            onClick = { onRestoreNote(note.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("restore_note_${note.id}")
                                        ) {
                                            Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.trash_restore))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text(stringResource(R.string.trash_confirm_empty_title)) },
            text = { Text(stringResource(R.string.trash_confirm_empty_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyConfirm = false
                        onEmptyTrash()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.trash_empty_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    pendingDeleteNote?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text(stringResource(R.string.trash_confirm_delete_one_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.trash_confirm_delete_one_msg,
                        target.note.title.ifBlank { stringResource(R.string.note_untitled_paren) }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = target.note.id
                        pendingDeleteNote = null
                        onPermanentDeleteNote(id)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.trash_delete_forever),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNote = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
