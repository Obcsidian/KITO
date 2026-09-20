package com.kito.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kito.core.datastore.domain.repository.PrefsRepository
import com.kito.core.designsystem.StartupSyncGuard
import com.kito.core.connectivity.domain.repository.ConnectivityRepository
import com.kito.core.ui.state.SyncUiState
import com.kito.core.sync.domain.SyncUseCase
import com.kito.core.auth.domain.repository.CredentialsRepository
import com.kito.feature.attendance.domain.model.Attendance
import com.kito.feature.attendance.domain.repository.AttendanceRepository
import com.kito.feature.home.domain.model.EventOrAd
import com.kito.feature.home.domain.repository.HomeRepository
import com.kito.feature.schedule.domain.model.ScheduleItem
import com.kito.feature.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Provided

class HomeViewModel(
    private val prefs: PrefsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val attendanceRepository: AttendanceRepository,
    private val scheduleRepository: ScheduleRepository,
    private val homeRepository: HomeRepository,
    private val appSyncUseCase: SyncUseCase,
    private val syncGuard: StartupSyncGuard,
    @Provided private val connectivityRepository: ConnectivityRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    val isOnline = connectivityRepository.isOnline
    val name = prefs.userNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )
    private val _isKhaooGullyEnabled = MutableStateFlow<Boolean>(false)
    val isKhaooGullyEnabled = _isKhaooGullyEnabled.asStateFlow()
    private val _ads = MutableStateFlow<List<EventOrAd>>(emptyList())
    val ads: StateFlow<List<EventOrAd>> = _ads.asStateFlow()

    init {
        fetchEventsAndAds()
        fetchFeatureFlag()
    }

    val sapLoggedIn = credentialsRepository.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private fun fetchFeatureFlag() {
        viewModelScope.launch(dispatcher) {
            runCatching {
                homeRepository.isKhaooGullyEnabled()
            }.onSuccess { enabled ->
                _isKhaooGullyEnabled.value = enabled
            }.onFailure {
                _isKhaooGullyEnabled.value = false
            }
        }
    }

    private fun fetchEventsAndAds() {
        viewModelScope.launch(dispatcher) {
            runCatching { homeRepository.getEventsAndAds() }
                .onSuccess { list ->
                    println("Ads loaded: ${list.size}")
                    _ads.value = list
                }
                .onFailure {
                    println("Ads error: ${it.message}")
                }
        }
    }

    private val _day = MutableStateFlow<String>("")
    val day: StateFlow<String> = _day
    private val nextDay: StateFlow<String> =
        day.map { currentDay ->
            when (currentDay) {
                "MON" -> "TUE"
                "TUE" -> "WED"
                "WED" -> "THU"
                "THU" -> "FRI"
                "FRI" -> "SAT"
                "SAT" -> "SUN"
                "SUN" -> "MON"
                else -> ""
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ""
        )

    private fun updateDay(day: String) {
        _day.value = day
    }

    // Scope to the selected year/term — the table can hold several terms at once
    // (rows are keyed by subject+year+term), so an unfiltered read leaks stale terms.
    @OptIn(ExperimentalCoroutinesApi::class)
    val attendance: StateFlow<List<Attendance>> =
        combine(prefs.academicYearFlow, prefs.termCodeFlow) { y, t -> y to t }
            .distinctUntilChanged()
            .flatMapLatest { (y, t) -> attendanceRepository.observeAttendance(y, t) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState = _syncState.asStateFlow()
    private val _syncEvents = MutableSharedFlow<SyncUiState>()
    val syncEvents: SharedFlow<SyncUiState> = _syncEvents

    private val _loginState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val loginState = _loginState.asStateFlow()

    private fun syncOnStartup() {
        if (syncGuard.hasSynced) return
        syncGuard.hasSynced = true
        viewModelScope.launch(dispatcher) {
            _syncEvents.emit(SyncUiState.Loading)
            _syncState.value = SyncUiState.Loading
            val roll = prefs.userRollFlow.first()
            val sapPassword = credentialsRepository.getSapPassword()
            val year = prefs.academicYearFlow.first()
            val term = prefs.termCodeFlow.first()

            val result = appSyncUseCase.syncAll(
                roll = roll,
                sapPassword = sapPassword,
                year = year,
                term = term
            )

            _syncState.value = result.fold(
                onSuccess = {
                    _syncEvents.emit(SyncUiState.Success)
                    SyncUiState.Success
                },
                onFailure = {
                    SyncUiState.Error(it.message ?: "Sync failed")
                }
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isScheduleEmpty: StateFlow<Boolean> =
        prefs.userRollFlow
            .flatMapLatest { roll ->
                scheduleRepository
                    .getAllSchedule(rollNo = roll)
                    .map { it.isEmpty() }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                true
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val schedule: StateFlow<List<ScheduleItem>> =
        prefs.userRollFlow
            .flatMapLatest { roll ->
                day.flatMapLatest { day ->
                    scheduleRepository.getScheduleForDay(
                        rollNo = roll,
                        day = day
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val nextSchedule: StateFlow<List<ScheduleItem>> =
        prefs.userRollFlow
            .flatMapLatest { roll ->
                nextDay.flatMapLatest { day ->
                    scheduleRepository.getScheduleForDay(
                        rollNo = roll,
                        day = day
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    private fun login(
        password: String
    ) {
        viewModelScope.launch(dispatcher) {
            _loginState.value = SyncUiState.Loading
            delay(1000)
            val roll = prefs.userRollFlow.first()
            val year = prefs.academicYearFlow.first()
            val term = prefs.termCodeFlow.first()

            val result = appSyncUseCase.syncAll(
                roll = roll,
                sapPassword = password,
                year = year,
                term = term
            )

            _loginState.value = result.fold(
                onSuccess = {
                    credentialsRepository.saveSapPassword(password)
                    SyncUiState.Success
                },
                onFailure = {
                    SyncUiState.Error(it.message ?: "Sync failed")
                }
            )
        }
    }

    private fun setLoginStateIdle() {
        _loginState.value = SyncUiState.Idle
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SyncOnStartup -> syncOnStartup()
            is HomeEvent.Login -> login(event.password)
            is HomeEvent.SetLoginStateIdle -> setLoginStateIdle()
            is HomeEvent.UpdateDay -> updateDay(event.day)
        }
    }
}
