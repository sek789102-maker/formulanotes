import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:sqflite/sqflite.dart';

import '../core/l10n/lang.dart';
import '../core/utils/errors.dart';
import '../data/database/app_database.dart';
import '../data/models/category.dart';
import '../data/models/document_item.dart';
import '../data/models/event_item.dart';
import '../data/models/task_item.dart';
import '../data/repositories/category_repository.dart';
import '../data/repositories/document_repository.dart';
import '../data/repositories/event_repository.dart';
import '../data/repositories/settings_repository.dart';
import '../data/repositories/task_repository.dart';
import '../services/attachment_service.dart';
import '../services/backup_service.dart';
import '../services/notification_gateway.dart';
import '../services/reminder_scheduler.dart';
import '../services/secure_window.dart';
import '../services/security_service.dart';

/// Container for long-lived services, created once at startup.
class AppServices {
  AppServices({
    required this.db,
    required this.gateway,
    required this.security,
    required this.attachments,
    required this.backupDir,
    required this.appVersion,
  })  : documents = DocumentRepository(db),
        tasks = TaskRepository(db),
        events = EventRepository(db),
        categories = CategoryRepository(db),
        settings = SettingsRepository(db) {
    scheduler = ReminderScheduler(db: db, gateway: gateway, settings: settings);
    backup = BackupService(
      db: db,
      attachments: attachments,
      settings: settings,
      backupDir: backupDir,
      appVersion: appVersion,
    );
    autoBackup = AutoBackupService(backup: backup, settings: settings);
  }

  final Database db;
  final NotificationGateway gateway;
  final SecurityService security;
  final AttachmentService attachments;
  final Directory backupDir;
  final String appVersion;

  final DocumentRepository documents;
  final TaskRepository tasks;
  final EventRepository events;
  final CategoryRepository categories;
  final SettingsRepository settings;
  late final ReminderScheduler scheduler;
  late final BackupService backup;
  late final AutoBackupService autoBackup;
}

/// App-wide state and the single entry point for data changes.
///
/// Screens read data through repositories and rebuild when [revision]
/// changes. Every mutation keeps database, reminders and files in sync.
class AppController extends ChangeNotifier {
  AppController(this.s);

  final AppServices s;

  int revision = 0;
  Map<String, String> settings = {};
  List<ItemCategory> categories = [];
  bool lockEnabled = false;
  bool biometricEnabled = false;

  ThemeMode get themeMode {
    switch (settings[SettingKeys.themeMode]) {
      case 'light':
        return ThemeMode.light;
      case 'dark':
        return ThemeMode.dark;
      default:
        return ThemeMode.system;
    }
  }

  AppLanguage get language => Lang.parse(settings[SettingKeys.language]);
  Locale get locale => language == AppLanguage.en ? const Locale('en') : const Locale('th');

  ReminderPrefs get reminderPrefs => ReminderPrefs.fromSettings(settings);

  ItemCategory categoryById(String? id) {
    for (final c in categories) {
      if (c.id == id) return c;
    }
    return categories.cast<ItemCategory>().firstWhere((c) => c.id == ItemCategory.otherId,
        orElse: () => DefaultCategories.build(Lang.isEn).last);
  }

  ItemCategory? maybeCategory(String? id) {
    if (id == null) return null;
    for (final c in categories) {
      if (c.id == id) return c;
    }
    return null;
  }

  /// Forces data-driven screens to reload.
  void refresh() => _changed();

  void _changed() {
    revision++;
    notifyListeners();
  }

  Future<T> _db<T>(Future<T> Function() f) async {
    try {
      return await f();
    } on AppException {
      rethrow;
    } catch (e) {
      throw dbError(e);
    }
  }

  // ------------------------------------------------------------ lifecycle

  Future<void> load() async {
    settings = await s.settings.all();
    Lang.current = language;
    categories = await s.categories.all();
    lockEnabled = await s.security.isLockEnabled();
    biometricEnabled = lockEnabled && await s.security.isBiometricEnabled();
    await SecureWindow.set(lockEnabled);
  }

  /// Background housekeeping after the first frame: re-sync reminders
  /// (handles time-zone changes and reboots) and run a due auto-backup.
  Future<void> startupTasks() async {
    try {
      await s.gateway.init();
      await s.settings.set(SettingKeys.lastTimezone, s.gateway.timezoneName);
      await s.scheduler.rescheduleAll();
    } catch (e) {
      if (kDebugMode) debugPrint('reminder sync failed: $e');
    }
    await runAutoBackupIfDue();
  }

  Future<void> onResume() async {
    // Detect time-zone change while the app was in background.
    try {
      final before = settings[SettingKeys.lastTimezone];
      await s.gateway.init();
      if (before != null && before != s.gateway.timezoneName) {
        await s.settings.set(SettingKeys.lastTimezone, s.gateway.timezoneName);
        await s.scheduler.rescheduleAll();
      }
    } catch (_) {}
    await runAutoBackupIfDue();
    _changed(); // refresh "today" based views
  }

  Future<void> runAutoBackupIfDue() async {
    final r = await s.autoBackup.runIfDue();
    if (r == false) {
      try {
        await s.gateway.showNow(
          id: 1,
          title: Lang.t('สำรองข้อมูลอัตโนมัติไม่สำเร็จ', 'Automatic backup failed'),
          body: Lang.t('เปิด LIFEKEEP > ตั้งค่า > สำรองและกู้คืน เพื่อตรวจสอบ',
              'Open LIFEKEEP > Settings > Backup & Restore to check.'),
        );
      } catch (_) {}
    }
    if (r != null) {
      settings = await s.settings.all();
      notifyListeners();
    }
  }

  // ------------------------------------------------------------ documents

  /// Saves a document. Attachments removed during editing are deleted only
  /// after the database write succeeded. Returns false if reminders could
  /// not be scheduled (the document itself is saved).
  Future<bool> saveDocument(DocumentItem d, {Iterable<String> removedAttachments = const []}) async {
    await _db(() => s.documents.upsert(d));
    await s.attachments.deleteMany(removedAttachments.where((n) => !d.attachmentPaths.contains(n)));
    final ok = await s.scheduler.syncDocument(d);
    _changed();
    return ok;
  }

  Future<void> deleteDocument(DocumentItem d) async {
    await _db(() => s.documents.delete(d.id));
    await s.scheduler.cancelFor(ReminderScheduler.typeDocument, d.id);
    await s.attachments.deleteMany(d.attachmentPaths);
    _changed();
  }

  Future<void> setPinned(DocumentItem d, bool pinned) async {
    await _db(() => s.documents.setPinned(d.id, pinned));
    _changed();
  }

  /// Removes references to attachment files that no longer exist.
  Future<DocumentItem> removeMissingAttachments(DocumentItem d) async {
    final keep = <String>[];
    for (final n in d.attachmentPaths) {
      if (await s.attachments.exists(n)) keep.add(n);
    }
    final updated = d.copyWith(attachmentPaths: keep, updatedAt: DateTime.now());
    await _db(() => s.documents.upsert(updated));
    _changed();
    return updated;
  }

  // ---------------------------------------------------------------- tasks

  Future<bool> saveTask(TaskItem t) async {
    await _db(() => s.tasks.upsert(t));
    final ok = await s.scheduler.syncTask(t);
    _changed();
    return ok;
  }

  Future<void> deleteTask(TaskItem t) async {
    await _db(() => s.tasks.delete(t.id));
    await s.scheduler.cancelFor(ReminderScheduler.typeTask, t.id);
    _changed();
  }

  Future<void> setTaskCompleted(TaskItem t, bool done) async {
    final updated = await _db(() => s.tasks.setCompleted(t.id, done));
    if (updated != null) {
      // Completed tasks drop their reminders; re-opened tasks get them back.
      await s.scheduler.syncTask(updated);
    }
    _changed();
  }

  // --------------------------------------------------------------- events

  Future<bool> saveEvent(EventItem e) async {
    await _db(() => s.events.upsert(e));
    final ok = await s.scheduler.syncEvent(e);
    _changed();
    return ok;
  }

  Future<void> deleteEvent(EventItem e) async {
    await _db(() => s.events.delete(e.id));
    await s.scheduler.cancelFor(ReminderScheduler.typeEvent, e.id);
    _changed();
  }

  // ----------------------------------------------------------- categories

  Future<void> saveCategory(ItemCategory c) async {
    await _db(() => s.categories.upsert(c));
    categories = await s.categories.all();
    _changed();
  }

  Future<void> deleteCategory(ItemCategory c) async {
    await _db(() => s.categories.delete(c.id));
    categories = await s.categories.all();
    _changed();
  }

  // ------------------------------------------------------------- settings

  Future<void> setSetting(String key, String? value, {bool reschedule = false}) async {
    await _db(() => s.settings.set(key, value));
    settings = await s.settings.all();
    if (reschedule) await s.scheduler.rescheduleAll();
    _changed();
  }

  Future<void> setThemeMode(ThemeMode m) =>
      setSetting(SettingKeys.themeMode, m == ThemeMode.system ? 'system' : (m == ThemeMode.dark ? 'dark' : 'light'));

  Future<void> setLanguage(AppLanguage l) async {
    Lang.current = l;
    // Notification texts are generated at scheduling time -> regenerate.
    await setSetting(SettingKeys.language, l == AppLanguage.en ? 'en' : 'th', reschedule: true);
  }

  Future<bool> ensureNotificationPermission() async {
    try {
      final st = await s.gateway.permissionState();
      if (st.notificationsAllowed == false) {
        return await s.gateway.requestNotificationPermission();
      }
      return true;
    } catch (_) {
      return false;
    }
  }

  // ------------------------------------------------------------- security

  Future<void> refreshSecurity() async {
    lockEnabled = await s.security.isLockEnabled();
    biometricEnabled = lockEnabled && await s.security.isBiometricEnabled();
    await SecureWindow.set(lockEnabled);
    notifyListeners();
  }

  // --------------------------------------------------------------- backup

  Future<BackupFileInfo> backupNow({required bool includeAttachments}) async {
    final info = await s.backup.createBackup(includeAttachments: includeAttachments);
    await s.settings.set(SettingKeys.lastManualBackupAt, '${DateTime.now().millisecondsSinceEpoch}');
    settings = await s.settings.all();
    notifyListeners();
    return info;
  }

  Future<RestoreResult> restore(BackupInspection inspection, RestoreMode mode) async {
    final result = await s.backup.restore(inspection, mode);
    await load();
    try {
      await s.scheduler.rescheduleAll();
    } catch (_) {}
    _changed();
    return result;
  }

  // -------------------------------------------------------- delete all

  /// Deletes all user content (documents, tasks, events, custom categories
  /// and attachments). Security settings and, unless requested, local
  /// backups are kept.
  Future<void> deleteAllData({required bool includeBackups}) async {
    await s.scheduler.cancelAllReminders();
    await _db(() => s.db.transaction((txn) async {
          for (final t in AppDatabase.contentTables) {
            await txn.delete(t);
          }
          for (final c in DefaultCategories.build(Lang.isEn)) {
            await txn.insert('categories', c.toMap());
          }
        }));
    await s.attachments.deleteAll();
    if (includeBackups) {
      for (final b in await s.backup.listBackups()) {
        await s.backup.deleteBackup(b.path);
      }
    }
    categories = await s.categories.all();
    _changed();
  }
}
