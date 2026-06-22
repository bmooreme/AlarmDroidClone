package com.splunchy.android.alarmclock.ui.ringer

import android.app.KeyguardManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splunchy.android.alarmclock.data.ObstacleType
import com.splunchy.android.alarmclock.service.AlarmRingService
import com.splunchy.android.alarmclock.ui.theme.AlarmDroidTheme
import kotlin.math.abs
import kotlin.random.Random

class RingerActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var flipToSnooze = false
    private var alarmId = -1L
    private var lastFlipTime = 0L

    // Shake detection
    private var obstacleType = ObstacleType.NONE
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private val onShakeComplete = mutableStateOf(false)
    private val currentShakeCount = mutableStateOf(0)
    private val requiredShakes = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val km = getSystemService(KeyguardManager::class.java)
        km.requestDismissKeyguard(this, null)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alarmId = intent.getLongExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label") ?: ""
        val time = intent.getStringExtra("alarm_time") ?: ""
        flipToSnooze = intent.getBooleanExtra("flip_to_snooze", false)
        obstacleType = try {
            ObstacleType.valueOf(intent.getStringExtra("obstacle_type") ?: "NONE")
        } catch (_: Exception) { ObstacleType.NONE }

        if (flipToSnooze || obstacleType == ObstacleType.SHAKE) {
            sensorManager = getSystemService(SensorManager::class.java)
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        setContent {
            AlarmDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RingerContent(
                        time = time,
                        label = label,
                        alarmId = alarmId,
                        obstacleType = obstacleType,
                        flipToSnooze = flipToSnooze,
                        shakeProgress = currentShakeCount,
                        requiredShakes = requiredShakes,
                        shakeComplete = onShakeComplete,
                        onSnooze = { snooze(alarmId) },
                        onDismiss = { dismiss(alarmId) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val z = event.values[2]
        val now = System.currentTimeMillis()

        // Flip detection: phone face down (z < -9)
        if (flipToSnooze && z < -9.0f && now - lastFlipTime > 2000) {
            lastFlipTime = now
            snooze(alarmId)
            return
        }

        // Shake detection for obstacle
        if (obstacleType == ObstacleType.SHAKE) {
            val x = event.values[0]
            val y = event.values[1]
            val acceleration = abs(x) + abs(y) + abs(z) - 9.81f
            if (acceleration > 12 && now - lastShakeTime > 200) {
                lastShakeTime = now
                shakeCount++
                currentShakeCount.value = shakeCount
                if (shakeCount >= requiredShakes) {
                    onShakeComplete.value = true
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

@Composable
fun RingerContent(
    time: String,
    label: String,
    alarmId: Long,
    obstacleType: ObstacleType,
    flipToSnooze: Boolean,
    shakeProgress: State<Int>,
    requiredShakes: Int,
    shakeComplete: State<Boolean>,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var obstacleCleared by remember { mutableStateOf(obstacleType == ObstacleType.NONE) }

    if (obstacleType == ObstacleType.SHAKE && shakeComplete.value) {
        LaunchedEffect(Unit) { obstacleCleared = true }
    }

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

        if (flipToSnooze) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Flip phone to snooze", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(48.dp))

        when {
            obstacleType == ObstacleType.MATH && !obstacleCleared -> {
                MathObstacle(onSolved = { obstacleCleared = true })
            }
            obstacleType == ObstacleType.SHAKE && !obstacleCleared -> {
                ShakeObstacle(
                    progress = shakeProgress.value,
                    required = requiredShakes
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            FilledTonalButton(
                onClick = onSnooze,
                modifier = Modifier.size(120.dp, 56.dp)
            ) {
                Icon(Icons.Default.Snooze, null)
                Spacer(Modifier.width(8.dp))
                Text("Snooze")
            }
            Button(
                onClick = onDismiss,
                enabled = obstacleCleared,
                modifier = Modifier.size(120.dp, 56.dp)
            ) {
                Icon(Icons.Default.AlarmOff, null)
                Spacer(Modifier.width(8.dp))
                Text("Stop")
            }
        }

        AnimatedVisibility(!obstacleCleared) {
            Spacer(Modifier.height(16.dp))
            Text(
                when (obstacleType) {
                    ObstacleType.MATH -> "Solve the problem to dismiss"
                    ObstacleType.SHAKE -> "Shake your phone to dismiss"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MathObstacle(onSolved: () -> Unit) {
    val a = remember { Random.nextInt(10, 50) }
    val b = remember { Random.nextInt(10, 50) }
    val answer = remember { a + b }
    var userInput by remember { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$a + $b = ?",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = userInput,
                onValueChange = {
                    userInput = it.filter { c -> c.isDigit() }
                    isWrong = false
                },
                label = { Text("Your answer") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {
                    if (userInput.toIntOrNull() == answer) {
                        onSolved()
                    } else {
                        isWrong = true
                    }
                }),
                isError = isWrong,
                supportingText = if (isWrong) {{ Text("Wrong! Try again.") }} else null,
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (userInput.toIntOrNull() == answer) {
                    onSolved()
                } else {
                    isWrong = true
                }
            }) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun ShakeObstacle(progress: Int, required: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Shake your phone!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (progress.toFloat() / required).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("$progress / $required shakes")
        }
    }
}
