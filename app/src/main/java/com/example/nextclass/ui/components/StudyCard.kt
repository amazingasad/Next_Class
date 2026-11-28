package com.example.nextclass.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.StudyPlan
import com.example.nextclass.domain.model.StudySlot
import com.example.nextclass.domain.model.TimeFormat
import kotlinx.coroutines.delay
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/** Suggested study blocks for the free time on the selected day. */
@Composable
fun StudySuggestionCard(
    plan: StudyPlan,
    dateKey: String,
    onAdd: (StudySlot) -> Unit,
    onStartTimer: () -> Unit,
    plannedTitles: List<String>,
    modifier: Modifier = Modifier,
) {
    if (plan.slots.isEmpty()) return
    var gone by remember(dateKey) { mutableStateOf(setOf<String>()) }
    var clicked by remember(dateKey) { mutableStateOf(setOf<String>()) }
    // A block that already became a task (earlier, or from another session) is not suggested again.
    LaunchedEffect(plannedTitles, plan.slots) {
        val planned = plan.slots
            .filter { slot ->
                val key = slotKey(slot)
                key !in clicked && plannedTitles.any { it.startsWith("Study " + slot.courseCode) }
            }
            .map { slotKey(it) }
        if (planned.isNotEmpty()) gone = gone + planned
    }
    val remaining = plan.slots.filter { slotKey(it) !in gone }

    AnimatedVisibility(
        visible = remaining.isNotEmpty(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        NcCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MicroLabel(text = "Study suggestions", color = NcTheme.extras.liveText)
                    MicroLabel(
                        text = "Start timer",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable(onClick = onStartTimer)
                            .padding(vertical = 4.dp),
                    )
                }
                Text(
                    text = plan.headline,
                    style = NcText.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp),
                )
                plan.slots.forEach { slot ->
                    val key = slotKey(slot)
                    StudyRow(
                        slot = slot,
                        visible = key !in gone,
                        onAdd = {
                            clicked = clicked + key
                            onAdd(slot)
                        },
                        onDone = { gone = gone + key },
                    )
                }
            }
        }
    }
}

private fun slotKey(slot: StudySlot): String = "${slot.startMinutes}-${slot.courseCode}"

@Composable
private fun StudyRow(
    slot: StudySlot,
    visible: Boolean,
    onAdd: () -> Unit,
    onDone: () -> Unit,
) {
    var added by remember { mutableStateOf(false) }
    LaunchedEffect(added) {
        if (added) {
            delay(650)
            onDone()
        }
    }
    AnimatedVisibility(
        visible = visible,
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            Hairline(Modifier.padding(top = 14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = slot.range, style = NcText.Time, color = NcTheme.extras.muted)
                    Text(
                        text = slot.courseCode + if (slot.courseTitle.isNotBlank()) "  " + slot.courseTitle else "",
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = TimeFormat.duration(slot.minutes) + ". " + slot.reason + ".",
                        style = NcText.Body,
                        color = NcTheme.extras.muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Column(Modifier.width(96.dp).padding(start = 12.dp)) {
                    SecondaryButton(
                        text = if (added) "Added" else "Add task",
                        enabled = !added,
                        onClick = {
                            if (!added) {
                                added = true
                                onAdd()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
