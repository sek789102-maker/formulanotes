package com.sek789102.jotwai.reminders

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
import com.sek789102.jotwai.JotwaiApp
import com.sek789102.jotwai.MainActivity
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.RepeatRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Shows a note's reminder and handles its "Done" and "Snooze" buttons. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val noteId = intent?.getLongExtra(EXTRA_NOTE_ID, -1L) ?: return
        if (noteId <= 0L) return
        val app = context.applicationContext as JotwaiApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = app.repository
                when (intent.action) {
                    ACTION_FIRE -> {
                        val note = repo.getNote(noteId)?.note
                        if (note != null && !note.isTrashed && note.reminderAt != null) {
                            show(context, note)
                            // One-off reminders are cleared; repeating ones move to the next time.
                            repo.advanceReminder(noteId)
                        }
                    }
                    ACTION_DONE -> {
                        cancelNotification(context, noteId)
                        val note = repo.getNote(noteId)?.note
                        if (note != null && note.reminderRepeat == RepeatRule.NONE) repo.setReminder(noteId, null)
                    }
                    ACTION_SNOOZE -> {
                        cancelNotification(context, noteId)
                        val note = repo.getNote(noteId)?.note
                        if (note != null && !note.isTrashed) {
                            val at = System.currentTimeMillis() + SNOOZE_MILLIS
                            // A snoozed repeating reminder becomes a one-off; its series stays in the note.
                            if (note.reminderRepeat == RepeatRule.NONE) {
                                repo.setReminder(noteId, at)
                            } else {
                                ReminderScheduler(context).schedule(note.copy(reminderAt = at))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ReminderReceiver", "Failed to handle ${intent.action} for note $noteId", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, note: NoteEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ReminderScheduler.createChannel(context)
        val code = ReminderScheduler.requestCode(note.id)
        val open = PendingIntent.getActivity(
            context, code,
            MainActivity.openNoteIntent(context, note.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun action(action: String) = PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_NOTE_ID, note.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = note.title.ifBlank { context.getString(R.string.reminder_notification_title) }
        // A locked note never shows its text on the lock screen or in the shade.
        val body = when {
            note.isLocked -> context.getString(R.string.locked_note_hidden)
            note.type == NoteType.CHECKLIST -> note.checklist.filter { !it.checked }.joinToString("\n") { "☐ ${it.text}" }
            else -> note.content
        }.take(600)
        val n = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(if (note.isLocked) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.reminder_action_done), action(ACTION_DONE))
            .addAction(0, context.getString(R.string.reminder_action_snooze), action(ACTION_SNOOZE))
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(code, n)
    }

    private fun cancelNotification(context: Context, noteId: Long) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.cancel(ReminderScheduler.requestCode(noteId))
    }

    companion object {
        const val ACTION_FIRE = "com.sek789102.jotwai.REMINDER_FIRE"
        const val ACTION_DONE = "com.sek789102.jotwai.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.sek789102.jotwai.REMINDER_SNOOZE"
        const val EXTRA_NOTE_ID = "note_id"
        const val SNOOZE_MILLIS = 10 * 60 * 1000L
    }
}
