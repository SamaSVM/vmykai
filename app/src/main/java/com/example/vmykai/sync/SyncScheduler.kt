package com.example.vmykai.sync

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

object SyncScheduler {

    private const val PERIODIC_WORK_NAME = "outage_schedule_sync"
    private const val MANUAL_WORK_NAME = "outage_schedule_sync_manual"
    private const val TRANSITION_WORK_NAME = "outage_schedule_sync_transition"
    private const val SYNC_INTERVAL_MINUTES = 30L

    private val NETWORK_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(NETWORK_CONSTRAINTS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun triggerImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(NETWORK_CONSTRAINTS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(MANUAL_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Schedules a one-off sync timed to [at], the next known power on/off transition, so the widget
     * and notification state flip promptly instead of waiting for the next periodic sync. */
    fun scheduleTransitionSync(context: Context, at: Instant) {
        val delay = Duration.between(Instant.now(), at).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(NETWORK_CONSTRAINTS)
            .setInitialDelay(delay)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(TRANSITION_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
