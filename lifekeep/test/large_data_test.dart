import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/services/backup_service.dart';
import 'package:lifekeep/services/reminder_scheduler.dart';

import 'helpers.dart';

/// Volume test: a heavy user's data set must stay fast, searchable and
/// survive a full backup → restore round-trip intact.
void main() {
  late TestEnv a;
  late TestEnv b;
  setUp(() async {
    a = await TestEnv.create();
    b = await TestEnv.create();
  });
  tearDown(() async {
    await a.dispose();
    await b.dispose();
  });

  test('3,000 records: queries, search, reminders cap, backup/restore round-trip', () async {
    final now = DateTime.now();
    final future = DateTime(now.year + 1, 1, 1);
    await a.db.transaction((txn) async {
      for (var i = 0; i < 1000; i++) {
        await txn.insert(
          'documents',
          DocumentItem(
            id: 'd$i',
            title: 'เอกสารหมายเลข $i',
            categoryId: i.isEven ? 'cat_personal' : 'cat_finance',
            notes: i % 100 == 0 ? 'มีคำว่าพิเศษ' : '',
            createdAt: now,
            updatedAt: now,
            expiryDate: future.add(Duration(days: i % 365)),
            reminder: const ReminderConfig([1440, 10080]),
          ).toMap(),
        );
        await txn.insert(
          'tasks',
          TaskItem(
            id: 't$i',
            title: 'งานที่ $i',
            dueAt: future.add(Duration(days: i % 200)),
            reminder: const ReminderConfig([0]),
            isCompleted: i % 3 == 0,
            createdAt: now,
            updatedAt: now,
          ).toMap(),
        );
        await txn.insert(
          'events',
          EventItem(
            id: 'e$i',
            title: 'กิจกรรม $i',
            startAt: future.add(Duration(days: i % 300, hours: 10)),
            createdAt: now,
            updatedAt: now,
          ).toMap(),
        );
      }
    });

    final sw = Stopwatch()..start();
    expect((await a.services.documents.all()).length, 1000);
    expect(await a.services.tasks.pendingCount(), 666);
    expect((await a.services.documents.search('พิเศษ')).length, 10);
    expect((await a.services.tasks.search('งานที่ 99')).length, 11); // 99, 990..999
    expect((await a.services.events.between(future, future.add(const Duration(days: 31)))).isNotEmpty, isTrue);
    final queryMs = sw.elapsedMilliseconds;
    expect(queryMs, lessThan(5000), reason: 'queries took ${queryMs}ms');

    // Reminders never exceed the Android pending-alarm budget.
    await a.services.scheduler.rescheduleAll();
    expect(a.gateway.scheduled.length, ReminderScheduler.maxPending);

    // Full backup → restore into a fresh install.
    final info = await a.services.backup.createBackup(includeAttachments: false);
    expect(await File(info.path).length(), greaterThan(0));
    final ins = await b.services.backup.inspect(info.path);
    expect(ins.canRestore, isTrue, reason: ins.fatal);
    expect(ins.warnings, isEmpty);
    final r = await b.controller.restore(ins, RestoreMode.replace);
    expect(r.counts.documents, 1000);
    expect((await b.services.documents.all()).length, 1000);
    expect((await b.services.tasks.all()).length, 1000);
    expect((await b.services.events.all()).length, 1000);
    final d500 = (await b.services.documents.byId('d500'))!;
    expect(d500.title, 'เอกสารหมายเลข 500');
    expect(d500.reminder.offsetsMinutes, [1440, 10080]);
  }, timeout: const Timeout(Duration(minutes: 3)));
}
