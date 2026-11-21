package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nextclass.R
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.DayNames
import com.example.nextclass.domain.model.NextClassState
import com.example.nextclass.domain.model.RoutineOverrides
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.components.ClassActionSheet
import com.example.nextclass.ui.components.EditSessionBottomSheet
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.HeroNextClassCard
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.PillSpacing
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.components.SelectPill
import com.example.nextclass.ui.components.TimelineSessionRow
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

@Composable
fun HomeScreen(
    sessions: List<ClassSession>?,
    overrides: List<ClassOverride>,
    hero: NextClassState,
    now: LocalDateTime,
    selectedDay: Int,
    showNotificationNotice: Boolean,
    onSelectDay: (Int) -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit,
    onCourses: () -> Unit,
    onCancelledChange: (ClassSession, LocalDate, Boolean) -> Unit,
    onRoomForDate: (ClassSession, LocalDate, String) -> Unit,
    onResetOverride: (ClassSession, LocalDate) -> Unit,
    onSaveSession: (ClassSession) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var editing by remember { mutableStateOf<ClassSession?>(null) }
    var creating by remember { mutableStateOf(false) }
    var acting by remember { mutableStateOf<ClassSession?>(null) }

    val today = now.dayOfWeek.value
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopBar(
            dateLabel = now.format(DATE_LABEL),
            onImport = onImport,
            onCourses = onCourses,
            onSettings = onSettings,
        )

        if (sessions == null) return@Column

        val daySessions = sessions.filter { it.dayOfWeek == selectedDay }
            .sortedBy { it.startTime }
        val liveId = (hero as? NextClassState.Ongoing)?.session?.id
        val selectedDate = RoutineOverrides.nextDate(selectedDay, now.toLocalDate())
        val entries = buildDayEntries(daySessions, overrides, selectedDate)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = navBottom + 24.dp),
        ) {
            item(key = "hero") {
                HeroNextClassCard(
                    state = hero,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            if (showNotificationNotice && sessions.isNotEmpty()) {
                item(key = "notice") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRequestNotifications)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Allow notifications for the first-class reminder.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                            modifier = Modifier.weight(1f),
                        )
                        MicroLabel(
                            text = "Allow",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            item(key = "days") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = PillSpacing,
                ) {
                    for (day in 1..7) {
                        SelectPill(
                            label = DayNames.short(day),
                            selected = selectedDay == day,
                            marker = today == day,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectDay(day)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (sessions.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "No routine yet",
                            style = NcText.Headline,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Import a PDF or photo of your class routine and the widget will follow it.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                        PrimaryButton(
                            text = "Import routine",
                            onClick = onImport,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SecondaryButton(
                            text = "Add a class manually",
                            onClick = { creating = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                item(key = "header") {
                    val count = entries.count { it is DayEntry.Row && it.override?.cancelled != true }
                    MicroLabel(
                        text = "${DayNames.full(selectedDay)} • $count ${if (count == 1) "class" else "classes"}",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    Hairline(Modifier.padding(horizontal = 20.dp))
                }

                items(
                    entries,
                    key = { entry ->
                        when (entry) {
                            is DayEntry.Row -> "s${entry.session.id}"
                            is DayEntry.Gap -> entry.key
                        }
                    },
                ) { entry ->
                    when (entry) {
                        is DayEntry.Row -> {
                            val cancelled = entry.override?.cancelled == true
                            TimelineSessionRow(
                                session = entry.session,
                                isLive = entry.session.id == liveId,
                                cancelled = cancelled,
                                roomOverride = if (cancelled) null else entry.override?.room,
                                onClick = { acting = entry.session },
                            )
                            Hairline(Modifier.padding(horizontal = 20.dp))
                        }

                        is DayEntry.Gap -> GapRow(entry.minutes)
                    }
                }

                if (daySessions.isEmpty()) {
                    item(key = "none-today") {
                        Text(
                            text = "No classes on ${DayNames.full(selectedDay)}.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }

                item(key = "add") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { creating = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        MicroLabel(
                            text = "Add class",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    editing?.let { target ->
        EditSessionBottomSheet(
            initial = target,
            defaultDay = target.dayOfWeek,
            onSave = {
                onSaveSession(it)
                editing = null
            },
            onDelete = {
                onDeleteSession(target.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    acting?.let { target ->
        val actDate = RoutineOverrides.nextDate(target.dayOfWeek, now.toLocalDate())
        val current = overrides.firstOrNull { it.sessionId == target.id && it.date == actDate.toString() }
        ClassActionSheet(
            session = target,
            dateLabel = actDate.format(DATE_LABEL),
            override = current,
            onEdit = {
                editing = target
                acting = null
            },
            onCancelledChange = {
                onCancelledChange(target, actDate, it)
                acting = null
            },
            onRoomForDate = {
                onRoomForDate(target, actDate, it)
                acting = null
            },
            onReset = {
                onResetOverride(target, actDate)
                acting = null
            },
            onDismiss = { acting = null },
        )
    }

    if (creating) {
        EditSessionBottomSheet(
            initial = null,
            defaultDay = selectedDay,
            onSave = {
                onSaveSession(it)
                creating = false
            },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
}

private sealed interface DayEntry {
    data class Row(val session: ClassSession, val override: ClassOverride?) : DayEntry
    data class Gap(val key: String, val minutes: Int) : DayEntry
}

private const val MIN_GAP_MINUTES = 20

/** Rows for one day with a "free" gap inserted between classes that are far enough apart. */
private fun buildDayEntries(
    sessions: List<ClassSession>,
    overrides: List<ClassOverride>,
    date: LocalDate,
): List<DayEntry> {
    val dateText = date.toString()
    val entries = ArrayList<DayEntry>()
    var previousEnd: Int? = null
    for (session in sessions) {
        val override = overrides.firstOrNull { it.sessionId == session.id && it.date == dateText }
        if (override?.cancelled != true) {
            val before = previousEnd
            if (before != null && session.startMinutes - before >= MIN_GAP_MINUTES) {
                entries += DayEntry.Gap("gap-${session.id}", session.startMinutes - before)
            }
            previousEnd = maxOf(before ?: 0, session.endMinutes)
        }
        entries += DayEntry.Row(session, override)
    }
    return entries
}

@Composable
private fun GapRow(minutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 100.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
    ) {
        MicroLabel(text = "${TimeFormat.duration(minutes)} free")
    }
}

@Composable
private fun TopBar(
    dateLabel: String,
    onImport: () -> Unit,
    onCourses: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            MicroLabel(dateLabel)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onImport) {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = stringResource(R.string.cd_import_routine),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onCourses) {
            Icon(
                imageVector = Icons.Rounded.Book,
                contentDescription = stringResource(R.string.cd_courses),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
