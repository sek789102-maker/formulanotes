import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/data/database/app_database.dart';
import 'package:lifekeep/data/models/category.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/data/repositories/category_repository.dart';
import 'package:lifekeep/data/repositories/document_repository.dart';
import 'package:lifekeep/data/repositories/event_repository.dart';
import 'package:lifekeep/data/repositories/settings_repository.dart';
import 'package:lifekeep/data/repositories/task_repository.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'helpers.dart';

DocumentItem doc(String id, {String title = 'Doc', String cat = 'cat_personal', DateTime? expiry, String notes = '', bool pinned = false, DateTime? updated}) =>
    DocumentItem(
      id: id,
      title: title,
      categoryId: cat,
      notes: notes,
      createdAt: DateTime(2026, 1, 1),
      updatedAt: updated ?? DateTime(2026, 1, 1),
      expiryDate: expiry,
      reminder: const ReminderConfig([1440]),
      attachmentPaths: const ['a.jpg'],
      isPinned: pinned,
    );

TaskItem task(String id, {String title = 'Task', DateTime? due, String? cat}) => TaskItem(
      id: id,
      title: title,
      categoryId: cat,
      dueAt: due,
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 1),
    );

void main() {
  late Database db;

  setUp(() async => db = await openTestDb());
  tearDown(() async => db.close());

  test('schema version and default categories are created', () async {
    expect(await db.getVersion(), AppDatabase.schemaVersion);
    final cats = await CategoryRepository(db).all();
    expect(cats.length, 9);
    expect(cats.map((c) => c.id), contains(ItemCategory.otherId));
    expect(cats.first.name, 'ส่วนตัว');
  });

  group('documents', () {
    test('CRUD round-trip keeps every field', () async {
      final repo = DocumentRepository(db);
      final d = doc('d1', title: 'บัตรประชาชน', expiry: DateTime(2030, 5, 1), notes: 'เลขที่ 123', pinned: true);
      await repo.upsert(d);
      final got = (await repo.byId('d1'))!;
      expect(got.title, 'บัตรประชาชน');
      expect(got.expiryDate, DateTime(2030, 5, 1));
      expect(got.reminder.offsetsMinutes, [1440]);
      expect(got.attachmentPaths, ['a.jpg']);
      expect(got.isPinned, isTrue);
      expect(got.notes, 'เลขที่ 123');

      await repo.upsert(got.copyWith(title: 'Updated', clearExpiry: true));
      final upd = (await repo.byId('d1'))!;
      expect(upd.title, 'Updated');
      expect(upd.expiryDate, isNull);
      expect(await repo.count(), 1);

      await repo.delete('d1');
      expect(await repo.byId('d1'), isNull);
      expect(await repo.count(), 0);
    });

    test('sorting puts pinned first and handles missing expiry', () async {
      final repo = DocumentRepository(db);
      await repo.upsert(doc('a', title: 'B', expiry: DateTime(2027)));
      await repo.upsert(doc('b', title: 'A'));
      await repo.upsert(doc('c', title: 'C', expiry: DateTime(2026, 6), pinned: true));
      expect((await repo.all(sort: DocumentSort.titleAsc)).map((d) => d.id), ['c', 'b', 'a']);
      expect((await repo.all(sort: DocumentSort.expiryAsc)).map((d) => d.id), ['c', 'a', 'b']);
    });

    test('expiring queries', () async {
      final repo = DocumentRepository(db);
      await repo.upsert(doc('old', expiry: DateTime(2026, 1, 1)));
      await repo.upsert(doc('soon', expiry: DateTime(2026, 12, 1)));
      await repo.upsert(doc('far', expiry: DateTime(2030, 1, 1)));
      await repo.upsert(doc('none'));
      expect((await repo.expiringBefore(DateTime(2026, 12, 31))).map((d) => d.id), ['old', 'soon']);
      expect((await repo.expiringBetween(DateTime(2026, 11, 1), DateTime(2026, 12, 31))).map((d) => d.id), ['soon']);
    });
  });

  group('tasks', () {
    test('completion toggles status and timestamp', () async {
      final repo = TaskRepository(db);
      await repo.upsert(task('t1', due: DateTime(2026, 12, 1)));
      expect(await repo.pendingCount(), 1);
      final done = (await repo.setCompleted('t1', true, now: DateTime(2026, 11, 20, 10)))!;
      expect(done.isCompleted, isTrue);
      expect(done.completedAt, DateTime(2026, 11, 20, 10));
      expect(await repo.pendingCount(), 0);
      final reopened = (await repo.setCompleted('t1', false))!;
      expect(reopened.isCompleted, isFalse);
      expect(reopened.completedAt, isNull);
    });

    test('priority and category survive round-trip; delete works', () async {
      final repo = TaskRepository(db);
      final t = task('t2', cat: 'cat_home').copyWith(priority: TaskPriority.high, hasTime: true, dueAt: DateTime(2026, 12, 1, 14, 30));
      await repo.upsert(t);
      final got = (await repo.byId('t2'))!;
      expect(got.priority, TaskPriority.high);
      expect(got.categoryId, 'cat_home');
      expect(got.dueAt, DateTime(2026, 12, 1, 14, 30));
      expect(got.hasTime, isTrue);
      await repo.delete('t2');
      expect(await repo.byId('t2'), isNull);
    });

    test('pendingDueBefore excludes completed and undated', () async {
      final repo = TaskRepository(db);
      await repo.upsert(task('a', due: DateTime(2026, 11, 21)));
      await repo.upsert(task('b', due: DateTime(2026, 11, 22)));
      await repo.upsert(task('c'));
      await repo.setCompleted('b', true);
      expect((await repo.pendingDueBefore(DateTime(2026, 12, 1))).map((t) => t.id), ['a']);
    });
  });

  group('events', () {
    test('between() returns overlapping multi-day events', () async {
      final repo = EventRepository(db);
      EventItem e(String id, DateTime s, [DateTime? end]) => EventItem(
          id: id, title: id, startAt: s, endAt: end, createdAt: DateTime(2026), updatedAt: DateTime(2026));
      await repo.upsert(e('trip', DateTime(2026, 11, 28), DateTime(2026, 12, 3)));
      await repo.upsert(e('meet', DateTime(2026, 12, 10, 9)));
      await repo.upsert(e('past', DateTime(2026, 10, 1)));
      final dec = await repo.between(DateTime(2026, 12, 1), DateTime(2026, 12, 31, 23, 59));
      expect(dec.map((x) => x.id).toSet(), {'trip', 'meet'});
      expect(dec.firstWhere((x) => x.id == 'trip').occursOn(DateTime(2026, 12, 2)), isTrue);
      expect(dec.firstWhere((x) => x.id == 'trip').occursOn(DateTime(2026, 12, 4)), isFalse);
    });
  });

  group('categories', () {
    test('deleting a category moves documents to Other and clears tasks', () async {
      final cats = CategoryRepository(db);
      await cats.upsert(ItemCategory(id: 'x', name: 'Custom', colorValue: 0xFF000000, iconKey: 'star', createdAt: DateTime(2026)));
      await DocumentRepository(db).upsert(doc('d', cat: 'x'));
      await TaskRepository(db).upsert(task('t', cat: 'x'));
      expect((await cats.usageCounts())['x'], 2);
      await cats.delete('x');
      expect(await cats.byId('x'), isNull);
      expect((await DocumentRepository(db).byId('d'))!.categoryId, ItemCategory.otherId);
      expect((await TaskRepository(db).byId('t'))!.categoryId, isNull);
    });

    test('the Other category cannot be deleted', () async {
      final cats = CategoryRepository(db);
      await cats.delete(ItemCategory.otherId);
      expect(await cats.byId(ItemCategory.otherId), isNotNull);
    });
  });

  group('search', () {
    setUp(() async {
      await DocumentRepository(db).upsert(doc('d1', title: 'ใบขับขี่', cat: 'cat_vehicle', notes: 'ต่ออายุที่ขนส่ง'));
      await DocumentRepository(db).upsert(doc('d2', title: 'Passport', cat: 'cat_personal'));
      await DocumentRepository(db).upsert(doc('d3', title: '100% discount_code', cat: 'cat_finance'));
      await TaskRepository(db).upsert(task('t1', title: 'จ่ายค่าประกันรถ', cat: 'cat_insurance'));
      await EventRepository(db).upsert(EventItem(
          id: 'e1', title: 'นัดหมอฟัน', location: 'คลินิก', startAt: DateTime(2026, 12, 1), createdAt: DateTime(2026), updatedAt: DateTime(2026)));
    });

    test('matches title, notes, category name and location', () async {
      final docs = DocumentRepository(db);
      expect((await docs.search('ขับขี่')).map((d) => d.id), ['d1']);
      expect((await docs.search('ขนส่ง')).map((d) => d.id), ['d1']); // notes
      expect((await docs.search('ยานพาหนะ')).map((d) => d.id), ['d1']); // category name
      expect((await docs.search('passport')).map((d) => d.id), ['d2']); // case-insensitive ASCII
      expect((await TaskRepository(db).search('ประกัน')).map((t) => t.id), ['t1']);
      expect((await EventRepository(db).search('คลินิก')).map((e) => e.id), ['e1']);
    });

    test('LIKE wildcards in user input are matched literally', () async {
      final docs = DocumentRepository(db);
      expect((await docs.search('%')).map((d) => d.id), ['d3']);
      expect((await docs.search('_code')).map((d) => d.id), ['d3']);
      expect(await docs.search('   '), isEmpty);
      expect(await docs.search('ไม่มีอยู่จริง'), isEmpty);
    });
  });

  test('settings key/value', () async {
    final s = SettingsRepository(db);
    await s.set('a', '1');
    expect(await s.get('a'), '1');
    await s.set('a', '2');
    expect((await s.all())['a'], '2');
    await s.set('a', null);
    expect(await s.get('a'), isNull);
  });

  test('opening a database from a newer schema version is refused without changes', () async {
    initFfi();
    final dir = await databaseFactoryFfiNoIsolate.getDatabasesPath();
    final path = '$dir/lifekeep_future_test.db';
    await databaseFactoryFfiNoIsolate.deleteDatabase(path);
    final future = await databaseFactoryFfiNoIsolate.openDatabase(path,
        options: OpenDatabaseOptions(version: AppDatabase.schemaVersion + 5, onCreate: (d, v) => d.execute('CREATE TABLE x(a)')));
    await future.close();
    await expectLater(AppDatabase.open(factory: databaseFactoryFfiNoIsolate, path: path), throwsA(anything));
    final check = await databaseFactoryFfiNoIsolate.openDatabase(path);
    expect(await check.getVersion(), AppDatabase.schemaVersion + 5);
    await check.close();
    await databaseFactoryFfiNoIsolate.deleteDatabase(path);
  });
}
