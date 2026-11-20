import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/app.dart';

import 'helpers.dart';

void main() {
  testWidgets('home → add task via form → visible in Tasks; calendar and settings open', (tester) async {
    await tester.binding.setSurfaceSize(const Size(412, 915));
    final env = (await tester.runAsync(() => TestEnv.create()))!;
    addTearDown(() => tester.runAsync(env.dispose));

    await tester.pumpWidget(LifeKeepApp(controller: env.controller));
    await tester.pumpAndSettle();

    // Empty dashboard
    expect(find.text('เริ่มต้นใช้งาน LIFEKEEP'), findsOneWidget);
    expect(find.text('เอกสารทั้งหมด'), findsOneWidget);

    // Quick action: add task
    await tester.tap(find.text('เพิ่มงาน').first);
    await tester.pumpAndSettle();
    expect(find.text('ชื่องาน *'), findsOneWidget);

    // Validation: empty title is rejected
    await tester.tap(find.widgetWithText(TextButton, 'บันทึก'));
    await tester.pumpAndSettle();
    expect(find.text('กรุณากรอกชื่อ'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField).first, 'ต่อภาษีรถยนต์');
    await tester.tap(find.widgetWithText(TextButton, 'บันทึก'));
    await tester.pumpAndSettle();
    expect(find.text('ชื่องาน *'), findsNothing); // form closed

    // Tasks tab shows the new task (no due date -> Upcoming)
    await tester.tap(find.text('งาน').last);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('กำลังจะมาถึง'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('กำลังจะมาถึง'));
    await tester.pumpAndSettle();
    expect(find.text('ต่อภาษีรถยนต์'), findsOneWidget);

    // Complete it -> moves to Completed tab
    await tester.tap(find.byType(Checkbox).first);
    await tester.pumpAndSettle();
    expect((await tester.runAsync(() => env.services.tasks.pendingCount())), 0);
    await tester.ensureVisible(find.text('เสร็จแล้ว'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('เสร็จแล้ว'));
    await tester.pumpAndSettle();
    expect(find.text('ต่อภาษีรถยนต์'), findsOneWidget);

    // Calendar tab
    await tester.tap(find.text('ปฏิทิน').last);
    await tester.pumpAndSettle();
    expect(find.text('กิจกรรม'), findsWidgets); // legend

    // Settings via Home
    await tester.tap(find.text('หน้าแรก'));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('ตั้งค่า'));
    await tester.pumpAndSettle();
    expect(find.text('สำรองและกู้คืนข้อมูล'), findsOneWidget);
    expect(find.text('ลบข้อมูลทั้งหมด'), findsOneWidget);

    // Switch language to English and back
    await tester.tap(find.text('ภาษา'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('English'));
    await tester.pumpAndSettle();
    expect(find.text('Settings'), findsOneWidget);
    expect(find.text('Backup & restore'), findsOneWidget);
  });
}
