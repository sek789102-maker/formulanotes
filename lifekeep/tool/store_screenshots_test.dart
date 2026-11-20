// Dev tool: renders Play Store screenshots (1080x1920, 9:16) of the real app
// UI with sample data. Not part of the test suite.
//   flutter test tool/store_screenshots_test.dart --update-goldens
// Output: tool/store_shots/*.png
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/app.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/data/repositories/settings_repository.dart';
import 'package:path/path.dart' as p;

import '../test/helpers.dart';

Future<void> _loadFonts() async {
  final f = FontLoader('IBMPlexSansThai');
  for (final w in ['Regular', 'Medium', 'SemiBold', 'Bold']) {
    f.addFont(Future.value(ByteData.sublistView(File('assets/fonts/IBMPlexSansThai-$w.ttf').readAsBytesSync())));
  }
  await f.load();
  // flutter_tester lives in <flutter>/bin/cache/artifacts/engine/<platform>/
  final artifacts = p.dirname(p.dirname(p.dirname(Platform.resolvedExecutable)));
  final icons = File(p.join(artifacts, 'material_fonts', 'MaterialIcons-Regular.otf'));
  final i = FontLoader('MaterialIcons')..addFont(Future.value(ByteData.sublistView(icons.readAsBytesSync())));
  await i.load();
}

Future<void> _seed(TestEnv env) async {
  final now = DateTime.now();
  final s = env.services;
  DateTime d(int days, [int h = 9]) => DateTime(now.year, now.month, now.day + days, h);
  Future<void> doc(String id, String t, String cat, DateTime? exp, {bool pin = false, String desc = '', String notes = ''}) =>
      s.documents.upsert(DocumentItem(id: id, title: t, categoryId: cat, description: desc, notes: notes, createdAt: now,
          updatedAt: now, expiryDate: exp, reminder: const ReminderConfig([1440, 10080]), isPinned: pin));
  await doc('1', 'บัตรประชาชน', 'cat_personal', d(1200), pin: true, desc: 'ตัวอย่างเอกสาร');
  await doc('2', 'ใบขับขี่รถยนต์', 'cat_vehicle', d(5), desc: 'ต่ออายุที่สำนักงานขนส่ง', notes: 'ต่ออายุได้ล่วงหน้า 90 วัน');
  await doc('3', 'กรมธรรม์ประกันสุขภาพ', 'cat_insurance', d(-3));
  await doc('4', 'หนังสือเดินทาง', 'cat_personal', d(400));
  await doc('5', 'ภาษีรถยนต์ (พ.ร.บ.)', 'cat_vehicle', d(25));
  await doc('6', 'ใบปริญญาบัตร', 'cat_education', null);
  Future<void> task(String id, String t, DateTime? due, TaskPriority pr, {bool time = false, bool done = false, String? cat}) =>
      s.tasks.upsert(TaskItem(id: id, title: t, dueAt: due, hasTime: time, priority: pr, isCompleted: done,
          completedAt: done ? now : null, categoryId: cat, reminder: const ReminderConfig([0]), createdAt: now, updatedAt: now));
  await task('t1', 'จ่ายค่าไฟฟ้า', d(0, 23), TaskPriority.high, time: true, cat: 'cat_home');
  await task('t2', 'นัดตรวจสุขภาพประจำปี', d(2, 10), TaskPriority.normal, time: true, cat: 'cat_health');
  await task('t3', 'ส่งเอกสารลดหย่อนภาษี', d(-1), TaskPriority.high, cat: 'cat_finance');
  await task('t4', 'ซื้อของเข้าบ้าน', d(5), TaskPriority.low);
  await task('t6', 'โทรหาประกันเรื่องเคลม', d(1, 14), TaskPriority.normal, time: true, cat: 'cat_insurance');
  await task('t5', 'ล้างรถ', d(-2), TaskPriority.normal, done: true);
  Future<void> ev(String id, String t, DateTime st, {DateTime? end, String loc = '', bool all = false, int? color}) =>
      s.events.upsert(EventItem(id: id, title: t, startAt: st, endAt: end, location: loc, allDay: all, colorValue: color,
          reminder: const ReminderConfig([60]), createdAt: now, updatedAt: now));
  await ev('e0', 'ประชุมทีม', d(0, 15), end: d(0, 16), loc: 'ออนไลน์');
  await ev('e1', 'ประชุมครอบครัว', d(1, 19), end: d(1, 21), loc: 'บ้าน');
  await ev('e2', 'ทริปต่างจังหวัด', d(6), end: d(8), all: true, color: 0xFF2E7D7A);
  await ev('e3', 'นัดทันตแพทย์', d(3, 14), loc: 'คลินิก', color: 0xFFB4473F);
  await s.settings.set(SettingKeys.lastManualBackupAt, '${now.subtract(const Duration(days: 1)).millisecondsSinceEpoch}');
}

void main() {
  late TestEnv env;
  Future<void> setup(WidgetTester t, {bool dark = false}) async {
    t.view.physicalSize = const Size(1080, 1920);
    t.view.devicePixelRatio = 2.625;
    addTearDown(t.view.reset);
    env = (await t.runAsync(() async {
      await _loadFonts();
      final e = await TestEnv.create();
      await _seed(e);
      if (dark) await e.services.settings.set(SettingKeys.themeMode, 'dark');
      await e.controller.load();
      return e;
    }))!;
    addTearDown(() => t.runAsync(env.dispose));
    await t.pumpWidget(LifeKeepApp(controller: env.controller));
    await t.pumpAndSettle();
  }

  Future<void> shot(WidgetTester t, String name) async {
    await t.pumpAndSettle();
    await expectLater(find.byType(LifeKeepApp), matchesGoldenFile('store_shots/$name.png'));
  }

  testWidgets('store screenshots', (t) async {
    await setup(t);
    await shot(t, '1_home');
    await t.tap(find.text('เอกสาร').last);
    await shot(t, '2_documents');
    await t.tap(find.text('ใบขับขี่รถยนต์'));
    await shot(t, '3_document_detail');
    await t.tap(find.byTooltip('แก้ไข'));
    await shot(t, '4_document_form');
    await t.tap(find.byType(BackButton));
    await t.pumpAndSettle();
    await t.tap(find.byType(BackButton));
    await t.pumpAndSettle();
    await t.tap(find.text('งาน').last);
    await t.pumpAndSettle();
    await t.tap(find.text('ถัดไป'));
    await shot(t, '5_tasks');
    await t.tap(find.text('ปฏิทิน').last);
    await shot(t, '6_calendar');
    await t.tap(find.text('หน้าแรก'));
    await t.pumpAndSettle();
    await t.tap(find.byTooltip('ตั้งค่า'));
    await shot(t, '7_settings');
  });

  testWidgets('dark', (t) async {
    await setup(t, dark: true);
    await shot(t, '8_home_dark');
  });
}
