package com.voipgrid.vialer.callrecord.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voipgrid.vialer.util.PhoneNumberUtils
import java.util.*

@Entity(tableName = "call_records")
data class CallRecordEntity (
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "call_time") val callTime : Long,
    @ColumnInfo(name = "direction") val direction : DIRECTION,
    @ColumnInfo(name = "duration") val duration : Int,
    @ColumnInfo(name = "first_party_number") val firstPartyNumber: String,
    @ColumnInfo(name = "third_party_number") val thirdPartyNumber : String,
    @ColumnInfo(name = "caller_id") val callerId : String,
    @ColumnInfo(name = "was_personal") val wasPersonalCall : Boolean,
    @ColumnInfo(name = "was_missed") val wasMissed : Boolean,
    @ColumnInfo(name = "was_internal") val wasInternalCall : Boolean,
    @ColumnInfo(name = "was_answered_elsewhere") val wasAnsweredElsewhere : Boolean,
    @ColumnInfo(name = "destination_account") val destinationAccount : String?
) {
    enum class DIRECTION {
        OUTBOUND, INBOUND
    }

    companion object {
        const val DATE_PATERRN = "yyyy-MM-dd'T'HH:mm:ss"
    }


    fun isAnonymous() : Boolean = PhoneNumberUtils.isAnonymousNumber(thirdPartyNumber)
}