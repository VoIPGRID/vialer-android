package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.onboarding_step_permissions.*

abstract class PermissionsStep: Step() {

    override val layout = R.layout.onboarding_step_permissions

    abstract val title: Int
    abstract val justification: Int
    abstract val permission: String
    abstract val icon: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.findViewById<TextView>(R.id.titleTv)?.text = onboarding?.getText(title)
        view?.findViewById<TextView>(R.id.justificationTv)?.text = onboarding?.getText(justification)
        view?.findViewById<ImageView>(R.id.iconIv)?.setImageResource(icon)
        return view
    }

    override fun onResume() {
        super.onResume()

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
        return ContextCompat.checkSelfPermission(onboarding as Context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return alreadyHasPermission()
    }
}