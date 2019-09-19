package com.voipgrid.vialer.onboarding.core

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.PermissionChecker
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.logging.Logger
import kotlinx.android.synthetic.main.onboarding_step_permissions.*

abstract class PermissionsStep : Step() {

    override val layout = R.layout.onboarding_step_permissions

    abstract val title: Int
    abstract val justification: Int
    abstract val permission: String
    abstract val icon: Int

    private val logger = Logger(this).forceRemoteLogging(true)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleTv?.text = onboarding?.getText(title)
        justificationTv.text = onboarding?.getText(justification)
        iconIv?.setImageResource(icon)

        logger.i("Prompting user for the $permission permission")

        denyButton.setOnClickListener {
            logger.i("User chose to skip $permission permission")
            onboarding?.progress(this)
        }

        acceptButton.setOnClickListener {
            logger.i("User chose to accept $permission permission, launching Android permission prompt")
            performPermissionRequest()
        }
    }

    /**
     * Perform the actual permission request, this can be overridden
     * to use for non-standard Android permissions.
     *
     */
    protected open fun performPermissionRequest() {
        onboarding?.requestPermission(permission) {
            logger.i("User has completed Android permission prompt for $permission, continuing with onboarding...")
            onboarding?.progress(this)
        }
    }

    /**
     * Check if we already have the permission, this can be overridden
     * to this for non-standard Android permissions.
     *
     */
    protected open fun alreadyHasPermission(): Boolean {
        return PermissionChecker.checkSelfPermission(VialerApplication.get(), permission) == PermissionChecker.PERMISSION_GRANTED
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return alreadyHasPermission()
    }
}