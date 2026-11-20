import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/document_item.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
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
        '“${d.title}” and its ${d.attachmentPaths.length} image(s) will be permanently deleted. Related reminders are cancelled.',
      ),
      confirmLabel: context.tr('ลบ', 'Delete'),
      destructive: true,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok || !context.mounted) return;
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    final done = await runAction(
      context,
      () => c.deleteDocument(d),
      success: context.tr('ลบเอกสารแล้ว', 'Document deleted'),
    );
    if (done) nav.pop();
  }

  @override
  Widget build(BuildContext context) {
    return DataBuilder<_DetailData>(
      load: _load,
      loading: const Scaffold(body: Center(child: CircularProgressIndicator())),
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
        final t = Theme.of(context).textTheme;
        return Scaffold(
          appBar: AppBar(
            actions: [
              IconButton(
                tooltip: d.isPinned ? context.tr('เลิกปักหมุด', 'Unpin') : context.tr('ปักหมุด', 'Pin'),
                icon: AnimatedSwitcher(
                  duration: Motion.fast,
                  transitionBuilder: (c, a) => ScaleTransition(scale: a, child: c),
                  child: Icon(
                    d.isPinned ? Icons.push_pin_rounded : Icons.push_pin_outlined,
                    key: ValueKey(d.isPinned),
                    color: d.isPinned ? scheme.primary : null,
                  ),
                ),
                onPressed: () => runAction(
                  context,
                  () => c.setPinned(d, !d.isPinned),
                  success: d.isPinned ? context.tr('เลิกปักหมุดแล้ว', 'Unpinned') : context.tr('ปักหมุดแล้ว', 'Pinned'),
                ),
              ),
              IconButton(
                tooltip: context.tr('แก้ไข', 'Edit'),
                icon: const Icon(Icons.edit_outlined),
                onPressed: () =>
                    Navigator.push(context, MaterialPageRoute(builder: (_) => DocumentFormScreen(document: d))),
              ),
              PopupMenuButton<String>(
                tooltip: context.tr('เพิ่มเติม', 'More'),
                onSelected: (v) {
                  if (v == 'delete') _delete(context, d);
                },
                itemBuilder: (ctx) => [
                  PopupMenuItem(
                    value: 'delete',
                    child: Row(
                      children: [
                        Icon(Icons.delete_outline_rounded, color: scheme.error, size: 20),
                        const SizedBox(width: Space.md),
                        Text(ctx.tr('ลบเอกสาร', 'Delete document'), style: TextStyle(color: scheme.error)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(width: Space.xs),
            ],
          ),
          body: ContentWidth(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
              children: [
                if (d.attachmentPaths.isNotEmpty) ...[
                  _Gallery(names: d.attachmentPaths),
                  const SizedBox(height: Space.lg),
                ],
                Row(
                  children: [
                    IconBadge(icon: cat.icon, color: cat.color, size: 28),
                    const SizedBox(width: Space.sm),
                    Flexible(
                      child: Text(cat.name, style: t.labelMedium?.copyWith(color: scheme.onSurfaceVariant)),
                    ),
                    if (d.isPinned) ...[
                      const SizedBox(width: Space.sm),
                      Pill(
                        label: context.tr('ปักหมุด', 'Pinned'),
                        icon: Icons.push_pin_rounded,
                        color: scheme.primary,
                        background: scheme.primaryContainer,
                        dense: true,
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: Space.sm),
                Text(d.title, style: t.headlineSmall),
                if (d.description.isNotEmpty) ...[
                  const SizedBox(height: Space.xs),
                  Text(d.description, style: t.bodyLarge?.copyWith(color: scheme.onSurfaceVariant)),
                ],
                const SizedBox(height: Space.lg),
                _StatusGroup(doc: d),
                if (data.missing.isNotEmpty) ...[
                  const SizedBox(height: Space.md),
                  LkBanner(
                    icon: Icons.warning_amber_rounded,
                    color: context.status.warning,
                    background: context.status.warningContainer,
                    message: context.tr(
                      'ไม่พบไฟล์แนบ ${data.missing.length} ไฟล์ ไฟล์อาจถูกย้ายหรือลบ',
                      '${data.missing.length} attachment(s) not found. They may have been moved or deleted.',
                    ),
                    action: TextButton(
                      onPressed: () => runAction(
                        context,
                        () => c.removeMissingAttachments(d),
                        success: context.tr('นำรายการที่หายไปออกแล้ว', 'Removed missing items'),
                      ),
                      child: Text(context.tr('นำออก', 'Remove')),
                    ),
                  ),
                ],
                if (d.notes.isNotEmpty) ...[
                  LkSectionHeader(title: context.tr('หมายเหตุ', 'Notes')),
                  LkPanel(child: SelectableText(d.notes, style: t.bodyMedium)),
                ],
                const SizedBox(height: Space.xl),
                Center(
                  child: Text(
                    '${context.tr('สร้าง', 'Created')} ${Fmt.dateTime(d.createdAt)}   ·   ${context.tr('แก้ไข', 'Updated')} ${Fmt.dateTime(d.updatedAt)}',
                    textAlign: TextAlign.center,
                    style: t.labelSmall,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _Gallery extends StatefulWidget {
  const _Gallery({required this.names});
  final List<String> names;

  @override
  State<_Gallery> createState() => _GalleryState();
}

class _GalleryState extends State<_Gallery> {
  int _page = 0;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final n = widget.names.length;
    return Column(
      children: [
        LayoutBuilder(
          builder: (context, box) {
            final h = (box.maxWidth * 0.62).clamp(180.0, 360.0);
            return ClipRRect(
              borderRadius: Radii.brLg,
              child: SizedBox(
                height: h,
                child: Stack(
                  children: [
                    PageView.builder(
                      itemCount: n,
                      onPageChanged: (i) => setState(() => _page = i),
                      itemBuilder: (context, i) => GestureDetector(
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => ImageViewerScreen(names: widget.names, initialIndex: i),
                          ),
                        ),
                        child: AttachmentThumb(name: widget.names[i], width: box.maxWidth, height: h, radius: 0),
                      ),
                    ),
                    Positioned(
                      right: Space.sm,
                      bottom: Space.sm,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: Space.sm, vertical: 3),
                        decoration: BoxDecoration(
                          color: Colors.black.withValues(alpha: 0.55),
                          borderRadius: BorderRadius.circular(Radii.pill),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.zoom_out_map_rounded, size: 13, color: Colors.white),
                            const SizedBox(width: 4),
                            Text(
                              '${_page + 1}/$n',
                              style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
        if (n > 1) ...[
          const SizedBox(height: Space.sm),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var i = 0; i < n; i++)
                AnimatedContainer(
                  duration: Motion.fast,
                  margin: const EdgeInsets.symmetric(horizontal: 3),
                  width: i == _page ? 16 : 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: i == _page ? scheme.primary : scheme.outline,
                    borderRadius: BorderRadius.circular(3),
                  ),
                ),
            ],
          ),
        ],
      ],
    );
  }
}

class _StatusGroup extends StatelessWidget {
  const _StatusGroup({required this.doc});
  final DocumentItem doc;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final exp = doc.expiryDate;
    final now = DateTime.now();
    final days = ExpiryCalc.daysLeft(exp, now) ?? 0;
    final info = expiryInfo(context, exp);
    final String headline = switch (ExpiryCalc.status(exp, now)) {
      ExpiryStatus.none => context.tr('ไม่มีวันหมดอายุ', 'No expiry date'),
      ExpiryStatus.expired => context.tr('หมดอายุแล้ว ${-days} วัน', 'Expired ${-days} days ago'),
      ExpiryStatus.expiresToday => context.tr('หมดอายุวันนี้', 'Expires today'),
      _ => context.tr('เหลืออีก $days วัน', '$days days left'),
    };
    Widget badge(IconData icon, Color fg, Color bg) => Container(
      width: 36,
      height: 36,
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(11)),
      child: Icon(icon, size: 19, color: fg),
    );
    return LkGroup(
      dividerIndent: 64,
      children: [
        LkRow(
          leading: badge(
            info?.icon ?? Icons.event_busy_outlined,
            info?.color ?? scheme.onSurfaceVariant,
            info?.bg ?? scheme.surfaceContainerHigh,
          ),
          title: headline,
          titleStyle: t.titleSmall?.copyWith(color: info?.color),
          subtitle: exp == null ? null : Text('${context.tr('วันหมดอายุ', 'Expiry date')} ${Fmt.fullDate(exp)}'),
        ),
        if (exp != null)
          LkRow(
            leading: badge(
              doc.reminder.isEnabled ? Icons.notifications_active_outlined : Icons.notifications_off_outlined,
              scheme.onSurfaceVariant,
              scheme.surfaceContainerHigh,
            ),
            title: doc.reminder.isEnabled
                ? context.tr('การแจ้งเตือน', 'Reminders')
                : context.tr('ไม่ได้ตั้งการแจ้งเตือน', 'No reminders'),
            subtitle: doc.reminder.isEnabled
                ? Text(doc.reminder.offsetsMinutes.map(Fmt.reminderOffset).join(' · '))
                : null,
          ),
      ],
    );
  }
}
