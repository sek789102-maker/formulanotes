package com.sek789102.formulabook.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sek789102.formulabook.MainActivity
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.local.UserPreferencesRepository
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.resolveDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExamAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val examId = intent.getLongExtra(ExamNotificationScheduler.EXTRA_EXAM_ID, 0L)
        val examTitle = intent.getStringExtra(ExamNotificationScheduler.EXTRA_EXAM_TITLE).orEmpty()
        val subjectId = intent.getStringExtra(ExamNotificationScheduler.EXTRA_SUBJECT_ID)
            ?: SubjectCategory.GENERAL.name
        val daysBefore = intent.getIntExtra(ExamNotificationScheduler.EXTRA_DAYS_BEFORE, 0)
        if (examTitle.isBlank()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(appContext).preferencesFlow.first()
                if (!prefs.examNotificationsEnabled) return@launch

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) return@launch
                }

                val dao = AppDatabase.getInstance(appContext).noteDao()
                val liveExam = if (examId > 0L) dao.getExamById(examId) ?: return@launch else null
                val resolvedTitle = liveExam?.title ?: examTitle
                val resolvedSubjectId = liveExam?.subject ?: subjectId
                val subjectName = dao.getSubjectById(resolvedSubjectId).resolveDisplayName(appContext)

                ExamNotificationScheduler.createNotificationChannel(appContext)

                val safeExamCode = (examId and 0x7FFF_FFFFL).toInt() % 100_000
                val openIntent = Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    appContext,
                    2000 + safeExamCode,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = if (daysBefore == 0) {
                    appContext.getString(R.string.notification_exam_today_title, resolvedTitle)
                } else {
                    appContext.getString(R.string.notification_exam_upcoming_title, daysBefore, resolvedTitle)
                }
                val body = if (daysBefore == 0) {
                    appContext.getString(R.string.notification_exam_today_body, subjectName)
                } else {
                    appContext.getString(R.string.notification_exam_upcoming_body, subjectName, daysBefore)
                }

                val notification = NotificationCompat.Builder(appContext, ExamNotificationScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .build()

                val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(5000 + (safeExamCode * 10) + daysBefore, notification)
            } catch (e: Exception) {
                Log.w("ExamAlarmReceiver", "Failed to show exam reminder for examId=$examId", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
