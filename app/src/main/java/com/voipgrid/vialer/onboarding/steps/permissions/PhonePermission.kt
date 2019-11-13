package com.voipgrid.vialer.onboarding.steps.permissions

import android.Manifest
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.core.PermissionsStep

class PhonePermission: PermissionsStep() {
    override val title = R.string.onboarding_permission_phone_title
    override val justification = R.string.onboarding_permission_phone_justification
    override val permission = Manifest.permission.CALL_PHONE
    override val icon = R.drawable.ic_dialer_sip
}