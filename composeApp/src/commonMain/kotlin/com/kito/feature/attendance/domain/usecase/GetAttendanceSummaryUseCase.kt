package com.kito.feature.attendance.domain.usecase

import com.kito.feature.attendance.domain.model.AttendanceSummary
import com.kito.feature.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes attendance for the currently-selected year/term (the repository filters a single stable
 * flow, so it stays reactive) and derives summary statistics (average / highest / lowest).
 */
class GetAttendanceSummaryUseCase(
    private val repository: AttendanceRepository,
) {
    operator fun invoke(year: Flow<String>, term: Flow<String>): Flow<AttendanceSummary> =
        repository.observeAttendance(year, term).map { items ->
            if (items.isEmpty()) {
                AttendanceSummary.Empty
            } else {
                val percentages = items.map { it.percentage }
                AttendanceSummary(
                    items = items,
                    averagePercentage = percentages.average(),
                    highestPercentage = percentages.max(),
                    lowestPercentage = percentages.min(),
                )
            }
        }
}
