import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/document_item.dart';
import '../../data/models/event_item.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../calendar/event_form_screen.dart';
import '../design/design.dart';
import '../documents/document_detail_screen.dart';
import '../documents/document_form_screen.dart';
import '../search/search_screen.dart';
import '../settings/backup_screen.dart';
import '../settings/notification_settings_screen.dart';
import '../settings/settings_screen.dart';
import '../shell/main_shell.dart';
import '../tasks/task_form_screen.dart';
import '../widgets/item_tiles.dart';

class _HomeData {
  _HomeData({
    required this.docCount,
    required this.pendingTasks,
    required this.overdueTasks,
    required this.soonTasks,
    required this.urgentDocs,
    required this.laterDocs,
    required this.events,
    required this.totalItems,
    this.remindersAtRisk = false,
  });
  final int docCount;
  final int pendingTasks;
  final List<TaskItem> overdueTasks;
  final List<TaskItem> soonTasks;

  /// Expired or expiring within 7 days.
  final List<DocumentItem> urgentDocs;

  /// Expiring in 8–30 days.
  final List<DocumentItem> laterDocs;
  final List<EventItem> events;
  final int totalItems;

  /// Reminders exist but Android may not deliver them on time.
  final bool remindersAtRisk;

  int get expiringCount => urgentDocs.length + laterDocs.length;
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  static Future<_HomeData> _load(AppController c) async {
    final now = DateTime.now();
    final due = await c.s.tasks.pendingDueBefore(
      Dates.endOfDay(now.add(const Duration(days: 3))),
    );
    final overdue = <TaskItem>[];
    final soon = <TaskItem>[];
    for (final t in due) {
      final g = TaskGrouping.classify(
        completed: false,
        dueAt: t.dueAt,
        hasTime: t.hasTime,
        now: now,
      );
      (g == TaskGroup.overdue ? overdue : soon).add(t);
    }
    final docs = await c.s.documents.expiringBefore(
      Dates.endOfDay(
        now.add(const Duration(days: ExpiryCalc.soonThresholdDays)),
      ),
    );
    final urgent = docs
        .where((d) => (ExpiryCalc.daysLeft(d.expiryDate, now) ?? 99) <= 7)
        .toList();
    final later = docs
        .where((d) => (ExpiryCalc.daysLeft(d.expiryDate, now) ?? 99) > 7)
        .toList();
    final docCount = await c.s.documents.count();
    final allTasks = (await c.s.tasks.all()).length;
    final allEvents = (await c.s.events.all()).length;
    var atRisk = false;
    try {
      if (c.reminderPrefs.enabled && await c.s.scheduler.pendingCount() > 0) {
        final p = await c.s.gateway.permissionState();
        atRisk =
            p.notificationsAllowed == false || p.exactAlarmsAllowed == false;
      }
    } catch (_) {}
    return _HomeData(
      docCount: docCount,
      pendingTasks: await c.s.tasks.pendingCount(),
      overdueTasks: overdue,
      soonTasks: soon,
      urgentDocs: urgent,
      laterDocs: later,
      events: await c.s.events.between(
        Dates.startOfDay(now),
        Dates.endOfDay(now.add(const Duration(days: 7))),
      ),
      totalItems: docCount + allTasks + allEvents,
      remindersAtRisk: atRisk,
    );
  }

  String _greeting(BuildContext context) {
    final h = DateTime.now().hour;
    if (h < 12) return context.tr('สวัสดีตอนเช้า', 'Good morning');
    if (h < 17) return context.tr('สวัสดีตอนบ่าย', 'Good afternoon');
    return context.tr('สวัสดีตอนเย็น', 'Good evening');
  }

  void _open(BuildContext context, Widget page) =>
      Navigator.push(context, MaterialPageRoute(builder: (_) => page));

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: SafeArea(
        bottom: false,
        child: DataBuilder<_HomeData>(
          load: _load,
          builder: (context, d) {
            final c = context.read<AppController>();
            Widget taskRow(TaskItem task) => TaskRow(
              task: task,
              onTap: () => _open(context, TaskFormScreen(task: task)),
              onToggle: (v) =>
                  runAction(context, () => c.setTaskCompleted(task, v)),
            );
            Widget docRow(DocumentItem doc) => DocumentRow(
              doc: doc,
              onTap: () =>
                  _open(context, DocumentDetailScreen(documentId: doc.id)),
            );

            final attention = [
              ...d.overdueTasks.take(5).map(taskRow),
              ...d.urgentDocs.take(5).map(docRow),
            ];
            var section = 0;
            Widget animated(Widget w) =>
                FadeSlideIn(index: section++, child: w);

            return ContentWidth(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(
                  Space.page,
                  Space.md,
                  Space.page,
                  Space.xxxl,
                ),
                children: [
                  // Header
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              Fmt.fullDate(DateTime.now()),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: t.bodySmall,
                            ),
                            const SizedBox(height: 2),
                            Text(
                              _greeting(context),
                              style: t.displaySmall?.copyWith(
                                fontSize: 34,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            Text(
                              context.tr(
                                'ขอให้วันนี้เป็นวันที่ดีนะครับ ✨',
                                'Have a great day ✨',
                              ),
                              style: t.bodyMedium?.copyWith(
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _HeaderButton(
                        icon: Icons.search_rounded,
                        tooltip: context.tr('ค้นหา', 'Search'),
                        onTap: () => _open(context, const SearchScreen()),
                      ),
                      const SizedBox(width: Space.sm),
                      _HeaderButton(
                        icon: Icons.settings_outlined,
                        tooltip: context.tr('ตั้งค่า', 'Settings'),
                        onTap: () => _open(context, const SettingsScreen()),
                      ),
                    ],
                  ),
                  const SizedBox(height: Space.lg),
                  animated(_Overview(data: d)),
                  const SizedBox(height: Space.md),
                  animated(
                    Row(
                      children: [
                        Expanded(
                          child: QuickActionButton(
                            icon: Icons.note_add_rounded,
                            color: const Color(0xFF2F6BFF),
                            label: context.tr('เพิ่มเอกสาร', 'Document'),
                            onTap: () =>
                                _open(context, const DocumentFormScreen()),
                          ),
                        ),
                        const SizedBox(width: Space.sm),
                        Expanded(
                          child: QuickActionButton(
                            icon: Icons.edit_square,
                            color: const Color(0xFF1FB36B),
                            label: context.tr('เพิ่มงาน', 'Task'),
                            onTap: () => _open(context, const TaskFormScreen()),
                          ),
                        ),
                        const SizedBox(width: Space.sm),
                        Expanded(
                          child: QuickActionButton(
                            icon: Icons.edit_calendar_rounded,
                            color: const Color(0xFF7B5CFF),
                            label: context.tr('เพิ่มกิจกรรม', 'Event'),
                            onTap: () =>
                                _open(context, const EventFormScreen()),
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (d.remindersAtRisk) ...[
                    const SizedBox(height: Space.md),
                    animated(
                      InkWell(
                        borderRadius: Radii.brMd,
                        onTap: () =>
                            _open(context, const NotificationSettingsScreen()),
                        child: LkBanner(
                          icon: Icons.notifications_paused_outlined,
                          color: context.status.warning,
                          background: context.status.warningContainer,
                          title: context.tr(
                            'การแจ้งเตือนอาจไม่ตรงเวลา',
                            'Reminders may be late',
                          ),
                          message: context.tr(
                            'ยังไม่ได้อนุญาตการแจ้งเตือนหรือการปลุกและการช่วยเตือน แตะเพื่อแก้ไข',
                            'Notifications or "Alarms & reminders" are not allowed. Tap to fix.',
                          ),
                          action: Icon(
                            Icons.chevron_right_rounded,
                            color: context.status.warning,
                          ),
                        ),
                      ),
                    ),
                  ],
                  if (d.totalItems == 0)
                    animated(const _Welcome())
                  else ...[
                    if (attention.isNotEmpty) ...[
                      LkSectionHeader(
                        title: context.tr('ต้องจัดการ', 'Needs attention'),
                        count: attention.length,
                      ),
                      animated(LkGroup(dividerIndent: 60, children: attention)),
                    ],
                    if (d.soonTasks.isNotEmpty) ...[
                      LkSectionHeader(
                        title: context.tr('งานใน 3 วันนี้', 'Due in 3 days'),
                        actionLabel: context.tr('ทั้งหมด', 'All'),
                        onAction: () => MainShell.goToTab(context, 2),
                      ),
                      animated(
                        LkGroup(
                          dividerIndent: 52,
                          children: d.soonTasks.take(5).map(taskRow).toList(),
                        ),
                      ),
                    ],
                    if (d.events.isNotEmpty) ...[
                      LkSectionHeader(
                        title: context.tr('กำหนดการ 7 วัน', 'Next 7 days'),
                        actionLabel: context.tr('ปฏิทิน', 'Calendar'),
                        onAction: () => MainShell.goToTab(context, 3),
                      ),
                      animated(
                        LkGroup(
                          dividerIndent: 80,
                          children: [
                            for (final e in d.events.take(5))
                              EventRow(
                                event: e,
                                onTap: () =>
                                    _open(context, EventFormScreen(event: e)),
                              ),
                          ],
                        ),
                      ),
                    ],
                    if (d.laterDocs.isNotEmpty) ...[
                      LkSectionHeader(
                        title: context.tr('เอกสารใกล้หมดอายุ', 'Expiring soon'),
                        actionLabel: context.tr('ทั้งหมด', 'All'),
                        onAction: () => MainShell.goToTab(context, 1),
                      ),
                      animated(
                        LkGroup(
                          dividerIndent: 72,
                          children: d.laterDocs.take(5).map(docRow).toList(),
                        ),
                      ),
                    ],
                    if (attention.isEmpty &&
                        d.soonTasks.isEmpty &&
                        d.events.isEmpty &&
                        d.laterDocs.isEmpty) ...[
                      const SizedBox(height: Space.lg),
                      animated(
                        LkBanner(
                          icon: Icons.task_alt_rounded,
                          title: context.tr('ทุกอย่างเรียบร้อย', 'All clear'),
                          message: context.tr(
                            'ไม่มีงานค้าง กิจกรรม หรือเอกสารที่ต้องดูแลในช่วงนี้',
                            'No tasks, events or documents need attention right now.',
                          ),
                          color: context.status.success,
                          background: context.status.successContainer,
                        ),
                      ),
                    ],
                  ],
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}

class _HeaderButton extends StatelessWidget {
  const _HeaderButton({
    required this.icon,
    required this.tooltip,
    required this.onTap,
  });
  final IconData icon;
  final String tooltip;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Tooltip(
      message: tooltip,
      child: DecoratedBox(
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          boxShadow: lkShadow(context, strength: 1.5),
        ),
        child: Material(
          color: scheme.surface,
          shape: const CircleBorder(),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: onTap,
            child: SizedBox(
              width: 54,
              height: 54,
              child: Icon(icon, size: 26, color: scheme.onSurface),
            ),
          ),
        ),
      ),
    );
  }
}

/// One compact strip with the four key numbers.
class _Overview extends StatelessWidget {
  const _Overview({required this.data});
  final _HomeData data;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final s = context.status;
    Widget divider() =>
        Container(width: 1, height: 32, color: scheme.outlineVariant);
    return LkPanel(
      padding: const EdgeInsets.symmetric(
        vertical: Space.lg,
        horizontal: Space.xs,
      ),
      child: Row(
        children: [
          Expanded(
            child: _Stat(
              value: data.docCount,
              label: context.tr('เอกสาร', 'Documents'),
              icon: Icons.folder_rounded,
              color: scheme.primary,
              onTap: () => MainShell.goToTab(context, 1),
            ),
          ),
          divider(),
          Expanded(
            child: _Stat(
              value: data.pendingTasks,
              label: context.tr('งานค้าง', 'Open tasks'),
              icon: Icons.assignment_turned_in_rounded,
              color: scheme.primary,
              onTap: () => MainShell.goToTab(context, 2),
            ),
          ),
          divider(),
          Expanded(
            child: _Stat(
              value: data.overdueTasks.length,
              label: context.tr('เลยกำหนด', 'Overdue'),
              icon: Icons.error_outline_rounded,
              color: data.overdueTasks.isEmpty
                  ? scheme.onSurfaceVariant
                  : s.danger,
              onTap: () => MainShell.goToTab(context, 2),
            ),
          ),
          divider(),
          Expanded(
            child: _Stat(
              value: data.expiringCount,
              label: context.tr('ใกล้หมดอายุ', 'Expiring'),
              icon: Icons.hourglass_bottom_rounded,
              color: data.expiringCount == 0
                  ? scheme.onSurfaceVariant
                  : s.warning,
              onTap: () => MainShell.goToTab(context, 1),
            ),
          ),
        ],
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({
    required this.value,
    required this.label,
    required this.icon,
    required this.color,
    required this.onTap,
  });
  final int value;
  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return InkWell(
      onTap: onTap,
      borderRadius: Radii.brMd,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: Space.xs,
          vertical: Space.xs,
        ),
        child: Column(
          children: [
            TweenAnimationBuilder<int>(
              tween: IntTween(begin: 0, end: value),
              duration: Motion.slow,
              curve: Motion.curve,
              builder: (context, v, _) => FittedBox(
                fit: BoxFit.scaleDown,
                child: Text(
                  '$v',
                  style: t.displaySmall?.copyWith(
                    fontSize: 32,
                    fontWeight: FontWeight.w800,
                    color: value == 0 ? null : color,
                    height: 1.15,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 2),
            FittedBox(
              fit: BoxFit.scaleDown,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(icon, size: 16, color: color),
                  const SizedBox(width: 4),
                  Text(
                    label,
                    maxLines: 1,
                    style: t.labelMedium?.copyWith(fontWeight: FontWeight.w500),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Welcome extends StatelessWidget {
  const _Welcome();

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    void open(Widget page) =>
        Navigator.push(context, MaterialPageRoute(builder: (_) => page));
    Widget step(
      IconData icon,
      Color color,
      String title,
      String body,
      VoidCallback onTap,
    ) => Padding(
      padding: const EdgeInsets.only(top: Space.md),
      child: Material(
        color: scheme.surfaceContainerHigh.withValues(alpha: 0.8),
        borderRadius: Radii.brMd,
        child: InkWell(
          borderRadius: Radii.brMd,
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(
              Space.lg,
              Space.md,
              Space.sm,
              Space.md,
            ),
            child: Row(
              children: [
                Icon(icon, size: 34, color: color),
                const SizedBox(width: Space.lg),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: t.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(body, style: t.bodySmall),
                    ],
                  ),
                ),
                Icon(Icons.chevron_right_rounded, color: scheme.onSurface),
              ],
            ),
          ),
        ),
      ),
    );
    return Padding(
      padding: const EdgeInsets.only(top: Space.xl),
      child: LkPanel(
        padding: const EdgeInsets.all(Space.xl),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const GradientTile(icon: Icons.verified_user_rounded, size: 60),
                const SizedBox(width: Space.lg),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        context.tr(
                          'เริ่มต้นใช้งาน LIFEKEEP',
                          'Welcome to LIFEKEEP',
                        ),
                        style: t.titleLarge?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: Space.xs),
                      Text(
                        context.tr(
                          'ทุกอย่างในชีวิตให้เป็นระเบียบ ไม่ว่าจะเอกสาร งาน หรือกิจกรรม ก็จัดการได้ในที่เดียว',
                          'Documents, tasks and events, all organised in one place.',
                        ),
                        style: t.bodyMedium?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            step(
              Icons.folder_rounded,
              const Color(0xFF2F6BFF),
              context.tr('เก็บเอกสารสำคัญ', 'Store key documents'),
              context.tr(
                'สำรองรูปบัตร ใบขับขี่ ทะเบียนรถ และเอกสารสำคัญ',
                'IDs, licences, car papers and more',
              ),
              () => open(const DocumentFormScreen()),
            ),
            step(
              Icons.event_available_rounded,
              const Color(0xFF1FB36B),
              context.tr('รับการแจ้งเตือนล่วงหน้า', 'Get reminded early'),
              context.tr(
                'เตือนก่อนหมดอายุ ทั้งเอกสารและงาน สำคัญไม่พลาด',
                'Before expiry dates and deadlines',
              ),
              () => open(const NotificationSettingsScreen()),
            ),
            step(
              Icons.cloud_upload_rounded,
              const Color(0xFF7B5CFF),
              context.tr('สำรองข้อมูลไว้ภายนอก', 'Keep a backup'),
              context.tr(
                'ส่งออกไฟล์สำรองไปเก็บใน USB หรือคอมพิวเตอร์',
                'Export a backup file to USB or a computer',
              ),
              () => open(const BackupScreen()),
            ),
            const SizedBox(height: Space.xl),
            SizedBox(
              width: double.infinity,
              child: GradientButton(
                label: context.tr('เพิ่มเอกสารแรก', 'Add first document'),
                trailing: Icons.chevron_right_rounded,
                onPressed: () => open(const DocumentFormScreen()),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
