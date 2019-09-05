package com.voipgrid.vialer.tasks.launch

import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.logging.LogUuidGenerator
import java.util.*

/**
 * Generate a remote logging id if one doesn't already exist.
 *
 */
class CreateLoggingId : OnLaunchTask {

    override fun execute(application: VialerApplication) {
        if (User.remoteLogging.id == null) {
            User.remoteLogging.id = generate()
        }
    }

    fun generate(): String {
        var uuid = UUID.randomUUID().toString()
        val stripIndex = uuid.indexOf("-")
        uuid = uuid.substring(0, stripIndex)
        return uuid
    }
}