package com.splunchy.android.alarmclock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.splunchy.android.alarmclock.data.Alarm
import com.splunchy.android.alarmclock.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(context, alarm)
            return
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val triggerTime = nextTriggerTime(alarm)
        val intent = createPendingIntent(context, alarm)

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, intent),
            intent
        )
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(createPendingIntent(context, alarm))
    }

    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        alarms.filter { it.enabled }.forEach { schedule(context, it) }
    }

    fun nextTriggerTime(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!alarm.isRepeating) {
            if (trigger.before(now) || trigger == now) {
                trigger.add(Calendar.DAY_OF_YEAR, 1)
            }
            return trigger.timeInMillis
        }

        val repeatDays = alarm.repeatDays
        for (i in 0..6) {
            val candidate = (trigger.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in repeatDays) {
                if (candidate.after(now)) {
                    return candidate.timeInMillis
                }
            }
        }

        return trigger.apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
    }

    private fun createPendingIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
