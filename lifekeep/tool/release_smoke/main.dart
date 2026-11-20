// Release-mode functional check (CI only; NOT part of the shipped app).
//
// Built with the same release/R8 configuration as the production APK:
//   flutter build apk --release -t tool/release_smoke/main.dart
// Phase 1 writes data, schedules a real notification and runs backup /
// restore / PIN checks. The CI script then force-stops the process and
// launches again: phase 2 verifies everything survived the restart.
// Results go to logcat as "LKSMOKE ..." lines.
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:lifekeep/core/app_info.dart';
import 'package:lifekeep/data/database/app_database.dart';
import 'package:lifekeep/data/models/document_item.dart';
import 'package:lifekeep/data/models/event_item.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/services/attachment_service.dart';
import 'package:lifekeep/services/backup_service.dart';
import 'package:lifekeep/services/notification_gateway.dart';
import 'package:lifekeep/services/security_service.dart';
import 'package:lifekeep/state/app_controller.dart';

void log(String s) {
  // ignore: avoid_print
  print('LKSMOKE $s');
}

void check(bool ok, String what) {
  if (!ok) throw StateError(what);
  log('PASS $what');
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MaterialApp(home: Scaffold(body: Center(child: Text('LIFEKEEP release check')))));
  try {
    final support = await getApplicationSupportDirectory();
    final db = await AppDatabase.open();
    final s = AppServices(
      db: db,
      gateway: LocalNotificationGateway(),
      security: SecurityService(FlutterSecureKV()),
      attachments: AttachmentService(support),
      backupDir: Directory(p.join(support.path, 'backups')),
      appVersion: AppInfo.version,
    );
    final c = AppController(s);
    await c.load();
    await s.gateway.init();
    final marker = File(p.join(support.path, 'smoke_phase1_done'));
    final now = DateTime.now();

    if (!await marker.exists()) {
      log('PHASE 1');
      // Attachment import (real file I/O)
      final src = File(p.join((await getTemporaryDirectory()).path, 'smoke.png'));
      await src.writeAsBytes(List<int>.generate(2048, (i) => i % 256));
      final att = await s.attachments.importFile(src.path);
      check(await s.attachments.exists(att), 'attachment imported');

      final doc = DocumentItem(
        id: 'smoke-doc',
        title: 'Smoke document',
        categoryId: 'cat_personal',
        createdAt: now,
        updatedAt: now,
        expiryDate: DateTime(now.year + 1, 1, 15),
        reminder: const ReminderConfig([1440, 10080]),
        attachmentPaths: [att],
      );
      check(await c.saveDocument(doc), 'document saved + reminders scheduled (Gson/R8 path)');
      check(await c.saveTask(TaskItem(
        id: 'smoke-task',
        title: 'Smoke task',
        dueAt: DateTime(now.year + 1, 2, 1, 10),
        hasTime: true,
        reminder: const ReminderConfig([0, 60]),
        createdAt: now,
        updatedAt: now,
      )), 'task saved + reminders scheduled');
      check(await c.saveEvent(EventItem(
        id: 'smoke-event',
        title: 'Smoke event',
        startAt: DateTime(now.year + 1, 3, 1, 9),
        reminder: const ReminderConfig([60]),
        createdAt: now,
        updatedAt: now,
      )), 'event saved + reminder scheduled');
      check((await s.documents.search('Smoke')).length == 1, 'search');

      // A notification that must actually fire ~70 s from now.
      await s.gateway.schedule(
        id: 999,
        title: 'LIFEKEEP smoke reminder',
        body: 'Scheduled notification delivered',
        at: now.add(const Duration(seconds: 70)),
      );
      final pending = await s.gateway.pendingIds();
      check(pending.contains(999) && pending.length >= 6, 'pending notifications (${pending.length})');
      final perm = await s.gateway.permissionState();
      log('INFO notificationsAllowed=${perm.notificationsAllowed} exact=${perm.exactAlarmsAllowed} tz=${s.gateway.timezoneName}');

      // Backup → inspect → merge restore (no duplicates) on real storage
      final b = await s.backup.createBackup(includeAttachments: true);
      final ins = await s.backup.inspect(b.path, verifyAttachments: true);
      check(ins.canRestore && ins.warnings.isEmpty && ins.data!.attachments.length == 1, 'backup verified with checksum');
      final r = await c.restore(ins, RestoreMode.merge);
      check(r.skippedDuplicates >= 3 && (await s.documents.all()).length == 1, 'merge restore skips duplicates');

      // PIN in Android Keystore-backed secure storage
      await s.security.setPin('4826');
      check((await s.security.verifyPin('4826')).check == PinCheck.ok, 'PIN verify (secure storage)');
      check((await s.security.verifyPin('0000')).check == PinCheck.wrong, 'wrong PIN rejected');
      await s.security.disableLock();

      await marker.writeAsString('ok');
      log('DONE PHASE 1');
    } else {
      log('PHASE 2');
      final doc = await s.documents.byId('smoke-doc');
      check(doc != null && doc.attachmentPaths.length == 1, 'document persisted after restart');
      check(await s.attachments.exists(doc!.attachmentPaths.first), 'attachment file persisted');
      check(await s.tasks.byId('smoke-task') != null, 'task persisted');
      check(await s.events.byId('smoke-event') != null, 'event persisted');
      check(await s.scheduler.pendingCount() >= 5, 'reminder bookkeeping persisted');
      check((await s.gateway.pendingIds()).length >= 5, 'OS alarms still pending after restart');
      check((await s.backup.listBackups()).isNotEmpty, 'backup file persisted');
      // Cleanup
      await c.deleteDocument(doc);
      check(!await s.attachments.exists(doc.attachmentPaths.first), 'attachment deleted with document');
      check(await s.scheduler.pendingCount() >= 3, 'other reminders untouched');
      log('DONE PHASE 2');
    }
  } catch (e, st) {
    log('FAIL $e');
    log('STACK ${st.toString().split('\n').take(6).join(' | ')}');
  }
}
