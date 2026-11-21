package com.sek789102.formulabook.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.NoteSortOrder
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.accentStripeColor
import com.sek789102.formulabook.data.model.badgeBgColor
import com.sek789102.formulabook.data.model.badgeFgColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.mascotDrawableRes
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import com.sek789102.formulabook.ui.components.CustomSubjectDialog
import com.sek789102.formulabook.ui.components.EmptyStateIllustration
import com.sek789102.formulabook.ui.components.EmptyStateView
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.HeroBlueStart
import com.sek789102.formulabook.ui.theme.HeroDeepBadgeScrim
import com.sek789102.formulabook.ui.theme.HeroSkyEnd
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily
import com.sek789102.formulabook.ui.viewmodel.FilterState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesHomeScreen(
    allNotesCount: Int,
    totalFormulasCount: Int,
    pendingTasksCount: Int,
    showBackupReminder: Boolean,
    subjects: List<SubjectEntity>,
    filteredNotes: List<NoteWithTags>,
    filterState: FilterState,
    sortOrder: NoteSortOrder,
    isWideLayout: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectSubject: (String?) -> Unit,
    onCreateSubject: (String, Long) -> Unit,
    onChangeSortOrder: (NoteSortOrder) -> Unit,
    onTogglePinnedFilter: () -> Unit,
    onToggleFormulaFilter: () -> Unit,
    onToggleReviewFilter: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onTogglePinNote: (Long, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onBackupNowFromReminder: () -> Unit,
    onDismissBackupReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSubjectDialog by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val mascotSpeech = when {
        allNotesCount == 0 -> stringResource(R.string.mascot_speech_empty)
        totalFormulasCount == 0 -> stringResource(R.string.mascot_speech_no_formula, allNotesCount)
        else -> stringResource(R.string.mascot_speech_active, allNotesCount, totalFormulasCount)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Vibrant Blue-Sky Gradient Hero Header with Selectable Cartoon Student Mascot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
                .clip(RoundedCornerShape(Dimens.Radius24))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(HeroBlueStart, HeroSkyEnd)
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = size.height * 0.65f,
                        center = Offset(size.width * 0.92f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.height * 0.45f,
                        center = Offset(size.width * 0.72f, size.height * 0.95f)
                    )
                }
                .padding(Dimens.Space16)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudentMascotAvatar(
                            sizeDp = 62,
                            drawableRes = R.drawable.img_student_mascot,
                            contentDescription = stringResource(R.string.mascot_avatar_desc),
                            modifier = Modifier.testTag("hero_mascot_avatar")
                        )
                        Spacer(Modifier.width(Dimens.Space12))
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(Dimens.Radius12),
                                color = HeroDeepBadgeScrim.copy(alpha = 0.28f)
                            ) {
                                Text(
                                    text = mascotSpeech,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space4)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(Dimens.Space8))
                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = HeroDeepBadgeScrim.copy(alpha = 0.28f),
                        modifier = Modifier
                            .size(Dimens.MinTouchTarget)
                            .testTag("open_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings_backup_desc),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Live 0-based Mini Stat Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroStatChip(stringResource(R.string.hero_stat_notes, allNotesCount))
                    HeroStatChip(stringResource(R.string.hero_stat_formulas, totalFormulasCount))
                    if (pendingTasksCount > 0) {
                        HeroStatChip(stringResource(R.string.hero_stat_tasks, pendingTasksCount))
                    }
                }
            }
        }

        // 14-day Backup Reminder Banner (Feature 12)
        if (showBackupReminder) {
            Surface(
                shape = RoundedCornerShape(Dimens.Radius16),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space16, vertical = Dimens.Space4)
                    .testTag("backup_reminder_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_reminder_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.backup_reminder_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
                        TextButton(onClick = onDismissBackupReminder) {
                            Text(stringResource(R.string.backup_reminder_later))
                        }
                        Button(
                            onClick = onBackupNowFromReminder,
                            shape = RoundedCornerShape(Dimens.Radius12)
                        ) {
                            Text(stringResource(R.string.backup_reminder_now))
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dimens.Space8))
        }

        // Search Bar
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_notes_hint),
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
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.search_clear_desc)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_notes_input")
        )

        Spacer(Modifier.height(8.dp))

        // Subject + Quick Status Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterState.onlyPinned,
                onClick = onTogglePinnedFilter,
                label = { Text(stringResource(R.string.filter_pinned)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (filterState.onlyPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = filterState.onlyWithFormulas,
                onClick = onToggleFormulaFilter,
                label = { Text(stringResource(R.string.filter_with_formulas)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Functions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                )
            )
            FilterChip(
                selected = filterState.onlyNeedsReview,
                onClick = onToggleReviewFilter,
                label = { Text(stringResource(R.string.filter_needs_review)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (filterState.onlyNeedsReview) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary
                )
            )
            subjects.forEach { subject ->
                FilterChip(
                    selected = filterState.selectedSubjectId == subject.id,
                    onClick = { onSelectSubject(subject.id) },
                    label = { Text(subject.displayName()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = subject.accentStripeColor(),
                        selectedLabelColor = Color.White
                    )
                )
            }
            AssistChip(
                onClick = { showAddSubjectDialog = true },
                label = { Text(stringResource(R.string.subject_add_button)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        // Sort Selector & Active Filter Count Row (Feature 6)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.testTag("sort_order_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.sort_label, stringResource(sortOrder.labelRes)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    NoteSortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            leadingIcon = {
                                if (option == sortOrder) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                showSortMenu = false
                                onChangeSortOrder(option)
                            }
                        )
                    }
                }
            }

            if (filterState.isAnyFilterActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.filter_found_count, filteredNotes.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearFilters) {
                        Text(stringResource(R.string.filter_clear))
                    }
                }
            }
        }

        // Content Area: Strict 0-state when empty! Only FAB (+ จดโน้ตใหม่) exists on Notes screen.
        when {
            allNotesCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = stringResource(R.string.empty_notes_title),
                        subtitle = stringResource(R.string.empty_notes_subtitle),
                        illustration = EmptyStateIllustration.NOTES
                    )
                }
            }
            filteredNotes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = stringResource(R.string.empty_filtered_title),
                        subtitle = stringResource(R.string.empty_filtered_subtitle),
                        illustration = EmptyStateIllustration.SEARCH,
                        actionLabel = stringResource(R.string.filter_clear_all),
                        onAction = onClearFilters
                    )
                }
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (isWideLayout) 2 else 1),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = Dimens.Space16,
                        end = Dimens.Space16,
                        top = Dimens.Space4,
                        bottom = 96.dp
                    ),
                    verticalItemSpacing = Dimens.Space12,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    items(filteredNotes, key = { it.note.id }) { item ->
                        NoteSummaryCard(
                            noteWithTags = item,
                            subject = subjects.resolveSubject(item.note.subject),
                            onClick = { onOpenNote(item.note.id) },
                            onTogglePin = { onTogglePinNote(item.note.id, item.note.isPinned) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        CustomSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, colorHex ->
                onCreateSubject(name, colorHex)
                showAddSubjectDialog = false
            }
        )
    }
}

@Composable
private fun HeroStatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(Dimens.Radius24),
        color = HeroDeepBadgeScrim.copy(alpha = 0.28f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space4)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteSummaryCard(
    noteWithTags: NoteWithTags,
    subject: SubjectEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    val note = noteWithTags.note
    val accent = subject.accentStripeColor()
    val badgeBg = subject.badgeBgColor()
    val badgeFg = subject.badgeFgColor()
    val dateLabel = SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("th-TH")).format(Date(note.updatedAt))
    val previewContent = remember(note.content) {
        SimpleMarkdown.toReadablePlainLines(note.content)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (note.isPinned) Dimens.BorderSelected else Dimens.BorderThin,
            color = if (note.isPinned) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (note.isPinned) Dimens.ElevationRaised else Dimens.ElevationNone
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
                    .padding(Dimens.Space16)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StudentMascotAvatar(
                            sizeDp = 28,
                            drawableRes = subject.mascotDrawableRes(),
                            contentDescription = subject.displayName(),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = subject.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (note.needsReview) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_needs_review),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (note.imagePaths.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = note.imagePaths.size.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (note.isPinned) R.string.pin_action_unpin else R.string.pin_action_pin
                            ),
                            tint = if (note.isPinned) accent else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = note.title.ifBlank { stringResource(R.string.note_untitled_paren) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (previewContent.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = previewContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.formulas.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val firstFormula = note.formulas.first()
                    val isFlashcard = firstFormula.isFlashcard
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFlashcard) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isFlashcard) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isFlashcard) Icons.Rounded.Style else Icons.Rounded.Functions,
                                    contentDescription = null,
                                    tint = if (isFlashcard) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isFlashcard) {
                                        firstFormula.title.ifBlank { firstFormula.expression }
                                    } else {
                                        firstFormula.expression.ifBlank { firstFormula.title }
                                    },
                                    fontFamily = if (isFlashcard) FontFamily.Default else JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFlashcard) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (note.formulas.size > 1) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.note_extra_formulas, note.formulas.size - 1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.note_updated_prefix, dateLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}
