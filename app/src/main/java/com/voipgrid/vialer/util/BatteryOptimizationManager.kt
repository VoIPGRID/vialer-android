package com.voipgrid.vialer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

class BatteryOptimizationManager(private val context: Context) {

    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }


    /**
     * Returns TRUE if the user has chosen to ignore battery optimization.
     *
     */
    fun isIgnoringBatteryOptimization() : Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Prompt the user to disable battery optimization.
     *
     */
    @SuppressLint("BatteryLife")
    fun prompt(activity: Activity) {
        activity.startActivityForResult(Intent().apply {
            action = if (isIgnoringBatteryOptimization()) Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS else Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            if (!isIgnoringBatteryOptimization())  {
                data = Uri.parse("package:${context.packageName}")
            }
        }, 50)
    }
}