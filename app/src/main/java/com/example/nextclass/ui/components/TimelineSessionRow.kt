package com.example.nextclass.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/** Left edge of the timeline rail, measured from the screen edge. */
val RailStart = 92.dp
private val RailWidth = 24.dp
private val NodeY = 23.dp

/**
 * A vertical rail with a node. The live class gets a pulsing vermilion node. Rows stack with no
 * dividers so the rail reads as one continuous line down the day.
 */
@Composable
fun TimelineRail(
    modifier: Modifier = Modifier,
    live: Boolean = false,
    cancelled: Boolean = false,
    showNode: Boolean = true,
) {
    val extras = NcTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface
    val pulse = rememberInfiniteTransition(label = "railPulse")
    val t by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "railPulseValue",
    )
    Canvas(modifier.width(RailWidth).fillMaxHeight()) {
        val x = size.width / 2f
        drawLine(
            color = extras.hairline,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        if (!showNode) return@Canvas
        val center = Offset(x, NodeY.toPx())
        when {
            live -> {
                drawCircle(
                    color = extras.live.copy(alpha = (1f - t) * 0.35f),
                    radius = 5.dp.toPx() + 7.dp.toPx() * t,
                    center = center,
                )
                drawCircle(color = extras.live, radius = 5.dp.toPx(), center = center)
            }
            cancelled -> drawCircle(
                color = extras.muted,
                radius = 4.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            else -> {
                drawCircle(color = onSurface, radius = 4.dp.toPx(), center = center)
            }
        }
    }
}

/**
 * One schedule row: times on the left, a rail, then course and room. No card, no shadow.
 */
@Composable
fun TimelineSessionRow(
    session: ClassSession,
    isLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cancelled: Boolean = false,
    roomOverride: String? = null,
    attendance: Boolean? = null,
    flag: String? = null,
) {
    val extras = NcTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier
                .width(72.dp)
                .padding(vertical = 14.dp),
        ) {
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

        TimelineRail(live = isLive, cancelled = cancelled)

        Column(
            Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CourseMark(
                    key = com.example.nextclass.domain.model.CourseCodes.key(session.courseCode),
                    markSize = 12.dp,
                    color = if (cancelled) extras.muted else onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = session.courseCode,
                    style = NcText.Title.copy(
                        textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                    ),
                    color = if (cancelled) extras.muted else onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    !roomOverride.isNullOrBlank() -> "Room changed • ${session.type.name}"
                    attendance == true -> "${session.type.name} • Present"
                    attendance == false -> "${session.type.name} • Absent"
                    else -> session.type.name
                },
                color = if (cancelled) extras.live else extras.muted,
            )
            if (flag != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(extras.live)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = flag,
                        style = NcText.Time,
                        color = extras.live,
                    )
                }
            }
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
            modifier = Modifier
                .padding(vertical = 14.dp)
                .widthIn(max = 100.dp),
        )
    }
}
