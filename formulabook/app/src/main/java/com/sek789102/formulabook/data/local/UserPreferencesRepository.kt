package com.sek789102.formulabook.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sek789102.formulabook.data.model.NoteSortOrder
import com.sek789102.formulabook.data.model.ThemeMode
import com.sek789102.formulabook.domain.study.GpaCalculator
import com.sek789102.formulabook.domain.study.GpaCourseEntry
import com.sek789102.formulabook.domain.study.PlannerHelper
import com.sek789102.formulabook.domain.study.TimetableEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "student_note_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultPaperStyle: Int = 0,
    val sortOrder: NoteSortOrder = NoteSortOrder.UPDATED_DESC,
    val examNotificationsEnabled: Boolean = true,
    val lastBackupExportedAtMillis: Long = 0L,
    val backupReminderDismissedUntilMillis: Long = 0L,
    val gpaCourses: List<GpaCourseEntry> = emptyList(),
    val timetableEntries: List<TimetableEntry> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_PAPER_STYLE = intPreferencesKey("default_paper_style")
        val SORT_ORDER = stringPreferencesKey("note_sort_order")
        val EXAM_NOTIFICATIONS_ENABLED = booleanPreferencesKey("exam_notifications_enabled")
        val LAST_BACKUP_EXPORTED_AT = longPreferencesKey("last_backup_exported_at_millis")
        val BACKUP_REMINDER_DISMISSED_UNTIL = longPreferencesKey("backup_reminder_dismissed_until_millis")
        val GPA_COURSES_JSON = stringPreferencesKey("gpa_courses_json")
        val TIMETABLE_ENTRIES_JSON = stringPreferencesKey("timetable_entries_json")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
        val storedTheme = prefs[Keys.THEME_MODE]
        val resolvedThemeMode = if (storedTheme != null) {
            ThemeMode.fromName(storedTheme)
        } else {
            when (prefs[Keys.DARK_MODE]) {
                true -> ThemeMode.DARK
                false -> ThemeMode.LIGHT
                null -> ThemeMode.SYSTEM
            }
        }
        UserPreferences(
            themeMode = resolvedThemeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            defaultPaperStyle = prefs[Keys.DEFAULT_PAPER_STYLE] ?: 0,
            sortOrder = NoteSortOrder.fromName(prefs[Keys.SORT_ORDER]),
            examNotificationsEnabled = prefs[Keys.EXAM_NOTIFICATIONS_ENABLED] ?: true,
            lastBackupExportedAtMillis = prefs[Keys.LAST_BACKUP_EXPORTED_AT] ?: 0L,
            backupReminderDismissedUntilMillis = prefs[Keys.BACKUP_REMINDER_DISMISSED_UNTIL] ?: 0L,
            gpaCourses = GpaCalculator.fromJson(prefs[Keys.GPA_COURSES_JSON]),
            timetableEntries = PlannerHelper.fromJson(prefs[Keys.TIMETABLE_ENTRIES_JSON])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
            prefs[Keys.THEME_MODE] = if (enabled) ThemeMode.DARK.name else ThemeMode.LIGHT.name
        }
    }

    suspend fun setDefaultPaperStyle(style: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_PAPER_STYLE] = style.coerceIn(0, 2)
        }
    }

    suspend fun setSortOrder(order: NoteSortOrder) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_ORDER] = order.name
        }
    }

    suspend fun setExamNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXAM_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setGpaCourses(courses: List<GpaCourseEntry>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GPA_COURSES_JSON] = GpaCalculator.toJson(courses)
        }
    }

    suspend fun updateGpaCourses(transform: (List<GpaCourseEntry>) -> List<GpaCourseEntry>) {
        context.dataStore.edit { prefs ->
            val current = GpaCalculator.fromJson(prefs[Keys.GPA_COURSES_JSON])
            prefs[Keys.GPA_COURSES_JSON] = GpaCalculator.toJson(transform(current))
        }
    }

    suspend fun setTimetableEntries(entries: List<TimetableEntry>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMETABLE_ENTRIES_JSON] = PlannerHelper.toJson(entries)
        }
    }

    suspend fun updateTimetableEntries(transform: (List<TimetableEntry>) -> List<TimetableEntry>) {
        context.dataStore.edit { prefs ->
            val current = PlannerHelper.fromJson(prefs[Keys.TIMETABLE_ENTRIES_JSON])
            prefs[Keys.TIMETABLE_ENTRIES_JSON] = PlannerHelper.toJson(transform(current))
        }
    }

    suspend fun setLastBackupExportedAt(timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_EXPORTED_AT] = timestampMillis
            prefs[Keys.BACKUP_REMINDER_DISMISSED_UNTIL] = 0L
        }
    }

    suspend fun dismissBackupReminderUntil(untilMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKUP_REMINDER_DISMISSED_UNTIL] = untilMillis
        }
    }
}
