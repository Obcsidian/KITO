package com.kito.feature.attendance.domain.usecase

import com.kito.feature.attendance.domain.model.AttendanceSummary
import com.kito.feature.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Observes attendance for the currently-selected year/term and derives summary statistics
 * (average / highest / lowest).
 *
 * Attendance rows are keyed by (subject, year, term), so the table can legitimately hold several
 * terms at once (e.g. mid year/term switch, or a background sync of another term). We therefore
 * scope the observed rows to the selected year/term rather than showing the whole table — otherwise
 * stale rows from a previous term leak into the view. Reactive: switching year/term re-queries.
 */
class GetAttendanceSummaryUseCase(
    private val repository: AttendanceRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(year: Flow<String>, term: Flow<String>): Flow<AttendanceSummary> =
        combine(year, term) { y, t -> y to t }
            .distinctUntilChanged()
            .flatMapLatest { (y, t) -> repository.observeAttendance(y, t) }
            .map { items ->
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
