package com.sek789102.jotwai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.FolderEntity
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.ui.theme.NoteColors
import com.sek789102.jotwai.util.Dates
import java.util.Calendar
import java.util.TimeZone

/** One-line name input (new folder, rename label…). */
@Composable
fun NameDialog(
    title: String,
    initial: String = "",
    confirmLabel: String = stringResource(R.string.action_save),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.copy(text = it.text.replace("\n", "").take(60)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("name_input")
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text.trim()) }, enabled = value.text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPalette(selected: Int?, darkTheme: Boolean, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0 until NoteColors.count) {
            val c = if (i == 0) MaterialTheme.colorScheme.surface else NoteColors.of(i, darkTheme)
            val name = stringResource(NoteColors.names[i])
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(
                        BorderStroke(if (selected == i) 2.5.dp else 1.dp, if (selected == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        CircleShape
                    )
                    .clickable(onClickLabel = name) { onSelect(i) }
                    .testTag("color_$i"),
                contentAlignment = Alignment.Center
            ) {
                when {
                    selected == i -> Icon(Icons.Outlined.Check, name, tint = MaterialTheme.colorScheme.primary)
                    i == 0 -> Icon(Icons.Outlined.FormatColorReset, name, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ColorDialog(selected: Int?, darkTheme: Boolean, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_color)) },
        text = { ColorPalette(selected, darkTheme, onSelect = { onSelect(it); onDismiss() }) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
fun FolderPickerDialog(
    folders: List<FolderEntity>,
    current: Long?,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit,
    onCreate: (String) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { creating = false },
            onConfirm = { onCreate(it); creating = false; onDismiss() }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_move_to_folder)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.folder_none)) },
                        leadingContent = { Icon(Icons.Outlined.FolderOff, null) },
                        trailingContent = { if (current == null) Icon(Icons.Outlined.Check, null) },
                        modifier = Modifier.clickable { onPick(null); onDismiss() }
                    )
                }
                items(folders, key = { it.id }) { f ->
                    ListItem(
                        headlineContent = { Text(f.name) },
                        leadingContent = { Icon(Icons.Outlined.Folder, null) },
                        trailingContent = { if (current == f.id) Icon(Icons.Outlined.Check, null) },
                        modifier = Modifier.clickable { onPick(f.id); onDismiss() }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.folder_new)) },
                        leadingContent = { Icon(Icons.Outlined.Add, null) },
                        modifier = Modifier.clickable { creating = true }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

/** Tick labels on/off for a note; typing a new name offers to create it. */
@Composable
fun LabelPickerDialog(
    labels: List<LabelEntity>,
    attached: Set<Long>,
    onDismiss: () -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onCreate: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val shown = labels.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val exact = labels.any { it.name.equals(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_labels)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.replace("\n", "").take(60) },
                    placeholder = { Text(stringResource(R.string.label_search_or_create)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("label_query")
                )
                Spacer(Modifier.size(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    if (query.isNotBlank() && !exact) {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.label_create_named, query.trim())) },
                                leadingContent = { Icon(Icons.Outlined.Add, null) },
                                modifier = Modifier.clickable { onCreate(query.trim()); query = "" }.testTag("label_create")
                            )
                        }
                    }
                    items(shown, key = { it.id }) { l ->
                        val on = l.id in attached
                        ListItem(
                            headlineContent = { Text(l.name) },
                            leadingContent = { Icon(Icons.AutoMirrored.Outlined.Label, null) },
                            trailingContent = { Checkbox(checked = on, onCheckedChange = { onToggle(l.id, it) }) },
                            modifier = Modifier.clickable { onToggle(l.id, !on) }
                        )
                    }
                    if (labels.isEmpty() && query.isBlank()) {
                        item { Text(stringResource(R.string.label_none_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } }
    )
}

/**
 * Reminder picker: quick choices (later today, tomorrow morning, next week) or a custom
 * date + time, with an optional repeat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    currentAt: Long?,
    currentRepeat: RepeatRule,
    onDismiss: () -> Unit,
    onSet: (Long, RepeatRule) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val initial = currentAt?.takeIf { it > now } ?: Dates.at(if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 17) 1 else 0, if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 17) 8 else 18)
    var at by rememberSaveable { mutableLongStateOf(initial) }
    var repeat by rememberSaveable { mutableStateOf(currentRepeat) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    val cal = Calendar.getInstance().apply { timeInMillis = at }

    val quick = buildList {
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 17) add(R.string.reminder_quick_today to Dates.at(0, 18, now))
        add(R.string.reminder_quick_tomorrow to Dates.at(1, 8, now))
        add(R.string.reminder_quick_next_week to Dates.at(7, 8, now))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                quick.forEach { (label, time) ->
                    ListItem(
                        headlineContent = { Text(stringResource(label)) },
                        supportingContent = { Text(Dates.dayAndTime(context, time)) },
                        leadingContent = { Icon(Icons.Outlined.Schedule, null) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSet(time, repeat); onDismiss() }
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(stringResource(R.string.reminder_custom), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(onClick = { pickDate = true }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.weight(1f).testTag("reminder_date")) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Event, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Dates.date(at), maxLines = 1)
                        }
                    }
                    Surface(onClick = { pickTime = true }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.testTag("reminder_time")) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Dates.time(at))
                        }
                    }
                }
                Box {
                    Surface(onClick = { repeatMenu = true }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Repeat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Dates.repeatLabel(context, repeat))
                        }
                    }
                    DropdownMenu(expanded = repeatMenu, onDismissRequest = { repeatMenu = false }) {
                        RepeatRule.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(Dates.repeatLabel(context, r)) }, onClick = { repeat = r; repeatMenu = false })
                        }
                    }
                }
                if (at <= System.currentTimeMillis()) {
                    Text(stringResource(R.string.reminder_in_past), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSet(at, repeat); onDismiss() },
                enabled = at > System.currentTimeMillis(),
                modifier = Modifier.testTag("reminder_save")
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (currentAt != null) {
                    TextButton(onClick = { onRemove(); onDismiss() }) {
                        Text(stringResource(R.string.reminder_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )

    if (pickDate) {
        // DatePicker works in UTC days; convert so the chosen day stays the same locally.
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            val c = Calendar.getInstance()
            clear()
            set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMidnight,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { sel ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = sel }
                        at = Calendar.getInstance().apply {
                            timeInMillis = at
                            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }

    if (pickTime) {
        val state = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    at = Calendar.getInstance().apply {
                        timeInMillis = at
                        set(Calendar.HOUR_OF_DAY, state.hour)
                        set(Calendar.MINUTE, state.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    pickTime = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}
