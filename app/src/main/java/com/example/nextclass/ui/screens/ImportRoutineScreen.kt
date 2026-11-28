package com.example.nextclass.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.width
import com.example.nextclass.domain.model.ScanReview
import com.example.nextclass.ui.components.StatusDot
import com.example.nextclass.ui.components.PillSpacing
import com.example.nextclass.ui.components.SelectPill
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nextclass.R
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.DayNames
import com.example.nextclass.domain.offline.GroupKey
import com.example.nextclass.ui.ImportUiState
import com.example.nextclass.ui.components.EditSessionBottomSheet
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.NcTextField
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.components.TimelineSessionRow
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

@Composable
fun ImportRoutineScreen(
    state: ImportUiState,
    hasApiKey: Boolean,
    scanMode: Int,
    onChooseGroup: (GroupKey) -> Unit,
    onBack: () -> Unit,
    onScan: (Uri, String) -> Unit,
    onUpdateDraft: (ClassSession) -> Unit,
    onAddDraft: (ClassSession) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = if (state is ImportUiState.Review) "Review classes" else "Import routine",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        when (state) {
            ImportUiState.Idle -> PickStep(
                hasApiKey = hasApiKey,
                scanMode = scanMode,
                onScan = onScan,
                onOpenSettings = onOpenSettings,
            )

            ImportUiState.Scanning -> ScanningStep()

            is ImportUiState.ChooseGroup -> ChooseGroupStep(groups = state.groups, onChoose = onChooseGroup)

            is ImportUiState.Failed -> FailedStep(message = state.message, onRetry = onReset)

            is ImportUiState.Review -> ReviewStep(
                sessions = state.sessions,
                source = state.source,
                onUpdateDraft = onUpdateDraft,
                onAddDraft = onAddDraft,
                onDeleteDraft = onDeleteDraft,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun PickStep(
    hasApiKey: Boolean,
    scanMode: Int,
    onScan: (Uri, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var pickedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var filter by rememberSaveable { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pickedUri = uri
    }
    val fileName = remember(pickedUri) { pickedUri?.let { displayName(context, it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = when {
                scanMode == 1 -> "Choose a PDF or a photo of your class routine. It is read on this phone with no internet, and you review the result before anything is saved."
                scanMode == 2 -> "Choose a PDF or a photo of your class routine. Gemini reads it and you review the result before anything is saved."
                hasApiKey -> "Choose a PDF or a photo of your class routine. Gemini reads it when you are online, otherwise it is read on this phone. You review the result before anything is saved."
                else -> "Choose a PDF or a photo of your class routine. It is read on this phone with no internet, and you review the result before anything is saved."
            },
            style = NcText.Body,
            color = NcTheme.extras.muted,
        )

        SecondaryButton(
            text = if (fileName == null) "Choose PDF or image" else "Change file",
            onClick = { launcher.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (fileName != null) {
            Column {
                MicroLabel("Selected file")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = fileName,
                    style = NcText.Time,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
        }

        NcTextField(
            value = filter,
            onValueChange = { filter = it },
            label = "Section or group (optional)",
            placeholder = "3rd year odd sem sec B",
        )

        if (!hasApiKey) {
            Text(
                text = if (scanMode == 2) {
                    "Add a Gemini API key before scanning. The guide shows how to get one for free."
                } else {
                    "Optional: a free Gemini key helps with blurry photos or unusual layouts."
                },
                style = NcText.Body,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SecondaryButton(
                text = "How to get a key",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PrimaryButton(
            text = "Scan routine",
            enabled = pickedUri != null && (hasApiKey || scanMode != 2),
            onClick = { pickedUri?.let { onScan(it, filter) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScanningStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MicroLabel("Reading routine")
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = NcTheme.extras.hairline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Reading your timetable. On this phone it takes a few seconds. Gemini usually takes 10 to 30.",
            style = NcText.Body,
            color = NcTheme.extras.muted,
        )
    }
}

@Composable
private fun ChooseGroupStep(groups: List<GroupKey>, onChoose: (GroupKey) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MicroLabel("Choose your section")
        Text(
            text = "This routine lists several sections. Tap yours and only its classes are imported.",
            style = NcText.Body,
            color = NcTheme.extras.muted,
        )
        groups.groupBy { it.year to it.semester }.forEach { (key, items) ->
            MicroLabel(
                text = "${items.first().label.substringBefore(',')}, ${key.second} semester",
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                items.forEach { group ->
                    SelectPill(
                        label = "Sec ${group.section}",
                        selected = false,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onChoose(group)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps pills the same width when a row has fewer sections.
                repeat((4 - items.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FailedStep(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MicroLabel("Could not read routine")
        Text(
            text = message,
            style = NcText.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PrimaryButton(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewStep(
    sessions: List<ClassSession>,
    source: String,
    onUpdateDraft: (ClassSession) -> Unit,
    onAddDraft: (ClassSession) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onSave: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var editing by remember { mutableStateOf<ClassSession?>(null) }
    var creating by remember { mutableStateOf(false) }
    var onlyFlagged by rememberSaveable { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val issues = remember(sessions) { ScanReview.issues(sessions) }
    // Leaves the filter when the last flagged row has been fixed, so the list never looks empty.
    val filtering = onlyFlagged && issues.isNotEmpty()
    val visible = if (filtering) sessions.filter { it.id in issues } else sessions

    Column(Modifier.fillMaxSize()) {
        Text(
            text = if (sessions.isEmpty()) {
                "No classes were found. Add one manually or go back and try another file."
            } else {
                "${sessions.size} classes found. Tap a row to edit, swipe left to delete." +
                    if (source.isNotBlank()) " $source" else ""
            },
            style = NcText.Body,
            color = NcTheme.extras.muted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        AnimatedVisibility(
            visible = issues.isNotEmpty(),
            enter = expandVertically(tween(260)) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(NcTheme.extras.live)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (issues.size == 1) {
                            "1 row needs a check. The reader was unsure about it."
                        } else {
                            "${issues.size} rows need a check. The reader was unsure about them."
                        },
                        style = NcText.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                    SelectPill(
                        label = "All rows",
                        selected = !filtering,
                        onClick = { onlyFlagged = false },
                        modifier = Modifier.weight(1f),
                    )
                    SelectPill(
                        label = "Needs check",
                        selected = filtering,
                        onClick = { onlyFlagged = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            val byDay = visible.groupBy { it.dayOfWeek }.toSortedMap()
            byDay.forEach { (day, daySessions) ->
                item(key = "day-$day") {
                    MicroLabel(
                        text = DayNames.full(day),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                    Hairline(Modifier.padding(horizontal = 20.dp))
                }
                items(daySessions, key = { it.id }) { session ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteDraft(session.id)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.onSurface),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                MicroLabel(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                    ) {
                        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                            TimelineSessionRow(
                                session = session,
                                isLive = false,
                                flag = issues[session.id],
                                onClick = { editing = session },
                            )
                        }
                    }
                    Hairline(Modifier.padding(horizontal = 20.dp))
                }
            }
        }

        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp + navBottom),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(
                text = "Add class",
                onClick = { creating = true },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "Save routine",
                enabled = sessions.isNotEmpty(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSave()
                },
                modifier = Modifier.weight(1.4f),
            )
        }
    }

    editing?.let { target ->
        EditSessionBottomSheet(
            initial = target,
            defaultDay = target.dayOfWeek,
            onSave = {
                onUpdateDraft(it)
                editing = null
            },
            onDelete = {
                onDeleteDraft(target.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (creating) {
        EditSessionBottomSheet(
            initial = null,
            defaultDay = sessions.firstOrNull()?.dayOfWeek ?: 1,
            onSave = {
                onAddDraft(it)
                creating = false
            },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
}

private fun displayName(context: Context, uri: Uri): String {
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
    } catch (e: SecurityException) {
        // Fall through to the URI's last segment.
    }
    return uri.lastPathSegment ?: "Selected file"
}
