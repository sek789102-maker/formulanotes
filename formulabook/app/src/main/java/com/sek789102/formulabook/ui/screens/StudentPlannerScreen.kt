package com.sek789102.formulabook.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.accentStripeColor
import com.sek789102.formulabook.data.model.badgeBgColor
import com.sek789102.formulabook.data.model.badgeFgColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.mascotDrawableRes
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.domain.study.PendingNoteTask
import com.sek789102.formulabook.domain.study.PlannerHelper
import com.sek789102.formulabook.domain.study.TimetableEntry
import com.sek789102.formulabook.ui.components.EmptyStateIllustration
import com.sek789102.formulabook.ui.components.EmptyStateView
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.HeroBlueStart
import com.sek789102.formulabook.ui.theme.HeroDeepBadgeScrim
import com.sek789102.formulabook.ui.theme.HeroSkyEnd
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily
import com.sek789102.formulabook.ui.theme.WarningBgLight
import com.sek789102.formulabook.ui.theme.WarningFgLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DAY_LABEL_RES = listOf(
    1 to R.string.planner_day_mon,
    2 to R.string.planner_day_tue,
    3 to R.string.planner_day_wed,
    4 to R.string.planner_day_thu,
    5 to R.string.planner_day_fri,
    6 to R.string.planner_day_sat,
    7 to R.string.planner_day_sun
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentPlannerScreen(
    timetableEntries: List<TimetableEntry>,
    pendingTasks: List<PendingNoteTask>,
    exams: List<ExamCountdownEntity>,
    subjects: List<SubjectEntity>,
    onAddTimetableEntry: (dayOfWeek: Int, startTime: String, endTime: String, subjectId: String, room: String) -> Unit,
    onRemoveTimetableEntry: (String) -> Unit,
    onBrowseSubjectNotes: (String) -> Unit,
    onToggleTaskDone: (noteId: Long, lineIndex: Int) -> Unit,
    onOpenNote: (Long) -> Unit,
    onAddExam: (String, String, Long, String) -> Unit,
    onDeleteExam: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayDay = remember { PlannerHelper.todayDayOfWeek() }
    var selectedDay by rememberSaveable { mutableIntStateOf(todayDay) }
    var showAddTimetableDialog by rememberSaveable { mutableStateOf(false) }
    var showAddExamDialog by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val todayClasses = remember(timetableEntries, todayDay) {
        PlannerHelper.entriesForDay(timetableEntries, todayDay)
    }
    val selectedDayClasses = remember(timetableEntries, selectedDay) {
        PlannerHelper.entriesForDay(timetableEntries, selectedDay)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.ContentMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
        ) {
            // Hero Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.Radius24))
                    .background(Brush.linearGradient(listOf(HeroBlueStart, HeroSkyEnd)))
                    .padding(Dimens.Space16)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    StudentMascotAvatar(
                        sizeDp = 58,
                        drawableRes = R.drawable.img_mascot_humanities,
                        contentDescription = stringResource(R.string.mascot_humanities_desc)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
                    ) {
                        Text(
                            text = stringResource(R.string.planner_header_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(Dimens.Radius12),
                            color = HeroDeepBadgeScrim.copy(alpha = 0.28f)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.planner_header_subtitle,
                                    todayClasses.size,
                                    pendingTasks.size,
                                    exams.size
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space4)
                            )
                        }
                    }
                }
            }

            // Section 1: Weekly Class Timetable
            Surface(
                shape = RoundedCornerShape(Dimens.Radius24),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.Space16),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.planner_timetable_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(Dimens.Space8))
                        OutlinedButton(
                            onClick = { showAddTimetableDialog = true },
                            shape = RoundedCornerShape(Dimens.Radius12),
                            modifier = Modifier.testTag("add_timetable_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(Dimens.Space4))
                            Text(stringResource(R.string.planner_add_class), maxLines = 1)
                        }
                    }

                    // Day of week selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                    ) {
                        DAY_LABEL_RES.forEach { (dayNum, labelRes) ->
                            val selected = selectedDay == dayNum
                            val dayCount = timetableEntries.count { it.dayOfWeek == dayNum }
                            FilterChip(
                                selected = selected,
                                onClick = { selectedDay = dayNum },
                                label = {
                                    val label = stringResource(labelRes)
                                    Text(if (dayCount > 0) "$label ($dayCount)" else label)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("planner_day_$dayNum")
                            )
                        }
                    }

                    if (selectedDayClasses.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.Radius16),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimens.Space16),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
                            ) {
                                Text(
                                    text = stringResource(R.string.planner_empty_day),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.planner_empty_day_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                            selectedDayClasses.forEach { entry ->
                                val subject = subjects.resolveSubject(entry.subjectId)
                                TimetableSlotCard(
                                    entry = entry,
                                    subject = subject,
                                    onBrowseNotes = { onBrowseSubjectNotes(subject.id) },
                                    onDelete = { onRemoveTimetableEntry(entry.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Pending Checklist Tasks Aggregated From All Notes
            Surface(
                shape = RoundedCornerShape(Dimens.Radius24),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.Space16),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.planner_tasks_title, pendingTasks.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.planner_tasks_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (pendingTasks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.Radius16),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimens.Space12),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.planner_tasks_empty),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                            pendingTasks.forEach { task ->
                                val subject = subjects.resolveSubject(task.subjectId)
                                Surface(
                                    onClick = { onOpenNote(task.noteId) },
                                    shape = RoundedCornerShape(Dimens.Radius12),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                                    border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("planner_task_${task.noteId}_${task.lineIndex}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            start = Dimens.Space4,
                                            end = Dimens.Space12,
                                            top = Dimens.Space4,
                                            bottom = Dimens.Space4
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = false,
                                            onCheckedChange = {
                                                onToggleTaskDone(task.noteId, task.lineIndex)
                                            }
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = Dimens.Space4),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = task.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${subject.displayName()} · ${task.noteTitle.ifBlank { stringResource(R.string.note_untitled_paren) }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = subject.accentStripeColor(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Exam Countdown Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.exam_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.exam_total_count, exams.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { showAddExamDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_exam_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.exam_add_button), maxLines = 1, softWrap = false)
                }
            }

            if (exams.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.empty_exams_title),
                    subtitle = stringResource(R.string.empty_exams_subtitle),
                    illustration = EmptyStateIllustration.EXAMS,
                    actionLabel = stringResource(R.string.exam_add_first),
                    onAction = { showAddExamDialog = true }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exams.forEach { exam ->
                        ExamCountdownCard(
                            exam = exam,
                            subject = subjects.resolveSubject(exam.subject),
                            onDelete = { onDeleteExam(exam.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(72.dp))
        }
    }

    if (showAddTimetableDialog) {
        AddTimetableDialog(
            initialDay = selectedDay,
            subjects = subjects,
            onDismiss = { showAddTimetableDialog = false },
            onConfirm = { day, start, end, subjectId, room ->
                selectedDay = day
                onAddTimetableEntry(day, start, end, subjectId, room)
                showAddTimetableDialog = false
            }
        )
    }

    if (showAddExamDialog) {
        AddExamDialog(
            subjects = subjects,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { title, subjectId, dateMillis, note ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                onAddExam(title, subjectId, dateMillis, note)
                showAddExamDialog = false
            }
        )
    }
}

@Composable
private fun TimetableSlotCard(
    entry: TimetableEntry,
    subject: SubjectEntity,
    onBrowseNotes: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = subject.accentStripeColor()
    Surface(
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_slot_${entry.id}")
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.Space12, vertical = Dimens.Space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
            ) {
                StudentMascotAvatar(
                    sizeDp = 38,
                    drawableRes = subject.mascotDrawableRes(),
                    contentDescription = subject.displayName()
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${entry.startTime} - ${entry.endTime}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = subject.displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.room.isNotBlank()) {
                        Text(
                            text = entry.room,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onBrowseNotes) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = stringResource(R.string.planner_open_subject_note),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.planner_delete_class_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTimetableDialog(
    initialDay: Int,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String, String) -> Unit
) {
    var selectedDay by rememberSaveable { mutableIntStateOf(initialDay) }
    var selectedSubjectId by rememberSaveable { mutableStateOf(SubjectCategory.MATH.name) }
    var startTime by rememberSaveable { mutableStateOf("08:30") }
    var endTime by rememberSaveable { mutableStateOf("09:30") }
    var room by rememberSaveable { mutableStateOf("") }
    val isStartValid = PlannerHelper.isValidTimeString(startTime)
    val isEndValid = PlannerHelper.isValidTimeString(endTime)
    val canConfirmTime = isStartValid && isEndValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.planner_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
            ) {
                Text(
                    text = stringResource(R.string.planner_dialog_day_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DAY_LABEL_RES.forEach { (dayNum, labelRes) ->
                        FilterChip(
                            selected = selectedDay == dayNum,
                            onClick = { selectedDay = dayNum },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { cat ->
                        FilterChip(
                            selected = selectedSubjectId == cat.id,
                            onClick = { selectedSubjectId = cat.id },
                            label = { Text(cat.displayName()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.accentStripeColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text(stringResource(R.string.planner_dialog_start_label)) },
                        isError = !isStartValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text(stringResource(R.string.planner_dialog_end_label)) },
                        isError = !isEndValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!canConfirmTime) {
                    Text(
                        text = stringResource(R.string.planner_invalid_time_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text(stringResource(R.string.planner_dialog_room_label)) },
                    placeholder = { Text(stringResource(R.string.planner_dialog_room_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedDay,
                        PlannerHelper.normalizeTimeString(startTime),
                        PlannerHelper.normalizeTimeString(endTime),
                        selectedSubjectId,
                        room
                    )
                },
                enabled = canConfirmTime
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamCountdownCard(
    exam: ExamCountdownEntity,
    subject: SubjectEntity,
    onDelete: () -> Unit
) {
    val daysRemaining = remember(exam.examDateMillis) {
        PlannerHelper.daysBetweenLocalDates(exam.examDateMillis)
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dateLabel = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("th-TH")).format(Date(exam.examDateMillis))
    val countdownBadgeBg = when {
        daysRemaining < 0 -> MaterialTheme.colorScheme.surfaceVariant
        daysRemaining <= 3 -> MaterialTheme.colorScheme.errorContainer
        daysRemaining <= 7 -> if (isDark) Color(0xFF452E0A) else WarningBgLight
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val countdownBadgeFg = when {
        daysRemaining < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        daysRemaining <= 3 -> MaterialTheme.colorScheme.onErrorContainer
        daysRemaining <= 7 -> if (isDark) Color(0xFFFCD34D) else WarningFgLight
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val countdownText = when {
        daysRemaining < 0 -> stringResource(R.string.exam_days_past, -daysRemaining)
        daysRemaining == 0 -> stringResource(R.string.exam_days_today)
        else -> stringResource(R.string.exam_days_left, daysRemaining)
    }

    Surface(
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.ElevationNone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Dimens.Space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = subject.badgeBgColor(),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = subject.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subject.badgeFgColor(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (exam.note.isNotBlank()) {
                    Text(
                        text = exam.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = countdownBadgeBg
                ) {
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = countdownBadgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.exam_delete_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExamDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var selectedSubjectId by rememberSaveable { mutableStateOf(SubjectCategory.MATH.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val dateFormatted = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("th-TH")).format(Date(selectedDateMillis))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exam_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.exam_dialog_name_label)) },
                    placeholder = { Text(stringResource(R.string.exam_dialog_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { cat ->
                        FilterChip(
                            selected = selectedSubjectId == cat.id,
                            onClick = { selectedSubjectId = cat.id },
                            label = { Text(cat.displayName()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.accentStripeColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.exam_dialog_date_label, dateFormatted))
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.exam_dialog_note_label)) },
                    placeholder = { Text(stringResource(R.string.exam_dialog_note_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, selectedSubjectId, selectedDateMillis, note) },
                enabled = title.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = PlannerHelper.localMillisToUtcMidnight(selectedDateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            selectedDateMillis = PlannerHelper.utcDatePickerMillisToLocalMidnight(utcMillis)
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.action_choose)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
