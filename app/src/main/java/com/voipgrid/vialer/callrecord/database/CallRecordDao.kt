package com.voipgrid.vialer.callrecord.database

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface CallRecordDao {

    @Insert
    fun insert(callRecord: CallRecordEntity)

    @Query("SELECT * FROM call_records WHERE was_personal = :personalCalls ORDER BY call_time DESC LIMIT 1")
    fun getMostRecentCallRecord(personalCalls: Boolean): CallRecordEntity?

    @Query("SELECT * FROM call_records WHERE was_personal IN (:wasPersonal) ORDER BY call_time DESC")
    fun callRecordsByDate(wasPersonal: BooleanArray): DataSource.Factory<Int, CallRecordEntity>

    @Query("SELECT * FROM call_records WHERE was_personal IN (:wasPersonal) AND was_missed = 1 ORDER BY call_time DESC")
    fun missedCallRecordsByDate(wasPersonal: BooleanArray): DataSource.Factory<Int, CallRecordEntity>

    @Query("SELECT * FROM call_records WHERE id = :id")
    fun findCallRecordById(id: Long)  : CallRecordEntity?

    @Query("UPDATE call_records SET was_personal = 1 WHERE id = :id")
    fun flagCallAsPersonal(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun truncate()
}