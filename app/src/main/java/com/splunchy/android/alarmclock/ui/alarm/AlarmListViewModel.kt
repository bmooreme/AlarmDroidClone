package com.splunchy.android.alarmclock.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.splunchy.android.alarmclock.data.Alarm
import com.splunchy.android.alarmclock.data.AlarmDatabase
import com.splunchy.android.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AlarmDatabase.getInstance(application).alarmDao()

    val alarms = dao.getAllAlarms().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun addAlarm(hour: Int, minute: Int) {
        viewModelScope.launch {
            val alarm = Alarm(hour = hour, minute = minute)
            val id = dao.insert(alarm)
            AlarmScheduler.schedule(getApplication(), alarm.copy(id = id))
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(enabled = !alarm.enabled)
            dao.update(updated)
            if (updated.enabled) {
                AlarmScheduler.schedule(getApplication(), updated)
            } else {
                AlarmScheduler.cancel(getApplication(), updated)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            dao.delete(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            dao.update(alarm)
            if (alarm.enabled) {
                AlarmScheduler.schedule(getApplication(), alarm)
            } else {
                AlarmScheduler.cancel(getApplication(), alarm)
            }
        }
    }
}
