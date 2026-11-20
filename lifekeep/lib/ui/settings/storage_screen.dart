import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/formatters.dart';
import '../../data/database/app_database.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';

class _Usage {
  _Usage(this.docs, this.tasks, this.events, this.dbBytes, this.attCount, this.attBytes, this.backupCount, this.backupBytes);
  final int docs, tasks, events, dbBytes, attCount, attBytes, backupCount, backupBytes;
}

class StorageScreen extends StatelessWidget {
  const StorageScreen({super.key});

  static Future<_Usage> _load(AppController c) async {
    var dbBytes = 0;
    try {
      final f = File(p.join(await getDatabasesPath(), AppDatabase.fileName));
      if (await f.exists()) dbBytes = await f.length();
    } catch (_) {}
    final att = await c.s.attachments.usage();
    final backups = await c.s.backup.listBackups();
    return _Usage(
      await c.s.documents.count(),
      (await c.s.tasks.all()).length,
      (await c.s.events.all()).length,
      dbBytes,
      att.count,
      att.bytes,
      backups.length,
      backups.fold(0, (s, b) => s + b.size),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('พื้นที่จัดเก็บ', 'Storage'))),
      body: DataBuilder<_Usage>(
        load: _load,
        builder: (context, u) {
          Widget row(IconData icon, String label, String value) => ListTile(
                leading: Icon(icon),
                title: Text(label),
                trailing: Text(value, style: Theme.of(context).textTheme.titleSmall),
              );
          final total = u.dbBytes + u.attBytes + u.backupBytes;
          return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), children: [
            AppCard(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(context.tr('พื้นที่ที่ใช้ทั้งหมด', 'Total used'), style: TextStyle(color: scheme.onSurfaceVariant)),
                const SizedBox(height: 4),
                Text(Fmt.fileSize(total), style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700)),
              ]),
            ),
            const SizedBox(height: 12),
            Card(
              child: Column(children: [
                row(Icons.folder_outlined, context.tr('เอกสาร', 'Documents'), '${u.docs}'),
                row(Icons.check_circle_outline_rounded, context.tr('งาน', 'Tasks'), '${u.tasks}'),
                row(Icons.event_outlined, context.tr('กิจกรรม', 'Events'), '${u.events}'),
                const Divider(indent: 56),
                row(Icons.dataset_outlined, context.tr('ฐานข้อมูล', 'Database'), Fmt.fileSize(u.dbBytes)),
                row(Icons.photo_library_outlined, context.tr('รูปภาพแนบ (${u.attCount})', 'Attachments (${u.attCount})'),
                    Fmt.fileSize(u.attBytes)),
                row(Icons.backup_outlined, context.tr('ไฟล์สำรองในเครื่อง (${u.backupCount})', 'Local backups (${u.backupCount})'),
                    Fmt.fileSize(u.backupBytes)),
              ]),
            ),
            const SizedBox(height: 16),
            Text(
              context.tr(
                'ข้อมูลทั้งหมดถูกเก็บในพื้นที่ส่วนตัวของแอปในเครื่องนี้เท่านั้น หากถอนการติดตั้งแอปหรือล้างข้อมูลแอป ข้อมูลรวมถึงไฟล์สำรองในเครื่องจะหายไป '
                    'กรุณาส่งออกไฟล์สำรองไปเก็บไว้ที่อื่น เช่น USB, SD Card หรือคอมพิวเตอร์',
                'All data is stored only in this app\'s private storage on this device. Uninstalling or clearing app data removes it, '
                    'including local backups. Export backups to another place such as USB, SD card or a computer.',
              ),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ]);
        },
      ),
    );
  }
}
