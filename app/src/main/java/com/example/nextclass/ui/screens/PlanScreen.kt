package com.example.nextclass.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.PlannerTask
import com.example.nextclass.ui.components.AddTaskField
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcCard
import com.example.nextclass.ui.components.NoteEditorSheet
import com.example.nextclass.ui.components.PlanTomorrowSheet
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.components.SelectPill
import com.example.nextclass.ui.components.TaskRow
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHEET_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

@Composable
fun PlanScreen(
    tasks: List<PlannerTask>,
    notes: List<Note>,
    now: LocalDateTime,
    tab: Int,
    onTab: (Int) -> Unit,
    showPlanSheet: Boolean,
    onPlanSheet: (Boolean) -> Unit,
    onAddTask: (LocalDate, String) -> Unit,
    onToggleTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onKeepTasks: (Set<Long>, LocalDate) -> Unit,
    onSaveNote: (Long, String, String, Boolean) -> Unit,
    onDeleteNote: (Long) -> Unit,
) {
    val today = now.toLocalDate()
    val tomorrow = today.plusDays(1)
    val todayTasks = tasks.filter { it.date == today.toString() }
    val tomorrowTasks = tasks.filter { it.date == tomorrow.toString() }
    var dayChoice by rememberSaveable { mutableIntStateOf(0) }
    var editingNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creatingNote by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Plan",
                style = NcText.Display.copy(fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.2).sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectPill(
                    label = "To-do",
                    selected = tab == 0,
                    onClick = { onTab(0) },
                    modifier = Modifier.weight(1f),
                )
                SelectPill(
                    label = "Notes",
                    selected = tab == 1,
                    onClick = { onTab(1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = targetState > initialState
                (
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { w -> if (forward) w / 6 else -w / 6 } +
                        fadeIn(tween(240))
                    ) togetherWith (
                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 6 else w / 6 } +
                        fadeOut(tween(140))
                    )
            },
            label = "planTab",
            modifier = Modifier.weight(1f),
        ) { current ->
            if (current == 0) {
                val shown = if (dayChoice == 0) todayTasks else tomorrowTasks
                val shownDate = if (dayChoice == 0) today else tomorrow
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "days") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SelectPill(
                                label = "Today " + todayTasks.count { it.done } + "/" + todayTasks.size,
                                selected = dayChoice == 0,
                                onClick = { dayChoice = 0 },
                                modifier = Modifier.weight(1f),
                            )
                            SelectPill(
                                label = "Tomorrow " + tomorrowTasks.size,
                                selected = dayChoice == 1,
                                onClick = { dayChoice = 1 },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (shown.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = if (dayChoice == 0) {
                                    "No tasks for today. Add one below."
                                } else {
                                    "Nothing planned for tomorrow yet."
                                },
                                style = NcText.Body,
                                color = NcTheme.extras.muted,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }
                    }
                    items(shown, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "add") {
                        AddTaskField(
                            label = if (dayChoice == 0) "Add a task for today" else "Add a task for tomorrow",
                            onAdd = { onAddTask(shownDate, it) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                    item(key = "plan") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            NcCard {
                                MicroLabel("Every evening")
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Plan tomorrow, and keep the tasks you did not finish.",
                                    style = NcText.Body,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(
                                    text = "Plan tomorrow",
                                    onClick = { onPlanSheet(true) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "new") {
                        PrimaryButton(
                            text = "New note",
                            onClick = { creatingNote = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (notes.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = "Keep your study process here, for example: revise the previous topic before starting a new one. Pin one note and it shows on the widget.",
                                style = NcText.Body,
                                color = NcTheme.extras.muted,
                            )
                        }
                    }
                    items(notes, key = { it.id }) { note ->
                        NcCard(
                            modifier = Modifier
                                .animateItem()
                                .clickable { editingNoteId = note.id },
                        ) {
                            if (note.pinned) {
                                MicroLabel("On the widget", color = NcTheme.extras.liveText)
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(
                                text = note.title.ifBlank { "Untitled" },
                                style = NcText.Title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (note.lines.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = note.lines.take(3).joinToString("\n"),
                                    style = NcText.Body,
                                    color = NcTheme.extras.muted,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlanSheet) {
        PlanTomorrowSheet(
            dateLabel = tomorrow.format(SHEET_DATE),
            todayTasks = todayTasks,
            tomorrowTasks = tomorrowTasks,
            onAddTomorrow = { onAddTask(tomorrow, it) },
            onDeleteTomorrow = onDeleteTask,
            onKeep = { ids -> onKeepTasks(ids, tomorrow) },
            onDismiss = { onPlanSheet(false) },
        )
    }

    if (creatingNote) {
        NoteEditorSheet(
            note = null,
            onSave = { title, body, pinned -> onSaveNote(0L, title, body, pinned) },
            onDelete = null,
            onDismiss = { creatingNote = false },
        )
    }

    val editing = notes.firstOrNull { it.id == editingNoteId }
    if (editing != null) {
        NoteEditorSheet(
            note = editing,
            onSave = { title, body, pinned -> onSaveNote(editing.id, title, body, pinned) },
            onDelete = { onDeleteNote(editing.id) },
            onDismiss = { editingNoteId = null },
        )
    }
}
