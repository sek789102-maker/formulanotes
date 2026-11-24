package com.sek789102.jotwai.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sek789102.jotwai.data.model.SortOrder
import com.sek789102.jotwai.data.model.TextSize
import com.sek789102.jotwai.data.model.ThemeMode
import com.sek789102.jotwai.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "jotwai_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOrder: SortOrder = SortOrder.UPDATED,
    val textSize: TextSize = TextSize.NORMAL,
    /** New checklist items go to the bottom (true) or top. */
    val checklistAddToBottom: Boolean = true,
    /** Ticked items move to a "done" section at the bottom. */
    val checklistMoveCheckedDown: Boolean = true,
    val lastBackupAt: Long = 0L,
    /** Reminder notifications on/off (the reminders stay saved either way). */
    val remindersEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val VIEW = stringPreferencesKey("view_mode")
        val SORT = stringPreferencesKey("sort_order")
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val CHECK_BOTTOM = booleanPreferencesKey("checklist_add_bottom")
        val CHECK_MOVE = booleanPreferencesKey("checklist_move_checked")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
        val REMINDERS_ON = booleanPreferencesKey("reminders_enabled")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserPreferences() }

    private fun Preferences.toUserPreferences() = UserPreferences(
        themeMode = enumOr(this[Keys.THEME], ThemeMode.DARK),
        dynamicColor = this[Keys.DYNAMIC] ?: false,
        viewMode = enumOr(this[Keys.VIEW], ViewMode.GRID),
        sortOrder = enumOr(this[Keys.SORT], SortOrder.UPDATED),
        textSize = enumOr(this[Keys.TEXT_SIZE], TextSize.NORMAL),
        checklistAddToBottom = this[Keys.CHECK_BOTTOM] ?: true,
        checklistMoveCheckedDown = this[Keys.CHECK_MOVE] ?: true,
        lastBackupAt = this[Keys.LAST_BACKUP] ?: 0L,
        remindersEnabled = this[Keys.REMINDERS_ON] ?: true
    )

    suspend fun setThemeMode(v: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = v.name }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = v }
    suspend fun setViewMode(v: ViewMode) = context.dataStore.edit { it[Keys.VIEW] = v.name }
    suspend fun setSortOrder(v: SortOrder) = context.dataStore.edit { it[Keys.SORT] = v.name }
    suspend fun setTextSize(v: TextSize) = context.dataStore.edit { it[Keys.TEXT_SIZE] = v.name }
    suspend fun setChecklistAddToBottom(v: Boolean) = context.dataStore.edit { it[Keys.CHECK_BOTTOM] = v }
    suspend fun setChecklistMoveCheckedDown(v: Boolean) = context.dataStore.edit { it[Keys.CHECK_MOVE] = v }
    suspend fun setRemindersEnabled(v: Boolean) = context.dataStore.edit { it[Keys.REMINDERS_ON] = v }
    suspend fun setLastBackupAt(v: Long) = context.dataStore.edit { it[Keys.LAST_BACKUP] = v }

    private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == raw } ?: default
}
