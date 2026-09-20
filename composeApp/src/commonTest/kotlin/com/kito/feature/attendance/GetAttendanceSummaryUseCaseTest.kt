package com.kito.feature.attendance

import com.kito.feature.attendance.domain.model.AttendanceSummary
import com.kito.feature.attendance.domain.usecase.GetAttendanceSummaryUseCase
import com.kito.testing.FakeAttendanceRepository
import com.kito.testing.attendance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAttendanceSummaryUseCaseTest {

    private fun useCase(repo: FakeAttendanceRepository) = GetAttendanceSummaryUseCase(repo)

    @Test
    fun invoke_emptyList_returnsEmpty() = runTest {
        val result = useCase(FakeAttendanceRepository()).invoke(flowOf("2024"), flowOf("010")).first()
        assertEquals(AttendanceSummary.Empty, result)
    }

    @Test
    fun invoke_singleItem_allStatsEqual() = runTest {
        val repo = FakeAttendanceRepository(listOf(attendance(percentage = 75.0)))
        val result = useCase(repo).invoke(flowOf("2024"), flowOf("010")).first()
        assertEquals(75.0, result.averagePercentage)
        assertEquals(75.0, result.highestPercentage)
        assertEquals(75.0, result.lowestPercentage)
        assertEquals(1, result.items.size)
    }

    @Test
    fun invoke_multipleItems_correctStats() = runTest {
        val repo = FakeAttendanceRepository(listOf(
            attendance(percentage = 60.0),
            attendance(percentage = 80.0),
            attendance(percentage = 100.0),
        ))
        val result = useCase(repo).invoke(flowOf("2024"), flowOf("010")).first()
        assertEquals(80.0, result.averagePercentage)
        assertEquals(100.0, result.highestPercentage)
        assertEquals(60.0, result.lowestPercentage)
        assertEquals(3, result.items.size)
    }

    @Test
    fun invoke_reEmit_updatesStats() = runTest {
        val repo = FakeAttendanceRepository(listOf(attendance(percentage = 50.0)))
        val uc = useCase(repo)

        val first = uc.invoke(flowOf("2024"), flowOf("010")).first()
        assertEquals(50.0, first.averagePercentage)

        repo.emit(listOf(attendance(percentage = 90.0)))
        val second = uc.invoke(flowOf("2024"), flowOf("010")).first()
        assertEquals(90.0, second.averagePercentage)
    }
}
