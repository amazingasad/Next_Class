package com.example.nextclass.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextclass.ui.screens.HomeScreen
import com.example.nextclass.ui.screens.ImportRoutineScreen
import com.example.nextclass.ui.screens.SettingsScreen
import com.example.nextclass.worker.AlarmHelper
import com.example.nextclass.worker.NotificationHelper

private enum class Screen { HOME, IMPORT, SETTINGS }

@Composable
fun NextClassRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val hero by viewModel.hero.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()

    // Bumped on every resume so permission status is re-read after the user returns from system settings.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
        viewModel.refreshSchedule()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        resumeTick++
        viewModel.refreshSchedule()
    }

    val notificationsGranted = remember(resumeTick) { NotificationHelper.hasPermission(context) }
    val exactAllowed = remember(resumeTick) { AlarmHelper.canScheduleExact(context) }
    val permissionNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val requestNotifications: () -> Unit = {
        if (permissionNeeded) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val requestExactAlarms: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:" + context.packageName)),
            )
        }
    }

    val leaveImport = {
        viewModel.resetImport()
        screen = Screen.HOME
    }

    BackHandler(enabled = screen == Screen.IMPORT) { leaveImport() }
    BackHandler(enabled = screen == Screen.SETTINGS) { screen = Screen.HOME }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.HOME -> HomeScreen(
                sessions = sessions,
                hero = hero,
                now = now,
                selectedDay = selectedDay,
                showNotificationNotice = permissionNeeded && !notificationsGranted,
                onSelectDay = viewModel::selectDay,
                onImport = { screen = Screen.IMPORT },
                onSettings = { screen = Screen.SETTINGS },
                onSaveSession = viewModel::saveSession,
                onDeleteSession = viewModel::deleteSession,
                onRequestNotifications = requestNotifications,
            )

            Screen.IMPORT -> ImportRoutineScreen(
                state = importState,
                hasApiKey = settings.hasKey,
                onBack = { leaveImport() },
                onScan = viewModel::scan,
                onUpdateDraft = viewModel::updateDraft,
                onAddDraft = viewModel::addDraft,
                onDeleteDraft = viewModel::deleteDraft,
                onSave = { viewModel.saveDraft { screen = Screen.HOME } },
                onReset = viewModel::resetImport,
                onOpenSettings = { screen = Screen.SETTINGS },
            )

            Screen.SETTINGS -> SettingsScreen(
                state = settings,
                notificationsGranted = notificationsGranted,
                notificationPermissionNeeded = permissionNeeded,
                exactAlarmsAllowed = exactAllowed,
                onBack = { screen = Screen.HOME },
                onVerifyKey = viewModel::verifyAndSaveKey,
                onClearKey = viewModel::clearKey,
                onLeadMinutes = viewModel::setLeadMinutes,
                onRemindersEnabled = viewModel::setRemindersEnabled,
                onRequestNotifications = requestNotifications,
                onRequestExactAlarms = requestExactAlarms,
            )
        }
    }
}
