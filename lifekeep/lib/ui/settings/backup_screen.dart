import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/errors.dart';
import '../../core/utils/formatters.dart';
import '../../data/repositories/settings_repository.dart';
import '../../services/backup_service.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../lock/lock_gate.dart';
import 'settings_screen.dart';

enum _Health { ok, due, never, failed }

class BackupScreen extends StatefulWidget {
  const BackupScreen({super.key});

  @override
  State<BackupScreen> createState() => _BackupScreenState();
}

class _BackupScreenState extends State<BackupScreen> {
  bool _includeAttachments = true;

  /// Blocking progress dialog while [task] runs.
  Future<T?> _withProgress<T>(String message, Future<T> Function() task) async {
    final nav = Navigator.of(context, rootNavigator: true);
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => PopScope(
        canPop: false,
        child: AlertDialog(
          content: Row(
            children: [
              const SizedBox(width: 26, height: 26, child: CircularProgressIndicator(strokeWidth: 2.6)),
              const SizedBox(width: Space.xl),
              Expanded(child: Text(message, style: Theme.of(ctx).textTheme.bodyLarge)),
            ],
          ),
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
      info = await _withProgress(
        context.tr('กำลังสำรองและตรวจสอบข้อมูล…', 'Backing up and verifying…'),
        () => c.backupNow(includeAttachments: _includeAttachments),
      );
    });
    if (!ok || info == null || !mounted) return;
    if (thenExport) {
      await _export(info!);
    } else {
      showSnack(
        context,
        context.tr('สำรองข้อมูลแล้ว (${Fmt.fileSize(info!.size)})', 'Backup created (${Fmt.fileSize(info!.size)})'),
      );
    }
  }

  /// Saves a backup to a location the user chooses (USB, SD card, Drive app…).
  Future<void> _export(BackupFileInfo b) async {
    try {
      final f = File(b.path);
      if (!await f.exists()) throw fileError('missing');
      // The Android save dialog needs the bytes; only this one file is loaded.
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
      if (mounted) {
        showSnack(context, context.tr('ไม่สามารถอ่านไฟล์ที่เลือกได้', 'Could not read the selected file'), error: true);
      }
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
      final mode = await showDialog<RestoreMode>(
        context: context,
        builder: (_) => _RestorePreviewDialog(inspection: inspection),
      );
      if (mode == null || !mounted) return;
      final confirmed = await confirmDialog(
        context,
        title: context.tr('ยืนยันการกู้คืน', 'Confirm restore'),
        message: mode == RestoreMode.replace
            ? context.tr(
                'ข้อมูลปัจจุบันทั้งหมดจะถูกแทนที่ด้วยข้อมูลในไฟล์สำรอง ระบบจะสำรองข้อมูลปัจจุบันไว้ให้อัตโนมัติก่อนดำเนินการ',
                'All current data will be replaced by the backup. Your current data is backed up automatically first.',
              )
            : context.tr(
                'ข้อมูลในไฟล์จะถูกเพิ่มเข้ามา รายการที่มีอยู่แล้วจะถูกข้าม (ไม่ซ้ำ) ระบบจะสำรองข้อมูลปัจจุบันไว้ก่อน',
                'Backup items will be added; existing items are skipped (no duplicates). Current data is backed up first.',
              ),
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
          icon: Icon(Icons.check_circle_rounded, color: ctx.status.success, size: 40),
          title: Text(ctx.tr('กู้คืนข้อมูลสำเร็จ', 'Restore complete')),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _ResultLine(Icons.folder_outlined, ctx.tr('เอกสาร', 'Documents'), r.counts.documents),
              _ResultLine(Icons.check_circle_outline_rounded, ctx.tr('งาน', 'Tasks'), r.counts.tasks),
              _ResultLine(Icons.event_outlined, ctx.tr('กิจกรรม', 'Events'), r.counts.events),
              _ResultLine(Icons.photo_library_outlined, ctx.tr('รูปภาพ', 'Images'), r.counts.attachments),
              if (r.skippedDuplicates > 0)
                _ResultLine(
                  Icons.content_copy_rounded,
                  ctx.tr('ข้ามรายการซ้ำ', 'Duplicates skipped'),
                  r.skippedDuplicates,
                ),
              if (r.safetyBackupPath != null) ...[
                const SizedBox(height: Space.md),
                Text(
                  ctx.tr(
                    'ข้อมูลก่อนกู้คืนถูกเก็บไว้ในประวัติการสำรอง',
                    'Your previous data was kept in backup history.',
                  ),
                  textAlign: TextAlign.center,
                  style: Theme.of(ctx).textTheme.bodySmall,
                ),
              ],
            ],
          ),
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

  _Health _health(Map<String, String> s, DateTime? last) {
    final err = s[SettingKeys.lastAutoBackupError];
    final errAt = int.tryParse(err?.split('|').first ?? '');
    if (err != null && (last == null || (errAt != null && errAt > last.millisecondsSinceEpoch))) return _Health.failed;
    if (last == null) return _Health.never;
    if (Dates.daysBetween(last, DateTime.now()) > 7) return _Health.due;
    return _Health.ok;
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final st = context.status;
    final s = c.settings;
    final autoOn = s[SettingKeys.autoBackupEnabled] == 'true';
    final last = lastBackupAt(s);
    final health = _health(s, last);
    final err = s[SettingKeys.lastAutoBackupError];

    final (Color hc, Color hbg, IconData hi, String htitle, String hsub) = switch (health) {
      _Health.ok => (
        st.success,
        st.successContainer,
        Icons.verified_user_outlined,
        context.tr('ข้อมูลได้รับการสำรองแล้ว', 'Your data is backed up'),
        context.tr('ล่าสุด ${Fmt.dateTime(last!)}', 'Last ${Fmt.dateTime(last)}'),
      ),
      _Health.due => (
        st.warning,
        st.warningContainer,
        Icons.schedule_rounded,
        context.tr('ควรสำรองข้อมูลอีกครั้ง', 'Time for a new backup'),
        context.tr('ล่าสุด ${Fmt.dateTime(last!)}', 'Last ${Fmt.dateTime(last)}'),
      ),
      _Health.never => (
        st.warning,
        st.warningContainer,
        Icons.cloud_off_rounded,
        context.tr('ยังไม่เคยสำรองข้อมูล', 'Not backed up yet'),
        context.tr('สำรองไว้เพื่อป้องกันข้อมูลสูญหาย', 'Back up to protect against data loss'),
      ),
      _Health.failed => (
        st.danger,
        st.dangerContainer,
        Icons.error_outline_rounded,
        context.tr('สำรองอัตโนมัติล้มเหลว', 'Automatic backup failed'),
        err!.split('|').skip(1).join('|'),
      ),
    };

    Future<void> setS(String k, String v) => runAction(context, () => c.setSetting(k, v));

    return Scaffold(
      appBar: AppBar(title: Text(context.tr('สำรองและกู้คืน', 'Backup & restore'))),
      body: ContentWidth(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
          children: [
            // Status hero
            FadeSlideIn(
              child: LkPanel(
                padding: const EdgeInsets.all(Space.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(color: hbg, borderRadius: BorderRadius.circular(16)),
                          child: Icon(hi, color: hc, size: 26),
                        ),
                        const SizedBox(width: Space.md),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(htitle, style: t.titleMedium?.copyWith(color: hc)),
                              Text(hsub, maxLines: 2, overflow: TextOverflow.ellipsis, style: t.bodySmall),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: Space.lg),
                    FilledButton.icon(
                      onPressed: () => _backupNow(thenExport: true),
                      icon: const Icon(Icons.save_alt_rounded, size: 20),
                      label: Text(context.tr('สำรองและบันทึกไปยัง…', 'Back up & save to…')),
                    ),
                    const SizedBox(height: Space.sm),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _backupNow(thenExport: false),
                            icon: const Icon(Icons.backup_outlined, size: 19),
                            label: FittedBox(child: Text(context.tr('สำรองในเครื่อง', 'Back up here'))),
                          ),
                        ),
                        const SizedBox(width: Space.sm),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: _importFromFile,
                            icon: const Icon(Icons.restore_rounded, size: 19),
                            label: FittedBox(child: Text(context.tr('กู้คืนจากไฟล์', 'Restore file'))),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: Space.md),
            LkGroup(
              children: [
                LkSwitchRow(
                  leading: Icon(Icons.photo_library_outlined, color: scheme.onSurfaceVariant),
                  title: context.tr('รวมรูปภาพแนบในการสำรอง', 'Include images in backups'),
                  subtitle: context.tr('ไฟล์ใหญ่ขึ้น แต่กู้คืนได้ครบถ้วน', 'Larger file, complete restore'),
                  value: _includeAttachments,
                  onChanged: (v) => setState(() => _includeAttachments = v),
                ),
              ],
            ),
            const SizedBox(height: Space.md),
            LkBanner(
              icon: Icons.usb_rounded,
              color: scheme.primary,
              background: scheme.primaryContainer.withValues(alpha: 0.6),
              message: context.tr(
                'ไฟล์สำรองในเครื่องจะหายไปหากลบแอป ควรบันทึกไปยัง USB, SD Card หรือคอมพิวเตอร์เป็นประจำ',
                'On-device backups are removed with the app. Regularly save a copy to USB, SD card or a computer.',
              ),
            ),
            LkSectionHeader(title: context.tr('สำรองอัตโนมัติ', 'Automatic backup')),
            LkGroup(
              dividerIndent: Space.lg,
              children: [
                LkSwitchRow(
                  title: context.tr('สำรองอัตโนมัติในเครื่อง', 'Automatic local backup'),
                  subtitle: autoOn
                      ? context.tr('ทำงานเมื่อเปิดแอปและถึงกำหนด', 'Runs when the app opens and a backup is due')
                      : context.tr('ปิดอยู่', 'Off'),
                  value: autoOn,
                  onChanged: (v) async {
                    await setS(SettingKeys.autoBackupEnabled, v ? 'true' : 'false');
                    if (v) await c.runAutoBackupIfDue();
                  },
                ),
                if (autoOn) ...[
                  _ChoiceRow<String>(
                    title: context.tr('ความถี่', 'Frequency'),
                    value: s[SettingKeys.autoBackupFrequency] ?? 'daily',
                    options: {'daily': context.tr('ทุกวัน', 'Daily'), 'weekly': context.tr('ทุกสัปดาห์', 'Weekly')},
                    onChanged: (v) => setS(SettingKeys.autoBackupFrequency, v),
                  ),
                  _ChoiceRow<String>(
                    title: context.tr('เก็บไฟล์ล่าสุด', 'Keep latest'),
                    value: s[SettingKeys.autoBackupKeep] ?? '5',
                    options: {
                      for (final n in ['3', '5', '10']) n: context.tr('$n ชุด', '$n backups'),
                    },
                    onChanged: (v) => setS(SettingKeys.autoBackupKeep, v),
                  ),
                  _ChoiceRow<String>(
                    title: context.tr('จำกัดพื้นที่', 'Space limit'),
                    value: s[SettingKeys.autoBackupMaxMb] ?? '500',
                    options: {
                      for (final n in ['100', '500', '1000', '2000']) n: '$n MB',
                    },
                    onChanged: (v) => setS(SettingKeys.autoBackupMaxMb, v),
                  ),
                  LkSwitchRow(
                    title: context.tr('รวมรูปภาพแนบ', 'Include images'),
                    value: s[SettingKeys.autoBackupIncludeAttachments] != 'false',
                    onChanged: (v) => setS(SettingKeys.autoBackupIncludeAttachments, v ? 'true' : 'false'),
                  ),
                ],
              ],
            ),
            if (autoOn)
              Padding(
                padding: const EdgeInsets.fromLTRB(Space.xs, Space.sm, Space.xs, 0),
                child: Text(
                  context.tr(
                    'ไฟล์เก่าจะถูกลบหลังจากไฟล์ใหม่สร้างและตรวจสอบสำเร็จแล้วเท่านั้น',
                    'Old backups are removed only after a new one is created and verified.',
                  ),
                  style: t.labelSmall,
                ),
              ),
            DataBuilder<List<BackupFileInfo>>(
              load: (c) => c.s.backup.listBackups(),
              builder: (context, list) => Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  LkSectionHeader(
                    title: context.tr('ประวัติการสำรองในเครื่อง', 'Backups on this device'),
                    count: list.length,
                  ),
                  if (list.isEmpty)
                    LkPanel(
                      child: Row(
                        children: [
                          Icon(Icons.inventory_2_outlined, color: scheme.onSurfaceVariant),
                          const SizedBox(width: Space.md),
                          Expanded(child: Text(context.tr('ยังไม่มีไฟล์สำรอง', 'No backups yet'), style: t.bodyMedium)),
                        ],
                      ),
                    )
                  else
                    LkGroup(
                      dividerIndent: 64,
                      children: [
                        for (final b in list)
                          LkRow(
                            leading: IconBadge(
                              icon: b.isAuto
                                  ? Icons.autorenew_rounded
                                  : (b.name.startsWith(BackupService.safetyPrefix)
                                        ? Icons.history_rounded
                                        : Icons.archive_outlined),
                              color: b.isAuto ? scheme.tertiary : scheme.primary,
                            ),
                            title: Fmt.dateTime(b.modified),
                            subtitle: Text(
                              '${b.isAuto ? context.tr('อัตโนมัติ', 'Auto') : (b.name.startsWith(BackupService.safetyPrefix) ? context.tr('ก่อนกู้คืน', 'Before restore') : context.tr('ด้วยตนเอง', 'Manual'))} · ${Fmt.fileSize(b.size)}',
                            ),
                            trailing: PopupMenuButton<String>(
                              tooltip: context.tr('ตัวเลือก', 'Options'),
                              onSelected: (a) {
                                switch (a) {
                                  case 'export':
                                    _export(b);
                                  case 'restore':
                                    _inspectAndRestore(b.path);
                                  case 'delete':
                                    _deleteBackup(b);
                                }
                              },
                              itemBuilder: (ctx) => [
                                PopupMenuItem(value: 'export', child: Text(ctx.tr('บันทึกไปยัง…', 'Save to…'))),
                                PopupMenuItem(value: 'restore', child: Text(ctx.tr('กู้คืน', 'Restore'))),
                                PopupMenuItem(
                                  value: 'delete',
                                  child: Text(ctx.tr('ลบ', 'Delete'), style: TextStyle(color: scheme.error)),
                                ),
                              ],
                            ),
                          ),
                      ],
                    ),
                ],
              ),
            ),
            const SizedBox(height: Space.lg),
            Text(
              context.tr(
                'รูปแบบไฟล์: ZIP ที่มี manifest.json (เวอร์ชัน วันที่สร้าง checksum SHA-256) data.json และโฟลเดอร์ attachments เปิดอ่านได้ด้วยโปรแกรมทั่วไปแม้ไม่มีแอป',
                'Format: ZIP with manifest.json (version, date, SHA-256 checksums), data.json and an attachments folder — readable with standard tools.',
              ),
              style: t.labelSmall,
            ),
          ],
        ),
      ),
    );
  }
}

class _ResultLine extends StatelessWidget {
  const _ResultLine(this.icon, this.label, this.value);
  final IconData icon;
  final String label;
  final int value;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Icon(icon, size: 18, color: Theme.of(context).colorScheme.onSurfaceVariant),
          const SizedBox(width: Space.sm),
          Expanded(child: Text(label, style: t.bodyMedium)),
          Text('$value', style: t.titleSmall),
        ],
      ),
    );
  }
}

class _ChoiceRow<T> extends StatelessWidget {
  const _ChoiceRow({required this.title, required this.value, required this.options, required this.onChanged});
  final String title;
  final T value;
  final Map<T, String> options;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Space.lg, Space.xs, Space.sm, Space.xs),
      child: Row(
        children: [
          Expanded(
            child: Text(title, style: Theme.of(context).textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.w500)),
          ),
          DropdownButton<T>(
            value: options.containsKey(value) ? value : options.keys.first,
            underline: const SizedBox(),
            borderRadius: Radii.brMd,
            style: Theme.of(context).textTheme.bodyMedium
                ?.copyWith(color: Theme.of(context).colorScheme.primary, fontWeight: FontWeight.w600),
            items: [for (final e in options.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
            onChanged: (v) {
              if (v != null) onChanged(v);
            },
          ),
        ],
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
    final t = Theme.of(context).textTheme;
    if (!i.canRestore) {
      return AlertDialog(
        icon: Icon(Icons.error_outline_rounded, color: status.danger, size: 40),
        title: Text(context.tr('กู้คืนไฟล์นี้ไม่ได้', 'This file cannot be restored')),
        content: Text(
          '${i.fatal}\n\n${context.tr('ข้อมูลปัจจุบันของคุณไม่ถูกเปลี่ยนแปลง', 'Your current data was not changed.')}',
        ),
        actions: [FilledButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ตกลง', 'OK')))],
      );
    }
    final d = i.data!;
    return AlertDialog(
      title: Text(context.tr('ตรวจสอบไฟล์สำรอง', 'Backup check')),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            LkBanner(
              icon: Icons.verified_rounded,
              color: status.success,
              background: status.successContainer,
              message: context.tr('ไฟล์ถูกต้องและสมบูรณ์ (checksum ตรงกัน)', 'File is valid (checksums match)'),
            ),
            const SizedBox(height: Space.md),
            if (i.createdAt != null)
              Text('${context.tr('สร้างเมื่อ', 'Created')} ${Fmt.dateTime(i.createdAt!)}', style: t.bodyMedium),
            Text(
              '${context.tr('รูปแบบ', 'Format')} v${i.formatVersion} · ${context.tr('แอป', 'App')} ${i.appVersion ?? '-'}',
              style: t.bodySmall,
            ),
            const SizedBox(height: Space.sm),
            _ResultLine(Icons.folder_outlined, context.tr('เอกสาร', 'Documents'), d.documents.length),
            _ResultLine(Icons.check_circle_outline_rounded, context.tr('งาน', 'Tasks'), d.tasks.length),
            _ResultLine(Icons.event_outlined, context.tr('กิจกรรม', 'Events'), d.events.length),
            _ResultLine(Icons.label_outline_rounded, context.tr('หมวดหมู่', 'Categories'), d.categories.length),
            _ResultLine(Icons.photo_library_outlined, context.tr('รูปภาพ', 'Images'), d.attachments.length),
            for (final w in i.warnings)
              Padding(
                padding: const EdgeInsets.only(top: Space.sm),
                child: LkBanner(
                  icon: Icons.warning_amber_rounded,
                  color: status.warning,
                  background: status.warningContainer,
                  message: w,
                ),
              ),
            const SizedBox(height: Space.md),
            RadioGroup<RestoreMode>(
              groupValue: _mode,
              onChanged: (v) => setState(() => _mode = v ?? _mode),
              child: Column(
                children: [
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
                ],
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ยกเลิก', 'Cancel'))),
        FilledButton(onPressed: () => Navigator.pop(context, _mode), child: Text(context.tr('ถัดไป', 'Next'))),
      ],
    );
  }
}
