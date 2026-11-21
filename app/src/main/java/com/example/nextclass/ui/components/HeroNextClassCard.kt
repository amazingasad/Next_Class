package com.example.nextclass.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.NextClassState
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.domain.model.focusSession
import com.example.nextclass.domain.model.statusLabel
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/**
 * The one focal card: the class happening now or the next one. Flat surface, 1dp hairline,
 * and a 3dp status bar on the leading edge. No shadow and no nested cards.
 */
@Composable
fun HeroNextClassCard(
    state: NextClassState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, NcTheme.extras.hairline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            contentKey = { animationKey(it) },
            label = "heroState",
        ) { current ->
            HeroBody(current)
        }
    }
}

/** Animate when the kind of state or the session changes, not on every minute tick. */
private fun animationKey(state: NextClassState): String {
    val kind = when (state) {
        is NextClassState.Ongoing -> "ongoing"
        is NextClassState.Upcoming -> "upcoming"
        is NextClassState.NextDayPreview -> "preview"
        NextClassState.NoClasses -> "none"
    }
    return kind + ":" + (state.focusSession?.id ?: -1L)
}

@Composable
private fun HeroBody(state: NextClassState) {
    val extras = NcTheme.extras
    val live = state is NextClassState.Ongoing
    val accent = if (live) extras.live else extras.upcoming
    val session = state.focusSession

    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(accent)
                Spacer(Modifier.width(8.dp))
                MicroLabel(
                    text = state.statusLabel,
                    color = if (live) extras.liveText else extras.muted,
                )
            }

            if (session == null) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Nothing scheduled",
                    style = NcText.Headline,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Import a routine to see your next class here.",
                    style = NcText.Body,
                    color = extras.muted,
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = session.room.uppercase().ifBlank { "NO ROOM" },
                    style = NcText.Display,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = session.courseCode,
                    style = NcText.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (session.courseTitle.isNotBlank()) {
                    Text(
                        text = session.courseTitle,
                        style = NcText.Body,
                        color = extras.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = TimeFormat.range(session),
                        style = NcText.Time,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (state is NextClassState.Ongoing) {
                        Text(
                            text = "${TimeFormat.duration(state.minutesRemaining)} LEFT",
                            style = NcText.Time,
                            color = extras.liveText,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                if (state is NextClassState.Ongoing) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(extras.hairline),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(state.progressFraction)
                                .fillMaxHeight()
                                .background(extras.live),
                        )
                    }
                }
            }
        }
    }
}
