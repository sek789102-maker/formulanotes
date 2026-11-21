package com.sek789102.jotwai.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.repository.ReminderSink

/** Schedules one alarm per note (request code = note id). */
class ReminderScheduler(private val context: Context) : ReminderSink {

    override fun schedule(note: NoteEntity) {
        val at = note.reminderAt ?: return cancel(note.id)
        if (note.isTrashed) return cancel(note.id)
        if (at <= System.currentTimeMillis()) {
            // A missed one-off reminder is not shown late on every app start; it stays visible
            // in the note as an overdue chip instead.
            cancel(note.id)
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(note.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm refused, using inexact", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    override fun cancel(noteId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        pendingIntent(noteId, PendingIntent.FLAG_NO_CREATE)?.let {
            am.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(noteId: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(context, requestCode(noteId), intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        const val CHANNEL_ID = "note_reminders"

        fun requestCode(noteId: Long): Int = (noteId and 0x7FFF_FFFFL).toInt()

        fun canScheduleExact(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            return am.canScheduleExactAlarms()
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_reminders_desc)
                    enableVibration(true)
                }
            )
        }
    }
}
