package com.splunchy.android.alarmclock.ui.alarm

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.splunchy.android.alarmclock.data.Alarm
import com.splunchy.android.alarmclock.data.ObstacleType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarm: Alarm,
    onSave: (Alarm) -> Unit,
    onBack: () -> Unit
) {
    var label by remember { mutableStateOf(alarm.label) }
    var vibrate by remember { mutableStateOf(alarm.vibrate) }
    var snoozeDuration by remember { mutableIntStateOf(alarm.snoozeDurationMinutes) }
    var monday by remember { mutableStateOf(alarm.monday) }
    var tuesday by remember { mutableStateOf(alarm.tuesday) }
    var wednesday by remember { mutableStateOf(alarm.wednesday) }
    var thursday by remember { mutableStateOf(alarm.thursday) }
    var friday by remember { mutableStateOf(alarm.friday) }
    var saturday by remember { mutableStateOf(alarm.saturday) }
    var sunday by remember { mutableStateOf(alarm.sunday) }
    var showTimePicker by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var internetRadioUrl by remember { mutableStateOf(alarm.internetRadioUrl ?: "") }
    var speakingClock by remember { mutableStateOf(alarm.speakingClock) }
    var flipToSnooze by remember { mutableStateOf(alarm.flipToSnooze) }
    var obstacleType by remember { mutableStateOf(alarm.obstacleType) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(alarm.copy(
                            hour = hour, minute = minute,
                            label = label, vibrate = vibrate,
                            snoozeDurationMinutes = snoozeDuration,
                            monday = monday, tuesday = tuesday, wednesday = wednesday,
                            thursday = thursday, friday = friday,
                            saturday = saturday, sunday = sunday,
                            internetRadioUrl = internetRadioUrl.ifBlank { null },
                            speakingClock = speakingClock,
                            flipToSnooze = flipToSnooze,
                            obstacleType = obstacleType,
                        ))
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTimePicker = true }
            ) {
                Text(
                    "%02d:%02d".format(hour, minute),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(24.dp)
                )
            }

            // Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Repeat days
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayChip("M", monday) { monday = it }
                DayChip("T", tuesday) { tuesday = it }
                DayChip("W", wednesday) { wednesday = it }
                DayChip("T", thursday) { thursday = it }
                DayChip("F", friday) { friday = it }
                DayChip("S", saturday) { saturday = it }
                DayChip("S", sunday) { sunday = it }
            }

            HorizontalDivider()

            // Sound section
            Text("Sound", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = internetRadioUrl,
                onValueChange = { internetRadioUrl = it },
                label = { Text("Internet Radio URL (optional)") },
                placeholder = { Text("https://stream.example.com/radio.mp3") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Leave empty to use default alarm tone") }
            )

            SwitchRow("Speaking Clock", "Announces time and day", speakingClock) {
                speakingClock = it
            }

            SwitchRow("Vibrate", null, vibrate) { vibrate = it }

            HorizontalDivider()

            // Snooze
            Text("Snooze", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(5, 10, 15, 20).forEach { mins ->
                    FilterChip(
                        selected = snoozeDuration == mins,
                        onClick = { snoozeDuration = mins },
                        label = { Text("${mins}m") }
                    )
                }
            }

            SwitchRow(
                "Flip to Snooze",
                "Place phone face down to snooze",
                flipToSnooze
            ) { flipToSnooze = it }

            HorizontalDivider()

            // Dismiss obstacle
            Text("Dismiss Challenge", style = MaterialTheme.typography.titleMedium)
            Text(
                "Require a challenge to dismiss the alarm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ObstacleType.entries.forEach { type ->
                    FilterChip(
                        selected = obstacleType == type,
                        onClick = { obstacleType = type },
                        label = {
                            Text(when (type) {
                                ObstacleType.NONE -> "None"
                                ObstacleType.MATH -> "Math"
                                ObstacleType.SHAKE -> "Shake"
                            })
                        }
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                hour = h
                minute = m
                showTimePicker = false
            }
        )
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DayChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) }
    )
}
