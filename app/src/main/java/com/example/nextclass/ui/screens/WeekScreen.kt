package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.R
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.DayNames
import com.example.nextclass.domain.model.RoutineOverrides
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcCard
import com.example.nextclass.ui.components.Reveal
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.LocalDateTime

/** The whole week at a glance. Tap a day to open it on the home screen. */
@Composable
fun WeekScreen(
    sessions: List<ClassSession>?,
    overrides: List<ClassOverride>,
    now: LocalDateTime,
    onBack: () -> Unit,
    onOpenDay: (Int) -> Unit,
) {
    val today = now.dayOfWeek.value
    val nowMinutes = now.hour * 60 + now.minute

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
                text = "This week",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sessions != null) {
                for (day in 1..7) {
                    val date = RoutineOverrides.nextDate(day, now.toLocalDate())
                    val dateText = date.toString()
                    val rows = sessions
                        .filter { it.dayOfWeek == day }
                        .sortedWith(compareBy({ it.startMinutes }, { it.endMinutes }))
                        .map { session ->
                            session to overrides.firstOrNull { it.sessionId == session.id && it.date == dateText }
                        }
                    val activeCount = rows.count { it.second?.cancelled != true }
                    val isToday = day == today

                    Reveal(index = day - 1) {
                        NcCard(
                            modifier = Modifier.clickable { onOpenDay(day) },
                            borderColor = if (isToday) NcTheme.extras.live else NcTheme.extras.hairline,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = DayNames.full(day),
                                    style = NcText.Title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (isToday) {
                                    Spacer(Modifier.width(8.dp))
                                    MicroLabel("Today", color = NcTheme.extras.liveText)
                                }
                                Spacer(Modifier.weight(1f))
                                MicroLabel("$activeCount ${if (activeCount == 1) "class" else "classes"}")
                            }

                            if (rows.isEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Free day",
                                    style = NcText.Body,
                                    color = NcTheme.extras.muted,
                                )
                            } else {
                                Spacer(Modifier.height(10.dp))
                                Hairline()
                                rows.forEachIndexed { index, (session, override) ->
                                    val cancelled = override?.cancelled == true
                                    val finished = isToday && session.endMinutes <= nowMinutes
                                    val room = override?.room?.takeIf { it.isNotBlank() && !cancelled } ?: session.room
                                    WeekRow(
                                        session = session,
                                        room = room,
                                        cancelled = cancelled,
                                        modifier = Modifier.alpha(if (finished || cancelled) 0.5f else 1f),
                                    )
                                    if (index != rows.lastIndex) Hairline()
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeekRow(
    session: ClassSession,
    room: String,
    cancelled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(80.dp)) {
            Text(
                text = TimeFormat.clock12(session.startTime),
                style = NcText.Time,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = TimeFormat.clock12(session.endTime),
                style = NcText.Time,
                color = NcTheme.extras.muted,
                maxLines = 1,
                softWrap = false,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = session.courseCode,
                style = NcText.Title.copy(
                    fontSize = 15.sp,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (session.courseTitle.isNotBlank()) {
                Text(
                    text = session.courseTitle,
                    style = NcText.Body.copy(fontSize = 13.sp),
                    color = NcTheme.extras.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (cancelled) MicroLabel("Cancelled", color = NcTheme.extras.live)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = room,
            style = NcText.Time,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
    }
}
