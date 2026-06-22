package com.splunchy.android.alarmclock.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.splunchy.android.alarmclock.data.Alarm
import com.splunchy.android.alarmclock.ui.alarm.AlarmEditScreen
import com.splunchy.android.alarmclock.ui.alarm.AlarmListScreen
import com.splunchy.android.alarmclock.ui.alarm.AlarmListViewModel
import com.splunchy.android.alarmclock.ui.theme.AlarmDroidTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        setContent {
            AlarmDroidTheme {
                val navController = rememberNavController()
                val viewModel: AlarmListViewModel = viewModel()
                var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

                NavHost(navController, startDestination = "alarms") {
                    composable("alarms") {
                        AlarmListScreen(
                            viewModel = viewModel,
                            onEditAlarm = { alarm ->
                                editingAlarm = alarm
                                navController.navigate("edit")
                            }
                        )
                    }
                    composable("edit") {
                        editingAlarm?.let { alarm ->
                            AlarmEditScreen(
                                alarm = alarm,
                                onSave = { updated ->
                                    viewModel.updateAlarm(updated)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }
}
