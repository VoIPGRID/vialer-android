package com.voipgrid.vialer.onboarding.steps.permissions

import android.Manifest
import com.voipgrid.vialer.R

class MicrophonePermissionStep: PermissionsStep() {
    override val title = R.string.onboarding_permission_microphone_title
    override val justification = R.string.onboarding_permission_microphone_justification
    override val permission = Manifest.permission.RECORD_AUDIO
    override val icon = R.drawable.ic_mic_large
}