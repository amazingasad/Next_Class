package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.example.nextclass.domain.model.Semester
import com.example.nextclass.ui.components.SemesterSheet
import com.example.nextclass.ui.components.StatusDot
import com.example.nextclass.ui.components.TimelineRail
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nextclass.R
import com.example.nextclass.data.remote.UpdateState
import com.example.nextclass.domain.model.AttendanceMark
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.StudyPlanner
import com.example.nextclass.ui.components.StudySuggestionCard
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
private val LONG_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

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
    onCourses: () -> Unit,
    onWeek: () -> Unit,
    semesters: List<Semester>,
    onSwitchSemester: (Long) -> Unit,
    onAddSemester: (String) -> Unit,
    onRenameSemester: (Long, String) -> Unit,
    onDeleteSemester: (Long) -> Unit,
    hasApiKey: Boolean,
    onKeyGuide: () -> Unit,
    onCancelledChange: (ClassSession, LocalDate, Boolean) -> Unit,
    onRoomForDate: (ClassSession, LocalDate, String) -> Unit,
    onResetOverride: (ClassSession, LocalDate) -> Unit,
    onSaveSession: (ClassSession) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onRequestNotifications: () -> Unit,
    marks: List<AttendanceMark>,
    onMark: (ClassSession, LocalDate, Boolean?) -> Unit,
    update: UpdateState.Available?,
    onOpenUpdate: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    showBatteryNotice: Boolean,
    onRequestBattery: () -> Unit,
    onHideBatteryNotice: () -> Unit,
    courses: List<Course>,
    onAddStudyTask: (LocalDate, String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var editing by remember { mutableStateOf<ClassSession?>(null) }
    var creating by remember { mutableStateOf(false) }
    var acting by remember { mutableStateOf<ClassSession?>(null) }
    var showSemesters by remember { mutableStateOf(false) }

    val today = now.dayOfWeek.value
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopBar(
            now = now,
            semesterName = semesters.firstOrNull { it.active }?.name ?: "Semester 1",
            onSemester = { showSemesters = true },
            onImport = onImport,
            onCourses = onCourses,
            onWeek = onWeek,
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

            if (update != null) {
                item(key = "update-notice") {
                    NoticeRow(
                        text = "A newer version of NextClass is ready.",
                        action = "Download",
                        secondary = "Later",
                        onAction = { onOpenUpdate(update.url) },
                        onSecondary = onDismissUpdate,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (showBatteryNotice && sessions.isNotEmpty()) {
                item(key = "battery-notice") {
                    NoticeRow(
                        text = "Let NextClass run in the background so the widget and reminder stay on time.",
                        action = "Allow",
                        secondary = "Hide",
                        onAction = onRequestBattery,
                        onSecondary = onHideBatteryNotice,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (!hasApiKey) {
                item(key = "key-notice") {
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .clickable(onClick = onKeyGuide)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Add a free Gemini key to import your routine.",
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                            modifier = Modifier.weight(1f),
                        )
                        MicroLabel(
                            text = "Guide",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
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
                        is DayEntry.Row -> Column(Modifier.animateItem()) {
                            val cancelled = entry.override?.cancelled == true
                            TimelineSessionRow(
                                session = entry.session,
                                isLive = entry.session.id == liveId,
                                cancelled = cancelled,
                                roomOverride = if (cancelled) null else entry.override?.room,
                                attendance = if (selectedDate == now.toLocalDate()) {
                                    markFor(marks, entry.session, selectedDate)
                                } else {
                                    null
                                },
                                onClick = { acting = entry.session },
                            )
                        }

                        is DayEntry.Gap -> Column(Modifier.animateItem()) { GapRow(entry.minutes) }
                    }
                }

                item(key = "study") {
                    val isToday = selectedDate == now.toLocalDate()
                    val nowMin = if (isToday) now.hour * 60 + now.minute else null
                    val plan = remember(sessions, overrides, courses, selectedDate, nowMin) {
                        StudyPlanner.plan(selectedDate, sessions, overrides, courses, nowMin)
                    }
                    StudySuggestionCard(
                        plan = plan,
                        dateKey = selectedDate.toString(),
                        onAdd = { slot -> onAddStudyTask(selectedDate, slot.taskTitle) },
                        modifier = Modifier.animateItem(),
                    )
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

    if (showSemesters) {
        SemesterSheet(
            semesters = semesters,
            onSwitch = onSwitchSemester,
            onAdd = onAddSemester,
            onRename = onRenameSemester,
            onDelete = onDeleteSemester,
            onDismiss = { showSemesters = false },
        )
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
            canMark = actDate == now.toLocalDate() &&
                target.startMinutes <= now.hour * 60 + now.minute &&
                current?.cancelled != true,
            attendance = markFor(marks, target, actDate),
            onMark = { onMark(target, actDate, it) },
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

private fun markFor(marks: List<AttendanceMark>, session: ClassSession, date: LocalDate): Boolean? {
    val key = CourseCodes.key(session.courseCode)
    val dateText = date.toString()
    return marks.firstOrNull {
        it.courseKey == key && it.date == dateText && it.startTime == session.startTime
    }?.present
}

@Composable
private fun NoticeRow(
    text: String,
    action: String,
    secondary: String,
    onAction: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = NcText.Body,
            color = NcTheme.extras.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MicroLabel(
                text = action,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onAction),
            )
            MicroLabel(
                text = secondary,
                modifier = Modifier.clickable(onClick = onSecondary),
            )
        }
    }
    Hairline(Modifier.padding(horizontal = 20.dp))
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
            .height(IntrinsicSize.Min)
            .padding(start = 20.dp, end = 20.dp),
    ) {
        Spacer(Modifier.width(72.dp))
        TimelineRail(showNode = false)
        Box(Modifier.padding(start = 8.dp, top = 10.dp, bottom = 10.dp)) {
            MicroLabel(text = "${TimeFormat.duration(minutes)} free")
        }
    }
}

@Composable
private fun TopBar(
    now: LocalDateTime,
    semesterName: String,
    onSemester: () -> Unit,
    onImport: () -> Unit,
    onCourses: () -> Unit,
    onWeek: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val chipShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(chipShape)
                    .border(1.dp, NcTheme.extras.hairline, chipShape)
                    .clickable(onClick = onSemester)
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(NcTheme.extras.live)
                Spacer(Modifier.width(8.dp))
                MicroLabel(text = semesterName, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = NcTheme.extras.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onWeek) {
                Icon(
                    imageVector = Icons.Rounded.CalendarViewWeek,
                    contentDescription = stringResource(R.string.cd_week_open),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text("Import routine", style = NcText.Body) },
                        leadingIcon = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onImport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Courses", style = NcText.Body) },
                        leadingIcon = { Icon(Icons.Rounded.Book, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onCourses()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = DayNames.full(now.dayOfWeek.value),
            style = NcText.Display.copy(fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-1.5).sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        MicroLabel(text = now.format(LONG_DATE))
    }
}
