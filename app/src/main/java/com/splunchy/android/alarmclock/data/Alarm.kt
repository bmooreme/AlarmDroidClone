package com.splunchy.android.alarmclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false,
    val vibrate: Boolean = true,
    val ringtoneUri: String? = null,
    val snoozeDurationMinutes: Int = 10,
    val volume: Int = -1,
    val gradualVolume: Boolean = false,
) {
    val isRepeating: Boolean
        get() = monday || tuesday || wednesday || thursday || friday || saturday || sunday

    val repeatDays: List<Int>
        get() = buildList {
            if (monday) add(java.util.Calendar.MONDAY)
            if (tuesday) add(java.util.Calendar.TUESDAY)
            if (wednesday) add(java.util.Calendar.WEDNESDAY)
            if (thursday) add(java.util.Calendar.THURSDAY)
            if (friday) add(java.util.Calendar.FRIDAY)
            if (saturday) add(java.util.Calendar.SATURDAY)
            if (sunday) add(java.util.Calendar.SUNDAY)
        }

    fun repeatSummary(): String {
        if (!isRepeating) return "Once"
        val days = mutableListOf<String>()
        if (monday) days.add("Mon")
        if (tuesday) days.add("Tue")
        if (wednesday) days.add("Wed")
        if (thursday) days.add("Thu")
        if (friday) days.add("Fri")
        if (saturday) days.add("Sat")
        if (sunday) days.add("Sun")
        if (days.size == 7) return "Every day"
        if (days == listOf("Mon", "Tue", "Wed", "Thu", "Fri")) return "Weekdays"
        if (days == listOf("Sat", "Sun")) return "Weekends"
        return days.joinToString(", ")
    }

    fun timeString(): String = "%02d:%02d".format(hour, minute)
}
