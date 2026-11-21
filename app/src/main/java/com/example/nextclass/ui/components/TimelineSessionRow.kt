package com.example.nextclass.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/**
 * One typographic schedule row: times on the left, course in the middle, room on the right.
 * No card, no shadow. The caller draws the hairline between rows.
 */
@Composable
fun TimelineSessionRow(
    session: ClassSession,
    isLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cancelled: Boolean = false,
    roomOverride: String? = null,
) {
    val extras = NcTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(80.dp)) {
            Text(
                text = TimeFormat.clock12(session.startTime),
                style = NcText.Time,
                color = if (cancelled) extras.muted else onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = TimeFormat.clock12(session.endTime),
                style = NcText.Time,
                color = extras.muted,
                maxLines = 1,
                softWrap = false,
            )
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.courseCode,
                    style = NcText.Title.copy(
                        textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                    ),
                    color = if (cancelled) extras.muted else onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isLive) {
                    Spacer(Modifier.width(8.dp))
                    StatusDot(extras.live)
                }
            }
            if (session.courseTitle.isNotBlank()) {
                Text(
                    text = session.courseTitle,
                    style = NcText.Body,
                    color = extras.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            MicroLabel(
                text = when {
                    cancelled -> "Cancelled"
                    !roomOverride.isNullOrBlank() -> "Room changed \u2022 ${session.type.name}"
                    else -> session.type.name
                },
                color = if (cancelled) extras.live else extras.muted,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = if (!roomOverride.isNullOrBlank()) roomOverride else session.room,
            style = NcText.Time,
            color = if (cancelled) extras.muted else onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 112.dp),
        )
    }
}
