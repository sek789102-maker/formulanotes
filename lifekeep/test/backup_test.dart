import 'dart:convert';
import 'dart:io';

import 'package:archive/archive_io.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/utils/errors.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/data/repositories/settings_repository.dart';
import 'package:lifekeep/services/backup_service.dart';

import 'helpers.dart';

Future<void> seed(TestEnv env, {String prefix = ''}) async {
  final now = DateTime(2026, 11, 20);
  final img = File('${env.dir.path}/src.jpg')..writeAsBytesSync(List.generate(5000, (i) => i % 256));
  final att = await env.services.attachments.importFile(img.path);
  await env.services.documents.upsert(
    DocumentItem(
      id: '${prefix}doc',
      title: 'บัตรประชาชน',
      categoryId: 'cat_personal',
      notes: 'หมายเหตุ',
      createdAt: now,
      updatedAt: now,
      expiryDate: DateTime(2031, 1, 1),
      reminder: const ReminderConfig([1440]),
      attachmentPaths: [att],
      isPinned: true,
    ),
  );
  await env.services.tasks.upsert(
    TaskItem(id: '${prefix}task', title: 'งาน', createdAt: now, updatedAt: now, dueAt: DateTime(2030)),
  );
  await env.services.events.upsert(
    EventItem(id: '${prefix}event', title: 'กิจกรรม', startAt: DateTime(2030), createdAt: now, updatedAt: now),
  );
  await env.services.settings.set(SettingKeys.themeMode, 'dark');
}

/// Rewrites a backup zip, letting [edit] modify manifest/data JSON.
Future<String> tamper(
  String src,
  String dst, {
  void Function(Map<String, dynamic> manifest, Map<String, dynamic> data)? edit,
  bool keepSha = false,
}) async {
  final input = InputFileStream(src);
  final archive = ZipDecoder().decodeStream(input);
  final manifest = jsonDecode(utf8.decode(archive.findFile('manifest.json')!.readBytes()!)) as Map<String, dynamic>;
  final data = jsonDecode(utf8.decode(archive.findFile('data.json')!.readBytes()!)) as Map<String, dynamic>;
  edit?.call(manifest, data);
  final out = Archive();
  out.add(ArchiveFile.string('manifest.json', jsonEncode(manifest)));
  out.add(ArchiveFile.bytes('data.json', utf8.encode(jsonEncode(data))));
  for (final f in archive.files.where((f) => f.name.startsWith('attachments/'))) {
    out.add(ArchiveFile.bytes(f.name, f.readBytes()!));
  }
  File(dst).writeAsBytesSync(ZipEncoder().encode(out));
  input.closeSync();
  return dst;
}

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

  test('backup contains manifest, checksums, data and attachments; inspection passes', () async {
    await seed(a);
    final info = await a.services.backup.createBackup(includeAttachments: true);
    expect(File(info.path).existsSync(), isTrue);
    expect(info.size, greaterThan(0));
    expect(File('${info.path}.partial').existsSync(), isFalse);

    final ins = await a.services.backup.inspect(info.path, verifyAttachments: true);
    expect(ins.canRestore, isTrue, reason: ins.fatal);
    expect(ins.warnings, isEmpty);
    expect(ins.formatVersion, BackupFormat.version);
    expect(ins.counts.documents, 1);
    expect(ins.counts.attachments, 1);
    expect(ins.data!.documents.single.title, 'บัตรประชาชน');
    expect(ins.data!.settings[SettingKeys.themeMode], 'dark');
  });

  test('replace-restore into another install recreates everything incl. attachments', () async {
    await seed(a);
    final info = await a.services.backup.createBackup(includeAttachments: true);
    await b.services.tasks.upsert(
      TaskItem(id: 'old', title: 'old', createdAt: DateTime(2026), updatedAt: DateTime(2026)),
    );

    final ins = await b.services.backup.inspect(info.path, verifyAttachments: true);
    final r = await b.controller.restore(ins, RestoreMode.replace);
    expect(r.counts.documents, 1);
    expect(r.counts.attachments, 1);
    expect(r.safetyBackupPath, isNotNull, reason: 'current data must be backed up before replacing');

    final d = (await b.services.documents.byId('doc'))!;
    expect(d.isPinned, isTrue);
    expect(await b.services.attachments.exists(d.attachmentPaths.single), isTrue);
    expect(await b.services.tasks.byId('old'), isNull);
    expect(await b.services.tasks.byId('task'), isNotNull);
    expect(await b.services.events.byId('event'), isNotNull);
    expect(b.controller.settings[SettingKeys.themeMode], 'dark');
    // Reminders re-created after restore.
    expect(b.gateway.scheduled, isNotEmpty);
    // Staging folder cleaned up.
    expect(Directory('${b.dir.path}/restore_staging').existsSync(), isFalse);
  });

  test('merge-restore skips duplicates and keeps existing data', () async {
    await seed(a);
    final info = await a.services.backup.createBackup(includeAttachments: false);
    await seed(b, prefix: 'b_');
    await b.services.tasks.upsert(
      TaskItem(id: 'task', title: 'mine', createdAt: DateTime(2026), updatedAt: DateTime(2026)),
    );

    final ins = await b.services.backup.inspect(info.path);
    final r = await b.controller.restore(ins, RestoreMode.merge);
    expect(r.skippedDuplicates, greaterThanOrEqualTo(1));
    expect((await b.services.tasks.byId('task'))!.title, 'mine'); // existing kept
    expect(await b.services.documents.byId('doc'), isNotNull); // new added
    expect(await b.services.documents.byId('b_doc'), isNotNull); // old kept
    // Restoring the same file again adds nothing new.
    final before = (await b.services.documents.all()).length;
    await b.controller.restore(await b.services.backup.inspect(info.path), RestoreMode.merge);
    expect((await b.services.documents.all()).length, before);
  });

  test('corrupted data (checksum mismatch) is rejected', () async {
    await seed(a);
    final info = await a.services.backup.createBackup(includeAttachments: false);
    final bad = await tamper(info.path, '${a.dir.path}/bad.zip', edit: (m, d) => (d['tasks'] as List).clear());
    final ins = await a.services.backup.inspect(bad);
    expect(ins.canRestore, isFalse);
    expect(ins.fatal, contains('checksum'));
    await expectLater(b.services.backup.restore(ins, RestoreMode.replace), throwsA(isA<AppException>()));
  });

  test('backup from a newer format version is rejected', () async {
    await seed(a);
    final info = await a.services.backup.createBackup(includeAttachments: false);
    final newer = await tamper(
      info.path,
      '${a.dir.path}/newer.zip',
      edit: (m, d) => m['formatVersion'] = BackupFormat.version + 1,
    );
    final ins = await a.services.backup.inspect(newer);
    expect(ins.canRestore, isFalse);
    expect(ins.formatVersion, BackupFormat.version + 1);
  });

  test('non-zip and foreign zip files are rejected', () async {
    final txt = File('${a.dir.path}/x.zip')..writeAsStringSync('hello');
    expect((await a.services.backup.inspect(txt.path)).canRestore, isFalse);
    final other = Archive()..add(ArchiveFile.string('readme.txt', 'hi'));
    final z = File('${a.dir.path}/other.zip')..writeAsBytesSync(ZipEncoder().encode(other));
    expect((await a.services.backup.inspect(z.path)).canRestore, isFalse);
    expect((await a.services.backup.inspect('${a.dir.path}/missing.zip')).canRestore, isFalse);
  });

  test('invalid records are skipped with a warning (valid ones kept)', () {
    final warnings = <String>[];
    final v = BackupService.validateData({
      'documents': [
        {'id': 'ok', 'title': 'Good', 'category_id': 'cat_other', 'created_at': 1, 'updated_at': 1},
        {'id': '', 'title': 'no id', 'created_at': 1},
        {'id': 'x', 'title': 'bad expiry', 'created_at': 1, 'expiry_date': 'tomorrow'},
        'garbage',
      ],
      'tasks': 'not a list',
      'events': [
        {'id': 'e', 'title': 'Ev', 'start_at': 5, 'created_at': 1, 'updated_at': 1, 'unknown_future_field': true},
      ],
      'settings': {'theme_mode': 'dark', 'pin_hash': 'never restored'},
    }, warnings);
    expect(v.documents.map((d) => d.id), ['ok']);
    expect(v.events.single.id, 'e');
    expect(v.tasks, isEmpty);
    expect(v.settings, {'theme_mode': 'dark'});
    expect(warnings, hasLength(1));
  });

  test('a failure during restore rolls back and leaves current data intact', () async {
    await seed(a);
    final info = await a.services.backup.createBackup(includeAttachments: false);
    await seed(b, prefix: 'b_');
    // Simulate a database failure in the middle of the transaction.
    await b.db.execute('DROP TABLE events');
    await b.db.execute('CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL CHECK(length(title) > 100))');
    final ins = await b.services.backup.inspect(info.path);
    await expectLater(b.services.backup.restore(ins, RestoreMode.replace), throwsA(isA<AppException>()));
    // Everything from before is still there; nothing from the backup leaked in.
    expect(await b.services.documents.byId('b_doc'), isNotNull);
    expect(await b.services.tasks.byId('b_task'), isNotNull);
    expect(await b.services.documents.byId('doc'), isNull);
  });

  test('auto backup runs when due, rotates old ones only after success', () async {
    await seed(a);
    final s = a.services.settings;
    await s.set(SettingKeys.autoBackupEnabled, 'true');
    await s.set(SettingKeys.autoBackupKeep, '2');
    final auto = a.services.autoBackup;
    final t0 = DateTime(2026, 11, 1, 8);
    expect(await auto.runIfDue(now: t0), isTrue);
    expect(await auto.runIfDue(now: t0.add(const Duration(hours: 2))), isNull); // not due yet
    expect(await auto.runIfDue(now: t0.add(const Duration(days: 1))), isTrue);
    expect(await auto.runIfDue(now: t0.add(const Duration(days: 2))), isTrue);
    final autos = (await a.services.backup.listBackups()).where((b) => b.isAuto).toList();
    expect(autos.length, 2);

    // Make the next backup fail: old backups must survive and error is recorded.
    final dir = Directory(a.services.attachments.dir.path);
    await a.db.execute('DROP TABLE tasks');
    expect(await auto.runIfDue(now: t0.add(const Duration(days: 3))), isFalse);
    expect((await a.services.backup.listBackups()).where((b) => b.isAuto).length, 2);
    expect(await s.get(SettingKeys.lastAutoBackupError), isNotNull);
    expect(dir.existsSync(), isTrue);
  });

  test('isDue respects frequency', () {
    final last = DateTime(2026, 11, 1, 12);
    expect(AutoBackupService.isDue(last: null, frequency: 'daily', now: last), isTrue);
    expect(
      AutoBackupService.isDue(last: last, frequency: 'daily', now: last.add(const Duration(hours: 23, minutes: 40))),
      isTrue,
    );
    expect(AutoBackupService.isDue(last: last, frequency: 'daily', now: last.add(const Duration(hours: 20))), isFalse);
    expect(AutoBackupService.isDue(last: last, frequency: 'weekly', now: last.add(const Duration(days: 3))), isFalse);
    expect(AutoBackupService.isDue(last: last, frequency: 'weekly', now: last.add(const Duration(days: 7))), isTrue);
  });

  test('delete all data removes content and attachments but keeps backups by default', () async {
    await seed(a);
    await a.services.backup.createBackup(includeAttachments: true);
    await a.controller.deleteAllData(includeBackups: false);
    expect(await a.services.documents.count(), 0);
    expect((await a.services.tasks.all()), isEmpty);
    expect((await a.services.attachments.usage()).count, 0);
    expect(a.controller.categories.length, 9);
    expect((await a.services.backup.listBackups()), hasLength(1));
    await a.controller.deleteAllData(includeBackups: true);
    expect((await a.services.backup.listBackups()), isEmpty);
  });
}
