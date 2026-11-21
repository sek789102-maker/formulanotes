// On-device UI tests (run on an Android emulator in CI):
//   flutter test integration_test -d <device>
// Uses the real app bootstrap: Android SQLite, file system, secure storage
// and notification plugins - no fakes.
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lifekeep/app.dart';
import 'package:lifekeep/ui/widgets/item_tiles.dart';

Future<void> settle(WidgetTester t, [int seconds = 2]) async {
  // Real platform I/O completes outside the fake clock: pump in real time.
  final end = DateTime.now().add(Duration(seconds: seconds));
  while (DateTime.now().isBefore(end)) {
    await t.pump(const Duration(milliseconds: 100));
  }
}

Future<void> boot(WidgetTester t) async {
  // Drop any previous app tree (open routes/sheets, keyboard) before starting.
  FocusManager.instance.primaryFocus?.unfocus();
  await SystemChannels.textInput.invokeMethod<void>('TextInput.hide');
  await t.pumpWidget(const SizedBox());
  await t.pump(const Duration(milliseconds: 300));
  await t.pumpWidget(const BootstrapApp());
  for (var i = 0; i < 100 && find.byType(NavigationBar).evaluate().isEmpty; i++) {
    await t.pump(const Duration(milliseconds: 100));
  }
  expect(find.byType(NavigationBar), findsOneWidget, reason: 'app did not finish starting');
  await settle(t, 1);
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  final stamp = DateTime.now().millisecondsSinceEpoch.toString().substring(7);
  final taskTitle = 'งานทดสอบ $stamp';
  final docTitle = 'เอกสารทดสอบ $stamp';

  testWidgets('create, complete, search, persist (real Android storage)', (t) async {
    await boot(t);

    // Add a task through the UI
    await t.tap(find.text('เพิ่มงาน').first);
    await settle(t);
    await t.enterText(find.byType(TextFormField).first, taskTitle);
    await t.tap(find.text('บันทึกงาน'));
    await settle(t);
    expect(find.text('ชื่องาน *'), findsNothing, reason: 'task form should close after save');

    // Add a document through the UI, choosing a category from the sheet
    await t.tap(find.text('เพิ่มเอกสาร').first);
    await settle(t);
    await t.enterText(find.byType(TextFormField).first, docTitle);
    await t.tap(find.text('หมวดหมู่'));
    await settle(t, 1);
    await t.tap(find.text('ยานพาหนะ'));
    await settle(t, 1);
    await t.tap(find.text('บันทึกเอกสาร'));
    await settle(t);

    // Documents tab shows it; edit via detail → pin
    await t.tap(find.text('เอกสาร').last);
    await settle(t);
    await t.enterText(find.byType(TextField).first, stamp);
    await settle(t);
    expect(find.byType(DocumentRow), findsOneWidget);
    await t.tap(find.byType(DocumentRow));
    await settle(t);
    await t.tap(find.byTooltip('ปักหมุด'));
    await settle(t);
    expect(find.byTooltip('เลิกปักหมุด'), findsOneWidget);
    await t.tap(find.byType(BackButton));
    await settle(t);

    // Tasks: complete it
    await t.tap(find.text('งาน').last);
    await settle(t);
    await t.tap(find.text('ถัดไป'));
    await settle(t);
    await t.scrollUntilVisible(find.text(taskTitle), 300, scrollable: find.byType(Scrollable).last);
    final row = find.ancestor(of: find.text(taskTitle), matching: find.byType(TaskRow));
    await t.tap(find.descendant(of: row, matching: find.byType(TaskCheck)));
    await settle(t);
    await t.tap(find.text('เสร็จแล้ว').first);
    await settle(t);
    expect(find.text(taskTitle), findsOneWidget);

    // Global search across types
    await t.tap(find.text('หน้าแรก'));
    await settle(t);
    await t.tap(find.byTooltip('ค้นหา'));
    await settle(t);
    await t.enterText(find.byType(TextField), stamp);
    await settle(t);
    expect(find.text('พบ 2 รายการ'), findsOneWidget);
    await t.tap(find.byType(BackButton));
    await settle(t);

    // Calendar renders
    await t.tap(find.text('ปฏิทิน').last);
    await settle(t);
    expect(find.text('เอกสารหมดอายุ'), findsOneWidget);

    // "Restart": a fresh bootstrap re-reads everything from disk
    await t.pumpWidget(const SizedBox());
    await settle(t, 1);
    await boot(t);
    await t.tap(find.text('เอกสาร').last);
    await settle(t);
    await t.enterText(find.byType(TextField).first, stamp);
    await settle(t);
    expect(find.text(docTitle), findsOneWidget, reason: 'document must persist');
  });

  testWidgets('local backup from Settings on real storage', (t) async {
    await boot(t);
    await t.tap(find.byTooltip('ตั้งค่า'));
    await settle(t);
    await t.scrollUntilVisible(find.text('สำรองและกู้คืนข้อมูล'), 200, scrollable: find.byType(Scrollable).first);
    await settle(t, 1);
    await t.tap(find.text('สำรองและกู้คืนข้อมูล'));
    await settle(t);
    await t.tap(find.text('สำรองในเครื่อง'));
    await settle(t, 6);
    // (The success snackbar is transient; check the lasting state instead.)
    expect(find.text('ข้อมูลได้รับการสำรองแล้ว'), findsOneWidget);
    await t.drag(find.byType(Scrollable).first, const Offset(0, -600));
    await settle(t, 1);
    expect(find.textContaining('ด้วยตนเอง'), findsWidgets, reason: 'manual backup listed in history');
  });
}
