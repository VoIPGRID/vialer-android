package com.voipgrid.vialer.onboarding.core

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.PermissionChecker
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import kotlinx.android.synthetic.main.onboarding_step_permissions.*

abstract class PermissionsStep: Step() {

    override val layout = R.layout.onboarding_step_permissions

    abstract val title: Int
    abstract val justification: Int
    abstract val permission: String
    abstract val icon: Int

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleTv?.text = onboarding?.getText(title)
        justificationTv.text = onboarding?.getText(justification)
        iconIv?.setImageResource(icon)

        denyButton.setOnClickListener {
            onboarding?.progress()
        }

        acceptButton.setOnClickListener {
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
            onboarding?.progress()
        }
    }

    /**
     * Check if we already have the permission, this can be overridden
     * to this for non-standard Android permissions.
     *
     */
    protected open fun alreadyHasPermission(): Boolean {
        Log.e("TEST123", "in ${this.javaClass.simpleName} checking $permission, returned: ${PermissionChecker.checkSelfPermission(VialerApplication.get(), permission)}")
        return PermissionChecker.checkSelfPermission(VialerApplication.get(), permission) == PermissionChecker.PERMISSION_GRANTED
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return alreadyHasPermission()
    }
}