import 'package:sqflite/sqflite.dart';

/// Simple key/value settings stored in SQLite (non-sensitive values only;
/// the PIN hash lives in secure storage).
class SettingsRepository {
  SettingsRepository(this.db);
  final Database db;

  Future<Map<String, String>> all() async {
    final rows = await db.query('settings');
    return {
      for (final r in rows)
        if (r['value'] != null) r['key'] as String: r['value'] as String,
    };
  }

  Future<String?> get(String key) async {
    final rows = await db.query('settings', where: 'key = ?', whereArgs: [key], limit: 1);
    return rows.isEmpty ? null : rows.first['value'] as String?;
  }

  Future<void> set(String key, String? value) async {
    if (value == null) {
      await db.delete('settings', where: 'key = ?', whereArgs: [key]);
    } else {
      await db.insert('settings', {'key': key, 'value': value}, conflictAlgorithm: ConflictAlgorithm.replace);
    }
  }
}

/// Setting keys. Keys listed in [backupable] are included in backups.
class SettingKeys {
  SettingKeys._();

  static const themeMode = 'theme_mode';
  static const language = 'language';
  static const notificationsEnabled = 'notif_enabled';
  static const notifyTasks = 'notif_tasks';
  static const notifyEvents = 'notif_events';
  static const notifyDocuments = 'notif_documents';
  static const reminderHour = 'reminder_hour';
  static const reminderMinute = 'reminder_minute';
  static const autoBackupEnabled = 'auto_backup_enabled';
  static const autoBackupFrequency = 'auto_backup_frequency';
  static const autoBackupKeep = 'auto_backup_keep';
  static const autoBackupIncludeAttachments = 'auto_backup_attachments';
  static const autoBackupMaxMb = 'auto_backup_max_mb';
  static const lastAutoBackupAt = 'last_auto_backup_at';
  static const lastAutoBackupError = 'last_auto_backup_error';
  static const lastManualBackupAt = 'last_manual_backup_at';
  static const lastTimezone = 'last_timezone';
  static const dataNoticeSeen = 'data_notice_seen';

  /// UI preferences (no schema change: stored in the key/value table).
  static const documentsView = 'documents_view';
  static const recentSearches = 'recent_searches';

  static const backupable = {
    themeMode,
    language,
    notificationsEnabled,
    notifyTasks,
    notifyEvents,
    notifyDocuments,
    reminderHour,
    reminderMinute,
    autoBackupEnabled,
    autoBackupFrequency,
    autoBackupKeep,
    autoBackupIncludeAttachments,
    autoBackupMaxMb,
  };
}
