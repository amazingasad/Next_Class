package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nextclass.R
import com.example.nextclass.domain.model.Breaks
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.domain.model.WeeklyReviewData
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcCard
import com.example.nextclass.ui.components.Reveal
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.LocalDate

@Composable
fun ReviewScreen(
    data: WeeklyReviewData,
    today: LocalDate,
    onBack: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenFocus: () -> Unit,
) {
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
                text = "Your week",
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
            item(key = "range") {
                MicroLabel(Breaks.shortDate(data.from) + " to " + Breaks.shortDate(data.to))
            }

            item(key = "attendance") {
                Reveal(index = 0) {
                    NcCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MicroLabel("Classes")
                            if (data.held == 0) {
                                Text(
                                    text = "No classes were held this week.",
                                    style = NcText.Body,
                                    color = NcTheme.extras.muted,
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                    Stat(data.present.toString(), "Present")
                                    Stat(data.absent.toString(), "Absent")
                                    Stat(data.unmarked.toString(), "Not marked")
                                }
                                if (data.unmarked > 0) {
                                    Hairline()
                                    Text(
                                        text = "Mark the rest so your percentage stays correct.",
                                        style = NcText.Body,
                                        color = NcTheme.extras.muted,
                                    )
                                    Text(
                                        text = "Open attendance",
                                        style = NcText.Body,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.clickable(onClick = onOpenAttendance),
                                    )
                                }
                            }
                            data.weakest?.let { weak ->
                                Hairline()
                                Text(
                                    text = weak.code + " is at " + (weak.percent ?: 0) + "%. " + weak.advice,
                                    style = NcText.Body,
                                    color = NcTheme.extras.liveText,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "tasks") {
                Reveal(index = 1) {
                    NcCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MicroLabel("Tasks")
                            if (data.tasksTotal == 0) {
                                Text(
                                    text = "No tasks this week.",
                                    style = NcText.Body,
                                    color = NcTheme.extras.muted,
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                    Stat(data.tasksDone.toString(), "Done")
                                    Stat((data.tasksTotal - data.tasksDone).toString(), "Left open")
                                }
                            }
                        }
                    }
                }
            }

            item(key = "study") {
                Reveal(index = 2) {
                    NcCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MicroLabel("Study time")
                            if (data.studyMinutes == 0) {
                                Text(
                                    text = "No study time logged. Start the study timer to track it.",
                                    style = NcText.Body,
                                    color = NcTheme.extras.muted,
                                )
                                Text(
                                    text = "Open study timer",
                                    style = NcText.Body,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable(onClick = onOpenFocus),
                                )
                            } else {
                                Stat(TimeFormat.duration(data.studyMinutes), "In total")
                                val top = data.studyByCourse.firstOrNull()?.second ?: 1
                                data.studyByCourse.take(5).forEach { (code, minutes) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
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
                        }
                    }
                }
            }

            data.nextBreak?.let { upcoming ->
                item(key = "break") {
                    Reveal(index = 3) {
                        NcCard {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MicroLabel("Break")
                                Text(upcoming.name, style = NcText.Title, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = Breaks.describe(upcoming, today),
                                    style = NcText.Body,
                                    color = NcTheme.extras.muted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = NcText.Headline.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
        MicroLabel(label)
    }
}

