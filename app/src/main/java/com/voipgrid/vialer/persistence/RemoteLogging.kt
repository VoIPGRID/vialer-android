package com.voipgrid.vialer.persistence

import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object RemoteLogging : DefaultKotPrefModel() {

    /**
     * The user's remote logging id, this is how we associate log records on the server
     * with the user reporting an issue.
     *
     */
    var id by nullableStringPref(key = "PREF_REMOTE_LOGGING_ID")

    /**
     * Whether or not the user has chosen to enable remote logging.
     *
     */
    var isEnabled by booleanPref(key = "PREF_REMOTE_LOGGING", default = false)
}