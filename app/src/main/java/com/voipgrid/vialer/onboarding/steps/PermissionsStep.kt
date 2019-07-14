package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R

class PermissionsStep(private val title: String, private val information: String, private val permission: String): Step() {

    override val layout = R.layout.onboarding_step_permissions

    override fun onAttach(context: Context) {
        super.onAttach(context)

        Log.e("TEST123", "title $title, permissioo: $permission")
    }
}