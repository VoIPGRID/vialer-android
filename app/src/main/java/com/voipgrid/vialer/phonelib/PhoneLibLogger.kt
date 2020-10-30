package com.voipgrid.vialer.phonelib

import android.util.Log
import com.voipgrid.vialer.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.openvoipalliance.phonelib.repository.initialise.LogLevel
import org.openvoipalliance.phonelib.repository.initialise.LogLevel.*
import org.openvoipalliance.phonelib.repository.initialise.LogListener

class PhoneLibLogger : LogListener {
    private val logger = Logger(this)

    override fun onLogMessageWritten(lev: LogLevel, message: String) {
        GlobalScope.launch(Dispatchers.IO) {
            when (lev) {
                DEBUG -> Log.i("LINPHONE", message)
                TRACE -> Log.i("LINPHONE", message)
                MESSAGE -> logger.i(message)
                WARNING -> logger.w(message)
                ERROR -> logger.e(message)
                FATAL -> logger.e(message)
            }
        }
    }
}