package com.kito.feature.attendance.data

import com.kito.core.database.dao.AttendanceDAO
import com.kito.core.database.entity.AttendanceEntity
import com.kito.feature.attendance.data.mapper.toDomain
import com.kito.feature.attendance.data.mapper.toEntity
import com.kito.feature.attendance.domain.model.Attendance
import com.kito.feature.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reuses the existing Room DAO as the data source and maps entities to domain models.
 */
class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDAO,
) : AttendanceRepository {
    override fun observeAttendance(year: String, term: String): Flow<List<Attendance>> =
        attendanceDao.getAttendance(year, term).map { entities -> entities.map { it.toDomain() } }

    override suspend fun deleteAllAttendance() = attendanceDao.deleteAllAttendance()

    override suspend fun insertAttendance(items: List<Attendance>, year: String, term: String) {
        attendanceDao.insertAttendance(items.map { it.toEntity(year, term) })
    }
}

