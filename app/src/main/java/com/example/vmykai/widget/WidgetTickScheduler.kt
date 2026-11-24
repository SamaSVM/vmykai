package com.example.vmykai.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.vmykai.notifications.NextChangeCalculator
import java.time.ZonedDateTime

/** Drives the per-minute "now" marker/clock label on the widgets while at least one instance is
 * pinned, since Glance content only repaints when something explicitly calls `updateAll`. */
object WidgetTickScheduler {

    private const val REQUEST_CODE = 1001

    fun hasAnyWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val full = manager.getAppWidgetIds(ComponentName(context, OutageWidgetFullReceiver::class.java))
        val compact = manager.getAppWidgetIds(ComponentName(context, OutageWidgetCompactReceiver::class.java))
        return full.isNotEmpty() || compact.isNotEmpty()
    }

    /** Starts the tick chain if any widget is pinned, otherwise cancels it. Safe to call repeatedly. */
    fun syncTickState(context: Context) {
        if (hasAnyWidgets(context)) scheduleNextTick(context) else cancelTicks(context)
    }

    fun scheduleNextTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = ZonedDateTime.now(NextChangeCalculator.KYIV_ZONE)
            .plusMinutes(1)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
        val pendingIntent = tickPendingIntent(context)
        if (ExactAlarmPermission.isGranted(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelTicks(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(tickPendingIntent(context))
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetTickReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
