package com.voipgrid.vialer

import android.app.Application
import com.voipgrid.vialer.dagger.DaggerVialerComponent
import com.voipgrid.vialer.dagger.VialerComponent
import com.voipgrid.vialer.dagger.VialerModule
import com.voipgrid.vialer.database.AppDatabase
import androidx.room.Room
import com.voipgrid.vialer.tasks.launch.*

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
            RegisterLibraries(),
            ConvertApiToken(),
            RegisterPeriodicTasks(),
            RegisterWithMiddleware(),
            ConfigureVoip()
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
        val db : AppDatabase by lazy {
            Room.databaseBuilder(instance, AppDatabase::class.java, VialerApplication::class.java.name).build()
        }

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