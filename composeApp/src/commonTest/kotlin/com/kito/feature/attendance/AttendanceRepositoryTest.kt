package com.kito.feature.attendance

import com.kito.feature.attendance.domain.repository.AttendanceRepository
import com.kito.testing.FakeAttendanceRepository
import com.kito.testing.attendance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttendanceRepositoryTest {

    private val repo: AttendanceRepository = FakeAttendanceRepository(
        listOf(attendance(subjectCode = "CS101"), attendance(subjectCode = "CS102"))
    )

    @Test
    fun observeAttendance_emitsInitialList() = runTest {
        val items = repo.observeAttendance("2024", "010").first()
        assertEquals(2, items.size)
        assertEquals("CS101", items[0].subjectCode)
        assertEquals("CS102", items[1].subjectCode)
    }

    @Test
    fun deleteAllAttendance_clearsTheList() = runTest {
        repo.deleteAllAttendance()
        val items = repo.observeAttendance("2024", "010").first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun observeAttendance_emitsDomainType() = runTest {
        // Structural: verifying the flow returns domain Attendance, not an entity
        val item = repo.observeAttendance("2024", "010").first().first()
        // If this compiles, it's a domain model (no Room annotations)
        assertEquals("CS101", item.subjectCode)
        assertEquals(80.0, item.percentage)
    }
}
