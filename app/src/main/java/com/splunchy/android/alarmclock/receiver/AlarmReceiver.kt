package com.splunchy.android.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.splunchy.android.alarmclock.service.AlarmRingService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) return

        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        context.startForegroundService(serviceIntent)
    }
}
