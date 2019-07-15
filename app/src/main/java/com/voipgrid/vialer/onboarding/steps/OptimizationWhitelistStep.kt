package com.voipgrid.vialer.onboarding.steps

import android.app.Activity
import android.content.Context
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.BatteryOptimizationManager

class OptimizationWhitelistStep: PermissionsStep() {
    override val title = R.string.onboarding_battery_optimization_whitelist_title
    override val justification = R.string.onboarding_battery_optimization_whitelist_justification
    override val permission: String = ""
    override val icon = R.drawable.ic_battery_full

    private val batteryOptimizationManager: BatteryOptimizationManager by lazy {
        BatteryOptimizationManager(onboarding as Context)
    }

    override fun performPermissionRequest() {
        batteryOptimizationManager.prompt(onboarding as Activity)
    }

    override fun alreadyHasPermission(): Boolean {
        return batteryOptimizationManager.isIgnoringBatteryOptimization()
    }
}