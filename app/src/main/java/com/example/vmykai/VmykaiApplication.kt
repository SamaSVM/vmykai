package com.example.vmykai

import android.app.Application
import com.example.vmykai.sync.SyncScheduler

class VmykaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodicSync(this)
    }
}
