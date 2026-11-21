package com.example.nextclass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/**
 * Tap sheet for one class. Offers a permanent edit, and changes that apply to one date only:
 * cancel the class or use a different room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassActionSheet(
    session: ClassSession,
    dateLabel: String,
    override: ClassOverride?,
    onEdit: () -> Unit,
    onCancelledChange: (Boolean) -> Unit,
    onRoomForDate: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var room by rememberSaveable(session.id) { mutableStateOf(override?.room.orEmpty()) }
    val cancelled = override?.cancelled == true

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
            MicroLabel(dateLabel)
            Text(
                text = session.courseCode,
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = TimeFormat.range(session),
                style = NcText.Time,
                color = NcTheme.extras.muted,
            )

            SecondaryButton(
                text = "Edit class",
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            )

            Hairline()
            MicroLabel("Only on this date")

            SecondaryButton(
                text = if (cancelled) "Restore this class" else "Cancel this class",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCancelledChange(!cancelled)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            NcTextField(
                value = room,
                onValueChange = { room = it },
                label = "Room for this date",
                placeholder = session.room.ifBlank { "Room 304" },
            )
            PrimaryButton(
                text = "Use this room",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRoomForDate(room.trim())
                },
                enabled = room.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (override != null) {
                SecondaryButton(
                    text = "Reset to the normal routine",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReset()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
