import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/document_item.dart';
import '../../data/models/event_item.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';

/// Thumbnail decoded at display size (low memory) with a clear placeholder
/// when the file was moved or deleted.
class AttachmentThumb extends StatelessWidget {
  const AttachmentThumb({
    super.key,
    required this.name,
    this.size = Sizes.thumb,
    this.radius = Radii.sm,
    this.width,
    this.height,
  });
  final String name;
  final double size;
  final double radius;
  final double? width;
  final double? height;

  @override
  Widget build(BuildContext context) {
    final c = context.read<AppController>();
    final scheme = Theme.of(context).colorScheme;
    File? file;
    try {
      file = c.s.attachments.fileFor(name);
    } catch (_) {}
    final w = width ?? size;
    final h = height ?? size;
    final dpr = MediaQuery.devicePixelRatioOf(context);
    Widget broken() => Container(
      color: scheme.surfaceContainerHigh,
      alignment: Alignment.center,
      child: Icon(Icons.broken_image_outlined, color: scheme.onSurfaceVariant, size: (w < h ? w : h) * 0.36),
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: SizedBox(
        width: w,
        height: h,
        child: file == null
            ? broken()
            : Image.file(
                file,
                fit: BoxFit.cover,
                cacheWidth: (w.isFinite ? w : 400) * dpr ~/ 1,
                gaplessPlayback: true,
                errorBuilder: (context, error, stack) => broken(),
              ),
      ),
    );
  }
}

/// Status + short label for a document expiry date.
({String label, Color color, Color bg, IconData icon})? expiryInfo(BuildContext context, DateTime? expiry) {
  if (expiry == null) return null;
  final now = DateTime.now();
  final s = context.status;
  final days = ExpiryCalc.daysLeft(expiry, now)!;
  switch (ExpiryCalc.status(expiry, now)) {
    case ExpiryStatus.none:
      return null;
    case ExpiryStatus.expired:
      return (
        label: context.tr('หมดอายุแล้ว', 'Expired'),
        color: s.danger,
        bg: s.dangerContainer,
        icon: Icons.error_outline_rounded,
      );
    case ExpiryStatus.expiresToday:
      return (
        label: context.tr('หมดอายุวันนี้', 'Expires today'),
        color: s.danger,
        bg: s.dangerContainer,
        icon: Icons.today_rounded,
      );
    case ExpiryStatus.expiringSoon:
      return (
        label: context.tr('อีก $days วัน', 'In $days days'),
        color: s.warning,
        bg: s.warningContainer,
        icon: Icons.schedule_rounded,
      );
    case ExpiryStatus.valid:
      return (label: Fmt.date(expiry), color: s.success, bg: s.successContainer, icon: Icons.verified_outlined);
  }
}

class ExpiryPill extends StatelessWidget {
  const ExpiryPill({super.key, required this.expiry, this.dense = true});
  final DateTime? expiry;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    final i = expiryInfo(context, expiry);
    if (i == null) return const SizedBox.shrink();
    return Pill(label: i.label, color: i.color, background: i.bg, icon: i.icon, dense: dense);
  }
}

/// Leading visual for a document: first image or category badge.
class DocumentLeading extends StatelessWidget {
  const DocumentLeading({super.key, required this.doc, this.size = Sizes.thumb});
  final DocumentItem doc;
  final double size;

  @override
  Widget build(BuildContext context) {
    final cat = context.watch<AppController>().categoryById(doc.categoryId);
    if (doc.attachmentPaths.isNotEmpty) return AttachmentThumb(name: doc.attachmentPaths.first, size: size);
    return IconBadge(icon: cat.icon, color: cat.color, size: size);
  }
}

/// Compact document row for use inside [LkGroup].
class DocumentRow extends StatelessWidget {
  const DocumentRow({super.key, required this.doc, required this.onTap, this.highlight});
  final DocumentItem doc;
  final VoidCallback onTap;

  /// Optional search term to highlight.
  final String? highlight;

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final cat = c.categoryById(doc.categoryId);
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: 10),
        child: Row(
          children: [
            DocumentLeading(doc: doc),
            const SizedBox(width: Space.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      if (doc.isPinned) ...[
                        Icon(Icons.push_pin_rounded, size: 14, color: scheme.primary),
                        const SizedBox(width: 3),
                      ],
                      Expanded(
                        child: HighlightText(
                          doc.title,
                          query: highlight,
                          maxLines: 1,
                          style: t.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      Icon(cat.icon, size: 13, color: cat.color),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          doc.description.isNotEmpty ? '${cat.name} · ${doc.description}' : cat.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: t.bodySmall,
                        ),
                      ),
                      if (doc.attachmentPaths.length > 1) ...[
                        const SizedBox(width: Space.sm),
                        Icon(Icons.photo_library_outlined, size: 13, color: scheme.onSurfaceVariant),
                        Text(' ${doc.attachmentPaths.length}', style: t.bodySmall),
                      ],
                    ],
                  ),
                ],
              ),
            ),
            if (doc.expiryDate != null) ...[
              const SizedBox(width: Space.sm),
              // Capped so the title always keeps most of the row.
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 112),
                child: ExpiryPill(expiry: doc.expiryDate),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Visual card for the documents grid view.
class DocumentGridCard extends StatelessWidget {
  const DocumentGridCard({super.key, required this.doc, required this.onTap});
  final DocumentItem doc;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final cat = c.categoryById(doc.categoryId);
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Pressable(
      child: Material(
        color: scheme.surface,
        borderRadius: Radii.brLg,
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    if (doc.attachmentPaths.isNotEmpty)
                      LayoutBuilder(
                        builder: (context, box) => AttachmentThumb(
                          name: doc.attachmentPaths.first,
                          width: box.maxWidth,
                          height: box.maxHeight,
                          radius: 0,
                        ),
                      )
                    else
                      Container(
                        color: cat.color.withValues(alpha: dark ? 0.18 : 0.08),
                        alignment: Alignment.center,
                        child: Icon(
                          cat.icon,
                          size: 34,
                          color: dark ? Color.lerp(cat.color, Colors.white, 0.5) : cat.color,
                        ),
                      ),
                    if (doc.isPinned)
                      Positioned(
                        top: Space.sm,
                        left: Space.sm,
                        child: Container(
                          padding: const EdgeInsets.all(4),
                          decoration: BoxDecoration(
                            color: scheme.surface.withValues(alpha: 0.92),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(Icons.push_pin_rounded, size: 13, color: scheme.primary),
                        ),
                      ),
                    if (doc.expiryDate != null)
                      Positioned(
                        right: Space.sm,
                        top: Space.sm,
                        left: doc.isPinned ? 40 : Space.sm,
                        child: Align(
                          alignment: Alignment.topRight,
                          child: ExpiryPill(expiry: doc.expiryDate),
                        ),
                      ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(Space.md, Space.sm, Space.md, Space.md),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(doc.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: t.titleSmall),
                    Text(cat.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: t.bodySmall),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Animated round checkbox; outline tinted by priority.
class TaskCheck extends StatelessWidget {
  const TaskCheck({super.key, required this.done, required this.color, required this.onChanged});
  final bool done;
  final Color color;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final s = context.status;
    return Semantics(
      checked: done,
      button: true,
      child: InkResponse(
        radius: 22,
        onTap: () {
          HapticFeedback.lightImpact();
          onChanged(!done);
        },
        child: SizedBox(
          width: 44,
          height: 44,
          child: Center(
            child: AnimatedContainer(
              duration: Motion.medium,
              curve: Motion.curve,
              width: 22,
              height: 22,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: done ? s.success : Colors.transparent,
                border: Border.all(color: done ? s.success : color, width: 2),
              ),
              child: AnimatedSwitcher(
                duration: Motion.fast,
                transitionBuilder: (c, a) => ScaleTransition(scale: a, child: c),
                child: done
                    ? const Icon(Icons.check_rounded, key: ValueKey(true), size: 15, color: Colors.white)
                    : const SizedBox(key: ValueKey(false)),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

String? taskDueLabel(TaskItem task, DateTime now) {
  final due = task.dueAt;
  if (due == null) return null;
  final days = Dates.daysBetween(now, due);
  if (days.abs() <= 6) {
    final rel = Fmt.relativeDays(days);
    return task.hasTime ? '$rel ${Fmt.time(due)}' : rel;
  }
  return task.hasTime ? Fmt.dateTime(due) : Fmt.date(due);
}

/// Task row with animated completion; swipe right to complete, left to delete.
class TaskRow extends StatelessWidget {
  const TaskRow({
    super.key,
    required this.task,
    required this.onTap,
    required this.onToggle,
    this.onDelete,
    this.showDate = true,
    this.highlight,
  });

  final TaskItem task;
  final VoidCallback onTap;
  final ValueChanged<bool> onToggle;
  final VoidCallback? onDelete;
  final bool showDate;
  final String? highlight;

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final s = context.status;
    final now = DateTime.now();
    final group = TaskGrouping.classify(
      completed: task.isCompleted,
      dueAt: task.dueAt,
      hasTime: task.hasTime,
      now: now,
    );
    final overdue = group == TaskGroup.overdue;
    final cat = c.maybeCategory(task.categoryId);
    final pColor = priorityColor(context, task.priority.index);
    final due = showDate
        ? taskDueLabel(task, now)
        : (task.hasTime && task.dueAt != null ? Fmt.time(task.dueAt!) : null);

    final meta = <Widget>[
      if (due != null)
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              overdue ? Icons.error_outline_rounded : Icons.schedule_rounded,
              size: 13,
              color: overdue ? s.danger : scheme.onSurfaceVariant,
            ),
            const SizedBox(width: 3),
            Flexible(
              child: Text(
                due,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: t.bodySmall?.copyWith(
                  color: overdue ? s.danger : null,
                  fontWeight: overdue ? FontWeight.w600 : null,
                ),
              ),
            ),
          ],
        ),
      if (cat != null)
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            StatusDot(color: cat.color, size: 7),
            const SizedBox(width: 4),
            Flexible(
              child: Text(cat.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: t.bodySmall),
            ),
          ],
        ),
      if (task.reminder.isEnabled && !task.isCompleted && task.dueAt != null)
        Icon(Icons.notifications_none_rounded, size: 14, color: scheme.onSurfaceVariant),
    ];

    Widget row = InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(Space.xs, 6, Space.lg, 6),
        child: Row(
          children: [
            TaskCheck(
              done: task.isCompleted,
              color: task.priority == TaskPriority.high ? pColor : scheme.outline,
              onChanged: onToggle,
            ),
            const SizedBox(width: Space.xs),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AnimatedDefaultTextStyle(
                    duration: Motion.medium,
                    style: (t.bodyLarge ?? const TextStyle()).copyWith(
                      fontWeight: FontWeight.w500,
                      height: 1.4,
                      decoration: task.isCompleted ? TextDecoration.lineThrough : null,
                      decorationColor: scheme.onSurfaceVariant,
                      color: task.isCompleted ? scheme.onSurfaceVariant : scheme.onSurface,
                    ),
                    child: HighlightText(task.title, query: highlight, maxLines: 2),
                  ),
                  if (meta.isNotEmpty && !task.isCompleted) ...[
                    const SizedBox(height: 2),
                    Wrap(
                      spacing: Space.md,
                      runSpacing: 2,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: meta,
                    ),
                  ],
                ],
              ),
            ),
            if (!task.isCompleted && task.priority == TaskPriority.high)
              Padding(
                padding: const EdgeInsets.only(left: Space.sm),
                child: Tooltip(
                  message: context.tr('สำคัญสูง', 'High priority'),
                  child: Icon(Icons.flag_rounded, size: 18, color: pColor),
                ),
              ),
          ],
        ),
      ),
    );

    if (onDelete == null) return row;
    return Dismissible(
      key: ValueKey('task-${task.id}'),
      background: _SwipeBg(
        color: s.success,
        icon: task.isCompleted ? Icons.undo_rounded : Icons.check_rounded,
        label: task.isCompleted ? context.tr('ยังไม่เสร็จ', 'Reopen') : context.tr('เสร็จแล้ว', 'Done'),
        alignStart: true,
      ),
      secondaryBackground: _SwipeBg(
        color: s.danger,
        icon: Icons.delete_outline_rounded,
        label: context.tr('ลบ', 'Delete'),
        alignStart: false,
      ),
      confirmDismiss: (dir) async {
        HapticFeedback.mediumImpact();
        if (dir == DismissDirection.startToEnd) {
          onToggle(!task.isCompleted);
          return false; // the list rebuilds and moves the row
        }
        onDelete!();
        return true;
      },
      child: Material(color: scheme.surface, child: row),
    );
  }
}

class _SwipeBg extends StatelessWidget {
  const _SwipeBg({required this.color, required this.icon, required this.label, required this.alignStart});
  final Color color;
  final IconData icon;
  final String label;
  final bool alignStart;

  @override
  Widget build(BuildContext context) {
    final content = [
      Icon(icon, color: Colors.white, size: 20),
      const SizedBox(width: Space.sm),
      Text(label, style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Colors.white)),
    ];
    return Container(
      color: color,
      padding: const EdgeInsets.symmetric(horizontal: Space.xl),
      alignment: alignStart ? Alignment.centerLeft : Alignment.centerRight,
      child: Row(mainAxisSize: MainAxisSize.min, children: alignStart ? content : content.reversed.toList()),
    );
  }
}

/// Agenda-style event row: time column, colour bar, title and location.
class EventRow extends StatelessWidget {
  const EventRow({super.key, required this.event, required this.onTap, this.showDate = true, this.highlight});
  final EventItem event;
  final VoidCallback onTap;
  final bool showDate;
  final String? highlight;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final color = event.colorValue != null ? Color(event.colorValue!) : scheme.primary;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final barColor = dark ? Color.lerp(color, Colors.white, 0.35)! : color;

    final String top;
    final String? bottom;
    if (event.allDay) {
      top = context.tr('ทั้งวัน', 'All day');
      bottom = null;
    } else {
      top = Fmt.time(event.startAt);
      bottom = event.endAt == null ? null : Fmt.time(event.endAt!);
    }
    final sub = <String>[
      if (showDate) Fmt.date(event.startAt),
      if (event.location.trim().isNotEmpty) event.location.trim(),
    ].join(' · ');

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: 10),
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(
                width: 50,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(top, maxLines: 1, style: t.labelMedium),
                    if (bottom != null) Text(bottom, style: t.labelSmall),
                  ],
                ),
              ),
              Container(
                width: 3,
                margin: const EdgeInsets.symmetric(horizontal: Space.sm),
                decoration: BoxDecoration(color: barColor, borderRadius: BorderRadius.circular(2)),
              ),
              const SizedBox(width: Space.xs),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    HighlightText(
                      event.title,
                      query: highlight,
                      maxLines: 1,
                      style: t.bodyLarge?.copyWith(fontWeight: FontWeight.w500, height: 1.4),
                    ),
                    if (sub.isNotEmpty) Text(sub, maxLines: 1, overflow: TextOverflow.ellipsis, style: t.bodySmall),
                  ],
                ),
              ),
              if (event.reminder.isEnabled)
                Padding(
                  padding: const EdgeInsets.only(left: Space.sm),
                  child: Icon(Icons.notifications_none_rounded, size: 16, color: scheme.onSurfaceVariant),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Text with the (case-insensitive) [query] emphasised.
class HighlightText extends StatelessWidget {
  const HighlightText(this.text, {super.key, this.query, this.style, this.maxLines = 1});
  final String text;
  final String? query;
  final TextStyle? style;
  final int maxLines;

  @override
  Widget build(BuildContext context) {
    final base = style ?? DefaultTextStyle.of(context).style;
    final q = query?.trim().toLowerCase() ?? '';
    final i = q.isEmpty ? -1 : text.toLowerCase().indexOf(q);
    if (i < 0) return Text(text, maxLines: maxLines, overflow: TextOverflow.ellipsis, style: style);
    final scheme = Theme.of(context).colorScheme;
    return Text.rich(
      TextSpan(
        style: base,
        children: [
          TextSpan(text: text.substring(0, i)),
          TextSpan(
            text: text.substring(i, i + q.length),
            style: TextStyle(
              color: scheme.primary,
              fontWeight: FontWeight.w700,
              backgroundColor: scheme.primaryContainer.withValues(alpha: 0.6),
            ),
          ),
          TextSpan(text: text.substring(i + q.length)),
        ],
      ),
      maxLines: maxLines,
      overflow: TextOverflow.ellipsis,
    );
  }
}

/// Header for a date group in task lists ("พรุ่งนี้", "ศ. 27 พ.ย. 2569").
String dateGroupLabel(BuildContext context, DateTime? d, DateTime now) {
  if (d == null) return context.tr('ไม่มีกำหนด', 'No date');
  final days = Dates.daysBetween(now, d);
  if (days >= -1 && days <= 1) return Fmt.relativeDays(days);
  return Fmt.fullDate(d);
}
