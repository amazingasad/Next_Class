package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.BreakPeriod
import com.example.nextclass.domain.model.Breaks
import com.example.nextclass.domain.model.FocusSession
import com.example.nextclass.domain.model.StudyEntry
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.domain.model.WeeklyReviewData
import com.example.nextclass.ui.components.BreaksSheet
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.Reveal
import com.example.nextclass.ui.components.pressScale
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.LocalDate

/** One place for everything that is not the day view: study tools and routine tools. */
@Composable
fun ToolsScreen(
    focus: FocusSession?,
    studyLog: List<StudyEntry>,
    review: WeeklyReviewData,
    breaks: List<BreakPeriod>,
    courseCount: Int,
    today: LocalDate,
    onFocus: () -> Unit,
    onReview: () -> Unit,
    onWeek: () -> Unit,
    onCourses: () -> Unit,
    onImport: () -> Unit,
    onAddBreak: (String, LocalDate, LocalDate) -> Unit,
    onDeleteBreak: (Long) -> Unit,
) {
    var showBreaks by remember { mutableStateOf(false) }
    val studiedToday = studyLog.filter { it.date == today.toString() }.sumOf { it.minutes }

    val timerDetail = when {
        focus == null -> if (studiedToday > 0) {
            TimeFormat.duration(studiedToday) + " studied today. Start another session."
        } else {
            "Pick a course, set a length and log the time."
        }
        focus.running -> {
            val left = (focus.leftSec(System.currentTimeMillis()) + 59) / 60
            focus.courseCode + " in progress, " + TimeFormat.duration(left) + " left."
        }
        else -> {
            val left = (focus.leftSec(System.currentTimeMillis()) + 59) / 60
            focus.courseCode + " paused, " + TimeFormat.duration(left) + " left."
        }
    }
    val upcoming = Breaks.current(today, breaks, 30)
    val breakDetail = when {
        upcoming != null -> upcoming.name + ". " + Breaks.describe(upcoming, today) + "."
        breaks.isNotEmpty() -> "No break coming up soon."
        else -> "Mark holidays so classes and attendance skip them."
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
            MicroLabel("NextClass")
            Text(
                text = "Tools",
                style = NcText.Display,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Study, review and routine settings in one place.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Reveal(index = 0) {
            Section(label = "Study") {
                ToolRow(
                    icon = Icons.Rounded.Timer,
                    title = "Study timer",
                    detail = timerDetail,
                    active = focus != null,
                    onClick = onFocus,
                )
                Hairline()
                ToolRow(
                    icon = Icons.Rounded.Insights,
                    title = "Weekly review",
                    detail = review.summary,
                    onClick = onReview,
                )
            }
        }

        Reveal(index = 1) {
            Section(label = "Routine") {
                ToolRow(
                    icon = Icons.Rounded.EventBusy,
                    title = "Breaks and holidays",
                    detail = breakDetail,
                    active = Breaks.on(today, breaks) != null,
                    onClick = { showBreaks = true },
                )
                Hairline()
                ToolRow(
                    icon = Icons.Rounded.CalendarViewWeek,
                    title = "Week view",
                    detail = "Your whole routine at a glance. Tap a day to open it.",
                    onClick = onWeek,
                )
                Hairline()
                ToolRow(
                    icon = Icons.Rounded.Book,
                    title = "Courses",
                    detail = if (courseCount == 0) "Add course codes and titles." else "$courseCount in this semester.",
                    onClick = onCourses,
                )
                Hairline()
                ToolRow(
                    icon = Icons.Rounded.UploadFile,
                    title = "Import routine",
                    detail = "Scan a timetable image or PDF.",
                    onClick = onImport,
                )
            }
        }
    }

    if (showBreaks) {
        BreaksSheet(
            breaks = breaks,
            today = today,
            onAdd = onAddBreak,
            onDelete = onDeleteBreak,
            onDismiss = { showBreaks = false },
        )
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        MicroLabel(text = label, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, NcTheme.extras.hairline, shape),
        ) {
            content()
        }
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val ring = if (active) NcTheme.extras.live else NcTheme.extras.hairline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .border(1.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) NcTheme.extras.liveText else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = NcText.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = NcText.Body,
                color = NcTheme.extras.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = NcTheme.extras.muted,
        )
    }
}
