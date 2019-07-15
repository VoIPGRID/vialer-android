package com.voipgrid.vialer.onboarding.steps

import android.Manifest
import com.voipgrid.vialer.R

class PhoneStatePermissionStep: PermissionsStep() {
    override val title = R.string.onboarding_permission_call_state_title
    override val justification = R.string.onboarding_permission_call_state_justification
    override val permission = Manifest.permission.READ_PHONE_STATE
    override val icon = R.drawable.ic_phone_large
}