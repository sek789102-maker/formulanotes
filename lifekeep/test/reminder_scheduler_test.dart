import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/utils/dates.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/data/repositories/settings_repository.dart';
import 'package:lifekeep/services/reminder_scheduler.dart';

import 'helpers.dart';

void main() {
  late TestEnv env;
  final now = DateTime.now();
  final future = DateTime(now.year + 1, 3, 15);

  setUp(() async => env = await TestEnv.create());
  tearDown(() async => env.dispose());

  DocumentItem doc({List<int> offsets = const [1440, 10080], DateTime? expiry}) => DocumentItem(
    id: 'doc1',
    title: 'Passport',
    categoryId: 'cat_personal',
    createdAt: now,
    updatedAt: now,
    expiryDate: expiry ?? future,
    reminder: ReminderConfig(offsets),
  );

  test('document reminders are scheduled at the reminder time before expiry', () async {
    final ok = await env.controller.saveDocument(doc());
    expect(ok, isTrue);
    final times = env.gateway.scheduled.values.map((v) => v.at).toList()..sort();
    expect(times, [DateTime(future.year, 3, 8, 9), DateTime(future.year, 3, 14, 9)]);
    expect(await env.services.scheduler.pendingCount(), 2);
  });

  test('editing cancels old notifications and schedules the new ones only', () async {
    await env.controller.saveDocument(doc());
    final oldIds = env.gateway.scheduled.keys.toSet();
    await env.controller.saveDocument(doc(offsets: [3 * 1440]));
    expect(env.gateway.cancelled.toSet(), containsAll(oldIds));
    expect(env.gateway.scheduled.length, 1);
    expect(env.gateway.scheduled.values.single.at, DateTime(future.year, 3, 12, 9));
    expect(await env.services.scheduler.pendingCount(), 1);
  });

  test('saving the same item twice never duplicates notifications', () async {
    await env.controller.saveDocument(doc());
    await env.controller.saveDocument(doc());
    await env.controller.saveDocument(doc());
    expect(env.gateway.scheduled.length, 2);
    expect(await env.services.scheduler.pendingCount(), 2);
  });

  test('deleting cancels reminders', () async {
    final d = doc();
    await env.controller.saveDocument(d);
    await env.controller.deleteDocument(d);
    expect(env.gateway.scheduled, isEmpty);
    expect(await env.services.scheduler.pendingCount(), 0);
  });

  test('completing a task cancels its reminder; reopening restores it', () async {
    final t = TaskItem(
      id: 't',
      title: 'Pay bill',
      dueAt: DateTime(future.year, 1, 10, 15, 0),
      hasTime: true,
      reminder: const ReminderConfig([0, 60]),
      createdAt: now,
      updatedAt: now,
    );
    await env.controller.saveTask(t);
    expect(env.gateway.scheduled.length, 2);
    await env.controller.setTaskCompleted(t, true);
    expect(env.gateway.scheduled, isEmpty);
    await env.controller.setTaskCompleted(t, false);
    expect(env.gateway.scheduled.values.map((v) => v.at).toSet(), {
      DateTime(future.year, 1, 10, 15),
      DateTime(future.year, 1, 10, 14),
    });
  });

  test('events: all-day uses default time; past reminders are skipped', () async {
    final e = EventItem(
      id: 'e',
      title: 'Trip',
      startAt: DateTime(future.year, 6, 1),
      allDay: true,
      reminder: const ReminderConfig([1440]),
      createdAt: now,
      updatedAt: now,
    );
    await env.controller.saveEvent(e);
    expect(env.gateway.scheduled.values.single.at, DateTime(future.year, 5, 31, 9));

    final past = e.copyWith(startAt: DateTime(now.year - 1, 1, 1));
    await env.controller.saveEvent(past);
    expect(env.gateway.scheduled, isEmpty);
  });

  test('disabling a reminder type and rescheduleAll removes them', () async {
    await env.controller.saveDocument(doc());
    expect(env.gateway.scheduled.length, 2);
    await env.controller.setSetting(SettingKeys.notifyDocuments, 'false', reschedule: true);
    expect(env.gateway.scheduled, isEmpty);
    await env.controller.setSetting(SettingKeys.notifyDocuments, 'true', reschedule: true);
    expect(env.gateway.scheduled.length, 2);
  });

  test('changing reminder time of day reschedules', () async {
    await env.controller.saveDocument(doc(offsets: [1440]));
    await env.controller.setSetting(SettingKeys.reminderHour, '7');
    await env.controller.setSetting(SettingKeys.reminderMinute, '30', reschedule: true);
    expect(env.gateway.scheduled.values.single.at, DateTime(future.year, 3, 14, 7, 30));
  });

  test('gateway failure is reported but the item is still saved', () async {
    env.gateway.failSchedule = true;
    final ok = await env.controller.saveDocument(doc());
    expect(ok, isFalse);
    expect(await env.services.documents.byId('doc1'), isNotNull);
  });

  test('rescheduleAll caps pending notifications', () async {
    for (var i = 0; i < 150; i++) {
      await env.services.documents.upsert(
        DocumentItem(
          id: 'd$i',
          title: 'D$i',
          categoryId: 'cat_other',
          createdAt: now,
          updatedAt: now,
          expiryDate: future.add(Duration(days: i)),
          reminder: const ReminderConfig([1440, 3 * 1440, 7 * 1440]),
        ),
      );
    }
    await env.services.scheduler.rescheduleAll();
    expect(env.gateway.scheduled.length, ReminderScheduler.maxPending);
  });

  test('stableHash is deterministic and outside reserved ids', () {
    final a = ReminderScheduler.stableHash('task:x:1');
    expect(a, ReminderScheduler.stableHash('task:x:1'));
    expect(a, greaterThanOrEqualTo(ReminderScheduler.minId));
    expect(a, lessThan(0x7FFFFFFF + 1));
  });

  test('plan respects global switch', () {
    final plans = ReminderScheduler.planDocument(doc(), const ReminderPrefs(enabled: false), now);
    expect(plans, isEmpty);
    final on = ReminderScheduler.planDocument(doc(), const ReminderPrefs(), now);
    expect(on.length, 2);
    expect(on.first.body, contains('Passport'));
    expect(Dates.isSameDay(on.last.at, DateTime(future.year, 3, 14)), isTrue);
  });
}
