package nl.voipgrid.vialer_voip.providers.pjsip.logging

import android.util.Log
import org.pjsip.pjsua2.LogEntry
import org.pjsip.pjsua2.LogWriter

class PjsipLogWriter : LogWriter() {

    override fun write(entry: LogEntry) {
        super.write(entry)
        Log.e("TEST1SIP", "pjsip: ${entry.msg}")
    }
}