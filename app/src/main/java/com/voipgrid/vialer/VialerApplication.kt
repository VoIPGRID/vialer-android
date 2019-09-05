package com.voipgrid.vialer

import android.app.Application
import com.voipgrid.vialer.dagger.DaggerVialerComponent
import com.voipgrid.vialer.dagger.VialerComponent
import com.voipgrid.vialer.dagger.VialerModule
import com.voipgrid.vialer.tasks.launch.ConvertApiToken
import com.voipgrid.vialer.tasks.launch.CreateLoggingId
import com.voipgrid.vialer.tasks.launch.RegisterLibraries

class VialerApplication : Application() {

    private val activityLifecycleTracker = ActivityLifecycleTracker()

    private val component = DaggerVialerComponent
            .builder()
            .vialerModule(VialerModule(this))
            .build()

    /**
     * All these tasks will be executed when the application's onCreate method is called.
     *
     */
    private val launchTasks = listOf(
            CreateLoggingId(),
            RegisterLibraries(),
            ConvertApiToken()
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(activityLifecycleTracker)
        launchTasks.forEach {
            it.execute(this)
        }
    }

    /**
     * Checks whether there is an activity in the foreground currently.
     *
     * @return TRUE if an activity is being displayed to the user.
     */
    fun isApplicationVisible(): Boolean {
        return activityLifecycleTracker.isApplicationVisible
    }

    /**
     * Return the main dagger component.
     *
     * @return
     */
    fun component(): VialerComponent {
        return component
    }

    companion object {
        lateinit var instance: VialerApplication
            private set

        @JvmStatic
        fun get(): VialerApplication {
            return instance
        }

        @JvmStatic
        fun getAppVersion(): String {
            return BuildConfig.VERSION_NAME
        }
    }
}