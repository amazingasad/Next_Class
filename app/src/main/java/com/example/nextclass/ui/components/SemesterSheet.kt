package com.example.nextclass.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.nextclass.domain.model.Semester
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

/** Switch, add, rename and delete semesters. Each semester keeps its own classes, courses and attendance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSheet(
    semesters: List<Semester>,
    onSwitch: (Long) -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by rememberSaveable { mutableStateOf("") }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

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
            MicroLabel("Semesters")
            Text(
                text = "Each semester keeps its own classes, courses and attendance. The active one drives the home screen and the widgets.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )

            semesters.forEach { semester ->
                SemesterRow(
                    semester = semester,
                    canDelete = semesters.size > 1,
                    renaming = renamingId == semester.id,
                    renameText = renameText,
                    confirmingDelete = confirmDeleteId == semester.id,
                    onRenameText = { renameText = it },
                    onPick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSwitch(semester.id)
                        onDismiss()
                    },
                    onStartRename = {
                        renamingId = semester.id
                        renameText = semester.name
                        confirmDeleteId = null
                    },
                    onSaveRename = {
                        onRename(semester.id, renameText)
                        renamingId = null
                    },
                    onDelete = {
                        if (confirmDeleteId == semester.id) {
                            onDelete(semester.id)
                            confirmDeleteId = null
                        } else {
                            confirmDeleteId = semester.id
                            renamingId = null
                        }
                    },
                )
            }

            Hairline()
            NcTextField(
                value = newName,
                onValueChange = { newName = it },
                label = "New semester",
                placeholder = "Spring 2027",
            )
            PrimaryButton(
                text = "Add and switch",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAdd(newName)
                    newName = ""
                    onDismiss()
                },
                enabled = newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SemesterRow(
    semester: Semester,
    canDelete: Boolean,
    renaming: Boolean,
    renameText: String,
    confirmingDelete: Boolean,
    onRenameText: (String) -> Unit,
    onPick: () -> Unit,
    onStartRename: () -> Unit,
    onSaveRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val border by animateColorAsState(
        targetValue = if (semester.active) colors.onSurface else NcTheme.extras.hairline,
        animationSpec = tween(220),
        label = "semesterBorder",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, border, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (semester.active) NcTheme.extras.live else NcTheme.extras.muted, CircleShape)
                    .padding(3.dp)
                    .background(
                        if (semester.active) NcTheme.extras.live else androidx.compose.ui.graphics.Color.Transparent,
                        CircleShape,
                    ),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = semester.name,
                style = NcText.Title,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (semester.active) MicroLabel("Active", color = NcTheme.extras.liveText)
        }
        if (renaming) {
            NcTextField(
                value = renameText,
                onValueChange = onRenameText,
                label = "Semester name",
                placeholder = semester.name,
            )
            SecondaryButton(
                text = "Save name",
                onClick = onSaveRename,
                enabled = renameText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MicroLabel(
                    text = "Rename",
                    color = colors.onSurface,
                    modifier = Modifier.clickable(onClick = onStartRename),
                )
                if (canDelete) {
                    MicroLabel(
                        text = if (confirmingDelete) "Tap again to delete everything" else "Delete",
                        color = if (confirmingDelete) NcTheme.extras.liveText else NcTheme.extras.muted,
                        modifier = Modifier.clickable(onClick = onDelete),
                    )
                }
            }
        }
    }
}
