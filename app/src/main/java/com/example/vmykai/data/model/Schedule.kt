package com.example.vmykai.data.model

import java.time.Instant
import java.time.LocalDate

data class TimeSlot(val startMinute: Int, val hasPower: Boolean) {
    companion object {
        const val MINUTES_PER_SLOT = 30
    }
}

data class DaySchedule(val date: LocalDate, val slots: List<TimeSlot>)

data class OutageSchedule(
    val lastSyncedAt: Instant,
    val byQueue: Map<QueueId, List<DaySchedule>>,
) {
    fun daysFor(queueId: QueueId): List<DaySchedule> = byQueue[queueId].orEmpty()
}
