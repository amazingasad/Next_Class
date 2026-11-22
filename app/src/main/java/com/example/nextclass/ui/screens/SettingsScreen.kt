package com.example.nextclass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextclass.R
import android.os.Build
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.remote.UpdateState
import com.example.nextclass.ui.SettingsUiState
import com.example.nextclass.ui.VerifyState
import com.example.nextclass.ui.components.Hairline
import com.example.nextclass.ui.components.MicroLabel
import com.example.nextclass.ui.components.PillSpacing
import com.example.nextclass.ui.components.PrimaryButton
import com.example.nextclass.ui.components.SecondaryButton
import com.example.nextclass.ui.components.SelectPill
import com.example.nextclass.ui.components.NcTextField
import com.example.nextclass.ui.theme.NcText
import com.example.nextclass.ui.theme.NcTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    notificationsGranted: Boolean,
    notificationPermissionNeeded: Boolean,
    exactAlarmsAllowed: Boolean,
    onBack: () -> Unit,
    onVerifyKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onLeadMinutes: (Int) -> Unit,
    onRemindersEnabled: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    message: String?,
    onDismissMessage: () -> Unit,
    onWidgetTheme: (Int) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onShare: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onImportShared: (String) -> Unit,
    onOpenGuide: () -> Unit,
    batteryUnrestricted: Boolean,
    onRequestBattery: () -> Unit,
    updateState: UpdateState,
    currentBuild: Int,
    onCheckUpdate: () -> Unit,
    onOpenUpdate: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var keyText by rememberSaveable { mutableStateOf("") }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var sharedText by rememberSaveable { mutableStateOf("") }

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
                text = "Settings",
                style = NcText.Headline,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Gemini key ----
            MicroLabel("Gemini API key")
            Text(
                text = "Each person uses their own free key from Google AI Studio. It is stored encrypted on this phone and sent only to Google.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )
            SecondaryButton(
                text = "Step-by-step key guide",
                onClick = onOpenGuide,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.hasKey) {
                Text(
                    text = "A key is saved on this device.",
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            NcTextField(
                value = keyText,
                onValueChange = { keyText = it.trim() },
                label = if (state.hasKey) "Replace key" else "API key",
                placeholder = "AIza...",
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            imageVector = if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(
                                if (keyVisible) R.string.cd_hide_key else R.string.cd_show_key,
                            ),
                            tint = NcTheme.extras.muted,
                        )
                    }
                },
            )
            PrimaryButton(
                text = if (state.verify is VerifyState.Checking) "Verifying" else "Verify key",
                enabled = keyText.isNotBlank() && state.verify !is VerifyState.Checking,
                onClick = { onVerifyKey(keyText) },
                modifier = Modifier.fillMaxWidth(),
            )
            when (val verify = state.verify) {
                VerifyState.Success -> Text(
                    text = "Key verified and saved.",
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is VerifyState.Failure -> Text(
                    text = verify.message,
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Unit
            }
            if (state.hasKey) {
                TextButton(onClick = {
                    keyText = ""
                    onClearKey()
                }) {
                    Text(
                        text = "Remove saved key",
                        style = NcText.Title.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Hairline()

            // ---- Reminder ----
            MicroLabel("First-class reminder")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Remind me before my first class",
                        style = NcText.Title,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "One notification a day. Later classes never notify.",
                        style = NcText.Body,
                        color = NcTheme.extras.muted,
                    )
                }
                Switch(
                    checked = state.remindersEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRemindersEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                        checkedBorderColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedThumbColor = NcTheme.extras.muted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = NcTheme.extras.hairline,
                    ),
                )
            }

            MicroLabel("Minutes before class start")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                AppPreferences.LEAD_OPTIONS.forEach { minutes ->
                    SelectPill(
                        label = "$minutes min",
                        selected = state.leadMinutes == minutes,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLeadMinutes(minutes)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Hairline()

            // ---- Appearance ----
            MicroLabel("Appearance")
            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                SelectPill(
                    label = "NextClass",
                    selected = !state.dynamicColor || !dynamicSupported,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDynamicColor(false)
                    },
                    modifier = Modifier.weight(1f),
                )
                SelectPill(
                    label = "Material You",
                    selected = state.dynamicColor && dynamicSupported,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (dynamicSupported) onDynamicColor(true)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (dynamicSupported) {
                    "NextClass keeps the warm off-white, obsidian and vermilion look. Material You takes its colors from your wallpaper, in the app and on both widgets."
                } else {
                    "Material You needs Android 12 or newer."
                },
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )

            Spacer(Modifier.height(4.dp))
            Hairline()

            // ---- Widget theme ----
            MicroLabel("Widget theme")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = PillSpacing) {
                listOf("Auto" to 0, "Light" to 1, "Dark" to 2).forEach { (label, mode) ->
                    SelectPill(
                        label = label,
                        selected = state.widgetTheme == mode,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onWidgetTheme(mode)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                text = "Auto follows your phone. For a smaller one-row widget, add NextClass (compact) from your widget list.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )

            Spacer(Modifier.height(4.dp))
            Hairline()

            // ---- Share and backup ----
            MicroLabel("Share and backup")
            Text(
                text = "Send your routine and course titles to a friend, or keep a backup file before changing phones.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )
            SecondaryButton(
                text = "Share my routine",
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Save backup",
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Restore",
                    onClick = onRestoreBackup,
                    modifier = Modifier.weight(1f),
                )
            }
            NcTextField(
                value = sharedText,
                onValueChange = { sharedText = it },
                label = "Shared routine",
                placeholder = "Paste the message here",
            )
            PrimaryButton(
                text = "Import shared routine",
                onClick = {
                    onImportShared(sharedText)
                    sharedText = ""
                },
                enabled = sharedText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Importing replaces the routine on this phone.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )
            if (message != null) {
                Text(
                    text = message,
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismissMessage),
                )
            }

            Spacer(Modifier.height(4.dp))
            Hairline()

            // ---- Permissions ----
            MicroLabel("Permissions")
            PermissionRow(
                title = "Notifications",
                granted = notificationsGranted || !notificationPermissionNeeded,
                onRequest = onRequestNotifications,
            )
            PermissionRow(
                title = "Exact alarms",
                granted = exactAlarmsAllowed,
                onRequest = onRequestExactAlarms,
            )
            Text(
                text = "Exact alarms keep the widget and the reminder on the minute. Without them Android may delay both by a few minutes.",
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )
            PermissionRow(
                title = "Run in background",
                granted = batteryUnrestricted,
                onRequest = onRequestBattery,
            )
            Text(
                text = batteryAdvice(),
                style = NcText.Body,
                color = NcTheme.extras.muted,
            )

            Spacer(Modifier.height(4.dp))
            Hairline()

            // ---- Updates ----
            MicroLabel("App updates")
            Text(
                text = "Installed build $currentBuild",
                style = NcText.Title,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (updateState) {
                UpdateState.Checking -> Text(
                    text = "Checking for a newer version.",
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
                is UpdateState.UpToDate -> Text(
                    text = "You have the latest version.",
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
                is UpdateState.Available -> Text(
                    text = "Build ${updateState.build} is available.",
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is UpdateState.Failed -> Text(
                    text = updateState.message,
                    style = NcText.Body,
                    color = MaterialTheme.colorScheme.error,
                )
                UpdateState.Idle -> Text(
                    text = "New versions are published on GitHub. Check to see if you are behind.",
                    style = NcText.Body,
                    color = NcTheme.extras.muted,
                )
            }
            if (updateState is UpdateState.Available) {
                PrimaryButton(
                    text = "Download update",
                    onClick = { onOpenUpdate(updateState.url) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SecondaryButton(
                text = if (updateState is UpdateState.Checking) "Checking" else "Check for updates",
                onClick = onCheckUpdate,
                enabled = updateState !is UpdateState.Checking,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Short, phone-brand specific advice on top of the standard battery exemption. */
private fun batteryAdvice(): String {
    val base = "Phones stop apps they think are idle, which can freeze the widget and skip the reminder. Allow this and NextClass stays on time."
    val extra = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi", "poco" ->
            " On Xiaomi, also open Security, then Permissions, then Autostart, and turn NextClass on."
        "oppo", "realme", "oneplus" ->
            " On this phone, also allow Auto-launch and Background activity for NextClass in the app's battery settings."
        "vivo", "iqoo" ->
            " On Vivo, also allow High background power consumption for NextClass in Battery settings."
        "samsung" ->
            " On Samsung, also set NextClass to Unrestricted under Battery, and remove it from Sleeping apps."
        "huawei", "honor" ->
            " On this phone, also set NextClass to Manage manually in App launch and turn on all three switches."
        else -> ""
    }
    return base + extra
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = NcText.Title,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MicroLabel(if (granted) "Allowed" else "Not allowed")
        }
        if (!granted) {
            SecondaryButton(text = "Allow", onClick = onRequest)
        }
    }
}
