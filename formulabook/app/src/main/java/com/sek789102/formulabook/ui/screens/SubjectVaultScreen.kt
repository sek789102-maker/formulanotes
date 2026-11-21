package com.sek789102.formulabook.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.accentStripeColor
import com.sek789102.formulabook.data.model.badgeBgColor
import com.sek789102.formulabook.data.model.badgeFgColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.mascotDrawableRes
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.domain.study.QuickReferenceCatalog
import com.sek789102.formulabook.domain.study.QuickReferenceItem
import com.sek789102.formulabook.domain.study.SubjectVaultSummary
import com.sek789102.formulabook.ui.components.EmptyStateIllustration
import com.sek789102.formulabook.ui.components.EmptyStateView
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.components.StudentMascotCompanionCard
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily

private enum class VaultTabMode {
    CHEAT_SHEETS,
    SUBJECT_FOLDERS
}

@Composable
fun SubjectVaultScreen(
    allNotes: List<NoteWithTags>,
    subjects: List<SubjectEntity>,
    onSaveReferenceAsNote: (QuickReferenceItem) -> Unit,
    onBrowseSubjectNotes: (String) -> Unit,
    onCreateNoteInSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var modeName by rememberSaveable { mutableStateOf(VaultTabMode.CHEAT_SHEETS.name) }
    val mode = VaultTabMode.entries.firstOrNull { it.name == modeName } ?: VaultTabMode.CHEAT_SHEETS

    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val availableReferenceSubjectIds = remember { QuickReferenceCatalog.availableSubjectIds() }
    val filteredReferences = remember(selectedSubjectId, searchQuery) {
        QuickReferenceCatalog.filterItems(selectedSubjectId, searchQuery)
    }
    val subjectSummaries = remember(subjects, allNotes) {
        QuickReferenceCatalog.summarizeSubjects(subjects, allNotes)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.ContentMaxWidth)
                .fillMaxSize()
        ) {
            StudentMascotCompanionCard(
                drawableRes = R.drawable.img_mascot_tags,
                avatarContentDescription = stringResource(R.string.mascot_tags_desc),
                title = stringResource(R.string.vault_header_title),
                message = stringResource(
                    R.string.vault_header_subtitle,
                    QuickReferenceCatalog.items.size,
                    subjects.size
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
            )

            // Clean 2-tab mode switch (ชีทสรุปลัด / สมุดรายวิชา)
            val modeOptions = listOf(
                VaultTabMode.CHEAT_SHEETS to R.string.vault_tab_cheat_sheets,
                VaultTabMode.SUBJECT_FOLDERS to R.string.vault_tab_subjects
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space16)
            ) {
                modeOptions.forEachIndexed { index, (tab, labelRes) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = modeOptions.size
                        ),
                        selected = mode == tab,
                        onClick = { modeName = tab.name },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("vault_mode_${tab.name.lowercase()}")
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.Space12))

            when (mode) {
                VaultTabMode.CHEAT_SHEETS -> {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.vault_search_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = stringResource(R.string.search_clear_desc)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.Radius16),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.Space16)
                            .testTag("vault_search_input")
                    )

                    Spacer(Modifier.height(Dimens.Space8))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.Space16),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = { selectedSubjectId = null },
                            label = { Text(stringResource(R.string.vault_filter_all)) }
                        )
                        availableReferenceSubjectIds.forEach { subjId ->
                            val subj = subjects.resolveSubject(subjId)
                            FilterChip(
                                selected = selectedSubjectId == subjId,
                                onClick = {
                                    selectedSubjectId = if (selectedSubjectId == subjId) null else subjId
                                },
                                label = { Text(subj.displayName()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = subj.accentStripeColor(),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.Space8))

                    if (filteredReferences.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                title = stringResource(R.string.vault_empty_search_title),
                                subtitle = stringResource(R.string.vault_empty_search_subtitle),
                                illustration = EmptyStateIllustration.SEARCH,
                                actionLabel = stringResource(R.string.filter_clear_all),
                                onAction = {
                                    selectedSubjectId = null
                                    searchQuery = ""
                                }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(
                                start = Dimens.Space16,
                                end = Dimens.Space16,
                                top = Dimens.Space4,
                                bottom = Dimens.Space24
                            ),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                        ) {
                            items(filteredReferences, key = { it.id }) { item ->
                                val subj = subjects.resolveSubject(item.subjectId)
                                QuickReferenceCard(
                                    item = item,
                                    subject = subj,
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(item.expressionOrRule))
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_formula_copied),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onSaveToNote = {
                                        onSaveReferenceAsNote(item)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.vault_saved_to_note_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }

                VaultTabMode.SUBJECT_FOLDERS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = Dimens.Space16,
                            end = Dimens.Space16,
                            top = Dimens.Space4,
                            bottom = Dimens.Space24
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                    ) {
                        items(subjectSummaries, key = { it.subject.id }) { summary ->
                            SubjectFolderCard(
                                summary = summary,
                                onBrowseNotes = { onBrowseSubjectNotes(summary.subject.id) },
                                onCreateNote = { onCreateNoteInSubject(summary.subject.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickReferenceCard(
    item: QuickReferenceItem,
    subject: SubjectEntity,
    onCopy: () -> Unit,
    onSaveToNote: () -> Unit
) {
    val accent = subject.accentStripeColor()
    val badgeBg = subject.badgeBgColor()
    val badgeFg = subject.badgeFgColor()

    Surface(
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.ElevationNone,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_ref_card_${item.id}")
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Dimens.Space16),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                        modifier = Modifier.weight(1f)
                    ) {
                        StudentMascotAvatar(
                            sizeDp = 30,
                            drawableRes = subject.mascotDrawableRes(),
                            contentDescription = subject.displayName()
                        )
                        Surface(
                            shape = RoundedCornerShape(Dimens.Radius8),
                            color = badgeBg
                        ) {
                            Text(
                                text = subject.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = Dimens.Space8, vertical = Dimens.Space4)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(Dimens.Radius8),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.Space8, vertical = Dimens.Space4),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isFormula) Icons.Rounded.Functions else Icons.Rounded.Style,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (item.isFormula) R.string.vault_type_formula else R.string.vault_type_summary
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Highlighted Core Rule / Formula Box
                Surface(
                    shape = RoundedCornerShape(Dimens.Radius12),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                    border = BorderStroke(
                        Dimens.BorderThin,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.expressionOrRule,
                        fontFamily = if (item.isFormula) JetBrainsMonoFontFamily else FontFamily.Default,
                        fontSize = if (item.isFormula) 14.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(Dimens.Space12)
                    )
                }

                Text(
                    text = item.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.tip.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.Radius12),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = item.tip,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .testTag("vault_copy_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Dimens.Space4))
                        Text(
                            text = stringResource(R.string.action_copy),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onSaveToNote,
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier
                            .weight(1.35f)
                            .heightIn(min = Dimens.MinTouchTarget)
                            .testTag("vault_save_note_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Dimens.Space4))
                        Text(
                            text = stringResource(R.string.vault_save_to_note),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectFolderCard(
    summary: SubjectVaultSummary,
    onBrowseNotes: () -> Unit,
    onCreateNote: () -> Unit
) {
    val subject = summary.subject
    val accent = subject.accentStripeColor()

    Surface(
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.ElevationNone,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_subject_${subject.id}")
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Dimens.Space16),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    StudentMascotAvatar(
                        sizeDp = 46,
                        drawableRes = subject.mascotDrawableRes(),
                        contentDescription = subject.displayName()
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = subject.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (summary.noteCount == 0 && summary.cardCount == 0) {
                                stringResource(R.string.vault_subject_empty)
                            } else {
                                stringResource(
                                    R.string.vault_subject_stats,
                                    summary.noteCount,
                                    summary.cardCount,
                                    summary.masteredCount
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.cardCount > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
                        LinearProgressIndicator(
                            progress = { summary.masteryProgress },
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(Dimens.Radius8))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                ) {
                    OutlinedButton(
                        onClick = onBrowseNotes,
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Dimens.Space4))
                        Text(
                            text = stringResource(R.string.vault_browse_notes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onCreateNote,
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Dimens.Space4))
                        Text(
                            text = stringResource(R.string.vault_new_in_subject),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
