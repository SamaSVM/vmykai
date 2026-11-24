package com.example.vmykai.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object OutageWidgetUpdater {
    suspend fun updateAll(context: Context) {
        OutageWidgetFull().updateAll(context)
        OutageWidgetCompact().updateAll(context)
    }
}
