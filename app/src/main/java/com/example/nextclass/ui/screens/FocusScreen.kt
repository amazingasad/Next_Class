package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.R
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.FocusSession
import com.example.nextclass.domain.model.StudyEntry
import com.example.nextclass.domain.model.StudyStats
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.components.CourseMark
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.PillSpacing
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.components.SelectPill
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

private val DURATIONS = listOf(25, 45, 60, 90)

@Composable
fun FocusScreen(
    courses: List<Course>,
    focus: FocusSession?,
    studyLog: List<StudyEntry>,
    today: LocalDate,
    onBack: () -> Unit,
    onStart: (String, String, Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(focus?.running) {
        while (focus?.running == true) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
        nowMs = System.currentTimeMillis()
    }
    val left = focus?.leftSec(nowMs) ?: 0
    LaunchedEffect(focus, left <= 0) {
        if (focus != null && focus.running && left <= 0) onFinish()
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
                text = "Study timer",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (focus == null) {
                IdlePicker(courses = courses, onStart = onStart)
            } else {
                Running(
                    focus = focus,
                    left = left,
                    onPause = onPause,
                    onResume = onResume,
                    onFinish = onFinish,
                    onDiscard = onDiscard,
                )
            }
            Hairline()
            StudyTotals(studyLog = studyLog, today = today)
        }
    }
}

@Composable
private fun IdlePicker(courses: List<Course>, onStart: (String, String, Int) -> Unit) {
    var pickedKey by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf(45) }
    val picked = courses.firstOrNull { it.key == pickedKey } ?: courses.firstOrNull()

    Text(
        text = "Pick a course and a length. The timer keeps running when you leave the app and tells you when it is done.",
        style = NcText.Body,
        color = NcTheme.extras.muted,
    )
    if (courses.isEmpty()) {
        Text(
            text = "Import a routine first, then your courses appear here.",
            style = NcText.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
        return
    }
    MicroLabel("Course")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = PillSpacing,
    ) {
        courses.forEach { course ->
            SelectPill(
                label = CourseCodes.pretty(course.code),
                selected = picked?.key == course.key,
                onClick = { pickedKey = course.key },
            )
        }
    }
    picked?.let { course ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            CourseMark(key = course.key)
            Spacer(Modifier.width(10.dp))
            Text(
                text = course.title.ifBlank { CourseCodes.pretty(course.code) },
                style = NcText.Body,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    MicroLabel("Length")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
        DURATIONS.forEach { option ->
            SelectPill(
                label = "$option min",
                selected = minutes == option,
                onClick = { minutes = option },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    PrimaryButton(
        text = "Start",
        enabled = picked != null,
        onClick = { picked?.let { onStart(it.key, CourseCodes.pretty(it.code), minutes) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Running(
    focus: FocusSession,
    left: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val fraction = if (focus.totalSec == 0) 0f else 1f - left.toFloat() / focus.totalSec
    Row(verticalAlignment = Alignment.CenterVertically) {
        CourseMark(key = focus.courseKey)
        Spacer(Modifier.width(10.dp))
        MicroLabel(
            text = focus.courseCode + if (focus.running) "" else "  Paused",
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Text(
        text = clockText(left),
        style = NcText.Display.copy(
            fontSize = 76.sp,
            lineHeight = 80.sp,
            letterSpacing = (-2).sp,
            fontFeatureSettings = "tnum",
        ),
        color = if (focus.running) NcTheme.extras.liveText else MaterialTheme.colorScheme.onSurface,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(NcTheme.extras.hairline),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(2.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
                .background(NcTheme.extras.live),
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
        SecondaryButton(
            text = if (focus.running) "Pause" else "Resume",
            onClick = if (focus.running) onPause else onResume,
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = "Finish",
            onClick = onFinish,
            modifier = Modifier.weight(1f),
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(onClick = onDiscard)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        MicroLabel("Discard without saving")
    }
}

@Composable
private fun StudyTotals(studyLog: List<StudyEntry>, today: LocalDate) {
    val weekStart = today.minusDays(6).toString()
    val week = studyLog.filter { it.date >= weekStart }
    val todayMinutes = studyLog.filter { it.date == today.toString() }.sumOf { it.minutes }
    val weekMinutes = StudyStats.total(week)
    val perCourse = StudyStats.perCourse(week)
    val top = perCourse.firstOrNull()?.second ?: 1

    MicroLabel("Logged")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text(
                text = TimeFormat.duration(todayMinutes),
                style = NcText.Headline.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
            )
            MicroLabel("Today")
        }
        Column {
            Text(
                text = TimeFormat.duration(weekMinutes),
                style = NcText.Headline.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
            )
            MicroLabel("Last 7 days")
        }
    }
    if (perCourse.isEmpty()) {
        Text(
            text = "Nothing logged yet. Finished sessions show up here by course.",
            style = NcText.Body,
            color = NcTheme.extras.muted,
        )
    }
    perCourse.forEach { (code, minutes) ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(code, style = NcText.Body, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = TimeFormat.duration(minutes),
                    style = NcText.Time,
                    color = NcTheme.extras.muted,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(NcTheme.extras.hairline),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(minutes.toFloat() / top)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }
}

private fun clockText(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
