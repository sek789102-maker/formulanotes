import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/app.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/ui/widgets/item_tiles.dart';

import 'helpers.dart';

void main() {
  Future<TestEnv> start(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(412, 915));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final env = (await tester.runAsync(() => TestEnv.create()))!;
    addTearDown(() => tester.runAsync(env.dispose));
    await tester.pumpWidget(LifeKeepApp(controller: env.controller));
    await tester.pumpAndSettle();
    return env;
  }

  testWidgets('home → add task via form → Tasks tabs → complete → calendar → settings', (tester) async {
    final env = await start(tester);

    // Empty dashboard: overview strip + welcome
    expect(find.text('เริ่มต้นใช้งาน LIFEKEEP'), findsOneWidget);
    expect(find.text('งานค้าง'), findsOneWidget);
    expect(find.text('เลยกำหนด'), findsOneWidget);

    // Quick action: add task
    await tester.tap(find.text('เพิ่มงาน').first);
    await tester.pumpAndSettle();
    expect(find.text('ชื่องาน *'), findsOneWidget);

    // Validation: empty title rejected
    await tester.tap(find.widgetWithText(TextButton, 'บันทึก'));
    await tester.pumpAndSettle();
    expect(find.text('กรุณากรอกชื่อ'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField).first, 'ต่อภาษีรถยนต์');
    await tester.tap(find.text('บันทึกงาน')); // sticky bottom save bar
    await tester.pumpAndSettle();
    expect(find.text('ชื่องาน *'), findsNothing);
    expect(await tester.runAsync(() => env.services.tasks.pendingCount()), 1);

    // Tasks tab: undated task is under "Upcoming", grouped as "No date"
    await tester.tap(find.text('งาน').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('ถัดไป'));
    await tester.pumpAndSettle();
    expect(find.text('ต่อภาษีรถยนต์'), findsOneWidget);
    expect(find.text('ไม่มีกำหนด'), findsOneWidget);

    // Complete with the animated check
    await tester.tap(find.byType(TaskCheck).first);
    await tester.pumpAndSettle();
    expect(await tester.runAsync(() => env.services.tasks.pendingCount()), 0);
    await tester.tap(find.text('เสร็จแล้ว').first);
    await tester.pumpAndSettle();
    expect(find.text('ต่อภาษีรถยนต์'), findsOneWidget);

    // Calendar tab with legend and week toggle
    await tester.tap(find.text('ปฏิทิน').last);
    await tester.pumpAndSettle();
    expect(find.text('เอกสารหมดอายุ'), findsOneWidget);
    await tester.tap(find.text('ดูเฉพาะสัปดาห์'));
    await tester.pumpAndSettle();
    expect(find.text('ดูทั้งเดือน'), findsOneWidget);

    // Settings via Home
    await tester.tap(find.text('หน้าแรก'));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('ตั้งค่า'));
    await tester.pumpAndSettle();
    expect(find.text('สำรองและกู้คืนข้อมูล'), findsOneWidget);
    expect(find.text('ยังไม่เคยสำรอง'), findsWidgets);
    await tester.scrollUntilVisible(find.text('ลบข้อมูลทั้งหมด'), 200);
    expect(find.text('ลบข้อมูลทั้งหมด'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('English'), -200);

    // Inline language switch
    await tester.tap(find.text('English'));
    await tester.pumpAndSettle();
    expect(find.text('Settings'), findsOneWidget);
    expect(find.text('Backup & restore'), findsOneWidget);
  });

  testWidgets('documents: add via form, list ↔ grid, search highlight, detail', (tester) async {
    final env = await start(tester);
    await tester.tap(find.text('เอกสาร').last);
    await tester.pumpAndSettle();
    expect(find.text('ยังไม่มีเอกสาร'), findsOneWidget);

    await tester.tap(find.byTooltip('เพิ่มเอกสาร'));
    await tester.pumpAndSettle();
    expect(find.text('ข้อมูลเอกสาร'), findsOneWidget);
    await tester.enterText(find.byType(TextFormField).first, 'ใบขับขี่');

    // Category sheet
    await tester.tap(find.text('หมวดหมู่'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('ยานพาหนะ'));
    await tester.pumpAndSettle();
    expect(find.text('ยานพาหนะ'), findsOneWidget);

    await tester.tap(find.text('บันทึกเอกสาร'));
    await tester.pumpAndSettle();
    final docs = (await tester.runAsync(() => env.services.documents.all()))!;
    expect(docs.single.title, 'ใบขับขี่');
    expect(docs.single.categoryId, 'cat_vehicle');

    // List row, then grid view
    expect(find.byType(DocumentRow), findsOneWidget);
    await tester.tap(find.byTooltip('มุมมองตาราง'));
    await tester.pumpAndSettle();
    expect(find.byType(DocumentGridCard), findsOneWidget);
    await tester.tap(find.byTooltip('มุมมองรายการ'));
    await tester.pumpAndSettle();

    // Search highlights the match
    await tester.enterText(find.byType(TextField).first, 'ขับ');
    await tester.pumpAndSettle();
    expect(find.byType(HighlightText), findsWidgets);
    await tester.enterText(find.byType(TextField).first, 'ไม่มีจริง');
    await tester.pumpAndSettle();
    expect(find.text('ไม่พบเอกสาร'), findsOneWidget);
    await tester.enterText(find.byType(TextField).first, '');
    await tester.pumpAndSettle();

    // Detail screen
    await tester.tap(find.byType(DocumentRow));
    await tester.pumpAndSettle();
    expect(find.text('ไม่มีวันหมดอายุ'), findsOneWidget);
    await tester.tap(find.byTooltip('ปักหมุด'));
    await tester.pumpAndSettle();
    expect((await tester.runAsync(() => env.services.documents.byId(docs.single.id)))!.isPinned, isTrue);
  });

  testWidgets('global search groups results and filters by type', (tester) async {
    final env = await start(tester);
    final now = DateTime.now();
    await tester.runAsync(
      () => env.services.documents.upsert(
        DocumentItem(id: 'd1', title: 'ประกันรถยนต์', categoryId: 'cat_insurance', createdAt: now, updatedAt: now),
      ),
    );
    env.controller.refresh();
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('ค้นหา'));
    await tester.pumpAndSettle();
    expect(find.text('ค้นหาทุกอย่างในเครื่อง'), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'รถ');
    await tester.pumpAndSettle();
    expect(find.byType(DocumentRow), findsOneWidget);
    expect(find.text('พบ 1 รายการ'), findsOneWidget);
  });
}
