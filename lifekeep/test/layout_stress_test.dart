import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/app.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';

import 'helpers.dart';

/// Renders every main screen on a small phone with large system text and
/// long Thai titles; any RenderFlex overflow fails the test.
void main() {
  for (final (w, h, scale) in const [(320.0, 640.0, 1.3), (412.0, 915.0, 1.0), (800.0, 1280.0, 1.0)]) {
    testWidgets('no overflow at ${w.toInt()}x${h.toInt()} text x$scale', (tester) async {
      await tester.binding.setSurfaceSize(Size(w, h));
      tester.platformDispatcher.textScaleFactorTestValue = scale;
      addTearDown(() {
        tester.binding.setSurfaceSize(null);
        tester.platformDispatcher.clearTextScaleFactorTestValue();
      });
      final env = (await tester.runAsync(() async {
        final e = await TestEnv.create();
        final now = DateTime.now();
        const long = 'กรมธรรม์ประกันสุขภาพแบบเหมาจ่ายพร้อมค่ารักษาพยาบาลผู้ป่วยในและผู้ป่วยนอกของครอบครัว';
        for (var i = 0; i < 4; i++) {
          await e.services.documents.upsert(
            DocumentItem(
              id: 'd$i',
              title: '$long $i',
              description: 'รายละเอียดยาวมากเพื่อทดสอบการตัดข้อความ',
              categoryId: 'cat_insurance',
              createdAt: now,
              updatedAt: now,
              expiryDate: now.add(Duration(days: i * 5 - 2)),
              reminder: const ReminderConfig([1440]),
              isPinned: i == 0,
            ),
          );
          await e.services.tasks.upsert(
            TaskItem(
              id: 't$i',
              title: '$long งาน $i',
              categoryId: 'cat_finance',
              dueAt: now.add(Duration(days: i - 1, hours: 2)),
              hasTime: true,
              priority: TaskPriority.values[i % 3],
              reminder: const ReminderConfig([0]),
              createdAt: now,
              updatedAt: now,
            ),
          );
          await e.services.events.upsert(
            EventItem(
              id: 'e$i',
              title: '$long กิจกรรม $i',
              location: 'สถานที่ที่มีชื่อยาวมากสำหรับการทดสอบ',
              startAt: now.add(Duration(days: i, hours: 1)),
              endAt: now.add(Duration(days: i, hours: 3)),
              createdAt: now,
              updatedAt: now,
            ),
          );
        }
        return e;
      }))!;
      addTearDown(() => tester.runAsync(env.dispose));

      await tester.pumpWidget(LifeKeepApp(controller: env.controller));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'home');

      for (final tab in ['เอกสาร', 'งาน', 'ปฏิทิน']) {
        await tester.tap(find.text(tab).last);
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull, reason: tab);
      }
      // Tasks: every segment
      await tester.tap(find.text('งาน').last);
      await tester.pumpAndSettle();
      for (final seg in ['ถัดไป', 'เลยกำหนด', 'เสร็จแล้ว']) {
        await tester.tap(find.text(seg).first);
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull, reason: seg);
      }
      // Forms
      await tester.tap(find.byTooltip('เพิ่มงาน'));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'task form');
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      await tester.tap(find.text('เอกสาร').last);
      await tester.pumpAndSettle();
      await tester.tap(find.byTooltip('เพิ่มเอกสาร'));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'document form');
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      // Grid view
      await tester.tap(find.byTooltip('มุมมองตาราง'));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'documents grid');
      // Settings
      await tester.tap(find.text('หน้าแรก'));
      await tester.pumpAndSettle();
      await tester.tap(find.byTooltip('ตั้งค่า'));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull, reason: 'settings');
    });
  }
}
