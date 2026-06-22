package com.splunchy.android.alarmclock.ui.ringer

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splunchy.android.alarmclock.service.AlarmRingService
import com.splunchy.android.alarmclock.ui.theme.AlarmDroidTheme

class RingerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val km = getSystemService(KeyguardManager::class.java)
        km.requestDismissKeyguard(this, null)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val alarmId = intent.getLongExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label") ?: ""
        val time = intent.getStringExtra("alarm_time") ?: ""

        setContent {
            AlarmDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            time,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (label.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(64.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { snooze(alarmId) },
                                modifier = Modifier.size(120.dp, 56.dp)
                            ) {
                                Icon(Icons.Default.Snooze, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Snooze")
                            }
                            Button(
                                onClick = { dismiss(alarmId) },
                                modifier = Modifier.size(120.dp, 56.dp)
                            ) {
                                Icon(Icons.Default.AlarmOff, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun snooze(alarmId: Long) {
        sendServiceAction(AlarmRingService.ACTION_SNOOZE, alarmId)
        finish()
    }

    private fun dismiss(alarmId: Long) {
        sendServiceAction(AlarmRingService.ACTION_DISMISS, alarmId)
        finish()
    }

    private fun sendServiceAction(action: String, alarmId: Long) {
        val intent = Intent(this, AlarmRingService::class.java).apply {
            this.action = action
            putExtra("alarm_id", alarmId)
        }
        startService(intent)
    }
}
