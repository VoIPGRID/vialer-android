package com.voipgrid.vialer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import com.voipgrid.vialer.database.converters.Converters

@Database(entities = [CallRecordEntity::class], version = 3)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun callRecordDao(): CallRecordDao
}