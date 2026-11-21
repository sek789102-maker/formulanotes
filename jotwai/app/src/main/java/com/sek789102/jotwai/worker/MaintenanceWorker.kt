package com.sek789102.jotwai.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sek789102.jotwai.JotwaiApp
import java.util.concurrent.TimeUnit

/** Daily: empties trash older than 30 days and removes image files nothing uses. */
class MaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as JotwaiApp).repository
        repo.purgeTrash()
        repo.purgeOrphanedImages()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "daily_maintenance",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<MaintenanceWorker>(1, TimeUnit.DAYS).build()
                )
            }
        }
    }
}
