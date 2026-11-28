package com.example.nextclass.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextclass.data.local.ApiKeyManager
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.BreakEntity
import com.example.nextclass.data.local.NoteEntity
import com.example.nextclass.data.local.TaskEntity
import com.example.nextclass.data.local.RoutineCodec
import com.example.nextclass.data.local.RoutineBundle
import com.example.nextclass.data.local.toDomain
import com.example.nextclass.data.local.toEntity
import com.example.nextclass.data.offline.OfflineRoutineReader
import com.example.nextclass.data.remote.RoutineParserRepository
import com.example.nextclass.domain.offline.GroupKey
import com.example.nextclass.domain.offline.OfflineOutcome
import com.example.nextclass.domain.offline.OfflineSelection
import com.example.nextclass.domain.offline.TimetableGrid
import com.example.nextclass.domain.offline.TimetableParser
import com.example.nextclass.data.remote.UpdateChecker
import com.example.nextclass.data.remote.UpdateState
import com.example.nextclass.domain.model.AttendanceMark
import com.example.nextclass.domain.model.AttendanceMath
import com.example.nextclass.domain.model.CourseAttendance
import com.example.nextclass.domain.model.BreakPeriod
import com.example.nextclass.domain.model.FocusSession
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.StudyEntry
import com.example.nextclass.domain.model.TimeFormat
import com.example.nextclass.domain.model.WeeklyReview
import com.example.nextclass.domain.model.WeeklyReviewData
import com.example.nextclass.worker.FocusManager
import com.example.nextclass.domain.model.PlannerTask
import com.example.nextclass.domain.model.Semester
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.RoutineOverrides
import com.example.nextclass.domain.model.NextClassState
import com.example.nextclass.domain.usecase.GetCurrentOrNextClassUseCase
import com.example.nextclass.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Scanning : ImportUiState
    data class Review(val sessions: List<ClassSession>, val source: String = "") : ImportUiState
    /** The offline reader found several sections and needs the user to pick one. */
    data class ChooseGroup(val groups: List<GroupKey>) : ImportUiState
    data class Failed(val message: String) : ImportUiState
}

sealed interface CourseScanState {
    data object Idle : CourseScanState
    data object Scanning : CourseScanState
    data class Done(val count: Int) : CourseScanState
    data class Failed(val message: String) : CourseScanState
}

sealed interface VerifyState {
    data object Idle : VerifyState
    data object Checking : VerifyState
    data object Success : VerifyState
    data class Failure(val message: String) : VerifyState
}

data class SettingsUiState(
    val hasKey: Boolean = false,
    val leadMinutes: Int = AppPreferences.DEFAULT_LEAD_MINUTES,
    val remindersEnabled: Boolean = true,
    val verify: VerifyState = VerifyState.Idle,
    val widgetTheme: Int = 0,
    val batteryNoticeHidden: Boolean = false,
    val dynamicColor: Boolean = false,
    val planEnabled: Boolean = true,
    val planMinutes: Int = AppPreferences.DEFAULT_PLAN_MINUTES,
    val reviewEnabled: Boolean = true,
    val reviewDay: Int = 5,
    /** 0 automatic, 1 offline only, 2 Gemini only. */
    val scanMode: Int = 0,
    /** 0 follows the phone, 1 light, 2 dark. */
    val themeMode: Int = 0,
    val paletteId: Int = 0,
)

private data class ReviewInputs(
    val sessions: List<ClassSession>,
    val overrides: List<ClassOverride>,
    val breaks: List<BreakPeriod>,
    val courses: List<Course>,
)

private data class ReviewLive(
    val marks: List<AttendanceMark>,
    val tasks: List<PlannerTask>,
    val study: List<StudyEntry>,
    val today: LocalDate,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).routineDao()
    private val keyManager = ApiKeyManager(application)
    private val preferences = AppPreferences(application)
    private val repository = RoutineParserRepository(application, keyManager)
    private val planner = AppDatabase.getInstance(application).plannerDao()
    private val resolver = GetCurrentOrNextClassUseCase()
    private val updateChecker = UpdateChecker(application)

    /** Null until the first database read finishes. */
    val sessions: StateFlow<List<ClassSession>?> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn<List<ClassSession>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Ticks at the top of every minute so countdowns stay correct while the app is open. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    /** One-day changes (cancelled classes, room swaps) from yesterday onward. */
    val overrides: StateFlow<List<ClassOverride>> = dao
        .observeOverrides(LocalDate.now().minusDays(1).toString())
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val courses: StateFlow<List<Course>> = dao.observeCourses()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<PlannerTask>> = now
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { today -> planner.observeTasks(today.toString()) }
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<Note>> = planner.observeNotes()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _openRequest = MutableStateFlow<String?>(null)
    val openRequest: StateFlow<String?> = _openRequest.asStateFlow()

    val semesters: StateFlow<List<Semester>> = dao.observeSemesters()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attendance: StateFlow<List<AttendanceMark>> = dao.observeAttendance()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attendanceSummary: StateFlow<List<CourseAttendance>> =
        combine(sessions, courses, attendance) { list, courseList, marks ->
            AttendanceMath.summarize(list.orEmpty(), courseList, marks)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val breaks: StateFlow<List<BreakPeriod>> = dao.observeBreaks()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Finished study sessions from the last 30 days. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val studyLog: StateFlow<List<StudyEntry>> = now
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { today -> planner.observeStudySince(today.minusDays(30).toString()) }
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val weekTasks: StateFlow<List<PlannerTask>> = now
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { today -> planner.observeTasks(today.minusDays(6).toString()) }
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _focus = MutableStateFlow(FocusManager.load(application))
    val focus: StateFlow<FocusSession?> = _focus.asStateFlow()

    val review: StateFlow<WeeklyReviewData> = combine(
        combine(sessions, overrides, breaks, courses) { list, changes, off, courseList ->
            ReviewInputs(list.orEmpty(), changes, off, courseList)
        },
        combine(attendance, weekTasks, studyLog, now) { marks, taskList, study, time ->
            ReviewLive(marks, taskList, study, time.toLocalDate())
        },
    ) { base, live ->
        WeeklyReview.compute(
            end = live.today,
            sessions = base.sessions,
            overrides = base.overrides,
            breaks = base.breaks,
            courses = base.courses,
            marks = live.marks,
            tasks = live.tasks,
            study = live.study,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WeeklyReviewData(
            from = LocalDate.now().minusDays(6),
            to = LocalDate.now(),
            held = 0, present = 0, absent = 0, unmarked = 0,
            tasksDone = 0, tasksTotal = 0, studyMinutes = 0,
            studyByCourse = emptyList(), weakest = null, nextBreak = null,
        ),
    )

    val hero: StateFlow<NextClassState> = combine(sessions, overrides, now, breaks) { list, changes, time, off ->
        if (list == null) {
            NextClassState.NoClasses
        } else {
            resolver(RoutineOverrides.effective(list, changes, time.toLocalDate(), off), time)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NextClassState.NoClasses)

    private val _selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _settings = MutableStateFlow(
        SettingsUiState(
            leadMinutes = preferences.leadMinutes,
            remindersEnabled = preferences.firstClassReminderEnabled,
            widgetTheme = preferences.widgetTheme,
            batteryNoticeHidden = preferences.batteryNoticeHidden,
            dynamicColor = preferences.dynamicColor,
            planEnabled = preferences.planReminderEnabled,
            planMinutes = preferences.planMinutes,
            reviewEnabled = preferences.reviewEnabled,
            reviewDay = preferences.reviewDay,
            scanMode = preferences.scanMode,
            themeMode = preferences.appThemeMode,
            paletteId = preferences.paletteId,
        ),
    )
    val settings: StateFlow<SettingsUiState> = _settings.asStateFlow()

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    private val _updateDismissed = MutableStateFlow(false)
    val updateDismissed: StateFlow<Boolean> = _updateDismissed.asStateFlow()

    val currentBuild: Int get() = updateChecker.currentBuild()

    private val _import = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _import.asStateFlow()

    private val _courseScan = MutableStateFlow<CourseScanState>(CourseScanState.Idle)
    val courseScan: StateFlow<CourseScanState> = _courseScan.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var courseJob: Job? = null
    private var scanJob: Job? = null
    private var nextDraftId = 1L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val hasKey = keyManager.hasKey()
            _settings.update { it.copy(hasKey = hasKey) }
        }
        autoCheckForUpdate()
    }

    // ---- Home --------------------------------------------------------------------------

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun saveSession(session: ClassSession) {
        viewModelScope.launch {
            dao.upsert(session.toEntity())
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    fun setCancelled(session: ClassSession, date: LocalDate, cancelled: Boolean) {
        saveOverride(session, date) { it.copy(cancelled = cancelled) }
    }

    fun setRoomForDate(session: ClassSession, date: LocalDate, room: String) {
        saveOverride(session, date) { it.copy(room = room, cancelled = false) }
    }

    fun resetOverride(session: ClassSession, date: LocalDate) {
        viewModelScope.launch {
            dao.deleteOverride(session.id, date.toString())
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    private fun saveOverride(
        session: ClassSession,
        date: LocalDate,
        change: (ClassOverride) -> ClassOverride,
    ) {
        viewModelScope.launch {
            val dateText = date.toString()
            val current = overrides.value.firstOrNull { it.sessionId == session.id && it.date == dateText }
                ?: ClassOverride(sessionId = session.id, date = dateText, cancelled = false, room = "")
            val updated = change(current)
            if (!updated.cancelled && updated.room.isBlank()) {
                dao.deleteOverride(session.id, dateText)
            } else {
                dao.upsertOverride(updated.toEntity())
            }
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    /** Re-arms the widget alarm and the daily reminder. Cheap, so it runs on every resume. */
    fun refreshSchedule() {
        refreshFocus()
        viewModelScope.launch(Dispatchers.Default) {
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    // ---- Attendance --------------------------------------------------------------------

    /** Marks a class present or absent. Passing null clears the mark. */
    fun markAttendance(session: ClassSession, date: LocalDate, present: Boolean?) {
        val key = CourseCodes.key(session.courseCode)
        viewModelScope.launch(Dispatchers.IO) {
            if (present == null) {
                dao.deleteAttendance(key, date.toString(), session.startTime)
            } else {
                dao.upsertAttendance(
                    AttendanceMark(key, date.toString(), session.startTime, present).toEntity(),
                )
            }
            WidgetUpdateHelper.updateWidgets(getApplication())
        }
    }

    fun clearAttendanceFor(courseKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAttendance().filter { it.courseKey == courseKey }.forEach {
                dao.deleteAttendance(it.courseKey, it.date, it.startTime)
            }
            WidgetUpdateHelper.updateWidgets(getApplication())
        }
    }

    // ---- Planner -----------------------------------------------------------------------

    // ---- Breaks ------------------------------------------------------------------------

    fun addBreak(name: String, start: LocalDate, end: LocalDate) {
        val clean = name.trim()
        if (clean.isEmpty() || end.isBefore(start)) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertBreak(BreakEntity(name = clean, startDate = start.toString(), endDate = end.toString()))
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    fun deleteBreak(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteBreak(id)
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    // ---- Study timer -------------------------------------------------------------------

    fun refreshFocus() {
        _focus.value = FocusManager.load(getApplication())
    }

    fun startFocus(courseKey: String, courseCode: String, minutes: Int) {
        FocusManager.start(getApplication(), courseKey, courseCode, minutes)
        refreshFocus()
    }

    fun pauseFocus() {
        FocusManager.pause(getApplication())
        refreshFocus()
    }

    fun resumeFocus() {
        FocusManager.resume(getApplication())
        refreshFocus()
    }

    fun finishFocus() {
        viewModelScope.launch(Dispatchers.IO) {
            val minutes = FocusManager.finish(getApplication())
            refreshFocus()
            if (minutes > 0) _message.value = "Logged " + TimeFormat.duration(minutes) + " of study."
        }
    }

    fun discardFocus() {
        FocusManager.discard(getApplication())
        refreshFocus()
    }

    fun setReviewEnabled(enabled: Boolean) {
        preferences.reviewEnabled = enabled
        _settings.update { it.copy(reviewEnabled = enabled) }
        refreshSchedule()
    }

    fun setReviewDay(day: Int) {
        preferences.reviewDay = day
        _settings.update { it.copy(reviewDay = day) }
        refreshSchedule()
    }

    fun requestOpen(kind: String) {
        _openRequest.value = kind
    }

    fun consumeOpenRequest() {
        _openRequest.value = null
    }

    private fun afterPlannerChange() {
        viewModelScope.launch(Dispatchers.Default) {
            WidgetUpdateHelper.updateWidgets(getApplication())
        }
    }

    fun addTask(date: LocalDate, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            planner.insertTask(TaskEntity(date = date.toString(), title = clean))
            afterPlannerChange()
        }
    }

    fun toggleTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            planner.toggle(id)
            afterPlannerChange()
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            planner.deleteTask(id)
            afterPlannerChange()
        }
    }

    /** Copies the chosen tasks to [toDate] as not done, skipping titles that are already there. */
    fun carryOver(ids: Set<Long>, toDate: LocalDate) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = planner.tasksOn(toDate.toString()).map { it.title.lowercase() }.toSet()
            val source = tasks.value.filter { it.id in ids }
            val copies = source
                .filter { it.title.lowercase() !in existing }
                .distinctBy { it.title.lowercase() }
                .map { TaskEntity(date = toDate.toString(), title = it.title) }
            if (copies.isNotEmpty()) planner.insertTasks(copies)
            afterPlannerChange()
        }
    }

    fun saveNote(id: Long, title: String, body: String, pinned: Boolean) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            planner.saveNote(
                NoteEntity(
                    id = id,
                    title = title.trim(),
                    body = body.trim(),
                    pinned = pinned,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            afterPlannerChange()
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            planner.deleteNote(id)
            afterPlannerChange()
        }
    }

    // ---- Semesters ---------------------------------------------------------------------

    fun addSemester(name: String) {
        val clean = name.trim().ifEmpty { "New semester" }
        viewModelScope.launch(Dispatchers.IO) {
            dao.createSemester(clean)
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    fun switchSemester(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.switchSemester(id)
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    fun renameSemester(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { dao.renameSemester(id, clean) }
    }

    fun deleteSemester(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val removed = dao.removeSemester(id)
            if (removed) {
                WidgetUpdateHelper.refreshAll(getApplication())
            } else {
                _message.value = "You need at least one semester."
            }
        }
    }

    // ---- Updates and battery -----------------------------------------------------------

    fun checkForUpdate() {
        if (_update.value is UpdateState.Checking) return
        _update.value = UpdateState.Checking
        viewModelScope.launch {
            val result = updateChecker.check()
            if (result !is UpdateState.Failed) preferences.lastUpdateCheck = System.currentTimeMillis()
            _updateDismissed.value = false
            _update.value = result
        }
    }

    /** At most one quiet check a day, so the Home banner appears without the user asking. */
    private fun autoCheckForUpdate() {
        val dayMillis = 24L * 60L * 60L * 1000L
        if (System.currentTimeMillis() - preferences.lastUpdateCheck < dayMillis) return
        viewModelScope.launch {
            val result = updateChecker.check()
            if (result is UpdateState.Available) {
                _update.value = result
            }
            if (result !is UpdateState.Failed) preferences.lastUpdateCheck = System.currentTimeMillis()
        }
    }

    fun dismissUpdateBanner() {
        _updateDismissed.value = true
    }

    fun hideBatteryNotice() {
        preferences.batteryNoticeHidden = true
        _settings.update { it.copy(batteryNoticeHidden = true) }
    }

    // ---- Import ------------------------------------------------------------------------

    private val offlineReader = OfflineRoutineReader(application)
    private var pendingGrid: TimetableGrid? = null

    private sealed interface ScanAttempt {
        data class Done(val sessions: List<ClassSession>, val source: String) : ScanAttempt
        data class Pick(val grid: TimetableGrid, val groups: List<GroupKey>) : ScanAttempt
        data class Failed(val message: String) : ScanAttempt
    }

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java)
            ?: return false
        return try {
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: SecurityException) {
            true
        }
    }

    private suspend fun scanOffline(uri: Uri, filter: String): ScanAttempt {
        return when (val outcome = offlineReader.read(uri)) {
            is OfflineOutcome.NotATimetable -> ScanAttempt.Failed(outcome.reason)
            is OfflineOutcome.Found -> when (val selection = TimetableParser.select(outcome.grid, filter)) {
                is OfflineSelection.NeedGroup -> ScanAttempt.Pick(outcome.grid, selection.groups)
                is OfflineSelection.Ready ->
                    if (selection.sessions.isEmpty()) {
                        ScanAttempt.Failed("No classes were found for " + selection.group.label + ".")
                    } else {
                        ScanAttempt.Done(selection.sessions, "Read on this phone. " + selection.group.label + ".")
                    }
            }
        }
    }

    private suspend fun scanCloud(uri: Uri, filter: String): ScanAttempt =
        repository.scanRoutine(uri, filter).fold(
            onSuccess = { list -> ScanAttempt.Done(list, "Read with Gemini.") },
            onFailure = { error -> ScanAttempt.Failed(error.message ?: "Could not read the routine.") },
        )

    /**
     * Reads a routine. Automatic mode uses Gemini when a key is saved and the phone is online,
     * and the on-device reader otherwise, and falls back to the other one if the first fails.
     */
    fun scan(uri: Uri, sectionFilter: String) {
        scanJob?.cancel()
        _import.value = ImportUiState.Scanning
        pendingGrid = null
        scanJob = viewModelScope.launch {
            val mode = preferences.scanMode
            val hasKey = keyManager.hasKey()
            val online = isOnline()
            val result: ScanAttempt = when (mode) {
                1 -> scanOffline(uri, sectionFilter)
                2 -> when {
                    !hasKey -> ScanAttempt.Failed("Gemini needs an API key. Add one in Settings, or switch the scan engine to Automatic.")
                    !online -> ScanAttempt.Failed("Gemini needs an internet connection. Switch the scan engine to Automatic to read offline.")
                    else -> scanCloud(uri, sectionFilter)
                }
                else -> if (hasKey && online) {
                    val cloud = scanCloud(uri, sectionFilter)
                    if (cloud is ScanAttempt.Failed) {
                        val local = scanOffline(uri, sectionFilter)
                        if (local is ScanAttempt.Failed) cloud else local
                    } else {
                        cloud
                    }
                } else {
                    val local = scanOffline(uri, sectionFilter)
                    if (local is ScanAttempt.Failed) {
                        ScanAttempt.Failed(
                            local.message + if (hasKey) " Connect to the internet to try Gemini." else " Add a free Gemini key in Settings for routines this reader cannot handle.",
                        )
                    } else {
                        local
                    }
                }
            }
            when (result) {
                is ScanAttempt.Done -> {
                    nextDraftId = 1L
                    _import.value = ImportUiState.Review(
                        result.sessions.map { it.copy(id = nextDraftId++) },
                        result.source,
                    )
                }
                is ScanAttempt.Pick -> {
                    pendingGrid = result.grid
                    _import.value = ImportUiState.ChooseGroup(result.groups)
                }
                is ScanAttempt.Failed -> _import.value = ImportUiState.Failed(result.message)
            }
        }
    }

    /** The user picked a section after the offline reader listed them. */
    fun chooseGroup(group: GroupKey) {
        val grid = pendingGrid ?: return
        val sessions = TimetableParser.sessionsFor(grid, group)
        if (sessions.isEmpty()) {
            _import.value = ImportUiState.Failed("No classes were found for " + group.label + ".")
            return
        }
        nextDraftId = 1L
        _import.value = ImportUiState.Review(
            sessions.map { it.copy(id = nextDraftId++) },
            "Read on this phone. " + group.label + ".",
        )
    }

    fun updateDraft(session: ClassSession) {
        updateReview { list -> list.map { if (it.id == session.id) session else it } }
    }

    fun addDraft(session: ClassSession) {
        updateReview { list -> list + session.copy(id = nextDraftId++) }
    }

    fun deleteDraft(id: Long) {
        updateReview { list -> list.filterNot { it.id == id } }
    }

    fun resetImport() {
        scanJob?.cancel()
        _import.value = ImportUiState.Idle
    }

    /** Replaces the saved routine with the reviewed draft. */
    fun saveDraft(onDone: () -> Unit) {
        val draft = (_import.value as? ImportUiState.Review)?.sessions ?: return
        if (draft.isEmpty()) return
        viewModelScope.launch {
            dao.replaceAll(draft.map { it.copy(id = 0L).toEntity() })
            applyCourseTitles()
            WidgetUpdateHelper.refreshAll(getApplication())
            _import.value = ImportUiState.Idle
            onDone()
        }
    }

    private fun updateReview(transform: (List<ClassSession>) -> List<ClassSession>) {
        _import.update { current ->
            if (current is ImportUiState.Review) {
                ImportUiState.Review(
                    transform(current.sessions)
                        .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
                    current.source,
                )
            } else {
                current
            }
        }
    }

    // ---- Settings ----------------------------------------------------------------------

    fun verifyAndSaveKey(key: String) {
        _settings.update { it.copy(verify = VerifyState.Checking) }
        viewModelScope.launch {
            repository.verifyKey(key).fold(
                onSuccess = {
                    kotlinx.coroutines.withContext(Dispatchers.IO) { keyManager.saveKey(key) }
                    _settings.update { it.copy(hasKey = true, verify = VerifyState.Success) }
                },
                onFailure = { error ->
                    _settings.update {
                        it.copy(verify = VerifyState.Failure(error.message ?: "Could not verify the key."))
                    }
                },
            )
        }
    }

    fun clearKey() {
        viewModelScope.launch(Dispatchers.IO) {
            keyManager.clearKey()
            _settings.update { it.copy(hasKey = false, verify = VerifyState.Idle) }
        }
    }

    fun setLeadMinutes(minutes: Int) {
        preferences.leadMinutes = minutes
        _settings.update { it.copy(leadMinutes = minutes) }
        refreshSchedule()
    }

    fun setRemindersEnabled(enabled: Boolean) {
        preferences.firstClassReminderEnabled = enabled
        _settings.update { it.copy(remindersEnabled = enabled) }
        refreshSchedule()
    }

    fun setPlanEnabled(enabled: Boolean) {
        preferences.planReminderEnabled = enabled
        _settings.update { it.copy(planEnabled = enabled) }
        refreshSchedule()
    }

    fun setPlanMinutes(minutes: Int) {
        preferences.planMinutes = minutes
        _settings.update { it.copy(planMinutes = minutes) }
        refreshSchedule()
    }

    fun setDynamicColor(enabled: Boolean) {
        preferences.dynamicColor = enabled
        _settings.update { it.copy(dynamicColor = enabled) }
        viewModelScope.launch { WidgetUpdateHelper.updateWidgets(getApplication()) }
    }

    fun setScanMode(mode: Int) {
        preferences.scanMode = mode
        _settings.update { it.copy(scanMode = mode) }
    }

    fun setThemeMode(mode: Int) {
        preferences.appThemeMode = mode
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setPalette(id: Int) {
        preferences.paletteId = id
        _settings.update { it.copy(paletteId = id) }
        viewModelScope.launch { WidgetUpdateHelper.updateWidgets(getApplication()) }
    }

    fun setWidgetTheme(mode: Int) {
        preferences.widgetTheme = mode
        _settings.update { it.copy(widgetTheme = mode) }
        viewModelScope.launch { WidgetUpdateHelper.updateWidgets(getApplication()) }
    }

    // ---- Courses -----------------------------------------------------------------------

    fun scanCourses(uri: Uri) {
        courseJob?.cancel()
        _courseScan.value = CourseScanState.Scanning
        courseJob = viewModelScope.launch {
            repository.scanCourses(uri).fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _courseScan.value = CourseScanState.Failed("No courses were found in that file.")
                    } else {
                        dao.upsertCourses(list.map { it.toEntity() })
                        applyCourseTitles()
                        WidgetUpdateHelper.refreshAll(getApplication())
                        _courseScan.value = CourseScanState.Done(list.size)
                    }
                },
                onFailure = { error ->
                    _courseScan.value = CourseScanState.Failed(error.message ?: "Could not read the course list.")
                },
            )
        }
    }

    fun clearCourseScan() {
        courseJob?.cancel()
        _courseScan.value = CourseScanState.Idle
    }

    fun addCourse(code: String, title: String) {
        if (CourseCodes.key(code).isEmpty() || title.isBlank()) return
        viewModelScope.launch {
            dao.upsertCourses(listOf(Course(CourseCodes.pretty(code), title.trim()).toEntity()))
            applyCourseTitles()
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { dao.deleteCourse(course.key) }
    }

    /** Course titles from the course list fill in every class with a matching code. */
    private suspend fun applyCourseTitles() {
        val titles = dao.getCourses().associate { it.codeKey to it.title }
        if (titles.isEmpty()) return
        dao.getAll().forEach { row ->
            val title = titles[CourseCodes.key(row.courseCode)] ?: return@forEach
            if (title.isNotBlank() && title != row.courseTitle) {
                dao.upsert(row.copy(courseTitle = title))
            }
        }
    }

    // ---- Share and backup --------------------------------------------------------------

    fun dismissMessage() {
        _message.value = null
    }

    private suspend fun currentBundle(): RoutineBundle = withContext(Dispatchers.IO) {
        RoutineCodec.bundleOf(
            dao.getAll().map { it.toDomain() },
            dao.getCourses().map { it.toDomain() },
            dao.getAttendance().map { it.toDomain() },
            planner.getNotes().map { it.toDomain() },
        )
    }

    fun prepareShare(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val bundle = currentBundle()
            if (bundle.sessions.isEmpty()) {
                _message.value = "Nothing to share yet. Import a routine first."
            } else {
                onReady(RoutineCodec.shareMessage(bundle.copy(attendance = emptyList(), notes = emptyList())))
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bundle = currentBundle()
                val written = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(RoutineCodec.toBackupJson(bundle).toByteArray(Charsets.UTF_8))
                    true
                } ?: false
                _message.value = if (written) "Backup saved." else "Could not save the backup."
            } catch (e: Exception) {
                _message.value = "Could not save the backup."
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                null
            }
            if (text == null) {
                _message.value = "Could not read that file."
            } else {
                applyShared(text)
            }
        }
    }

    fun importShared(text: String) {
        viewModelScope.launch { applyShared(text) }
    }

    private suspend fun applyShared(text: String) {
        val bundle = RoutineCodec.parse(text)
        if (bundle == null || bundle.sessions.isEmpty()) {
            _message.value = "That is not a NextClass routine."
            return
        }
        withContext(Dispatchers.IO) {
            dao.replaceAll(RoutineCodec.toSessions(bundle).map { it.toEntity() })
            val courseList = RoutineCodec.toCourses(bundle)
            if (courseList.isNotEmpty()) {
                dao.clearCourses()
                dao.upsertCourses(courseList.map { it.toEntity() })
            }
            val marks = RoutineCodec.toAttendance(bundle)
            if (marks.isNotEmpty()) {
                dao.replaceAttendance(marks.map { it.toEntity() })
            }
            val noteList = RoutineCodec.toNotes(bundle)
            if (noteList.isNotEmpty()) {
                planner.replaceNotes(
                    noteList.map { NoteEntity(title = it.title, body = it.body, pinned = it.pinned) },
                )
            }
            applyCourseTitles()
        }
        WidgetUpdateHelper.refreshAll(getApplication())
        _message.value = "Imported ${bundle.sessions.size} classes."
    }
}
