package com.sek789102.formulabook.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.local.UserPreferencesRepository
import com.sek789102.formulabook.widget.ExamCountdownWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(appContext).preferencesFlow.first()
                val db = AppDatabase.getInstance(appContext)
                val exams = db.noteDao().getAllExamsSnapshot()
                ExamNotificationScheduler.rescheduleAll(
                    context = appContext,
                    exams = exams,
                    enabled = prefs.examNotificationsEnabled
                )
                ExamCountdownWidgetUpdater.updateAll(appContext)
            } catch (e: Exception) {
                Log.w("BootCompletedReceiver", "Failed to reschedule exam reminders after $action", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Re-schedule as exact alarms right after the user grants "Alarms & reminders".
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
