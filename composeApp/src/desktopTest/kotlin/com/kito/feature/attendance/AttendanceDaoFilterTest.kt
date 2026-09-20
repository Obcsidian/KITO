package com.kito.feature.attendance

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kito.core.database.AppDB
import com.kito.feature.attendance.data.AttendanceRepositoryImpl
import com.kito.feature.attendance.domain.model.Attendance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression guard for the "both old and new attendance show after a year/term switch" bug.
 *
 * Attendance rows are keyed by (subjectName, year, term), so the table legitimately holds several
 * terms at once. The repository observes ONE stable Room flow (SELECT *) and filters it in memory
 * by the selected year/term — this both scopes the view to one term AND stays reactive to inserts
 * (an earlier filtered-query-per-switch approach dropped Room's invalidation and left rows hidden).
 * Drives the REAL Room DAO through the repository, not a fake.
 */
class AttendanceDaoFilterTest {

    private val dbFile = File(
        System.getProperty("java.io.tmpdir"),
        "kito_attendance_test_${System.nanoTime()}.db"
    )
    private val db = Room.databaseBuilder<AppDB>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repo = AttendanceRepositoryImpl(db.attendanceDao())

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun observeAttendance_returnsOnlySelectedYearTerm() = runBlocking {
        // Two terms coexist — exactly the post-switch state that used to show both.
        repo.insertAttendance(
            listOf(
                Attendance("CS1", "Maths", 8, 10, 80.0, "Dr A"),
                Attendance("CS2", "Physics", 5, 10, 50.0, "Dr B"),
            ),
            year = "2026", term = "010",
        )
        repo.insertAttendance(
            listOf(Attendance("CS1", "DSA", 9, 10, 90.0, "Dr C")),
            year = "2025", term = "020",
        )

        val autumn2026 = repo.observeAttendance(flowOf("2026"), flowOf("010")).first()
        assertEquals(
            setOf("Maths", "Physics"),
            autumn2026.map { it.subjectName }.toSet(),
            "should see only the two 2026/010 subjects, not all three",
        )

        val spring2025 = repo.observeAttendance(flowOf("2025"), flowOf("020")).first()
        assertEquals(listOf("DSA"), spring2025.map { it.subjectName })

        // A term with no rows returns nothing (not the stale other-term rows).
        assertEquals(0, repo.observeAttendance(flowOf("2099"), flowOf("010")).first().size)
    }

    @Test
    fun observeAttendance_reflectsInsertsAfterSubscription() = runBlocking {
        // Reactivity guard: a row inserted for the selected term after the initial (empty) read
        // must appear — this is what regressed when the query was re-created per switch.
        assertEquals(0, repo.observeAttendance(flowOf("2026"), flowOf("010")).first().size)
        repo.insertAttendance(
            listOf(Attendance("CS9", "AI", 10, 10, 100.0, "Dr D")),
            year = "2026", term = "010",
        )
        assertEquals(
            listOf("AI"),
            repo.observeAttendance(flowOf("2026"), flowOf("010")).first().map { it.subjectName },
        )
    }
}
