package com.kito.feature.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kito.core.auth.AuthRepository
import com.kito.core.datastore.domain.repository.PrefsRepository
import com.kito.core.datastore.data.PrefsRepositoryImpl
import com.kito.core.platform.SecureStorage
import com.kito.core.ui.state.SyncUiState
import com.kito.feature.attendance.domain.model.Attendance
import com.kito.feature.schedule.notification.NotificationController
import com.kito.feature.settings.presentation.SettingsViewModel
import com.kito.feature.settings.presentation.SettingsEvent
import com.kito.testing.FakeAttendanceRepository
import com.kito.testing.FakeAuthRepository
import com.kito.testing.FakeCredentialsRepository
import com.kito.core.sync.domain.SyncUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val tempPath = "settings_prefs_test.preferences_pb".toPath()
    private lateinit var prefsRepository: PrefsRepository
    private lateinit var datastoreScope: CoroutineScope

    private lateinit var secureStorage: SecureStorage
    private lateinit var fakeCredentials: FakeCredentialsRepository
    private lateinit var fakeAttendanceRepository: FakeAttendanceRepository
    private lateinit var spySyncUseCase: SpySyncUseCase
    private lateinit var fakeNotificationController: FakeNotificationController
    private lateinit var spyAuthRepository: SpyAuthRepository

    class FakeNotificationController : NotificationController {
        var syncCalled = false
        override suspend fun sync() {
            syncCalled = true
        }
    }

    class SpySyncUseCase : SyncUseCase {
        var syncAllRoll: String? = null
        var syncAllPassword: String? = null
        var syncAllYear: String? = null
        var syncAllTerm: String? = null
        var result = Result.success(Unit)

        override suspend fun syncAll(
            roll: String,
            sapPassword: String,
            year: String,
            term: String
        ): Result<Unit> {
            syncAllRoll = roll
            syncAllPassword = sapPassword
            syncAllYear = year
            syncAllTerm = term
            return result
        }
    }

    class SpyAuthRepository : AuthRepository by FakeAuthRepository() {
        var updatedDisplayName: String? = null
        override suspend fun updateDisplayName(name: String) {
            updatedDisplayName = name
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        datastoreScope = CoroutineScope(testDispatcher + SupervisorJob())
        prefsRepository = PrefsRepositoryImpl(
            PreferenceDataStoreFactory.createWithPath(
                scope = datastoreScope,
                produceFile = { tempPath }
            )
        )
        secureStorage = SecureStorage()
        fakeCredentials = FakeCredentialsRepository()
        fakeAttendanceRepository = FakeAttendanceRepository()
        spySyncUseCase = SpySyncUseCase()
        fakeNotificationController = FakeNotificationController()
        spyAuthRepository = SpyAuthRepository()
    }

    @AfterTest
    fun teardown() {
        datastoreScope.cancel()
        Dispatchers.resetMain()
        try {
            FileSystem.SYSTEM.delete(tempPath)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun vm() = SettingsViewModel(
        prefs = prefsRepository,
        credentialsRepository = fakeCredentials,
        attendanceRepository = fakeAttendanceRepository,
        appSyncUseCase = spySyncUseCase,
        notificationController = fakeNotificationController,
        changeYearTermUseCase = com.kito.core.sync.domain.usecase.ChangeYearTermUseCase(
            prefs = prefsRepository,
            credentialsRepository = fakeCredentials,
            attendanceRepository = fakeAttendanceRepository,
            syncUseCase = spySyncUseCase
        ),
        authRepository = spyAuthRepository,
        dispatcher = testDispatcher,
    )

    @Test
    fun initialStates_areDefault() = runTest(testDispatcher) {
        val v = vm()
        assertIs<SyncUiState.Idle>(v.syncState.value)
        assertEquals("", v.name.value)
        assertEquals("", v.rollNumber.value)
        assertEquals("", v.year.value)
        assertEquals("Unknown", v.term.value)
        assertEquals(0, v.requiredAttendance.value)
        assertFalse(v.isLoggedIn.value)
        assertFalse(v.notificationState.value)
        assertFalse(v.pendingNotificationEnable.value)
    }

    @Test
    fun syncStateIdle_resetsToIdle() = runTest(testDispatcher) {
        val v = vm()
        v.onEvent(SettingsEvent.SyncStateIdle)
        assertIs<SyncUiState.Idle>(v.syncState.value)
    }

    @Test
    fun requestEnableNotifications_setsPendingEnableTrue() = runTest(testDispatcher) {
        val v = vm()
        v.onEvent(SettingsEvent.RequestEnableNotifications)
        assertTrue(v.pendingNotificationEnable.value)
    }

    @Test
    fun clearPendingNotificationEnable_setsPendingEnableFalse() = runTest(testDispatcher) {
        val v = vm()
        v.onEvent(SettingsEvent.RequestEnableNotifications)
        assertTrue(v.pendingNotificationEnable.value)

        v.onEvent(SettingsEvent.ClearPendingNotificationEnable)
        assertFalse(v.pendingNotificationEnable.value)
    }

    @Test
    fun retryPendingNotificationEnable_retriesPendingNotification() = runTest(testDispatcher) {
        val v = vm()
        v.onEvent(SettingsEvent.RequestEnableNotifications)
        assertTrue(v.pendingNotificationEnable.value)

        v.onEvent(SettingsEvent.RetryPendingNotificationEnable)
        assertTrue(v.pendingNotificationEnable.value)
    }

    @Test
    fun changeName_formatsAndSavesName() = runTest(testDispatcher) {
        val v = vm()
        val namesList = mutableListOf<String>()
        val syncStates = mutableListOf<SyncUiState>()

        val job1 = launch { v.name.collect { namesList.add(it) } }
        val job2 = launch { v.syncState.collect { syncStates.add(it) } }

        v.onEvent(SettingsEvent.ChangeName("  jane   doe  "))
        advanceUntilIdle()

        assertEquals("Jane Doe", prefsRepository.userNameFlow.first())
        assertEquals("Jane Doe", spyAuthRepository.updatedDisplayName)
        assertIs<SyncUiState.Success>(v.syncState.value)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun changeRoll_clearsSapPassword_deletesAttendance_schedulesSync() = runTest(testDispatcher) {
        val v = vm()
        fakeCredentials.saveSapPassword("old_pwd")
        fakeAttendanceRepository.emit(
            listOf(
                Attendance(
                    subjectCode = "MATH101",
                    subjectName = "Math",
                    attendedClasses = 5,
                    totalClasses = 6,
                    percentage = 83.3,
                    facultyName = "Dr. Smith"
                )
            )
        )

        v.onEvent(SettingsEvent.ChangeRoll("999999"))
        advanceUntilIdle()

        assertEquals("999999", prefsRepository.userRollFlow.first())
        assertEquals("", fakeCredentials.getSapPassword())
        assertTrue(fakeAttendanceRepository.observeAttendance(flowOf("2024"), flowOf("010")).first().isEmpty())
        assertEquals("999999", spySyncUseCase.syncAllRoll)
        assertEquals("", spySyncUseCase.syncAllPassword)
        assertIs<SyncUiState.Success>(v.syncState.value)
    }

    @Test
    fun changeAttendance_setsRequiredAttendance() = runTest(testDispatcher) {
        val v = vm()
        v.onEvent(SettingsEvent.ChangeAttendance(85))
        advanceUntilIdle()

        assertEquals(85, prefsRepository.requiredAttendanceFlow.first())
        assertIs<SyncUiState.Success>(v.syncState.value)
    }

    @Test
    fun changeYearTerm_updatesPrefs_deletesAttendance_syncsAll() = runTest(testDispatcher) {
        val v = vm()
        prefsRepository.setUserRollNumber("123456")
        fakeCredentials.saveSapPassword("pwd")
        fakeAttendanceRepository.emit(
            listOf(
                Attendance(
                    subjectCode = "MATH101",
                    subjectName = "Math",
                    attendedClasses = 5,
                    totalClasses = 6,
                    percentage = 83.3,
                    facultyName = "Dr. Smith"
                )
            )
        )

        v.onEvent(SettingsEvent.ChangeYearTerm("2026", "020"))
        advanceUntilIdle()

        assertEquals("2026", prefsRepository.academicYearFlow.first())
        assertEquals("020", prefsRepository.termCodeFlow.first())
        assertTrue(fakeAttendanceRepository.observeAttendance(flowOf("2024"), flowOf("010")).first().isEmpty())
        assertEquals("123456", spySyncUseCase.syncAllRoll)
        assertEquals("pwd", spySyncUseCase.syncAllPassword)
        assertEquals("2026", spySyncUseCase.syncAllYear)
        assertEquals("020", spySyncUseCase.syncAllTerm)
        assertIs<SyncUiState.Success>(v.syncState.value)
    }

    @Test
    fun logOut_clearsSapPassword_deletesAttendance() = runTest(testDispatcher) {
        val v = vm()
        fakeCredentials.saveSapPassword("my_password")
        fakeAttendanceRepository.emit(
            listOf(
                Attendance(
                    subjectCode = "CS101",
                    subjectName = "CS",
                    attendedClasses = 3,
                    totalClasses = 4,
                    percentage = 75.0,
                    facultyName = "Dr. Doe"
                )
            )
        )

        v.onEvent(SettingsEvent.LogOut)
        advanceUntilIdle()

        assertEquals("", fakeCredentials.getSapPassword())
        assertTrue(fakeAttendanceRepository.observeAttendance(flowOf("2024"), flowOf("010")).first().isEmpty())
        assertIs<SyncUiState.Success>(v.syncState.value)
    }

    @Test
    fun logIn_success_savesSapPassword() = runTest(testDispatcher) {
        val v = vm()
        prefsRepository.setUserRollNumber("777777")
        prefsRepository.setAcademicYear("2025")
        prefsRepository.setTermCode("010")

        v.onEvent(SettingsEvent.LogIn("secret"))
        advanceUntilIdle()

        assertEquals("secret", fakeCredentials.getSapPassword())
        assertEquals("777777", spySyncUseCase.syncAllRoll)
        assertEquals("secret", spySyncUseCase.syncAllPassword)
        assertEquals("2025", spySyncUseCase.syncAllYear)
        assertEquals("010", spySyncUseCase.syncAllTerm)
        assertIs<SyncUiState.Success>(v.syncState.value)
    }

    @Test
    fun logIn_failure_doesNotSaveSapPassword_setsError() = runTest(testDispatcher) {
        val v = vm()
        prefsRepository.setUserRollNumber("777777")
        prefsRepository.setAcademicYear("2025")
        prefsRepository.setTermCode("010")
        spySyncUseCase.result = Result.failure(Exception("invalid password"))

        v.onEvent(SettingsEvent.LogIn("wrong"))
        advanceUntilIdle()

        assertEquals("", fakeCredentials.getSapPassword())
        assertIs<SyncUiState.Error>(v.syncState.value)
        assertEquals("invalid password", (v.syncState.value as SyncUiState.Error).message)
    }

    @Test
    fun setNotificationState_updatesPrefsAndSyncsController() = runTest(testDispatcher) {
        val v = vm()
        v.onEvent(SettingsEvent.SetNotificationState(true))
        advanceUntilIdle()

        assertTrue(prefsRepository.notificationStateFlow.first())
        assertTrue(fakeNotificationController.syncCalled)
    }

    @Test
    fun changeRoll_passesCurrentPrefsYearTermToSyncAll() = runTest(testDispatcher) {
        val v = vm()
        prefsRepository.setAcademicYear("2025")
        prefsRepository.setTermCode("010")

        v.onEvent(SettingsEvent.ChangeRoll("111111"))
        advanceUntilIdle()

        assertEquals("111111", spySyncUseCase.syncAllRoll)
        assertEquals("", spySyncUseCase.syncAllPassword)
        assertEquals("2025", spySyncUseCase.syncAllYear)
        assertEquals("010", spySyncUseCase.syncAllTerm)
        assertIs<SyncUiState.Success>(v.syncState.value)
    }

    @Test
    fun changeRoll_syncAllEmptyPassword_neverPassesSapCredential() = runTest(testDispatcher) {
        val v = vm()
        fakeCredentials.saveSapPassword("existing_sap_password")

        v.onEvent(SettingsEvent.ChangeRoll("222222"))
        advanceUntilIdle()

        // After changeRoll, SAP password is cleared AND syncAll is called with empty password
        assertEquals("", fakeCredentials.getSapPassword())
        assertEquals("", spySyncUseCase.syncAllPassword)
    }
}
