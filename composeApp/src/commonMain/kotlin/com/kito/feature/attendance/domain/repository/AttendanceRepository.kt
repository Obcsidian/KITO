package com.kito.feature.attendance.domain.repository

import com.kito.feature.attendance.domain.model.Attendance
import kotlinx.coroutines.flow.Flow

/**
 * Domain boundary for attendance reads and deletes. The implementation (data layer) maps Room
 * entities to domain models; the presentation layer never sees an entity.
 */
interface AttendanceRepository {
    fun observeAttendance(year: String, term: String): Flow<List<Attendance>>
    suspend fun deleteAllAttendance()
    suspend fun insertAttendance(items: List<Attendance>, year: String, term: String)
}

