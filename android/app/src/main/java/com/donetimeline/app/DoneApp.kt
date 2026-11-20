package com.donetimeline.app

import android.app.Application
import com.donetimeline.app.data.ApiClient
import com.donetimeline.app.data.AppDatabase
import com.donetimeline.app.data.SettingsStore
import com.donetimeline.app.data.TaskRepository
import com.donetimeline.app.sync.SyncScheduler

class DoneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.schedulePeriodic(this)
    }
}

/** Manual dependency container; the app is small enough not to need a DI framework. */
class AppContainer(app: Application) {
    val settings = SettingsStore(app)
    val api = ApiClient()
    private val db = AppDatabase.create(app)
    val repository = TaskRepository(db, settings, api) { SyncScheduler.requestSync(app) }
}
