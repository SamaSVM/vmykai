package com.example.vmykai.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vmykai.R

object NotificationHelper {

    private const val CHANNEL_ID = "outage_status_channel"
    private const val NOTIFICATION_ID = 1

    fun showStatusChangeNotification(context: Context, powerWillBeOn: Boolean) {
        ensureChannel(context)

        val title = if (powerWillBeOn) {
            context.getString(R.string.notification_power_on_title)
        } else {
            context.getString(R.string.notification_power_off_title)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
