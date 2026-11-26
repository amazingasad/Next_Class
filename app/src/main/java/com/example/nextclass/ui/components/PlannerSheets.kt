package com.example.nextclass.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.PlannerTask
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/** Square check box that fills in with a short animation. */
@Composable
fun TaskCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (checked) colors.onSurface else Color.Transparent,
        animationSpec = tween(200),
        label = "checkFill",
    )
    val border by animateColorAsState(
        targetValue = if (checked) colors.onSurface else NcTheme.extras.muted,
        animationSpec = tween(200),
        label = "checkBorder",
    )
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.surface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun TaskRow(
    task: PlannerTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheck(task.done)
        Spacer(Modifier.width(14.dp))
        Text(
            text = task.title,
            style = NcText.Body.copy(
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            ),
            color = if (task.done) NcTheme.extras.muted else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Delete task",
                tint = NcTheme.extras.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** A text field with an add button at the end, used for typing task after task. */
@Composable
fun AddTaskField(
    label: String,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    NcTextField(
        value = text,
        onValueChange = { text = it },
        label = label,
        placeholder = "Revise chapter 3",
        modifier = modifier,
        trailing = {
            IconButton(
                onClick = {
                    onAdd(text)
                    text = ""
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add task",
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        NcTheme.extras.muted
                    },
                )
            }
        },
    )
}

/**
 * The evening flow: choose which of today's tasks to keep for tomorrow, add new ones, and save.
 * Unfinished tasks start ticked; finished ones start unticked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTomorrowSheet(
    dateLabel: String,
    todayTasks: List<PlannerTask>,
    tomorrowTasks: List<PlannerTask>,
    onAddTomorrow: (String) -> Unit,
    onDeleteTomorrow: (Long) -> Unit,
    onKeep: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kept by remember { mutableStateOf(todayTasks.filter { !it.done }.map { it.id }.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NcTheme.extras.hairline),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MicroLabel(dateLabel)
                Text(
                    text = "Plan tomorrow",
                    style = NcText.Headline,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (todayTasks.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MicroLabel("Keep from today")
                    Text(
                        text = "Ticked tasks move to tomorrow. Unfinished ones are ticked for you.",
                        style = NcText.Body,
                        color = NcTheme.extras.muted,
                    )
                }
                todayTasks.forEach { task ->
                    val selected = task.id in kept
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                kept = if (selected) kept - task.id else kept + task.id
                            }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TaskCheck(selected)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = NcText.Body,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MicroLabel(if (task.done) "Done" else "Not finished")
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    MicroLabel(
                        text = "Keep all unfinished",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            kept = todayTasks.filter { !it.done }.map { it.id }.toSet()
                        },
                    )
                    MicroLabel(
                        text = "Clear",
                        modifier = Modifier.clickable { kept = emptySet() },
                    )
                }
                Hairline(Modifier.padding(horizontal = 20.dp))
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MicroLabel("New tasks for tomorrow")
                AddTaskField(label = "Add a task", onAdd = onAddTomorrow)
            }
            tomorrowTasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = task.title,
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDeleteTomorrow(task.id) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Delete task",
                            tint = NcTheme.extras.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            PrimaryButton(
                text = "Save plan",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onKeep(kept)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }
    }
}

private const val STUDY_TEMPLATE =
    "1. Revise the previous topic for 10 minutes\n" +
        "2. Read the new topic once, without notes\n" +
        "3. Work through the examples\n" +
        "4. Solve practice problems\n" +
        "5. Write a three-line summary"

/** Create or edit a study note. The note marked for the widget is the one the widget shows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    note: Note?,
    onSave: (title: String, body: String, pinned: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf(note?.title.orEmpty()) }
    var body by rememberSaveable { mutableStateOf(note?.body.orEmpty()) }
    var pinned by rememberSaveable { mutableStateOf(note?.pinned ?: false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NcTheme.extras.hairline),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MicroLabel(if (note == null) "New note" else "Edit note")
            NcTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                placeholder = "How I study",
            )
            NcTextField(
                value = body,
                onValueChange = { body = it },
                label = "Note",
                placeholder = "One step per line",
                singleLine = false,
                minLines = 6,
            )
            if (body.isBlank()) {
                SecondaryButton(
                    text = "Use a study routine template",
                    onClick = {
                        if (title.isBlank()) title = "Study routine"
                        body = STUDY_TEMPLATE
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Show on the widget",
                        style = NcText.Title,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "The widget shows one note, as its first page.",
                        style = NcText.Body,
                        color = NcTheme.extras.muted,
                    )
                }
                Switch(
                    checked = pinned,
                    onCheckedChange = { pinned = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                        checkedBorderColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedThumbColor = NcTheme.extras.muted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = NcTheme.extras.hairline,
                    ),
                )
            }
            PrimaryButton(
                text = "Save note",
                enabled = title.isNotBlank() || body.isNotBlank(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSave(title, body, pinned)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDelete != null) {
                SecondaryButton(
                    text = "Delete note",
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
