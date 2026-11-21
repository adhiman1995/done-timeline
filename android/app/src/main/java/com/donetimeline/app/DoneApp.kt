package com.donetimeline.app

import android.app.Application
import android.content.Context
import com.donetimeline.app.data.AiClient
import com.donetimeline.app.data.AuthRepository
import com.donetimeline.app.data.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow

class DoneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container; the app is small enough not to need a DI framework. */
class AppContainer(val appContext: Context) {
    /** Failed writes (e.g. permission denied) reported back to the UI. */
    val writeErrors = MutableSharedFlow<Exception>(extraBufferCapacity = 8)

    // Firestore keeps an offline copy on the device by default.
    val auth = AuthRepository(FirebaseAuth.getInstance())
    val ai = AiClient(FirebaseAuth.getInstance())
    val repository = TaskRepository(FirebaseFirestore.getInstance()) { writeErrors.tryEmit(it) }
}
