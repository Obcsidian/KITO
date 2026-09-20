package com.kito.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kito.core.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Provided

@Provided
@Dao
interface AttendanceDAO {
    @Upsert
    suspend fun insertAttendance(attendance: List<AttendanceEntity>)

    @Delete
    suspend fun deleteAttendance(attendanceEntity: AttendanceEntity)

    @Query("SELECT * FROM AttendanceEntity")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("DELETE FROM AttendanceEntity")
    suspend fun deleteAllAttendance()
}
