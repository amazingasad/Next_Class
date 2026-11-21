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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.example.nextclass.ui.screens.ApiKeyGuideScreen
import com.example.nextclass.ui.screens.CoursesScreen
import com.example.nextclass.ui.screens.HomeScreen
import com.example.nextclass.ui.screens.ImportRoutineScreen
import com.example.nextclass.ui.screens.SettingsScreen
import com.example.nextclass.ui.screens.WeekScreen
import com.example.nextclass.worker.AlarmHelper
import com.example.nextclass.worker.NotificationHelper

private enum class Screen { HOME, IMPORT, SETTINGS, COURSES, WEEK, GUIDE }

@Composable
fun NextClassRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var guideFrom by rememberSaveable { mutableStateOf(Screen.HOME) }
    val openGuide: () -> Unit = {
        guideFrom = screen
        screen = Screen.GUIDE
    }

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val overrides by viewModel.overrides.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val courseScan by viewModel.courseScan.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.restoreBackup(uri) }
    val shareRoutine: () -> Unit = {
        viewModel.prepareShare { text ->
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, "Share routine"))
        }
    }

    val leaveImport = {
        viewModel.resetImport()
        screen = Screen.HOME
    }

    BackHandler(enabled = screen == Screen.IMPORT) { leaveImport() }
    BackHandler(enabled = screen == Screen.SETTINGS) { screen = Screen.HOME }
    BackHandler(enabled = screen == Screen.WEEK) { screen = Screen.HOME }
    BackHandler(enabled = screen == Screen.GUIDE) { screen = guideFrom }
    BackHandler(enabled = screen == Screen.COURSES) {
        viewModel.clearCourseScan()
        screen = Screen.HOME
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = targetState != Screen.HOME
            val enterOffset: (Int) -> Int = { width -> if (forward) width / 6 else -width / 8 }
            val exitOffset: (Int) -> Int = { width -> if (forward) -width / 8 else width / 6 }
            (
                slideInHorizontally(tween(340, easing = FastOutSlowInEasing), enterOffset) +
                    fadeIn(tween(280))
                ) togetherWith (
                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing), exitOffset) +
                    fadeOut(tween(200))
                )
        },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.HOME -> HomeScreen(
                sessions = sessions,
                overrides = overrides,
                hero = hero,
                now = now,
                selectedDay = selectedDay,
                showNotificationNotice = permissionNeeded && !notificationsGranted,
                onSelectDay = viewModel::selectDay,
                onImport = { screen = Screen.IMPORT },
                onSettings = { screen = Screen.SETTINGS },
                onCourses = { screen = Screen.COURSES },
                onWeek = { screen = Screen.WEEK },
                hasApiKey = settings.hasKey,
                onKeyGuide = openGuide,
                onCancelledChange = viewModel::setCancelled,
                onRoomForDate = viewModel::setRoomForDate,
                onResetOverride = viewModel::resetOverride,
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
                onOpenSettings = openGuide,
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
                message = message,
                onDismissMessage = viewModel::dismissMessage,
                onWidgetTheme = viewModel::setWidgetTheme,
                onShare = shareRoutine,
                onExportBackup = { exportLauncher.launch("nextclass-backup.json") },
                onRestoreBackup = { restoreLauncher.launch(arrayOf("*/*")) },
                onImportShared = viewModel::importShared,
                onOpenGuide = openGuide,
            )

            Screen.COURSES -> CoursesScreen(
                courses = courses,
                scan = courseScan,
                hasApiKey = settings.hasKey,
                onBack = {
                    viewModel.clearCourseScan()
                    screen = Screen.HOME
                },
                onScan = viewModel::scanCourses,
                onAdd = viewModel::addCourse,
                onDelete = viewModel::deleteCourse,
                onOpenSettings = openGuide,
            )

            Screen.WEEK -> WeekScreen(
                sessions = sessions,
                overrides = overrides,
                now = now,
                onBack = { screen = Screen.HOME },
                onOpenDay = { day ->
                    viewModel.selectDay(day)
                    screen = Screen.HOME
                },
            )

            Screen.GUIDE -> ApiKeyGuideScreen(
                hasKey = settings.hasKey,
                onBack = { screen = guideFrom },
                onOpenSettings = { screen = Screen.SETTINGS },
            )
        }
    }
}
