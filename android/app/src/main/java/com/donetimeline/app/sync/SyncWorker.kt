package com.donetimeline.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.donetimeline.app.DoneApp
import com.donetimeline.app.data.AuthException
import com.donetimeline.app.data.ServerException
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as DoneApp).container.repository
        return try {
            repo.sync()
            Result.success()
        } catch (e: AuthException) {
            Result.failure() // needs the user to fix the token
        } catch (e: IOException) {
            Result.retry()
        } catch (e: ServerException) {
            Result.retry()
        }
    }
}

object SyncScheduler {
    private const val ONE_OFF = "sync-now"
    private const val PERIODIC = "sync-periodic"

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Push local changes as soon as the device is online. */
    fun requestSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Pull changes made elsewhere (e.g. on the website) every ~15 minutes. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(online)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
