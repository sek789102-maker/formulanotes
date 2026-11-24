package com.sek789102.jotwai

import android.app.Application
import com.sek789102.jotwai.data.local.AppDatabase
import com.sek789102.jotwai.data.local.UserPreferencesRepository
import com.sek789102.jotwai.data.repository.NotesRepository
import com.sek789102.jotwai.reminders.ReminderScheduler
import com.sek789102.jotwai.widget.NotesWidget
import com.sek789102.jotwai.worker.MaintenanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class JotwaiApp : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.sek789102.jotwai.util.Lang.wrap(base))
    }

    /** Outlives screens: used for final saves when the editor closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: NotesRepository by lazy {
        NotesRepository(
            context = this,
            db = AppDatabase.getInstance(this),
            reminders = ReminderScheduler(this),
            onDataChanged = { scheduleWidgetRefresh() }
        )
    }

    val preferences: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }

    private var widgetJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        MaintenanceWorker.schedule(this)
    }

    /** Typing saves often; the widget is redrawn once things settle. */
    @Synchronized
    fun scheduleWidgetRefresh() {
        widgetJob?.cancel()
        widgetJob = appScope.launch {
            delay(1500)
            NotesWidget.refresh(this@JotwaiApp)
        }
    }

    suspend fun refreshWidgets() = NotesWidget.refresh(this)
}
