package com.example.vmykai.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        if (!WidgetTickScheduler.hasAnyWidgets(appContext)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                OutageWidgetUpdater.updateAll(appContext)
            } finally {
                WidgetTickScheduler.scheduleNextTick(appContext)
                pendingResult.finish()
            }
        }
    }
}
