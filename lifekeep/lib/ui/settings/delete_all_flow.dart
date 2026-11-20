import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import 'backup_screen.dart';

/// Two explicit confirmation steps before anything is deleted:
///   1. warning dialog (with a shortcut to make a backup first),
///   2. the user must type the confirmation word.
Future<void> startDeleteAllFlow(BuildContext context) async {
  final c = context.read<AppController>();
  final step1 = await showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      icon: Icon(Icons.warning_amber_rounded, color: ctx.status.danger, size: 32),
      title: Text(ctx.tr('ลบข้อมูลทั้งหมด?', 'Delete all data?')),
      content: Text(
        ctx.tr(
          'เอกสาร งาน กิจกรรม หมวดหมู่ที่สร้างเอง และรูปภาพแนบทั้งหมดจะถูกลบถาวรและไม่สามารถกู้คืนได้ '
              'เว้นแต่คุณมีไฟล์สำรองข้อมูล\n\nแนะนำให้สำรองข้อมูลก่อน',
          'All documents, tasks, events, custom categories and attachments will be permanently deleted and cannot be recovered '
              'unless you have a backup.\n\nWe recommend backing up first.',
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: Text(ctx.tr('ยกเลิก', 'Cancel'))),
        TextButton(
          onPressed: () => Navigator.pop(ctx, 'backup'),
          child: Text(ctx.tr('สำรองข้อมูลก่อน', 'Back up first')),
        ),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: ctx.status.danger),
          onPressed: () => Navigator.pop(ctx, 'next'),
          child: Text(ctx.tr('ดำเนินการต่อ', 'Continue')),
        ),
      ],
    ),
  );
  if (!context.mounted) return;
  if (step1 == 'backup') {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => const BackupScreen()));
    return;
  }
  if (step1 != 'next') return;

  final result = await showDialog<(bool, bool)>(context: context, builder: (_) => const _TypeToConfirmDialog());
  if (result == null || !result.$1 || !context.mounted) return;
  await runAction(
    context,
    () => c.deleteAllData(includeBackups: result.$2),
    success: context.tr('ลบข้อมูลทั้งหมดแล้ว', 'All data deleted'),
  );
}

class _TypeToConfirmDialog extends StatefulWidget {
  const _TypeToConfirmDialog();

  @override
  State<_TypeToConfirmDialog> createState() => _TypeToConfirmDialogState();
}

class _TypeToConfirmDialogState extends State<_TypeToConfirmDialog> {
  final _ctrl = TextEditingController();
  bool _includeBackups = false;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final word = context.tr('ลบทั้งหมด', 'DELETE');
    final ok = _ctrl.text.trim() == word;
    return AlertDialog(
      title: Text(context.tr('ยืนยันขั้นสุดท้าย', 'Final confirmation')),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(context.tr('พิมพ์คำว่า “$word” เพื่อยืนยัน', 'Type “$word” to confirm')),
          const SizedBox(height: 12),
          TextField(
            controller: _ctrl,
            autofocus: true,
            onChanged: (_) => setState(() {}),
            decoration: InputDecoration(hintText: word),
          ),
          const SizedBox(height: 8),
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: _includeBackups,
            onChanged: (v) => setState(() => _includeBackups = v ?? false),
            title: Text(context.tr('ลบไฟล์สำรองในเครื่องด้วย', 'Also delete local backups')),
            controlAffinity: ListTileControlAffinity.leading,
          ),
          Text(
            context.tr('การตั้งค่าและ PIN จะยังคงอยู่', 'Settings and PIN are kept.'),
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ยกเลิก', 'Cancel'))),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: context.status.danger),
          onPressed: ok ? () => Navigator.pop(context, (true, _includeBackups)) : null,
          child: Text(context.tr('ลบถาวร', 'Delete permanently')),
        ),
      ],
    );
  }
}
