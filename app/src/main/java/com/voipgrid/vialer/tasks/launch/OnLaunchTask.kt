package com.voipgrid.vialer.tasks.launch

import com.voipgrid.vialer.VialerApplication

interface OnLaunchTask {

    fun execute(application: VialerApplication)
}