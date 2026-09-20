package com.kito.feature.attendance.data

import com.kito.core.database.dao.AttendanceDAO
import com.kito.core.database.entity.AttendanceEntity
import com.kito.feature.attendance.data.mapper.toDomain
import com.kito.feature.attendance.data.mapper.toEntity
import com.kito.feature.attendance.domain.model.Attendance
import com.kito.feature.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Reuses the existing Room DAO as the data source and maps entities to domain models.
 */
class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDAO,
) : AttendanceRepository {
    // One stable Room flow (SELECT *) — always reactive to inserts/deletes — filtered in memory
    // by the selected year/term. Avoids re-creating the query per switch (which drops Room's
    // invalidation and leaves the screen stale). Rows are keyed by subject+year+term.
    override fun observeAttendance(year: Flow<String>, term: Flow<String>): Flow<List<Attendance>> =
        combine(attendanceDao.getAllAttendance(), year, term) { entities, y, t ->
            entities.filter { it.year == y && it.term == t }.map { it.toDomain() }
        }

    override suspend fun deleteAllAttendance() = attendanceDao.deleteAllAttendance()

    override suspend fun insertAttendance(items: List<Attendance>, year: String, term: String) {
        attendanceDao.insertAttendance(items.map { it.toEntity(year, term) })
    }
}

