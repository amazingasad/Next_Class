@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.nextclass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.BreakPeriod
import com.example.nextclass.domain.model.Breaks
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class PickTarget { START, END }

/** Lists the saved breaks and lets the user add or remove one. Classes are skipped on those dates. */
@Composable
fun BreaksSheet(
    breaks: List<BreakPeriod>,
    today: LocalDate,
    onAdd: (String, LocalDate, LocalDate) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today) }
    var end by remember { mutableStateOf(today) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NcTheme.extras.hairline),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MicroLabel("Breaks")
            Text(
                text = "Days with no classes",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Classes are hidden on these dates, attendance is not counted, and study suggestions treat them as off days.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )

            if (breaks.isEmpty()) {
                Text(
                    text = "No breaks yet.",
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
            }
            breaks.forEach { item ->
                Hairline()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = NcText.Title, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = Breaks.describe(item, today),
                            style = NcText.Body,
                            color = NcTheme.extras.muted,
                        )
                    }
                    Box(
                        Modifier
                            .clickable { onDelete(item.id) }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        MicroLabel("Remove", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Hairline()
            MicroLabel("Add a break")
            NcTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                placeholder = "Eid break, exam week",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                SecondaryButton(
                    text = "From " + Breaks.shortDate(start),
                    onClick = { picking = PickTarget.START },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "To " + Breaks.shortDate(end),
                    onClick = { picking = PickTarget.END },
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryButton(
                text = "Add break",
                enabled = name.isNotBlank() && !end.isBefore(start),
                onClick = {
                    onAdd(name.trim(), start, end)
                    name = ""
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    picking?.let { target ->
        val initial = (if (target == PickTarget.START) start else end)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            if (target == PickTarget.START) {
                                start = picked
                                if (end.isBefore(picked)) end = picked
                            } else {
                                end = picked
                                if (start.isAfter(picked)) start = picked
                            }
                        }
                        picking = null
                    },
                ) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
