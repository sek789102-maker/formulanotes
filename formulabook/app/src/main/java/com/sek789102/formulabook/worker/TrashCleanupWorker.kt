package com.sek789102.formulabook.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.repository.NotesRepository
import com.sek789102.formulabook.widget.ExamCountdownWidgetUpdater
import java.util.concurrent.TimeUnit

class TrashCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repo = NotesRepository(applicationContext, db)
        repo.purgeExpiredTrash(retentionDays = RETENTION_DAYS)
        repo.purgeOrphanedImages()
        ExamCountdownWidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "trash_cleanup_daily_work"
        const val RETENTION_DAYS = 30L

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashCleanupWorker>(1, TimeUnit.DAYS)
                .build()
            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
