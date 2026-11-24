package com.example.vmykai.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.vmykai.sync.SyncScheduler

/** Triggers an immediate schedule sync from the widget's manual refresh button. */
class RefreshScheduleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SyncScheduler.triggerImmediateSync(context)
    }
}
