package com.example.vmykai.notifications

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/** Schedules (and replaces) the single pending "status is about to change" notification. */
class NotificationScheduler(private val context: Context) {

    fun schedule(change: StatusChange, leadMinutes: Int) {
        val fireAt = change.at.minus(Duration.ofMinutes(leadMinutes.toLong()))
        val delay = Duration.between(Instant.now(), fireAt)
        if (delay.isNegative) {
            cancel()
            return
        }

        val request = OneTimeWorkRequestBuilder<OutageNotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(OutageNotificationWorker.KEY_POWER_WILL_BE_ON to change.powerWillBeOn))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "outage_status_notification"
    }
}
