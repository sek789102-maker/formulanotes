package com.sek789102.formulabook.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import java.util.Calendar

object ExamNotificationScheduler {

    const val CHANNEL_ID = "exam_reminder_channel"
    const val EXTRA_EXAM_ID = "extra_exam_id"
    const val EXTRA_EXAM_TITLE = "extra_exam_title"
    const val EXTRA_SUBJECT_ID = "extra_subject_id"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"

    private val REMINDER_DAYS_BEFORE = listOf(7, 3, 1, 0)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_exam_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_exam_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun rescheduleAll(
        context: Context,
        exams: List<ExamCountdownEntity>,
        enabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Always cancel previously scheduled slots for these exams first
        exams.forEach { cancelForExam(context, alarmManager, it.id) }
        if (!enabled) return

        exams.forEach { exam ->
            REMINDER_DAYS_BEFORE.forEachIndexed { idx, daysBefore ->
                val triggerAt = computeTriggerTimeMillis(exam.examDateMillis, daysBefore)
                if (triggerAt > nowMillis) {
                    val requestCode = requestCodeFor(exam.id, idx)
                    val pendingIntent = buildPendingIntent(
                        context = context,
                        requestCode = requestCode,
                        exam = exam,
                        daysBefore = daysBefore
                    )
                    try {
                        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager.canScheduleExactAlarms()
                        } else {
                            true
                        }
                        if (canExact) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                        }
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                }
            }
        }
    }

    fun cancelForExam(context: Context, examId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelForExam(context, alarmManager, examId)
    }

    private fun cancelForExam(context: Context, alarmManager: AlarmManager, examId: Long) {
        REMINDER_DAYS_BEFORE.indices.forEach { idx ->
            val intent = Intent(context, ExamAlarmReceiver::class.java)
            val existing = PendingIntent.getBroadcast(
                context,
                requestCodeFor(examId, idx),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                alarmManager.cancel(existing)
                existing.cancel()
            }
        }
    }

    fun computeTriggerTimeMillis(
        examDateMillis: Long,
        daysBefore: Int,
        timeZone: java.util.TimeZone = java.util.TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = examDateMillis
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            set(Calendar.HOUR_OF_DAY, if (daysBefore == 0) 7 else 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun requestCodeFor(examId: Long, offsetIdx: Int): Int {
        val positiveId = (examId and 0x7FFF_FFFFL) % 100_000L
        return 10_000 + (positiveId.toInt() * 10) + offsetIdx
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        exam: ExamCountdownEntity,
        daysBefore: Int
    ): PendingIntent {
        val intent = Intent(context, ExamAlarmReceiver::class.java).apply {
            putExtra(EXTRA_EXAM_ID, exam.id)
            putExtra(EXTRA_EXAM_TITLE, exam.title)
            putExtra(EXTRA_SUBJECT_ID, exam.subject)
            putExtra(EXTRA_DAYS_BEFORE, daysBefore)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
