import 'package:flutter/foundation.dart';
import 'package:sqflite/sqflite.dart';

import '../core/l10n/lang.dart';
import '../core/utils/dates.dart';
import '../core/utils/formatters.dart';
import '../data/models/document_item.dart';
import '../data/models/event_item.dart';
import '../data/models/task_item.dart';
import '../data/repositories/settings_repository.dart';
import 'notification_gateway.dart';

class ReminderPrefs {
  const ReminderPrefs({
    this.enabled = true,
    this.tasks = true,
    this.events = true,
    this.documents = true,
    this.hour = 9,
    this.minute = 0,
  });

  final bool enabled;
  final bool tasks;
  final bool events;
  final bool documents;

  /// Time of day used for document expiry and date-only items.
  final int hour;
  final int minute;

  static ReminderPrefs fromSettings(Map<String, String> s) => ReminderPrefs(
        enabled: s[SettingKeys.notificationsEnabled] != 'false',
        tasks: s[SettingKeys.notifyTasks] != 'false',
        events: s[SettingKeys.notifyEvents] != 'false',
        documents: s[SettingKeys.notifyDocuments] != 'false',
        hour: int.tryParse(s[SettingKeys.reminderHour] ?? '')?.clamp(0, 23) ?? 9,
        minute: int.tryParse(s[SettingKeys.reminderMinute] ?? '')?.clamp(0, 59) ?? 0,
      );
}

class PlannedNotification {
  const PlannedNotification(this.at, this.title, this.body);
  final DateTime at;
  final String title;
  final String body;
}

/// Plans and (re)schedules local notifications.
///
/// Every scheduled OS notification is recorded in `scheduled_notifications`
/// so that editing an item cancels its old notifications first, deleting an
/// item cancels them, and nothing is scheduled twice.
class ReminderScheduler {
  ReminderScheduler({required this.db, required this.gateway, required this.settings});

  final Database db;
  final NotificationGateway gateway;
  final SettingsRepository settings;

  static const typeDocument = 'doc';
  static const typeTask = 'task';
  static const typeEvent = 'event';

  /// ids below this are reserved for system notifications (e.g. backup).
  static const minId = 1000;

  /// Android allows ~500 pending alarms per app; stay well below it.
  static const maxPending = 400;

  Future<ReminderPrefs> _prefs() async => ReminderPrefs.fromSettings(await settings.all());

  // ---------- planning (pure) ----------

  static List<PlannedNotification> planDocument(DocumentItem d, ReminderPrefs p, DateTime now) {
    if (!p.enabled || !p.documents || d.expiryDate == null || !d.reminder.isEnabled) return const [];
    final anchor = ReminderCalc.documentAnchor(d.expiryDate!, p.hour, p.minute);
    return ReminderCalc.fireTimes(anchor, d.reminder.offsetsMinutes, now).map((at) {
      final days = Dates.daysBetween(at, d.expiryDate!);
      final when = days == 0
          ? Lang.t('หมดอายุวันนี้', 'expires today')
          : Lang.t('จะหมดอายุในอีก $days วัน', 'expires in $days days');
      return PlannedNotification(
        at,
        Lang.t('เอกสารใกล้หมดอายุ', 'Document expiring soon'),
        '${d.title} — $when (${Fmt.date(d.expiryDate!)})',
      );
    }).toList();
  }

  static List<PlannedNotification> planTask(TaskItem t, ReminderPrefs p, DateTime now) {
    if (!p.enabled || !p.tasks || t.isCompleted || t.dueAt == null || !t.reminder.isEnabled) {
      return const [];
    }
    final anchor = ReminderCalc.itemAnchor(t.dueAt!,
        hasTime: t.hasTime, defaultHour: p.hour, defaultMinute: p.minute);
    final due = t.hasTime ? Fmt.dateTime(t.dueAt!) : Fmt.date(t.dueAt!);
    return ReminderCalc.fireTimes(anchor, t.reminder.offsetsMinutes, now)
        .map((at) => PlannedNotification(
              at,
              Lang.t('งานใกล้ถึงกำหนด', 'Task due soon'),
              '${t.title} — ${Lang.t('กำหนด', 'due')} $due',
            ))
        .toList();
  }

  static List<PlannedNotification> planEvent(EventItem e, ReminderPrefs p, DateTime now) {
    if (!p.enabled || !p.events || !e.reminder.isEnabled) return const [];
    final anchor = ReminderCalc.itemAnchor(e.startAt,
        hasTime: !e.allDay, defaultHour: p.hour, defaultMinute: p.minute);
    final when = e.allDay ? Fmt.date(e.startAt) : Fmt.dateTime(e.startAt);
    final loc = e.location.trim().isEmpty ? '' : ' · ${e.location.trim()}';
    return ReminderCalc.fireTimes(anchor, e.reminder.offsetsMinutes, now)
        .map((at) => PlannedNotification(
              at,
              Lang.t('กิจกรรมที่กำลังจะมาถึง', 'Upcoming event'),
              '${e.title} — $when$loc',
            ))
        .toList();
  }

  // ---------- scheduling ----------

  Future<bool> syncDocument(DocumentItem d, {DateTime? now}) async =>
      _replace(typeDocument, d.id, planDocument(d, await _prefs(), now ?? DateTime.now()));

  Future<bool> syncTask(TaskItem t, {DateTime? now}) async =>
      _replace(typeTask, t.id, planTask(t, await _prefs(), now ?? DateTime.now()));

  Future<bool> syncEvent(EventItem e, {DateTime? now}) async =>
      _replace(typeEvent, e.id, planEvent(e, await _prefs(), now ?? DateTime.now()));

  Future<bool> cancelFor(String type, String id) => _replace(type, id, const []);

  /// Cancels the item's existing notifications, then schedules [planned].
  /// Returns false if any OS call failed (the item itself is still saved).
  Future<bool> _replace(String type, String entityId, List<PlannedNotification> planned) async {
    var ok = true;
    final rows = await db.query('scheduled_notifications',
        columns: ['notif_id'], where: 'entity_type = ? AND entity_id = ?', whereArgs: [type, entityId]);
    for (final r in rows) {
      try {
        await gateway.cancel((r['notif_id'] as num).toInt());
      } catch (e) {
        ok = false;
        if (kDebugMode) debugPrint('cancel failed: $e');
      }
    }
    await db.delete('scheduled_notifications',
        where: 'entity_type = ? AND entity_id = ?', whereArgs: [type, entityId]);
    for (final p in planned) {
      ok = await _scheduleOne(type, entityId, p) && ok;
    }
    return ok;
  }

  Future<bool> _scheduleOne(String type, String entityId, PlannedNotification p) async {
    final id = await _allocateId('$type:$entityId:${p.at.millisecondsSinceEpoch}');
    try {
      await gateway.schedule(
        id: id,
        title: p.title,
        body: p.body,
        at: p.at,
        payload: '$type:$entityId',
      );
      await db.insert('scheduled_notifications', {
        'notif_id': id,
        'entity_type': type,
        'entity_id': entityId,
        'fire_at': p.at.millisecondsSinceEpoch,
      });
      return true;
    } catch (e) {
      if (kDebugMode) debugPrint('schedule failed: $e');
      return false;
    }
  }

  /// Deterministic id from a key, probing forward on collisions.
  Future<int> _allocateId(String key) async {
    var id = stableHash(key);
    for (var i = 0; i < 1000; i++) {
      final exists = await db.query('scheduled_notifications',
          columns: ['notif_id'], where: 'notif_id = ?', whereArgs: [id], limit: 1);
      if (exists.isEmpty) return id;
      id = id >= 0x7FFFFFFF ? minId : id + 1;
    }
    return id;
  }

  /// FNV-1a 32-bit hash mapped into [minId, 2^31).
  static int stableHash(String s) {
    var h = 0x811c9dc5;
    for (final c in s.codeUnits) {
      h ^= c;
      h = (h * 0x01000193) & 0xFFFFFFFF;
    }
    final v = h & 0x7FFFFFFF;
    return v < minId ? v + minId : v;
  }

  /// Rebuilds every reminder from the database. Used at app start, after a
  /// restore, after the time zone / language changes and when settings change.
  Future<bool> rescheduleAll({DateTime? now}) async {
    final n = now ?? DateTime.now();
    var ok = true;
    // Cancel everything we know about plus any orphaned pending requests.
    final known = await db.query('scheduled_notifications', columns: ['notif_id']);
    final ids = <int>{for (final r in known) (r['notif_id'] as num).toInt()};
    try {
      ids.addAll((await gateway.pendingIds()).where((i) => i >= minId));
    } catch (_) {}
    for (final id in ids) {
      try {
        await gateway.cancel(id);
      } catch (_) {
        ok = false;
      }
    }
    await db.delete('scheduled_notifications');

    final prefs = await _prefs();
    final all = <(String, String, PlannedNotification)>[];
    for (final r in await db.query('documents', where: 'expiry_date IS NOT NULL')) {
      final d = DocumentItem.fromMap(r);
      all.addAll(planDocument(d, prefs, n).map((p) => (typeDocument, d.id, p)));
    }
    for (final r in await db.query('tasks', where: 'is_completed = 0 AND due_at IS NOT NULL')) {
      final t = TaskItem.fromMap(r);
      all.addAll(planTask(t, prefs, n).map((p) => (typeTask, t.id, p)));
    }
    for (final r in await db.query('events')) {
      final e = EventItem.fromMap(r);
      all.addAll(planEvent(e, prefs, n).map((p) => (typeEvent, e.id, p)));
    }
    all.sort((a, b) => a.$3.at.compareTo(b.$3.at));
    for (final item in all.take(maxPending)) {
      ok = await _scheduleOne(item.$1, item.$2, item.$3) && ok;
    }
    return ok;
  }

  Future<void> cancelAllReminders() async {
    final known = await db.query('scheduled_notifications', columns: ['notif_id']);
    for (final r in known) {
      try {
        await gateway.cancel((r['notif_id'] as num).toInt());
      } catch (_) {}
    }
    await db.delete('scheduled_notifications');
  }

  Future<int> pendingCount() async =>
      Sqflite.firstIntValue(await db.rawQuery('SELECT COUNT(*) FROM scheduled_notifications')) ?? 0;
}
