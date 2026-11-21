package com.example.nextclass.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.DayNames
import com.example.nextclass.domain.model.SessionType
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/**
 * Edit an existing session, or add a new one when [initial] is null.
 * [onDelete] is shown only for existing sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionBottomSheet(
    initial: ClassSession?,
    defaultDay: Int,
    onSave: (ClassSession) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var day by rememberSaveable(initial?.id) { mutableStateOf(initial?.dayOfWeek ?: defaultDay) }
    var code by rememberSaveable(initial?.id) { mutableStateOf(initial?.courseCode.orEmpty()) }
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.courseTitle.orEmpty()) }
    var room by rememberSaveable(initial?.id) { mutableStateOf(initial?.room.orEmpty()) }
    var start by rememberSaveable(initial?.id) { mutableStateOf(initial?.startTime ?: "09:00") }
    var end by rememberSaveable(initial?.id) { mutableStateOf(initial?.endTime ?: "10:00") }
    var type by rememberSaveable(initial?.id) { mutableStateOf((initial?.type ?: SessionType.LECTURE).name) }
    var showErrors by rememberSaveable(initial?.id) { mutableStateOf(false) }

    val normalizedStart = TimeFormat.normalize(start)
    val normalizedEnd = TimeFormat.normalize(end)
    val timesOrdered = normalizedStart != null && normalizedEnd != null &&
        TimeFormat.toMinutes(normalizedEnd) > TimeFormat.toMinutes(normalizedStart)
    val valid = code.isNotBlank() && timesOrdered

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (initial == null) "Add class" else "Edit class",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )

            MicroLabel("Day")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                for (d in 1..7) {
                    SelectPill(
                        label = DayNames.short(d),
                        selected = day == d,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            day = d
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            NcTextField(
                value = code,
                onValueChange = { code = it },
                label = "Course code",
                placeholder = "CSE-2101",
                isError = showErrors && code.isBlank(),
            )
            NcTextField(
                value = title,
                onValueChange = { title = it },
                label = "Course title",
                placeholder = "Algorithms",
            )
            NcTextField(
                value = room,
                onValueChange = { room = it },
                label = "Room",
                placeholder = "Room 304",
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NcTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = "Start (24h)",
                    placeholder = "09:00",
                    keyboardType = KeyboardType.Text,
                    isError = showErrors && normalizedStart == null,
                    modifier = Modifier.weight(1f),
                )
                NcTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = "End (24h)",
                    placeholder = "10:50",
                    keyboardType = KeyboardType.Text,
                    isError = showErrors && !timesOrdered,
                    modifier = Modifier.weight(1f),
                )
            }

            MicroLabel("Type")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                SessionType.entries.forEach { option ->
                    SelectPill(
                        label = option.name,
                        selected = type == option.name,
                        onClick = { type = option.name },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (showErrors && !valid) {
                Text(
                    text = when {
                        code.isBlank() -> "Enter a course code."
                        normalizedStart == null || normalizedEnd == null -> "Use 24-hour times such as 09:00."
                        else -> "End time must be after the start time."
                    },
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            PrimaryButton(
                text = "Save",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!valid) {
                        showErrors = true
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSave(
                            ClassSession(
                                id = initial?.id ?: 0L,
                                dayOfWeek = day,
                                courseCode = code.trim().uppercase(),
                                courseTitle = title.trim(),
                                room = room.trim(),
                                startTime = normalizedStart.orEmpty(),
                                endTime = normalizedEnd.orEmpty(),
                                type = SessionType.fromRaw(type),
                            ),
                        )
                    }
                },
            )

            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "Delete class",
                        style = NcText.Title.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
