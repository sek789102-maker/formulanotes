import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/app.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';

import 'helpers.dart';

void main() {
  TaskItem task(DateTime now) => TaskItem(
    id: 't1',
    title: 'จ่ายค่าไฟ',
    dueAt: DateTime(now.year + 1, 1, 5, 10),
    hasTime: true,
    reminder: const ReminderConfig([0]),
    createdAt: now,
    updatedAt: now,
  );

  test('granting exact alarms later re-schedules every reminder', () async {
    final env = await TestEnv.create();
    addTearDown(env.dispose);
    env.gateway.exactAlarmsAllowed = false;
    await env.controller.refreshPermissions();
    await env.controller.saveTask(task(DateTime.now()));
    final before = env.gateway.cancelled.length;

    env.gateway.exactAlarmsAllowed = true; // user enabled it in system settings
    await env.controller.refreshPermissions();
    expect(env.gateway.cancelled.length, greaterThan(before), reason: 'old (inexact) alarms replaced');
    expect(env.gateway.scheduled.length, 1);

    // No change -> no needless re-scheduling.
    final count = env.gateway.cancelled.length;
    await env.controller.refreshPermissions();
    expect(env.gateway.cancelled.length, count);
  });

  testWidgets('home warns when reminders exist but exact alarms are not allowed', (tester) async {
    await tester.binding.setSurfaceSize(const Size(412, 915));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final env = (await tester.runAsync(() async {
      final e = await TestEnv.create();
      e.gateway.exactAlarmsAllowed = false;
      await e.controller.saveTask(task(DateTime.now()));
      return e;
    }))!;
    addTearDown(() => tester.runAsync(env.dispose));
    await tester.pumpWidget(LifeKeepApp(controller: env.controller));
    await tester.pumpAndSettle();
    expect(find.text('การแจ้งเตือนอาจไม่ตรงเวลา'), findsOneWidget);

    await tester.tap(find.text('การแจ้งเตือนอาจไม่ตรงเวลา'));
    await tester.pumpAndSettle();
    expect(find.text('ตั้งเวลาแบบแม่นยำ'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('ทดสอบแจ้งเตือนแบบตั้งเวลา (1 นาที)'), 300,
        scrollable: find.byType(Scrollable).last);
    expect(find.text('ทดสอบแจ้งเตือนแบบตั้งเวลา (1 นาที)'), findsOneWidget);
  });

  testWidgets('no warning when everything is allowed', (tester) async {
    await tester.binding.setSurfaceSize(const Size(412, 915));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final env = (await tester.runAsync(() async {
      final e = await TestEnv.create();
      await e.controller.saveTask(task(DateTime.now()));
      return e;
    }))!;
    addTearDown(() => tester.runAsync(env.dispose));
    await tester.pumpWidget(LifeKeepApp(controller: env.controller));
    await tester.pumpAndSettle();
    expect(find.text('การแจ้งเตือนอาจไม่ตรงเวลา'), findsNothing);
  });
}
