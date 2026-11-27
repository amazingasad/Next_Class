package com.example.nextclass.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/** Suggested study blocks for the free time on the selected day. */
@Composable
fun StudySuggestionCard(
    plan: StudyPlan,
    dateKey: String,
    onAdd: (StudySlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (plan.slots.isEmpty()) return
    var added by remember(dateKey, plan.slots) { mutableStateOf(setOf<Int>()) }

    NcCard(modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            MicroLabel(text = "Study suggestions", color = NcTheme.extras.liveText)
            Text(
                text = plan.headline,
                style = NcText.Title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
            plan.slots.forEachIndexed { index, slot ->
                Hairline(Modifier.padding(top = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = slot.range,
                            style = NcText.Time,
                            color = NcTheme.extras.muted,
                        )
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
                            text = if (index in added) "Added" else "Add task",
                            enabled = index !in added,
                            onClick = {
                                added = added + index
                                onAdd(slot)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
