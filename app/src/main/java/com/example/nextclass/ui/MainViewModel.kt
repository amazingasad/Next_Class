package com.example.nextclass.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextclass.data.local.ApiKeyManager
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.toDomain
import com.example.nextclass.data.local.toEntity
import com.example.nextclass.data.remote.RoutineParserRepository
import com.example.nextclass.domain.model.ClassSession
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Scanning : ImportUiState
    data class Review(val sessions: List<ClassSession>) : ImportUiState
    data class Failed(val message: String) : ImportUiState
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
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).routineDao()
    private val keyManager = ApiKeyManager(application)
    private val preferences = AppPreferences(application)
    private val repository = RoutineParserRepository(application, keyManager)
    private val resolver = GetCurrentOrNextClassUseCase()

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

    val hero: StateFlow<NextClassState> = combine(sessions, now) { list, time ->
        if (list == null) NextClassState.NoClasses else resolver(list, time)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NextClassState.NoClasses)

    private val _selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _settings = MutableStateFlow(
        SettingsUiState(
            leadMinutes = preferences.leadMinutes,
            remindersEnabled = preferences.firstClassReminderEnabled,
        ),
    )
    val settings: StateFlow<SettingsUiState> = _settings.asStateFlow()

    private val _import = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _import.asStateFlow()

    private var scanJob: Job? = null
    private var nextDraftId = 1L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val hasKey = keyManager.hasKey()
            _settings.update { it.copy(hasKey = hasKey) }
        }
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

    /** Re-arms the widget alarm and the daily reminder. Cheap, so it runs on every resume. */
    fun refreshSchedule() {
        viewModelScope.launch(Dispatchers.Default) {
            WidgetUpdateHelper.refreshAll(getApplication())
        }
    }

    // ---- Import ------------------------------------------------------------------------

    fun scan(uri: Uri, sectionFilter: String) {
        scanJob?.cancel()
        _import.value = ImportUiState.Scanning
        scanJob = viewModelScope.launch {
            repository.scanRoutine(uri, sectionFilter).fold(
                onSuccess = { list ->
                    nextDraftId = 1L
                    _import.value = ImportUiState.Review(list.map { it.copy(id = nextDraftId++) })
                },
                onFailure = { error ->
                    _import.value = ImportUiState.Failed(error.message ?: "Could not read the routine.")
                },
            )
        }
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
}
