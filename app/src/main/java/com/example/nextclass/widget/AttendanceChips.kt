package com.example.nextclass.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Two tap targets, Present and Absent. The chosen one is filled. */
@Composable
internal fun AttendanceChips(
    sessionId: Long,
    status: Boolean?,
    p: WidgetPalette,
    compact: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Chip("Present", true, sessionId, status == true, p, compact)
        Spacer(modifier = GlanceModifier.width(6.dp))
        Chip("Absent", false, sessionId, status == false, p, compact)
    }
}

@Composable
private fun Chip(
    label: String,
    present: Boolean,
    sessionId: Long,
    selected: Boolean,
    p: WidgetPalette,
    compact: Boolean,
) {
    Box(
        modifier = GlanceModifier
            .background(if (selected) p.ink else p.hairline)
            .cornerRadius(8.dp)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp)
            .clickable(
                actionRunCallback<MarkAttendanceAction>(
                    actionParametersOf(
                        MarkAttendanceAction.SESSION_ID to sessionId,
                        MarkAttendanceAction.PRESENT to present,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = if (selected) p.surface else p.ink,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
