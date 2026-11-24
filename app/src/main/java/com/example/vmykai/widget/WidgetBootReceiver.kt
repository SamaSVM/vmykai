package com.example.vmykai.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** AlarmManager alarms don't survive reboot or an app update, so re-arm the tick chain here. */
class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetTickScheduler.syncTickState(context.applicationContext)
    }
}
