package com.splunchy.android.alarmclock

import android.app.Application
import com.splunchy.android.alarmclock.service.AlarmRingService

class AlarmDroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmRingService.createNotificationChannels(this)
    }
}
