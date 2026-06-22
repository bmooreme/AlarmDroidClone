package com.splunchy.android.alarmclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 2, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var instance: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarmdroid.db"
                ).fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
