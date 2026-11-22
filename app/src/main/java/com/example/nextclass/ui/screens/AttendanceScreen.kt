package com.example.nextclass.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.R
import com.example.nextclass.domain.model.AttendanceMark
import com.example.nextclass.domain.model.AttendanceMath
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.CourseAttendance
import com.example.nextclass.domain.model.Occurrence
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcCard
import com.example.nextclass.ui.components.Reveal
import com.example.nextclass.ui.components.SelectPill
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val OCCURRENCE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private const val LOOKBACK_DAYS = 21

@Composable
fun AttendanceScreen(
    summary: List<CourseAttendance>,
    sessions: List<ClassSession>,
    overrides: List<ClassOverride>,
    marks: List<AttendanceMark>,
    now: LocalDateTime,
    onBack: () -> Unit,
    onMark: (ClassSession, LocalDate, Boolean?) -> Unit,
    onClearCourse: (String) -> Unit,
) {
    val occurrences = remember(sessions, overrides, now.toLocalDate(), now.hour, now.minute) {
        AttendanceMath.occurrences(sessions, overrides, now, LOOKBACK_DAYS).groupBy { it.courseKey }
    }
    val markIndex = remember(marks) {
        marks.associate { "${it.courseKey}|${it.date}|${it.startTime}" to it.present }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Attendance",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "intro") {
                Text(
                    text = "Tap a class on the home screen once it has started to mark it. Missed one earlier? Open a course below and mark it here. The line on each bar is the ${AttendanceMath.TARGET}% mark many universities ask for.",
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
            }

            if (summary.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Import a routine first, then your courses appear here.",
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            items(summary, key = { it.key }) { course ->
                Reveal(index = summary.indexOf(course)) {
                    CourseCard(
                        course = course,
                        occurrences = occurrences[course.key].orEmpty(),
                        markIndex = markIndex,
                        onMark = onMark,
                        onClear = { onClearCourse(course.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: CourseAttendance,
    occurrences: List<Occurrence>,
    markIndex: Map<String, Boolean>,
    onMark: (ClassSession, LocalDate, Boolean?) -> Unit,
    onClear: () -> Unit,
) {
    var open by rememberSaveable(course.key) { mutableStateOf(false) }
    val low = !course.meetsTarget
    val accent = if (low) NcTheme.extras.live else MaterialTheme.colorScheme.onSurface

    NcCard(modifier = Modifier.animateContentSize(tween(260, easing = FastOutSlowInEasing))) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = course.code,
                    style = NcText.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (course.title.isNotBlank()) {
                    Text(
                        text = course.title,
                        style = NcText.Body,
                        color = NcTheme.extras.muted,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = course.percent?.let { "$it%" } ?: "--",
                style = NcText.Display,
                color = accent,
            )
        }

        Spacer(Modifier.height(14.dp))
        ProgressBar(fraction = course.fraction, color = accent, showTarget = course.total > 0)

        Spacer(Modifier.height(10.dp))
        MicroLabel(
            text = "${course.present} present • ${course.absent} absent • ${course.total} total",
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = course.advice,
            style = NcText.Body,
            color = if (low) NcTheme.extras.live else MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MicroLabel(
                text = if (open) "Hide recent classes" else "Mark recent classes",
                color = MaterialTheme.colorScheme.onSurface,
            )
            MicroLabel("${occurrences.size} in 3 weeks")
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(240)),
            exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing)) + fadeOut(tween(160)),
        ) {
            Column(Modifier.padding(top = 8.dp)) {
                if (occurrences.isEmpty()) {
                    Text(
                        text = "No classes of this course in the last three weeks.",
                        style = NcText.Body,
                        color = NcTheme.extras.muted,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                occurrences.forEach { occurrence ->
                    OccurrenceRow(
                        occurrence = occurrence,
                        status = markIndex[occurrence.id],
                        onMark = { onMark(occurrence.session, occurrence.date, it) },
                    )
                }
                if (course.total > 0) {
                    Spacer(Modifier.height(8.dp))
                    MicroLabel(
                        text = "Clear all marks for this course",
                        modifier = Modifier
                            .clickable(onClick = onClear)
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OccurrenceRow(
    occurrence: Occurrence,
    status: Boolean?,
    onMark: (Boolean?) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = occurrence.date.format(OCCURRENCE_DATE),
                style = NcText.Title.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = TimeFormat.clock12(occurrence.session.startTime),
                style = NcText.Time,
                color = NcTheme.extras.muted,
            )
        }
        SelectPill(
            label = "Present",
            selected = status == true,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMark(if (status == true) null else true)
            },
            modifier = Modifier.width(92.dp),
        )
        Spacer(Modifier.width(6.dp))
        SelectPill(
            label = "Absent",
            selected = status == false,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMark(if (status == false) null else false)
            },
            modifier = Modifier.width(92.dp),
        )
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
    showTarget: Boolean,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "attendanceBar",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NcTheme.extras.hairline),
        )
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        if (showTarget) {
            Box(
                Modifier
                    .fillMaxWidth(AttendanceMath.TARGET / 100f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }
}
