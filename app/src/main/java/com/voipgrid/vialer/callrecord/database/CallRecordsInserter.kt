package com.voipgrid.vialer.callrecord.database

import android.database.sqlite.SQLiteConstraintException
import com.voipgrid.vialer.api.models.CallRecord
import com.voipgrid.vialer.api.models.CallRecord.DIRECTION_OUTBOUND
import com.voipgrid.vialer.callrecord.importing.CallRecordsImporter
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

class CallRecordsInserter(private val db: CallRecordDao) {

    /**
     * Insert a call record into the database, performing any necessary data
     * integrity checks first.
     *
     */
    @Throws(SQLiteConstraintException::class)
    fun insert(record: CallRecord, personal: Boolean) {
        val existingRecordEntity = db.findCallRecordById(record.id)

        if (existingRecordEntity != null) {
            updatePersonalFlagWhenNecessary(existingRecordEntity, personal)
            return
        }

        db.insert(createCallRecordEntity(record, personal))
    }

    /**
     * Creates the actual call record entity to insert into the database from the call record
     * in the api response.
     *
     */
    private fun createCallRecordEntity(record: CallRecord, personal: Boolean) : CallRecordEntity =
        CallRecordEntity(
                record.id,
                convertCallTime(record.callDate),
                if (DIRECTION_OUTBOUND == record.direction) CallRecordEntity.DIRECTION.OUTBOUND else CallRecordEntity.DIRECTION.INBOUND,
                record.duration,
                record.firstPartyNumber,
                record.thirdPartyNumber,
                record.callerId,
                personal,
                record.wasMissed(),
                record.isInternalCall,
                record.wasAnsweredElsewhere(),
                record.destinationAccount
        )

    /**
     * Convert the dutch time received from the api to a UTC timestamp to store
     * in the database.
     *
     */
    private fun convertCallTime(callDate: String): Long =
        DateTime.parse(callDate, DateTimeFormat.forPattern(CallRecordEntity.DATE_PATTERN))
                .withZoneRetainFields(CallRecordsImporter.TIMEZONE)
                .toDateTime()
                .withZone(DateTimeZone.UTC)
                .millis


    /**
     * It is possible that we have already stored a call in the database and flagged it as non-personal
     * but then it shows up again in the personal calls. If we encounter one of these, we want to make sure
     * we flip the was personal calls flag.
     *
     */
    private fun updatePersonalFlagWhenNecessary(existingRecordEntity: CallRecordEntity, personal: Boolean) {
        if (!existingRecordEntity.wasPersonalCall && personal) {
            db.flagCallAsPersonal(existingRecordEntity.id)
        }
    }
}