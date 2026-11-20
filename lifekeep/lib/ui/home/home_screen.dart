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
import '../calendar/event_form_screen.dart';
import '../documents/document_detail_screen.dart';
import '../documents/document_form_screen.dart';
import '../search/search_screen.dart';
import '../settings/settings_screen.dart';
import '../shell/main_shell.dart';
import '../tasks/task_form_screen.dart';
import '../widgets/common.dart';
import '../widgets/item_tiles.dart';

class _HomeData {
  _HomeData({
    required this.docCount,
    required this.pendingTasks,
    required this.dueTasks,
    required this.expiringDocs,
    required this.events,
    required this.totalItems,
  });
  final int docCount;
  final int pendingTasks;
  final List<TaskItem> dueTasks;
  final List<DocumentItem> expiringDocs;
  final List<EventItem> events;
  final int totalItems;
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  static Future<_HomeData> _load(AppController c) async {
    final now = DateTime.now();
    final docCount = await c.s.documents.count();
    final pending = await c.s.tasks.pendingCount();
    final dueTasks = await c.s.tasks.pendingDueBefore(Dates.endOfDay(now.add(const Duration(days: 3))));
    final expiring = await c.s.documents.expiringBefore(Dates.endOfDay(now.add(const Duration(days: ExpiryCalc.soonThresholdDays))));
    final events = await c.s.events.between(Dates.startOfDay(now), Dates.endOfDay(now.add(const Duration(days: 7))));
    final allTasks = (await c.s.tasks.all()).length;
    final allEvents = (await c.s.events.all()).length;
    return _HomeData(
      docCount: docCount,
      pendingTasks: pending,
      dueTasks: dueTasks.take(5).toList(),
      expiringDocs: expiring.take(5).toList(),
      events: events.take(5).toList(),
      totalItems: docCount + allTasks + allEvents,
    );
  }

  String _greeting(BuildContext context) {
    final h = DateTime.now().hour;
    if (h < 12) return context.tr('สวัสดีตอนเช้า', 'Good morning');
    if (h < 17) return context.tr('สวัสดีตอนบ่าย', 'Good afternoon');
    return context.tr('สวัสดีตอนเย็น', 'Good evening');
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: DataBuilder<_HomeData>(
          load: _load,
          builder: (context, d) => CustomScrollView(slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 12, 0),
              sliver: SliverToBoxAdapter(
                child: Row(children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(_greeting(context), style: text.headlineSmall),
                      const SizedBox(height: 4),
                      Text(Fmt.fullDate(DateTime.now()), style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant)),
                    ]),
                  ),
                  IconButton(
                    tooltip: context.tr('ค้นหา', 'Search'),
                    icon: const Icon(Icons.search_rounded),
                    onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SearchScreen())),
                  ),
                  IconButton(
                    tooltip: context.tr('ตั้งค่า', 'Settings'),
                    icon: const Icon(Icons.settings_outlined),
                    onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen())),
                  ),
                ]),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 20, 16, 0),
              sliver: SliverToBoxAdapter(
                child: Row(children: [
                  Expanded(
                    child: _StatCard(
                      icon: Icons.folder_rounded,
                      value: d.docCount,
                      label: context.tr('เอกสารทั้งหมด', 'Documents'),
                      highlight: true,
                      onTap: () => MainShell.goToTab(context, 1),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _StatCard(
                      icon: Icons.check_circle_rounded,
                      value: d.pendingTasks,
                      label: context.tr('งานที่ยังไม่เสร็จ', 'Pending tasks'),
                      onTap: () => MainShell.goToTab(context, 2),
                    ),
                  ),
                ]),
              ),
            ),
            const SliverPadding(
              padding: EdgeInsets.fromLTRB(16, 16, 16, 0),
              sliver: SliverToBoxAdapter(child: _QuickActions()),
            ),
            if (d.totalItems == 0)
              SliverToBoxAdapter(
                child: EmptyState(
                  icon: Icons.inventory_2_outlined,
                  title: context.tr('เริ่มต้นใช้งาน LIFEKEEP', 'Welcome to LIFEKEEP'),
                  message: context.tr(
                    'เก็บเอกสารสำคัญ จัดการงาน และกำหนดการไว้ในเครื่องของคุณ\nข้อมูลทั้งหมดอยู่ในเครื่อง ไม่มีการส่งออกภายนอก',
                    'Keep documents, tasks and plans on your device.\nAll data stays on this device.',
                  ),
                  actionLabel: context.tr('เพิ่มเอกสารแรก', 'Add first document'),
                  onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const DocumentFormScreen())),
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                sliver: SliverList.list(children: [
                  SectionHeader(
                    title: context.tr('งานที่ใกล้กำหนด', 'Tasks due soon'),
                    trailing: TextButton(
                      onPressed: () => MainShell.goToTab(context, 2),
                      child: Text(context.tr('ดูทั้งหมด', 'See all')),
                    ),
                  ),
                  if (d.dueTasks.isEmpty)
                    _InlineEmpty(text: context.tr('ไม่มีงานที่ใกล้กำหนดใน 3 วันนี้', 'No tasks due in the next 3 days'))
                  else
                    for (final t in d.dueTasks)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: TaskTile(
                          task: t,
                          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskFormScreen(task: t))),
                          onToggle: (v) => runAction(context, () => context.read<AppController>().setTaskCompleted(t, v)),
                        ),
                      ),
                  SectionHeader(
                    title: context.tr('เอกสารใกล้หมดอายุ', 'Expiring documents'),
                    trailing: TextButton(
                      onPressed: () => MainShell.goToTab(context, 1),
                      child: Text(context.tr('ดูทั้งหมด', 'See all')),
                    ),
                  ),
                  if (d.expiringDocs.isEmpty)
                    _InlineEmpty(text: context.tr('ไม่มีเอกสารที่จะหมดอายุใน 30 วัน', 'Nothing expires in the next 30 days'))
                  else
                    for (final doc in d.expiringDocs)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: DocumentCard(
                          doc: doc,
                          onTap: () => Navigator.push(
                              context, MaterialPageRoute(builder: (_) => DocumentDetailScreen(documentId: doc.id))),
                        ),
                      ),
                  SectionHeader(
                    title: context.tr('กิจกรรมที่กำลังจะมาถึง', 'Upcoming events'),
                    trailing: TextButton(
                      onPressed: () => MainShell.goToTab(context, 3),
                      child: Text(context.tr('ปฏิทิน', 'Calendar')),
                    ),
                  ),
                  if (d.events.isEmpty)
                    _InlineEmpty(text: context.tr('ไม่มีกิจกรรมใน 7 วันข้างหน้า', 'No events in the next 7 days'))
                  else
                    for (final e in d.events)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: EventTile(
                          event: e,
                          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => EventFormScreen(event: e))),
                        ),
                      ),
                ]),
              ),
          ]),
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.icon, required this.value, required this.label, required this.onTap, this.highlight = false});
  final IconData icon;
  final int value;
  final String label;
  final VoidCallback onTap;
  final bool highlight;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final bg = highlight ? (dark ? scheme.primaryContainer : AppColors.primary) : scheme.surface;
    final fg = highlight ? (dark ? scheme.onPrimaryContainer : Colors.white) : scheme.onSurface;
    return Card(
      color: bg,
      shape: highlight
          ? RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppTheme.radius))
          : null,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(icon, color: highlight ? fg.withValues(alpha: 0.85) : scheme.primary, size: 22),
            const SizedBox(height: 14),
            TweenAnimationBuilder<int>(
              tween: IntTween(begin: 0, end: value),
              duration: const Duration(milliseconds: 500),
              curve: Curves.easeOutCubic,
              builder: (context, v, _) => Text(
                '$v',
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700, color: fg),
              ),
            ),
            const SizedBox(height: 2),
            Text(label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(color: fg.withValues(alpha: 0.75))),
          ]),
        ),
      ),
    );
  }
}

class _QuickActions extends StatelessWidget {
  const _QuickActions();

  @override
  Widget build(BuildContext context) {
    Widget action(IconData icon, String label, Widget page) => Expanded(
          child: AppCard(
            padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => page)),
            child: Column(children: [
              IconBadge(icon: icon, color: Theme.of(context).colorScheme.primary, size: 40),
              const SizedBox(height: 8),
              Text(label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(fontWeight: FontWeight.w600)),
            ]),
          ),
        );
    return Row(children: [
      action(Icons.note_add_outlined, context.tr('เพิ่มเอกสาร', 'Add Document'), const DocumentFormScreen()),
      const SizedBox(width: 10),
      action(Icons.add_task_rounded, context.tr('เพิ่มงาน', 'Add Task'), const TaskFormScreen()),
      const SizedBox(width: 10),
      action(Icons.event_available_outlined, context.tr('เพิ่มกิจกรรม', 'Add Event'), const EventFormScreen()),
    ]);
  }
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.text});
  final String text;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: scheme.outlineVariant),
      ),
      child: Row(children: [
        Icon(Icons.check_rounded, size: 18, color: scheme.onSurfaceVariant),
        const SizedBox(width: 10),
        Expanded(child: Text(text, style: TextStyle(color: scheme.onSurfaceVariant))),
      ]),
    );
  }
}
