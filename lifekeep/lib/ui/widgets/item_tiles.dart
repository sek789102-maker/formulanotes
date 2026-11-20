import 'dart:io';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/document_item.dart';
import '../../data/models/event_item.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import 'common.dart';

/// Thumbnail that decodes at a small size (low memory) and shows a clear
/// placeholder when the file was moved or deleted.
class AttachmentThumb extends StatelessWidget {
  const AttachmentThumb({super.key, required this.name, this.size = 56, this.radius = 12});
  final String name;
  final double size;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final c = context.read<AppController>();
    final scheme = Theme.of(context).colorScheme;
    File? file;
    try {
      file = c.s.attachments.fileFor(name);
    } catch (_) {}
    final dpr = MediaQuery.devicePixelRatioOf(context);
    Widget broken() => Container(
          color: scheme.surfaceContainerHigh,
          alignment: Alignment.center,
          child: Icon(Icons.broken_image_outlined, color: scheme.onSurfaceVariant, size: size * 0.4),
        );
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: SizedBox(
        width: size,
        height: size,
        child: file == null
            ? broken()
            : Image.file(
                file,
                fit: BoxFit.cover,
                cacheWidth: (size * dpr).round(),
                gaplessPlayback: true,
                errorBuilder: (context, error, stack) => broken(),
              ),
      ),
    );
  }
}

class ExpiryPill extends StatelessWidget {
  const ExpiryPill({super.key, required this.expiry});
  final DateTime? expiry;

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final st = ExpiryCalc.status(expiry, now);
    final s = context.status;
    final days = ExpiryCalc.daysLeft(expiry, now);
    switch (st) {
      case ExpiryStatus.none:
        return const SizedBox.shrink();
      case ExpiryStatus.expired:
        return Pill(
          label: context.tr('หมดอายุแล้ว', 'Expired'),
          color: s.danger,
          background: s.dangerContainer,
          icon: Icons.error_outline_rounded,
        );
      case ExpiryStatus.expiresToday:
        return Pill(label: context.tr('หมดอายุวันนี้', 'Expires today'), color: s.danger, background: s.dangerContainer);
      case ExpiryStatus.expiringSoon:
        return Pill(
          label: context.tr('อีก $days วัน', 'In $days days'),
          color: s.warning,
          background: s.warningContainer,
          icon: Icons.schedule_rounded,
        );
      case ExpiryStatus.valid:
        return Pill(
          label: Fmt.date(expiry!),
          color: s.success,
          background: s.successContainer,
          icon: Icons.verified_outlined,
        );
    }
  }
}

class DocumentCard extends StatelessWidget {
  const DocumentCard({super.key, required this.doc, required this.onTap});
  final DocumentItem doc;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final cat = c.categoryById(doc.categoryId);
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    return AppCard(
      onTap: onTap,
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        doc.attachmentPaths.isNotEmpty
            ? AttachmentThumb(name: doc.attachmentPaths.first)
            : IconBadge(icon: cat.icon, color: cat.color, size: 56),
        const SizedBox(width: 14),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              if (doc.isPinned) ...[
                Icon(Icons.push_pin_rounded, size: 15, color: scheme.primary),
                const SizedBox(width: 4),
              ],
              Expanded(
                child: Text(doc.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: text.titleSmall),
              ),
            ]),
            const SizedBox(height: 3),
            Text(
              doc.description.isNotEmpty ? doc.description : cat.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
            const SizedBox(height: 8),
            Wrap(spacing: 6, runSpacing: 6, crossAxisAlignment: WrapCrossAlignment.center, children: [
              Pill(label: cat.name, color: cat.color, background: cat.color.withValues(alpha: 0.10)),
              ExpiryPill(expiry: doc.expiryDate),
              if (doc.attachmentPaths.length > 1)
                Pill(
                  label: '${doc.attachmentPaths.length}',
                  icon: Icons.photo_library_outlined,
                  color: scheme.onSurfaceVariant,
                  background: scheme.surfaceContainerHigh,
                ),
            ]),
          ]),
        ),
      ]),
    );
  }
}

class TaskTile extends StatelessWidget {
  const TaskTile({super.key, required this.task, required this.onTap, required this.onToggle});
  final TaskItem task;
  final VoidCallback onTap;
  final ValueChanged<bool> onToggle;

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final now = DateTime.now();
    final group = TaskGrouping.classify(
        completed: task.isCompleted, dueAt: task.dueAt, hasTime: task.hasTime, now: now);
    final cat = c.maybeCategory(task.categoryId);
    final pColor = priorityColor(context, task.priority.index);
    final overdue = group == TaskGroup.overdue;

    String? due;
    if (task.dueAt != null) {
      final rel = Fmt.relativeDays(Dates.daysBetween(now, task.dueAt!));
      due = task.hasTime ? '$rel · ${Fmt.time(task.dueAt!)}' : rel;
      if (Dates.daysBetween(now, task.dueAt!).abs() > 6) {
        due = task.hasTime ? Fmt.dateTime(task.dueAt!) : Fmt.date(task.dueAt!);
      }
    }

    return AppCard(
      onTap: onTap,
      padding: const EdgeInsets.fromLTRB(4, 6, 14, 6),
      child: Row(children: [
        Checkbox(
          value: task.isCompleted,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
          onChanged: (v) => onToggle(v ?? false),
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              AnimatedDefaultTextStyle(
                duration: const Duration(milliseconds: 200),
                style: (text.titleSmall ?? const TextStyle()).copyWith(
                  decoration: task.isCompleted ? TextDecoration.lineThrough : null,
                  color: task.isCompleted ? scheme.onSurfaceVariant : scheme.onSurface,
                ),
                child: Text(task.title, maxLines: 2, overflow: TextOverflow.ellipsis),
              ),
              if (due != null || cat != null) ...[
                const SizedBox(height: 4),
                Wrap(spacing: 10, runSpacing: 4, children: [
                  if (due != null)
                    Row(mainAxisSize: MainAxisSize.min, children: [
                      Icon(Icons.event_outlined, size: 14, color: overdue ? context.status.danger : scheme.onSurfaceVariant),
                      const SizedBox(width: 4),
                      Text(due,
                          style: text.bodySmall?.copyWith(
                              color: overdue ? context.status.danger : scheme.onSurfaceVariant,
                              fontWeight: overdue ? FontWeight.w600 : null)),
                    ]),
                  if (cat != null)
                    Row(mainAxisSize: MainAxisSize.min, children: [
                      Icon(cat.icon, size: 14, color: cat.color),
                      const SizedBox(width: 4),
                      Text(cat.name, style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant)),
                    ]),
                  if (task.reminder.isEnabled && !task.isCompleted)
                    Icon(Icons.notifications_active_outlined, size: 14, color: scheme.onSurfaceVariant),
                ]),
              ],
            ]),
          ),
        ),
        if (!task.isCompleted && task.priority != TaskPriority.normal)
          Tooltip(
            message: task.priority == TaskPriority.high ? context.tr('สำคัญสูง', 'High') : context.tr('สำคัญต่ำ', 'Low'),
            child: Icon(
              task.priority == TaskPriority.high ? Icons.keyboard_double_arrow_up_rounded : Icons.keyboard_arrow_down_rounded,
              color: pColor,
              size: 20,
            ),
          ),
      ]),
    );
  }
}

class EventTile extends StatelessWidget {
  const EventTile({super.key, required this.event, required this.onTap, this.showDate = true});
  final EventItem event;
  final VoidCallback onTap;
  final bool showDate;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final color = event.colorValue != null ? Color(event.colorValue!) : scheme.primary;
    final String when;
    if (event.allDay) {
      when = showDate ? '${Fmt.date(event.startAt)} · ${context.tr('ทั้งวัน', 'All day')}' : context.tr('ทั้งวัน', 'All day');
    } else {
      final end = event.endAt;
      final range = end == null ? Fmt.time(event.startAt) : '${Fmt.time(event.startAt)} – ${Fmt.time(end)}';
      when = showDate ? '${Fmt.date(event.startAt)} · $range' : range;
    }
    return AppCard(
      onTap: onTap,
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        Container(width: 4, height: 40, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(4))),
        const SizedBox(width: 12),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(event.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: text.titleSmall),
            const SizedBox(height: 3),
            Text(
              event.location.trim().isEmpty ? when : '$when · ${event.location.trim()}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ]),
        ),
        if (event.reminder.isEnabled) Icon(Icons.notifications_active_outlined, size: 16, color: scheme.onSurfaceVariant),
      ]),
    );
  }
}
