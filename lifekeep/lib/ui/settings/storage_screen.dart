import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/formatters.dart';
import '../../data/database/app_database.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';

class _Usage {
  _Usage(
    this.docs,
    this.tasks,
    this.events,
    this.dbBytes,
    this.attCount,
    this.attBytes,
    this.backupCount,
    this.backupBytes,
  );
  final int docs, tasks, events, dbBytes, attCount, attBytes, backupCount, backupBytes;
  int get total => dbBytes + attBytes + backupBytes;
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
    final t = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('พื้นที่จัดเก็บ', 'Storage'))),
      body: ContentWidth(
        child: DataBuilder<_Usage>(
          load: _load,
          builder: (context, u) {
            final total = u.total == 0 ? 1 : u.total;
            final parts = [
              (context.tr('ฐานข้อมูล', 'Database'), u.dbBytes, scheme.primary, Icons.dataset_outlined, null),
              (
                context.tr('รูปภาพแนบ', 'Images'),
                u.attBytes,
                scheme.tertiary,
                Icons.photo_library_outlined,
                u.attCount,
              ),
              (
                context.tr('ไฟล์สำรองในเครื่อง', 'Local backups'),
                u.backupBytes,
                context.status.warning,
                Icons.backup_outlined,
                u.backupCount,
              ),
            ];
            return ListView(
              padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
              children: [
                LkPanel(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(context.tr('พื้นที่ที่ใช้', 'Space used'), style: t.bodySmall),
                      Text(Fmt.fileSize(u.total), style: t.headlineMedium),
                      const SizedBox(height: Space.md),
                      // Proportional usage bar
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: SizedBox(
                          height: 10,
                          child: Row(
                            children: [
                              for (final part in parts)
                                if (part.$2 > 0)
                                  Expanded(
                                    flex: (part.$2 * 1000 ~/ total).clamp(1, 1000),
                                    child: Container(color: part.$3),
                                  ),
                              if (u.total == 0) Expanded(child: Container(color: scheme.surfaceContainerHigh)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: Space.md),
                LkGroup(
                  dividerIndent: 56,
                  children: [
                    for (final part in parts)
                      LkRow(
                        leading: Icon(part.$4, color: part.$3),
                        title: part.$1,
                        subtitle: part.$5 == null ? null : Text(context.tr('${part.$5} ไฟล์', '${part.$5} files')),
                        trailing: Text(Fmt.fileSize(part.$2), style: t.titleSmall),
                      ),
                  ],
                ),
                LkSectionHeader(title: context.tr('จำนวนรายการ', 'Items')),
                LkGroup(
                  dividerIndent: 56,
                  children: [
                    LkRow(
                      leading: Icon(Icons.folder_outlined, color: scheme.onSurfaceVariant),
                      title: context.tr('เอกสาร', 'Documents'),
                      trailing: Text('${u.docs}', style: t.titleSmall),
                    ),
                    LkRow(
                      leading: Icon(Icons.check_circle_outline_rounded, color: scheme.onSurfaceVariant),
                      title: context.tr('งาน', 'Tasks'),
                      trailing: Text('${u.tasks}', style: t.titleSmall),
                    ),
                    LkRow(
                      leading: Icon(Icons.event_outlined, color: scheme.onSurfaceVariant),
                      title: context.tr('กิจกรรม', 'Events'),
                      trailing: Text('${u.events}', style: t.titleSmall),
                    ),
                  ],
                ),
                const SizedBox(height: Space.md),
                LkBanner(
                  icon: Icons.info_outline_rounded,
                  color: scheme.primary,
                  background: scheme.primaryContainer.withValues(alpha: 0.6),
                  message: context.tr(
                    'ข้อมูลทั้งหมดอยู่ในพื้นที่ส่วนตัวของแอปบนเครื่องนี้ หากถอนการติดตั้งหรือล้างข้อมูลแอป ข้อมูลรวมถึงไฟล์สำรองในเครื่องจะหายไป',
                    'All data lives in this app\'s private storage. Uninstalling or clearing app data removes it, including local backups.',
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
