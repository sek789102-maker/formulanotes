import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/document_item.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';
import '../widgets/item_tiles.dart';
import 'document_form_screen.dart';
import 'image_viewer_screen.dart';

class _DetailData {
  _DetailData(this.doc, this.missing);
  final DocumentItem? doc;
  final Set<String> missing;
}

class DocumentDetailScreen extends StatelessWidget {
  const DocumentDetailScreen({super.key, required this.documentId});
  final String documentId;

  Future<_DetailData> _load(AppController c) async {
    final d = await c.s.documents.byId(documentId);
    final missing = <String>{};
    if (d != null) {
      for (final n in d.attachmentPaths) {
        if (!await c.s.attachments.exists(n)) missing.add(n);
      }
    }
    return _DetailData(d, missing);
  }

  Future<void> _delete(BuildContext context, DocumentItem d) async {
    final ok = await confirmDialog(
      context,
      title: context.tr('ลบเอกสารนี้?', 'Delete this document?'),
      message: context.tr(
        '“${d.title}” และรูปภาพแนบ ${d.attachmentPaths.length} รูปจะถูกลบถาวร การแจ้งเตือนที่เกี่ยวข้องจะถูกยกเลิก',
        '“${d.title}” and its ${d.attachmentPaths.length} attached image(s) will be permanently deleted. Related reminders will be cancelled.',
      ),
      confirmLabel: context.tr('ลบ', 'Delete'),
      destructive: true,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok || !context.mounted) return;
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    final done = await runAction(context, () => c.deleteDocument(d), success: context.tr('ลบเอกสารแล้ว', 'Document deleted'));
    if (done) nav.pop();
  }

  @override
  Widget build(BuildContext context) {
    return DataBuilder<_DetailData>(
      load: _load,
      builder: (context, data) {
        final d = data.doc;
        if (d == null) {
          return Scaffold(
            appBar: AppBar(),
            body: EmptyState(
              icon: Icons.search_off_rounded,
              title: context.tr('ไม่พบเอกสาร', 'Document not found'),
              message: context.tr('เอกสารนี้อาจถูกลบไปแล้ว', 'It may have been deleted'),
            ),
          );
        }
        final c = context.watch<AppController>();
        final cat = c.categoryById(d.categoryId);
        final scheme = Theme.of(context).colorScheme;
        final text = Theme.of(context).textTheme;
        return Scaffold(
          appBar: AppBar(
            actions: [
              IconButton(
                tooltip: d.isPinned ? context.tr('เลิกปักหมุด', 'Unpin') : context.tr('ปักหมุด', 'Pin'),
                icon: Icon(d.isPinned ? Icons.push_pin_rounded : Icons.push_pin_outlined),
                onPressed: () => runAction(context, () => c.setPinned(d, !d.isPinned),
                    success: d.isPinned ? context.tr('เลิกปักหมุดแล้ว', 'Unpinned') : context.tr('ปักหมุดแล้ว', 'Pinned')),
              ),
              IconButton(
                tooltip: context.tr('แก้ไข', 'Edit'),
                icon: const Icon(Icons.edit_outlined),
                onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => DocumentFormScreen(document: d))),
              ),
              IconButton(
                tooltip: context.tr('ลบ', 'Delete'),
                icon: const Icon(Icons.delete_outline_rounded),
                onPressed: () => _delete(context, d),
              ),
              const SizedBox(width: 4),
            ],
          ),
          body: ListView(padding: const EdgeInsets.fromLTRB(20, 0, 20, 32), children: [
            Row(children: [
              IconBadge(icon: cat.icon, color: cat.color, size: 44),
              const SizedBox(width: 12),
              Expanded(child: Text(cat.name, style: text.labelLarge?.copyWith(color: cat.color))),
            ]),
            const SizedBox(height: 14),
            Text(d.title, style: text.headlineSmall),
            if (d.description.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(d.description, style: text.bodyLarge?.copyWith(color: scheme.onSurfaceVariant)),
            ],
            const SizedBox(height: 20),
            _ExpiryCard(doc: d),
            if (data.missing.isNotEmpty) ...[
              const SizedBox(height: 12),
              AppCard(
                color: context.status.warningContainer,
                child: Row(children: [
                  Icon(Icons.warning_amber_rounded, color: context.status.warning),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(context.tr(
                      'ไม่พบไฟล์แนบ ${data.missing.length} ไฟล์ ไฟล์อาจถูกย้ายหรือลบ',
                      '${data.missing.length} attachment(s) not found. They may have been moved or deleted.',
                    )),
                  ),
                  TextButton(
                    onPressed: () => runAction(context, () => c.removeMissingAttachments(d),
                        success: context.tr('นำรายการที่หายไปออกแล้ว', 'Removed missing items')),
                    child: Text(context.tr('นำออก', 'Remove')),
                  ),
                ]),
              ),
            ],
            if (d.attachmentPaths.isNotEmpty) ...[
              SectionHeader(title: context.tr('รูปภาพแนบ (${d.attachmentPaths.length})', 'Attachments (${d.attachmentPaths.length})')),
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                  maxCrossAxisExtent: 140,
                  mainAxisSpacing: 8,
                  crossAxisSpacing: 8,
                ),
                itemCount: d.attachmentPaths.length,
                itemBuilder: (context, i) => GestureDetector(
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => ImageViewerScreen(names: d.attachmentPaths, initialIndex: i)),
                  ),
                  child: AttachmentThumb(name: d.attachmentPaths[i], size: 140),
                ),
              ),
            ],
            if (d.notes.isNotEmpty) ...[
              SectionHeader(title: context.tr('หมายเหตุ', 'Notes')),
              AppCard(child: SelectableText(d.notes, style: text.bodyMedium)),
            ],
            const SizedBox(height: 24),
            Text(
              '${context.tr('สร้างเมื่อ', 'Created')} ${Fmt.dateTime(d.createdAt)}\n'
              '${context.tr('แก้ไขล่าสุด', 'Updated')} ${Fmt.dateTime(d.updatedAt)}',
              style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ]),
        );
      },
    );
  }
}

class _ExpiryCard extends StatelessWidget {
  const _ExpiryCard({required this.doc});
  final DocumentItem doc;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final exp = doc.expiryDate;
    final now = DateTime.now();
    final st = ExpiryCalc.status(exp, now);
    final days = ExpiryCalc.daysLeft(exp, now);
    final s = context.status;
    final (Color color, Color bg) = switch (st) {
      ExpiryStatus.expired || ExpiryStatus.expiresToday => (s.danger, s.dangerContainer),
      ExpiryStatus.expiringSoon => (s.warning, s.warningContainer),
      ExpiryStatus.valid => (s.success, s.successContainer),
      ExpiryStatus.none => (scheme.onSurfaceVariant, scheme.surfaceContainerHigh),
    };
    final String headline = switch (st) {
      ExpiryStatus.none => context.tr('ไม่มีวันหมดอายุ', 'No expiry date'),
      ExpiryStatus.expired => context.tr('หมดอายุแล้ว ${-days!} วัน', 'Expired ${-days} days ago'),
      ExpiryStatus.expiresToday => context.tr('หมดอายุวันนี้', 'Expires today'),
      _ => context.tr('เหลืออีก $days วัน', '$days days left'),
    };
    return AppCard(
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(14)),
            child: Icon(Icons.event_busy_outlined, color: color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(headline, style: text.titleMedium?.copyWith(color: st == ExpiryStatus.none ? null : color)),
              if (exp != null)
                Text('${context.tr('วันหมดอายุ', 'Expiry date')} ${Fmt.date(exp)}',
                    style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant)),
            ]),
          ),
        ]),
        if (exp != null) ...[
          const Divider(height: 24),
          Row(children: [
            Icon(
              doc.reminder.isEnabled ? Icons.notifications_active_outlined : Icons.notifications_off_outlined,
              size: 18,
              color: scheme.onSurfaceVariant,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                doc.reminder.isEnabled
                    ? doc.reminder.offsetsMinutes.map(Fmt.reminderOffset).join(', ')
                    : context.tr('ไม่ได้ตั้งการแจ้งเตือน', 'No reminders'),
                style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant),
              ),
            ),
          ]),
        ],
      ]),
    );
  }
}
