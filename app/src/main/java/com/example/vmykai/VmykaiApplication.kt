package com.example.vmykai

import android.app.Application
import com.example.vmykai.sync.SyncScheduler
import com.example.vmykai.widget.WidgetTickScheduler

class VmykaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodicSync(this)
        WidgetTickScheduler.syncTickState(this)
    }
}
