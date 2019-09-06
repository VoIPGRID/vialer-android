package com.voipgrid.vialer.database.converters

import androidx.room.TypeConverter
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import java.text.SimpleDateFormat
import java.util.*

class Converters {
    @TypeConverter
    fun directionFromString(value: String?) : CallRecordEntity.DIRECTION? {
        return value?.let { CallRecordEntity.DIRECTION.valueOf(value) }
    }

    @TypeConverter
    fun stringFromDirection(value: CallRecordEntity.DIRECTION?) : String? {
        return value.toString()
    }
}