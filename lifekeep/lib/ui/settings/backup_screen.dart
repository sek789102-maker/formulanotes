import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/errors.dart';
import '../../core/utils/formatters.dart';
import '../../data/repositories/settings_repository.dart';
import '../../services/backup_service.dart';
import '../../state/app_controller.dart';
import '../lock/lock_gate.dart';
import '../widgets/common.dart';

class BackupScreen extends StatefulWidget {
  const BackupScreen({super.key});

  @override
  State<BackupScreen> createState() => _BackupScreenState();
}

class _BackupScreenState extends State<BackupScreen> {
  bool _includeAttachments = true;

  /// Shows a blocking progress dialog while [task] runs.
  Future<T?> _withProgress<T>(String message, Future<T> Function() task) async {
    final nav = Navigator.of(context, rootNavigator: true);
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => PopScope(
        canPop: false,
        child: AlertDialog(
          content: Row(children: [
            const SizedBox(width: 28, height: 28, child: CircularProgressIndicator(strokeWidth: 3)),
            const SizedBox(width: 20),
            Expanded(child: Text(message)),
          ]),
        ),
      ),
    );
    try {
      return await task();
    } finally {
      nav.pop();
    }
  }

  Future<void> _backupNow({required bool thenExport}) async {
    final c = context.read<AppController>();
    BackupFileInfo? info;
    final ok = await runAction(context, () async {
      info = await _withProgress(context.tr('กำลังสำรองข้อมูล…', 'Backing up…'),
          () => c.backupNow(includeAttachments: _includeAttachments));
    });
    if (!ok || info == null || !mounted) return;
    if (thenExport) {
      await _export(info!);
    } else {
      showSnack(context, context.tr('สำรองข้อมูลแล้ว (${Fmt.fileSize(info!.size)})', 'Backup created (${Fmt.fileSize(info!.size)})'));
    }
  }

  /// Saves a backup to a location the user chooses (USB, SD card, Drive app…).
  Future<void> _export(BackupFileInfo b) async {
    try {
      final f = File(b.path);
      if (!await f.exists()) throw fileError('missing');
      // The system save dialog on Android needs the bytes; this is done only
      // for the single file being exported.
      final bytes = await f.readAsBytes();
      final uri = await LockGuard.external(
        () => FilePicker.saveFile(fileName: b.name, bytes: bytes, mimeType: 'application/zip'),
      );
      if (!mounted) return;
      if (uri != null) showSnack(context, context.tr('บันทึกไฟล์สำรองแล้ว', 'Backup saved'));
    } catch (e) {
      if (!mounted) return;
      showSnack(
        context,
        e is AppException ? e.userMessage : context.tr('ส่งออกไฟล์ไม่สำเร็จ', 'Could not export the file'),
        error: true,
      );
    }
  }

  Future<void> _importFromFile() async {
    final c = context.read<AppController>();
    String? localPath;
    try {
      final files = await LockGuard.external(() => FilePicker.pickFiles(type: FileType.any));
      if (files.isEmpty || !mounted) return;
      final f = files.first;
      // Copy into a private temp file with streaming I/O.
      final tmp = await getTemporaryDirectory();
      localPath = p.join(tmp.path, 'lifekeep_import_${DateTime.now().millisecondsSinceEpoch}.zip');
      final out = File(localPath).openWrite();
      await out.addStream(f.xFile.openRead());
      await out.close();
    } catch (e) {
      if (mounted) showSnack(context, context.tr('ไม่สามารถอ่านไฟล์ที่เลือกได้', 'Could not read the selected file'), error: true);
      return;
    }
    if (!mounted) return;
    await _inspectAndRestore(localPath, deleteAfter: true);
    if (!mounted) return;
    c.runAutoBackupIfDue();
  }

  Future<void> _inspectAndRestore(String path, {bool deleteAfter = false}) async {
    final c = context.read<AppController>();
    try {
      final inspection = await _withProgress(
        context.tr('กำลังตรวจสอบไฟล์…', 'Checking file…'),
        () => c.s.backup.inspect(path, verifyAttachments: true),
      );
      if (inspection == null || !mounted) return;
      final mode = await showDialog<RestoreMode>(context: context, builder: (_) => _RestorePreviewDialog(inspection: inspection));
      if (mode == null || !mounted) return;
      final confirmed = await confirmDialog(
        context,
        title: context.tr('ยืนยันการกู้คืน', 'Confirm restore'),
        message: mode == RestoreMode.replace
            ? context.tr(
                'ข้อมูลปัจจุบันทั้งหมดจะถูกแทนที่ด้วยข้อมูลในไฟล์สำรอง ระบบจะสำรองข้อมูลปัจจุบันไว้ให้อัตโนมัติก่อนดำเนินการ',
                'All current data will be replaced by the backup. Your current data is backed up automatically first.')
            : context.tr(
                'ข้อมูลในไฟล์จะถูกเพิ่มเข้ามา รายการที่มีอยู่แล้วจะถูกข้าม (ไม่ซ้ำ) ระบบจะสำรองข้อมูลปัจจุบันไว้ก่อน',
                'Backup items will be added; existing items are skipped (no duplicates). Current data is backed up first.'),
        confirmLabel: context.tr('กู้คืน', 'Restore'),
        destructive: mode == RestoreMode.replace,
        icon: Icons.restore_rounded,
      );
      if (!confirmed || !mounted) return;
      RestoreResult? result;
      final ok = await runAction(context, () async {
        result = await _withProgress(context.tr('กำลังกู้คืนข้อมูล…', 'Restoring…'), () => c.restore(inspection, mode));
      });
      if (!ok || result == null || !mounted) return;
      final r = result!;
      await showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          icon: Icon(Icons.check_circle_rounded, color: ctx.status.success, size: 36),
          title: Text(ctx.tr('กู้คืนข้อมูลสำเร็จ', 'Restore complete')),
          content: Text([
            ctx.tr('เอกสาร ${r.counts.documents} · งาน ${r.counts.tasks} · กิจกรรม ${r.counts.events}',
                'Documents ${r.counts.documents} · Tasks ${r.counts.tasks} · Events ${r.counts.events}'),
            ctx.tr('รูปภาพ ${r.counts.attachments} ไฟล์', 'Images ${r.counts.attachments}'),
            if (r.skippedDuplicates > 0) ctx.tr('ข้ามรายการซ้ำ ${r.skippedDuplicates} รายการ', 'Skipped ${r.skippedDuplicates} duplicates'),
            if (r.safetyBackupPath != null)
              ctx.tr('ข้อมูลก่อนกู้คืนถูกเก็บไว้ในรายการไฟล์สำรองในเครื่อง', 'Your previous data was saved as a local backup.'),
          ].join('\n')),
          actions: [FilledButton(onPressed: () => Navigator.pop(ctx), child: Text(ctx.tr('ตกลง', 'OK')))],
        ),
      );
    } finally {
      if (deleteAfter) {
        try {
          await File(path).delete();
        } catch (_) {}
      }
    }
  }

  Future<void> _deleteBackup(BackupFileInfo b) async {
    final ok = await confirmDialog(
      context,
      title: context.tr('ลบไฟล์สำรองนี้?', 'Delete this backup?'),
      message: b.name,
      confirmLabel: context.tr('ลบ', 'Delete'),
      destructive: true,
    );
    if (!ok || !mounted) return;
    final c = context.read<AppController>();
    await runAction(context, () async {
      await c.s.backup.deleteBackup(b.path);
      c.refresh();
    }, success: context.tr('ลบไฟล์สำรองแล้ว', 'Backup deleted'));
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final s = c.settings;
    final autoOn = s[SettingKeys.autoBackupEnabled] == 'true';
    final lastAuto = int.tryParse(s[SettingKeys.lastAutoBackupAt] ?? '');
    final lastManual = int.tryParse(s[SettingKeys.lastManualBackupAt] ?? '');
    final err = s[SettingKeys.lastAutoBackupError];

    Future<void> setS(String k, String v) => runAction(context, () => c.setSetting(k, v));

    return Scaffold(
      appBar: AppBar(title: Text(context.tr('สำรองและกู้คืนข้อมูล', 'Backup & restore'))),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 40), children: [
        AppCard(
          color: context.status.warningContainer,
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(Icons.shield_outlined, color: context.status.warning),
            const SizedBox(width: 12),
            Expanded(
              child: Text(context.tr(
                'ข้อมูลอยู่ในเครื่องนี้เท่านั้น หากลบแอปหรือเครื่องเสีย ข้อมูลจะหายไป กรุณาส่งออกไฟล์สำรองไปเก็บที่ USB, SD Card หรือคอมพิวเตอร์เป็นประจำ',
                'Data lives only on this device. If the app is removed or the phone breaks, data is lost. '
                    'Regularly export backups to USB, SD card or a computer.',
              )),
            ),
          ]),
        ),
        SectionHeader(title: context.tr('สำรองข้อมูล', 'Back up')),
        Card(
          child: Column(children: [
            SwitchListTile(
              secondary: const Icon(Icons.photo_library_outlined),
              title: Text(context.tr('รวมรูปภาพแนบ', 'Include attachments')),
              subtitle: Text(context.tr('ไฟล์จะใหญ่ขึ้นแต่กู้คืนได้ครบ', 'Larger file, complete restore')),
              value: _includeAttachments,
              onChanged: (v) => setState(() => _includeAttachments = v),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
              child: Column(children: [
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () => _backupNow(thenExport: true),
                    icon: const Icon(Icons.save_alt_rounded),
                    label: Text(context.tr('สำรองและบันทึกไปยัง…', 'Back up & save to…')),
                  ),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () => _backupNow(thenExport: false),
                    icon: const Icon(Icons.backup_outlined),
                    label: Text(context.tr('สำรองไว้ในเครื่องตอนนี้', 'Back up on device now')),
                  ),
                ),
                if (lastManual != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    context.tr('สำรองล่าสุด ${Fmt.dateTime(DateTime.fromMillisecondsSinceEpoch(lastManual))}',
                        'Last backup ${Fmt.dateTime(DateTime.fromMillisecondsSinceEpoch(lastManual))}'),
                    style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                ],
              ]),
            ),
          ]),
        ),
        SectionHeader(title: context.tr('กู้คืนข้อมูล', 'Restore')),
        Card(
          child: ListTile(
            leading: const Icon(Icons.file_open_outlined),
            title: Text(context.tr('กู้คืนจากไฟล์…', 'Restore from file…')),
            subtitle: Text(context.tr('เลือกไฟล์ .zip ที่สำรองจาก LIFEKEEP', 'Choose a LIFEKEEP backup .zip')),
            trailing: const Icon(Icons.chevron_right_rounded),
            onTap: _importFromFile,
          ),
        ),
        SectionHeader(title: context.tr('สำรองอัตโนมัติ', 'Automatic backup')),
        Card(
          child: Column(children: [
            SwitchListTile(
              secondary: const Icon(Icons.autorenew_rounded),
              title: Text(context.tr('สำรองอัตโนมัติในเครื่อง', 'Automatic local backup')),
              subtitle: Text(context.tr('ทำงานเมื่อเปิดแอปและถึงกำหนด', 'Runs when the app opens and a backup is due')),
              value: autoOn,
              onChanged: (v) async {
                await setS(SettingKeys.autoBackupEnabled, v ? 'true' : 'false');
                if (v) await c.runAutoBackupIfDue();
              },
            ),
            if (autoOn) ...[
              const Divider(indent: 56),
              _ChoiceTile<String>(
                icon: Icons.event_repeat_rounded,
                title: context.tr('ความถี่', 'Frequency'),
                value: s[SettingKeys.autoBackupFrequency] ?? 'daily',
                options: {'daily': context.tr('ทุกวัน', 'Daily'), 'weekly': context.tr('ทุกสัปดาห์', 'Weekly')},
                onChanged: (v) => setS(SettingKeys.autoBackupFrequency, v),
              ),
              _ChoiceTile<String>(
                icon: Icons.layers_outlined,
                title: context.tr('เก็บไฟล์ล่าสุด', 'Keep latest'),
                value: s[SettingKeys.autoBackupKeep] ?? '5',
                options: {for (final n in ['3', '5', '10']) n: context.tr('$n ชุด', '$n backups')},
                onChanged: (v) => setS(SettingKeys.autoBackupKeep, v),
              ),
              _ChoiceTile<String>(
                icon: Icons.data_usage_rounded,
                title: context.tr('จำกัดพื้นที่', 'Space limit'),
                value: s[SettingKeys.autoBackupMaxMb] ?? '500',
                options: {for (final n in ['100', '500', '1000', '2000']) n: '$n MB'},
                onChanged: (v) => setS(SettingKeys.autoBackupMaxMb, v),
              ),
              SwitchListTile(
                secondary: const Icon(Icons.photo_library_outlined),
                title: Text(context.tr('รวมรูปภาพแนบ', 'Include attachments')),
                value: s[SettingKeys.autoBackupIncludeAttachments] != 'false',
                onChanged: (v) => setS(SettingKeys.autoBackupIncludeAttachments, v ? 'true' : 'false'),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(
                    lastAuto == null
                        ? context.tr('ยังไม่เคยสำรองอัตโนมัติ', 'No automatic backup yet')
                        : context.tr('สำรองอัตโนมัติล่าสุด ${Fmt.dateTime(DateTime.fromMillisecondsSinceEpoch(lastAuto))}',
                            'Last automatic backup ${Fmt.dateTime(DateTime.fromMillisecondsSinceEpoch(lastAuto))}'),
                    style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                  if (err != null) ...[
                    const SizedBox(height: 6),
                    Text(
                      context.tr('ครั้งล่าสุดล้มเหลว: ', 'Last attempt failed: ') + err.split('|').skip(1).join('|'),
                      style: text.bodySmall?.copyWith(color: context.status.danger),
                    ),
                  ],
                  const SizedBox(height: 4),
                  Text(
                    context.tr('ไฟล์เก่าจะถูกลบหลังจากไฟล์ใหม่ถูกสร้างและตรวจสอบสำเร็จแล้วเท่านั้น',
                        'Old backups are removed only after a new one is created and verified.'),
                    style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                ]),
              ),
            ],
          ]),
        ),
        SectionHeader(title: context.tr('ไฟล์สำรองในเครื่อง', 'Backups on this device')),
        DataBuilder<List<BackupFileInfo>>(
          load: (c) => c.s.backup.listBackups(),
          builder: (context, list) {
            if (list.isEmpty) {
              return EmptyState(
                icon: Icons.inventory_2_outlined,
                title: context.tr('ยังไม่มีไฟล์สำรอง', 'No backups yet'),
                compact: true,
              );
            }
            return Card(
              child: Column(children: [
                for (var i = 0; i < list.length; i++) ...[
                  if (i > 0) const Divider(indent: 56),
                  ListTile(
                    leading: Icon(list[i].isAuto ? Icons.autorenew_rounded : Icons.archive_outlined),
                    title: Text(Fmt.dateTime(list[i].modified)),
                    subtitle: Text(
                      '${list[i].isAuto ? context.tr('อัตโนมัติ', 'Auto') : (list[i].name.startsWith(BackupService.safetyPrefix) ? context.tr('ก่อนกู้คืน', 'Pre-restore') : context.tr('ด้วยตนเอง', 'Manual'))} · ${Fmt.fileSize(list[i].size)}',
                    ),
                    trailing: PopupMenuButton<String>(
                      onSelected: (a) {
                        switch (a) {
                          case 'export':
                            _export(list[i]);
                          case 'restore':
                            _inspectAndRestore(list[i].path);
                          case 'delete':
                            _deleteBackup(list[i]);
                        }
                      },
                      itemBuilder: (ctx) => [
                        PopupMenuItem(value: 'export', child: Text(ctx.tr('บันทึกไปยัง…', 'Save to…'))),
                        PopupMenuItem(value: 'restore', child: Text(ctx.tr('กู้คืน', 'Restore'))),
                        PopupMenuItem(value: 'delete', child: Text(ctx.tr('ลบ', 'Delete'))),
                      ],
                    ),
                  ),
                ],
              ]),
            );
          },
        ),
        const SizedBox(height: 16),
        Text(
          context.tr(
            'รูปแบบไฟล์: ZIP ที่มี manifest.json (เวอร์ชันรูปแบบ วันที่สร้าง และ checksum SHA-256) data.json และโฟลเดอร์ attachments '
                'สามารถเปิดอ่านได้ด้วยโปรแกรมทั่วไปแม้ไม่มีแอป',
            'Format: ZIP with manifest.json (format version, date, SHA-256 checksums), data.json and an attachments folder — '
                'readable with standard tools even without the app.',
          ),
          style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
        ),
      ]),
    );
  }
}

class _ChoiceTile<T> extends StatelessWidget {
  const _ChoiceTile({required this.icon, required this.title, required this.value, required this.options, required this.onChanged});
  final IconData icon;
  final String title;
  final T value;
  final Map<T, String> options;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      trailing: DropdownButton<T>(
        value: options.containsKey(value) ? value : options.keys.first,
        underline: const SizedBox(),
        items: [for (final e in options.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
        onChanged: (v) {
          if (v != null) onChanged(v);
        },
      ),
    );
  }
}

class _RestorePreviewDialog extends StatefulWidget {
  const _RestorePreviewDialog({required this.inspection});
  final BackupInspection inspection;

  @override
  State<_RestorePreviewDialog> createState() => _RestorePreviewDialogState();
}

class _RestorePreviewDialogState extends State<_RestorePreviewDialog> {
  RestoreMode _mode = RestoreMode.replace;

  @override
  Widget build(BuildContext context) {
    final i = widget.inspection;
    final status = context.status;
    if (!i.canRestore) {
      return AlertDialog(
        icon: Icon(Icons.error_outline_rounded, color: status.danger, size: 36),
        title: Text(context.tr('ไม่สามารถกู้คืนไฟล์นี้ได้', 'This file cannot be restored')),
        content: Text('${i.fatal}\n\n${context.tr('ข้อมูลปัจจุบันของคุณไม่ถูกเปลี่ยนแปลง', 'Your current data was not changed.')}'),
        actions: [FilledButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ตกลง', 'OK')))],
      );
    }
    final d = i.data!;
    return AlertDialog(
      title: Text(context.tr('ตรวจสอบไฟล์สำรอง', 'Backup check')),
      content: SingleChildScrollView(
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Icon(Icons.verified_rounded, color: status.success, size: 20),
            const SizedBox(width: 8),
            Expanded(child: Text(context.tr('ไฟล์ถูกต้องและสมบูรณ์ (checksum ตรงกัน)', 'File is valid (checksums match)'))),
          ]),
          const SizedBox(height: 12),
          if (i.createdAt != null) Text('${context.tr('สร้างเมื่อ', 'Created')}: ${Fmt.dateTime(i.createdAt!)}'),
          Text('${context.tr('เวอร์ชันรูปแบบ', 'Format version')}: ${i.formatVersion} · ${context.tr('แอป', 'App')} ${i.appVersion ?? '-'}'),
          const SizedBox(height: 8),
          Text(context.tr(
            'เอกสาร ${d.documents.length} · งาน ${d.tasks.length} · กิจกรรม ${d.events.length} · หมวดหมู่ ${d.categories.length} · รูปภาพ ${d.attachments.length}',
            'Documents ${d.documents.length} · Tasks ${d.tasks.length} · Events ${d.events.length} · Categories ${d.categories.length} · Images ${d.attachments.length}',
          )),
          if (i.warnings.isNotEmpty) ...[
            const SizedBox(height: 12),
            for (final w in i.warnings)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Icon(Icons.warning_amber_rounded, color: status.warning, size: 18),
                  const SizedBox(width: 6),
                  Expanded(child: Text(w)),
                ]),
              ),
          ],
          const SizedBox(height: 12),
          RadioGroup<RestoreMode>(
            groupValue: _mode,
            onChanged: (v) => setState(() => _mode = v ?? _mode),
            child: Column(children: [
              RadioListTile<RestoreMode>(
                contentPadding: EdgeInsets.zero,
                value: RestoreMode.replace,
                title: Text(context.tr('แทนที่ข้อมูลทั้งหมด', 'Replace all data')),
                subtitle: Text(context.tr('ข้อมูลปัจจุบันจะถูกแทนที่', 'Current data will be replaced')),
              ),
              RadioListTile<RestoreMode>(
                contentPadding: EdgeInsets.zero,
                value: RestoreMode.merge,
                title: Text(context.tr('รวมข้อมูล', 'Merge')),
                subtitle: Text(context.tr('เพิ่มเฉพาะรายการที่ยังไม่มี', 'Add only items that don\'t exist yet')),
              ),
            ]),
          ),
        ]),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ยกเลิก', 'Cancel'))),
        FilledButton(onPressed: () => Navigator.pop(context, _mode), child: Text(context.tr('ถัดไป', 'Next'))),
      ],
    );
  }
}
