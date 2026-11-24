package com.example.vmykai.data

import com.example.vmykai.data.local.UserPreferences
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.remote.RemoteScheduleSource
import kotlinx.coroutines.flow.first

class OutageScheduleRepository(
    private val remote: RemoteScheduleSource,
    private val preferences: UserPreferences,
) {

    val cachedSchedule = preferences.cachedSchedule

    /** Fetches a fresh schedule and persists it. Throws on failure; callers keep the last cache. */
    suspend fun refresh(): OutageSchedule {
        val previous = preferences.cachedSchedule.first()
        val schedule = remote.fetchSchedule()
        preferences.setCachedSchedule(schedule)

        val queueId = preferences.selectedQueue.first()
        if (queueId != null && previous != null && previous.daysFor(queueId) != schedule.daysFor(queueId)) {
            preferences.setUnseenScheduleChange(true)
        }
        return schedule
    }
}
