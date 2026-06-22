package com.splunchy.android.alarmclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.splunchy.android.alarmclock.R
import com.splunchy.android.alarmclock.data.Alarm
import com.splunchy.android.alarmclock.data.AlarmDatabase
import com.splunchy.android.alarmclock.ui.ringer.RingerActivity
import com.splunchy.android.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra("alarm_id", -1) ?: -1
        val action = intent?.action

        when (action) {
            ACTION_SNOOZE -> {
                handleSnooze(alarmId)
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                handleDismiss(alarmId)
                return START_NOT_STICKY
            }
        }

        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildRingingNotification(alarmId))

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = AlarmDatabase.getInstance(this@AlarmRingService).alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                launch(Dispatchers.Main) {
                    startRinging(alarm)
                    launchRingerActivity(alarm)
                }
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startRinging(alarm: Alarm) {
        val ringtoneUri = if (alarm.ringtoneUri != null) {
            Uri.parse(alarm.ringtoneUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmRingService, ringtoneUri)
            isLooping = true
            prepare()
            start()
        }

        if (alarm.vibrate) {
            val vm = getSystemService(VibratorManager::class.java)
            vibrator = vm.defaultVibrator
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
            )
        }
    }

    private fun launchRingerActivity(alarm: Alarm) {
        val intent = Intent(this, RingerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_time", alarm.timeString())
        }
        startActivity(intent)
    }

    private fun handleSnooze(alarmId: Long) {
        stopRinging()
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = AlarmDatabase.getInstance(this@AlarmRingService).alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                val snoozeAlarm = alarm.copy(
                    hour = (alarm.hour + (alarm.minute + alarm.snoozeDurationMinutes) / 60) % 24,
                    minute = (alarm.minute + alarm.snoozeDurationMinutes) % 60
                )
                AlarmScheduler.schedule(this@AlarmRingService, snoozeAlarm)
            }
            launch(Dispatchers.Main) { stopSelf() }
        }
    }

    private fun handleDismiss(alarmId: Long) {
        stopRinging()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AlarmDatabase.getInstance(this@AlarmRingService).alarmDao()
            val alarm = dao.getAlarmById(alarmId)
            if (alarm != null) {
                if (alarm.isRepeating) {
                    AlarmScheduler.schedule(this@AlarmRingService, alarm)
                } else {
                    dao.setEnabled(alarmId, false)
                }
            }
            launch(Dispatchers.Main) { stopSelf() }
        }
    }

    private fun stopRinging() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    private fun buildRingingNotification(alarmId: Long): Notification {
        val fullScreenIntent = Intent(this, RingerActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, alarmId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmRingService::class.java).apply {
            action = ACTION_DISMISS
            putExtra("alarm_id", alarmId)
        }
        val dismissPending = PendingIntent.getService(
            this, (alarmId + 1000).toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmRingService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("alarm_id", alarmId)
        }
        val snoozePending = PendingIntent.getService(
            this, (alarmId + 2000).toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm Ringing")
            .setContentText("Tap to view")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .addAction(R.drawable.ic_alarm, "Dismiss", dismissPending)
            .addAction(R.drawable.ic_alarm, "Snooze", snoozePending)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_UPCOMING = "upcoming_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SNOOZE = "com.splunchy.android.alarmclock.SNOOZE"
        const val ACTION_DISMISS = "com.splunchy.android.alarmclock.DISMISS"

        fun createNotificationChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM, "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val upcomingChannel = NotificationChannel(
                CHANNEL_UPCOMING, "Upcoming Alarms",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows next alarm info"
            }

            manager.createNotificationChannel(alarmChannel)
            manager.createNotificationChannel(upcomingChannel)
        }
    }
}
