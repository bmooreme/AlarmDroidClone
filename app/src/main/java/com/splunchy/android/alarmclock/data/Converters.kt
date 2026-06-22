package com.splunchy.android.alarmclock.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromObstacleType(value: ObstacleType): String = value.name

    @TypeConverter
    fun toObstacleType(value: String): ObstacleType =
        try { ObstacleType.valueOf(value) } catch (_: Exception) { ObstacleType.NONE }
}
