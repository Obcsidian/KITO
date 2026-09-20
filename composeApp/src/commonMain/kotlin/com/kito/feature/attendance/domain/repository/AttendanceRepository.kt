package com.kito.feature.attendance.domain.repository

import com.kito.feature.attendance.domain.model.Attendance
import kotlinx.coroutines.flow.Flow

/** Scopes the observed attendance to the currently-selected year/term (both reactive). */

/**
 * Domain boundary for attendance reads and deletes. The implementation (data layer) maps Room
 * entities to domain models; the presentation layer never sees an entity.
 */
interface AttendanceRepository {
    fun observeAttendance(year: Flow<String>, term: Flow<String>): Flow<List<Attendance>>
    suspend fun deleteAllAttendance()
    suspend fun insertAttendance(items: List<Attendance>, year: String, term: String)
}

