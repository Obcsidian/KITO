package com.kito.feature.attendance

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kito.core.database.AppDB
import com.kito.core.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression guard for the "both old and new attendance show after a year/term switch" bug.
 *
 * Attendance rows are keyed by (subjectName, year, term), so the table legitimately holds several
 * terms at once. The UI read must therefore filter by the selected term — the old `SELECT *`
 * leaked every term into the view. This drives the REAL Room DAO to prove the filter, not a fake.
 */
class AttendanceDaoFilterTest {

    private val dbFile = File(
        System.getProperty("java.io.tmpdir"),
        "kito_attendance_test_${System.nanoTime()}.db"
    )
    private val db = Room.databaseBuilder<AppDB>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
    private val dao = db.attendanceDao()

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun getAttendance_returnsOnlySelectedYearTerm() = runBlocking {
        // Two terms coexist in the table — exactly the post-switch state that used to show both.
        dao.insertAttendance(
            listOf(
                AttendanceEntity("CS1", "Maths", 8, 10, 80.0, "Dr A", year = "2024", term = "010"),
                AttendanceEntity("CS2", "Physics", 5, 10, 50.0, "Dr B", year = "2024", term = "010"),
                AttendanceEntity("CS1", "DSA", 9, 10, 90.0, "Dr C", year = "2025", term = "020"),
            )
        )

        val autumn2024 = dao.getAttendance("2024", "010").first()
        assertEquals(2, autumn2024.size, "should see only the two 2024/010 subjects, not all three")
        assertEquals(setOf("Maths", "Physics"), autumn2024.map { it.subjectName }.toSet())

        val spring2025 = dao.getAttendance("2025", "020").first()
        assertEquals(1, spring2025.size, "should see only the single 2025/020 subject")
        assertEquals("DSA", spring2025.first().subjectName)

        // A term with no rows returns nothing (not the stale other-term rows).
        assertEquals(0, dao.getAttendance("2099", "010").first().size)
    }
}
