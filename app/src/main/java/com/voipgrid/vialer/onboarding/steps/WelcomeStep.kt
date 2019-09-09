package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.view.View
import androidx.work.*
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.AutoContinuingStep
import com.voipgrid.vialer.tasks.launch.UpdateVoipAccountParametersWorker
import kotlinx.android.synthetic.main.onboarding_step_welcome.*

class WelcomeStep : AutoContinuingStep() {
    override val delay = 3000
    override val layout = R.layout.onboarding_step_welcome

    private val logger = Logger(this)

    private val user: SystemUser by lazy {
        VialerApplication.get().component().systemUser
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            HistoricCallRecordsImporter.Worker.start(it)

            WorkManager.getInstance(it).enqueueUniqueWork(
                    UpdateVoipAccountParametersWorker::class.java.name,
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<UpdateVoipAccountParametersWorker>()
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build()
            )
        }
        logger.i("Welcome ${user.fullName} to the app and completing onboarding")
        subtitle_label.text = user.fullName
    }
}