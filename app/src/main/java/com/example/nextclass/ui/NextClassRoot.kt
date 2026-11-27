package com.example.nextclass.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
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
import com.example.nextclass.data.remote.UpdateState
import com.example.nextclass.ui.components.DockTab
import com.example.nextclass.ui.components.NcDock
import com.example.nextclass.ui.screens.ApiKeyGuideScreen
import com.example.nextclass.ui.screens.AttendanceScreen
import com.example.nextclass.ui.screens.CoursesScreen
import com.example.nextclass.ui.screens.HomeScreen
import com.example.nextclass.ui.screens.PlanScreen
import com.example.nextclass.ui.screens.ImportRoutineScreen
import com.example.nextclass.ui.screens.SettingsScreen
import com.example.nextclass.ui.screens.WeekScreen
import com.example.nextclass.worker.AlarmHelper
import com.example.nextclass.worker.NotificationHelper

private enum class Screen { HOME, IMPORT, SETTINGS, COURSES, WEEK, GUIDE, ATTENDANCE, PLAN }

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
    val planTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val planNotes by viewModel.notes.collectAsStateWithLifecycle()
    val openRequest by viewModel.openRequest.collectAsStateWithLifecycle()
    var planTab by rememberSaveable { mutableIntStateOf(0) }
    var planSheet by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        when (request) {
            "plan" -> {
                planTab = 0
                planSheet = true
                screen = Screen.PLAN
            }
            "tasks" -> {
                planTab = 0
                screen = Screen.PLAN
            }
            "notes" -> {
                planTab = 1
                screen = Screen.PLAN
            }
        }
        viewModel.consumeOpenRequest()
    }
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val marks by viewModel.attendance.collectAsStateWithLifecycle()
    val attendanceSummary by viewModel.attendanceSummary.collectAsStateWithLifecycle()
    val updateState by viewModel.update.collectAsStateWithLifecycle()
    val updateDismissed by viewModel.updateDismissed.collectAsStateWithLifecycle()

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

    val batteryUnrestricted = remember(resumeTick) {
        val power = context.getSystemService(PowerManager::class.java)
        power?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
    val requestBattery: () -> Unit = {
        val packageUri = Uri.parse("package:" + context.packageName)
        val attempts = listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(packageUri),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri),
        )
        for (intent in attempts) {
            val opened = try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
            if (opened) break
        }
    }
    val openUpdate: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No browser available; nothing else to do.
        }
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
    BackHandler(enabled = screen == Screen.PLAN) { screen = Screen.HOME }
    BackHandler(enabled = screen == Screen.ATTENDANCE) { screen = Screen.HOME }
    BackHandler(enabled = screen == Screen.WEEK) { screen = Screen.HOME }
    BackHandler(enabled = screen == Screen.GUIDE) { screen = guideFrom }
    BackHandler(enabled = screen == Screen.COURSES) {
        viewModel.clearCourseScan()
        screen = Screen.HOME
    }

    val dockTab = when (screen) {
        Screen.HOME -> DockTab.TODAY
        Screen.PLAN -> DockTab.PLAN
        Screen.ATTENDANCE -> DockTab.ATTENDANCE
        Screen.SETTINGS -> DockTab.SETTINGS
        else -> null
    }
    val selectTab: (DockTab) -> Unit = { tab ->
        screen = when (tab) {
            DockTab.TODAY -> Screen.HOME
            DockTab.PLAN -> Screen.PLAN
            DockTab.ATTENDANCE -> Screen.ATTENDANCE
            DockTab.SETTINGS -> Screen.SETTINGS
        }
    }

    Column(Modifier.fillMaxSize()) {
    Box(
        Modifier
            .weight(1f)
            .then(if (dockTab != null) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
    ) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val topLevel = setOf(Screen.HOME, Screen.PLAN, Screen.ATTENDANCE, Screen.SETTINGS)
            if (initialState in topLevel && targetState in topLevel) {
                // Siblings in the dock: a short horizontal drift with a fade-through.
                val order = listOf(Screen.HOME, Screen.PLAN, Screen.ATTENDANCE, Screen.SETTINGS)
                val direction = if (order.indexOf(targetState) > order.indexOf(initialState)) 1 else -1
                (
                    slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> direction * w / 10 } +
                        fadeIn(tween(240, delayMillis = 70)) +
                        scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.97f)
                    ) togetherWith (
                    slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { w -> -direction * w / 10 } +
                        fadeOut(tween(110))
                    )
            } else {
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
            }
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
                onCourses = { screen = Screen.COURSES },
                onWeek = { screen = Screen.WEEK },
                semesters = semesters,
                onSwitchSemester = viewModel::switchSemester,
                onAddSemester = viewModel::addSemester,
                onRenameSemester = viewModel::renameSemester,
                onDeleteSemester = viewModel::deleteSemester,
                hasApiKey = settings.hasKey,
                onKeyGuide = openGuide,
                onCancelledChange = viewModel::setCancelled,
                onRoomForDate = viewModel::setRoomForDate,
                onResetOverride = viewModel::resetOverride,
                onSaveSession = viewModel::saveSession,
                onDeleteSession = viewModel::deleteSession,
                onRequestNotifications = requestNotifications,
                marks = marks,
                onMark = viewModel::markAttendance,
                update = if (updateDismissed) null else updateState as? UpdateState.Available,
                onOpenUpdate = openUpdate,
                onDismissUpdate = viewModel::dismissUpdateBanner,
                showBatteryNotice = !batteryUnrestricted && !settings.batteryNoticeHidden,
                onRequestBattery = requestBattery,
                courses = courses,
                onAddStudyTask = viewModel::addTask,
                onHideBatteryNotice = viewModel::hideBatteryNotice,
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
                onDynamicColor = viewModel::setDynamicColor,
                onPlanEnabled = viewModel::setPlanEnabled,
                onPlanMinutes = viewModel::setPlanMinutes,
                onShare = shareRoutine,
                onExportBackup = { exportLauncher.launch("nextclass-backup.json") },
                onRestoreBackup = { restoreLauncher.launch(arrayOf("*/*")) },
                onImportShared = viewModel::importShared,
                onOpenGuide = openGuide,
                batteryUnrestricted = batteryUnrestricted,
                onRequestBattery = requestBattery,
                updateState = updateState,
                currentBuild = viewModel.currentBuild,
                onCheckUpdate = viewModel::checkForUpdate,
                onOpenUpdate = openUpdate,
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

            Screen.PLAN -> PlanScreen(
                tasks = planTasks,
                notes = planNotes,
                now = now,
                tab = planTab,
                onTab = { planTab = it },
                showPlanSheet = planSheet,
                onPlanSheet = { planSheet = it },
                onAddTask = viewModel::addTask,
                onToggleTask = viewModel::toggleTask,
                onDeleteTask = viewModel::deleteTask,
                onKeepTasks = viewModel::carryOver,
                onSaveNote = viewModel::saveNote,
                onDeleteNote = viewModel::deleteNote,
            )

            Screen.ATTENDANCE -> AttendanceScreen(
                summary = attendanceSummary,
                sessions = sessions.orEmpty(),
                overrides = overrides,
                marks = marks,
                now = now,
                onBack = { screen = Screen.HOME },
                onMark = viewModel::markAttendance,
                onClearCourse = viewModel::clearAttendanceFor,
            )

            Screen.GUIDE -> ApiKeyGuideScreen(
                hasKey = settings.hasKey,
                verify = settings.verify,
                onVerifyKey = viewModel::verifyAndSaveKey,
                onBack = { screen = guideFrom },
                onFinished = { screen = guideFrom },
            )
        }
    }
    }
    AnimatedVisibility(
        visible = dockTab != null,
        enter = slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(240)),
        exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(160)),
    ) {
        NcDock(
            selected = dockTab ?: DockTab.TODAY,
            onSelect = selectTab,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
    }
}
