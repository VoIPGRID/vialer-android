package com.voipgrid.vialer.onboarding.steps.permissions

import android.Manifest
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.core.PermissionsStep

class ContactsWritePermissionStep: PermissionsStep() {
    override val title = R.string.onboarding_permission_contacts_title
    override val justification = R.string.onboarding_permission_contacts_write_justification
    override val permission = Manifest.permission.WRITE_CONTACTS
    override val icon = R.drawable.ic_perm_contact_calendar
}