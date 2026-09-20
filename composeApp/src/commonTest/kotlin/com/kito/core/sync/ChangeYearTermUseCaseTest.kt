package com.kito.core.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kito.core.datastore.data.PrefsRepositoryImpl
import com.kito.core.datastore.domain.repository.PrefsRepository
import com.kito.core.sync.domain.SyncUseCase
import com.kito.core.sync.domain.usecase.ChangeYearTermUseCase
import com.kito.testing.FakeAttendanceRepository
import com.kito.testing.FakeCredentialsRepository
import com.kito.testing.attendance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeYearTermUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val tempPath = "usecase_prefs_test.preferences_pb".toPath()
    private lateinit var prefsRepository: PrefsRepository
    private lateinit var datastoreScope: CoroutineScope

    private lateinit var fakeCredentials: FakeCredentialsRepository
    private lateinit var fakeAttendanceRepository: FakeAttendanceRepository
    private lateinit var spySyncUseCase: SpySyncUseCase
    private lateinit var useCase: ChangeYearTermUseCase

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
        fakeCredentials = FakeCredentialsRepository()
        fakeAttendanceRepository = FakeAttendanceRepository()
        spySyncUseCase = SpySyncUseCase()
        useCase = ChangeYearTermUseCase(
            prefs = prefsRepository,
            credentialsRepository = fakeCredentials,
            attendanceRepository = fakeAttendanceRepository,
            syncUseCase = spySyncUseCase
        )
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

    @Test
    fun invoke_updatesPrefs_clearsAttendance_syncsAll() = runTest(testDispatcher) {
        prefsRepository.setUserRollNumber("roll123")
        fakeCredentials.saveSapPassword("sapPass")
        fakeAttendanceRepository.emit(listOf(attendance()))

        val result = useCase("2026", "020")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals("2026", prefsRepository.academicYearFlow.first())
        assertEquals("020", prefsRepository.termCodeFlow.first())
        assertTrue(fakeAttendanceRepository.observeAttendance("2024", "010").first().isEmpty())

        assertEquals("roll123", spySyncUseCase.syncAllRoll)
        assertEquals("sapPass", spySyncUseCase.syncAllPassword)
        assertEquals("2026", spySyncUseCase.syncAllYear)
        assertEquals("020", spySyncUseCase.syncAllTerm)
    }
}
