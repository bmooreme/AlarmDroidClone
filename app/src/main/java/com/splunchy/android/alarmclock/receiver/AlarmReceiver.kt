package com.splunchy.android.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.splunchy.android.alarmclock.data.AlarmDatabase
import com.splunchy.android.alarmclock.service.AlarmRingService
import com.splunchy.android.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                val alarm = dao.getAlarmById(alarmId) ?: return@launch

                if (alarm.skipNext) {
                    dao.setSkipNext(alarmId, false)
                    if (alarm.isRepeating) {
                        AlarmScheduler.schedule(context, alarm.copy(skipNext = false))
                    } else {
                        dao.setEnabled(alarmId, false)
                    }
                    return@launch
                }

                val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
                    putExtra("alarm_id", alarmId)
                }
                context.startForegroundService(serviceIntent)
            } finally {
                pending.finish()
            }
        }
    }
}
