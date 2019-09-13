package com.voipgrid.vialer.persistence

import android.util.Log
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel
import java.util.*

object RemoteLogging : DefaultKotPrefModel() {

    /**
     * We want to persist the remote id even when the shared preferences are cleared,
     * so we will store it on this object too.
     *
     */
    private var localRemoteLoggingId: String? = null

    /**
     * The user's remote logging id, this is how we associate log records on the server
     * with the user reporting an issue.
     *
     */
    private var storedRemoteLoggingId by nullableStringPref(key = "PREF_REMOTE_LOGGING_ID")

    /**
     * Whether or not the user has chosen to enable remote logging.
     *
     */
    var isEnabled by booleanPref(key = "PREF_REMOTE_LOGGING", default = false)

    /**
     * The connection preference, whether or not to prompt to change networks when calling.
     *
     */
    val id : String?
        get() {
            storedRemoteLoggingId?.let { return it }
            localRemoteLoggingId?.let { return it }
            localRemoteLoggingId = generate()
            storedRemoteLoggingId = localRemoteLoggingId
            return localRemoteLoggingId
        }

    /**
     * Generate a remote logging id.
     *
     */
    private fun generate(): String {
        Log.e("TEST123", "Generating..")
        var uuid = UUID.randomUUID().toString()
        val stripIndex = uuid.indexOf("-")
        uuid = uuid.substring(0, stripIndex)
        return uuid
    }
}