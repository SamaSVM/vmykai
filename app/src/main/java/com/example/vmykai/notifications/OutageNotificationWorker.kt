package com.example.vmykai.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class OutageNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val powerWillBeOn = inputData.getBoolean(KEY_POWER_WILL_BE_ON, true)
        NotificationHelper.showStatusChangeNotification(applicationContext, powerWillBeOn)
        return Result.success()
    }

    companion object {
        const val KEY_POWER_WILL_BE_ON = "power_will_be_on"
    }
}
