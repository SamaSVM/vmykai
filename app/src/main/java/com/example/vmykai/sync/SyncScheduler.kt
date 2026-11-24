package com.example.vmykai.sync

import android.content.Context
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

object SyncScheduler {

    private const val PERIODIC_WORK_NAME = "outage_schedule_sync"
    private const val MANUAL_WORK_NAME = "outage_schedule_sync_manual"
    private const val SYNC_INTERVAL_MINUTES = 30L

    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun triggerImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(MANUAL_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
