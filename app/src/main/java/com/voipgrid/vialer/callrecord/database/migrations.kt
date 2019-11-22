package com.voipgrid.vialer.callrecord.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM call_records")
        User.internal.callRecordMonthsImported = mutableListOf()
        HistoricCallRecordsImporter.Worker.start(VialerApplication.get())
    }
}