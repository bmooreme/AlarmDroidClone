package com.splunchy.android.alarmclock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabledAlarms(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?

    @Insert
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET skipNext = :skip WHERE id = :id")
    suspend fun setSkipNext(id: Long, skip: Boolean)
}
