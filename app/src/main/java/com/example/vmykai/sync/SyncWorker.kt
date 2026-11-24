package com.example.vmykai.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vmykai.data.OutageScheduleRepository
import com.example.vmykai.data.local.UserPreferences
import com.example.vmykai.data.model.OutageSchedule
import com.example.vmykai.data.remote.KiroeRemoteScheduleSource
import com.example.vmykai.notifications.NextChangeCalculator
import com.example.vmykai.notifications.NotificationScheduler
import com.example.vmykai.widget.OutageWidgetUpdater
import java.time.Instant
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = UserPreferences(applicationContext)
        val repository = OutageScheduleRepository(
            remote = KiroeRemoteScheduleSource(applicationContext),
            preferences = preferences,
        )

        return try {
            val schedule = repository.refresh()
            reschedulePendingNotification(schedule, preferences)
            OutageWidgetUpdater.updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun reschedulePendingNotification(
        schedule: OutageSchedule,
        preferences: UserPreferences,
    ) {
        val scheduler = NotificationScheduler(applicationContext)
        val queueId = preferences.selectedQueue.first()
        val settings = preferences.notificationSettings.first()

        if (queueId == null || !settings.enabled) {
            scheduler.cancel()
            return
        }

        val change = NextChangeCalculator.nextChange(schedule, queueId, Instant.now())
        if (change == null) scheduler.cancel() else scheduler.schedule(change, settings.leadMinutes)
    }
}
