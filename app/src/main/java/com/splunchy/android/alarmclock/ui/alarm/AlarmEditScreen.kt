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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.splunchy.android.alarmclock.data.Alarm
import java.util.Calendar

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
                            saturday = saturday, sunday = sunday
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

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vibrate", modifier = Modifier.padding(top = 12.dp))
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Snooze (minutes)", modifier = Modifier.padding(top = 12.dp))
                Row {
                    listOf(5, 10, 15, 20).forEach { mins ->
                        FilterChip(
                            selected = snoozeDuration == mins,
                            onClick = { snoozeDuration = mins },
                            label = { Text("$mins") },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
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
fun DayChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) }
    )
}
