import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';
import '../widgets/item_tiles.dart';
import 'task_form_screen.dart';

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key});

  @override
  State<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends State<TasksScreen> {
  final _search = TextEditingController();
  String _query = '';
  String? _categoryId;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  bool _matches(AppController c, TaskItem t) {
    if (_categoryId != null && t.categoryId != _categoryId) return false;
    final q = _query.trim().toLowerCase();
    if (q.isEmpty) return true;
    final cat = c.maybeCategory(t.categoryId)?.name.toLowerCase() ?? '';
    return t.title.toLowerCase().contains(q) || t.description.toLowerCase().contains(q) || cat.contains(q);
  }

  Future<void> _toggle(BuildContext context, TaskItem t, bool v) async {
    final c = context.read<AppController>();
    final messenger = ScaffoldMessenger.of(context);
    final undoLabel = context.tr('เลิกทำ', 'Undo');
    final msg = v ? context.tr('ทำเครื่องหมายว่าเสร็จแล้ว', 'Marked as done') : context.tr('ย้ายกลับไปยังงานค้าง', 'Moved back to open tasks');
    final ok = await runAction(context, () => c.setTaskCompleted(t, v));
    if (!ok) return;
    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(SnackBar(
      content: Text(msg),
      action: SnackBarAction(label: undoLabel, onPressed: () => c.setTaskCompleted(t, !v)),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final tabs = [
      (TaskGroup.today, context.tr('วันนี้', 'Today')),
      (TaskGroup.upcoming, context.tr('กำลังจะมาถึง', 'Upcoming')),
      (TaskGroup.overdue, context.tr('เลยกำหนด', 'Overdue')),
      (TaskGroup.completed, context.tr('เสร็จแล้ว', 'Completed')),
    ];
    return DefaultTabController(
      length: tabs.length,
      child: Scaffold(
        appBar: AppBar(
          title: Text(context.tr('งาน', 'Tasks')),
          actions: [
            PopupMenuButton<String?>(
              tooltip: context.tr('กรองหมวดหมู่', 'Filter category'),
              icon: Icon(_categoryId == null ? Icons.filter_list_rounded : Icons.filter_alt_rounded),
              onSelected: (v) => setState(() => _categoryId = v == '' ? null : v),
              itemBuilder: (context) => [
                CheckedPopupMenuItem(value: '', checked: _categoryId == null, child: Text(context.tr('ทุกหมวดหมู่', 'All categories'))),
                for (final cat in c.categories)
                  CheckedPopupMenuItem(value: cat.id, checked: _categoryId == cat.id, child: Text(cat.name)),
              ],
            ),
            const SizedBox(width: 4),
          ],
        ),
        floatingActionButton: FloatingActionButton(
          heroTag: 'fab_tasks',
          tooltip: context.tr('เพิ่มงาน', 'Add task'),
          onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const TaskFormScreen())),
          child: const Icon(Icons.add_rounded),
        ),
        body: DataBuilder<List<TaskItem>>(
          load: (c) => c.s.tasks.all(),
          builder: (context, all) {
            final now = DateTime.now();
            final filtered = all.where((t) => _matches(c, t)).toList();
            final groups = <TaskGroup, List<TaskItem>>{for (final g in TaskGroup.values) g: []};
            for (final t in filtered) {
              groups[TaskGrouping.classify(completed: t.isCompleted, dueAt: t.dueAt, hasTime: t.hasTime, now: now)]!.add(t);
            }
            // Completed: most recent first.
            groups[TaskGroup.completed]!.sort((a, b) => (b.completedAt ?? b.updatedAt).compareTo(a.completedAt ?? a.updatedAt));
            return Column(children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 4),
                child: SearchField(
                  controller: _search,
                  hint: context.tr('ค้นหางาน', 'Search tasks'),
                  onChanged: (v) => setState(() => _query = v),
                ),
              ),
              TabBar(
                isScrollable: true,
                tabAlignment: TabAlignment.start,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                tabs: [
                  for (final t in tabs)
                    Tab(
                      child: Row(mainAxisSize: MainAxisSize.min, children: [
                        Text(t.$2),
                        if (groups[t.$1]!.isNotEmpty) ...[
                          const SizedBox(width: 6),
                          _CountBadge(count: groups[t.$1]!.length, alert: t.$1 == TaskGroup.overdue),
                        ],
                      ]),
                    ),
                ],
              ),
              Expanded(
                child: TabBarView(children: [
                  for (final t in tabs)
                    _TaskList(
                      tasks: groups[t.$1]!,
                      group: t.$1,
                      hasAnyTask: all.isNotEmpty,
                      searching: _query.isNotEmpty || _categoryId != null,
                      onToggle: (task, v) => _toggle(context, task, v),
                    ),
                ]),
              ),
            ]);
          },
        ),
      ),
    );
  }
}

class _CountBadge extends StatelessWidget {
  const _CountBadge({required this.count, required this.alert});
  final int count;
  final bool alert;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1),
      decoration: BoxDecoration(
        color: alert ? scheme.error : scheme.secondaryContainer,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Text('$count',
          style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: alert ? scheme.onError : scheme.onSecondaryContainer)),
    );
  }
}

class _TaskList extends StatelessWidget {
  const _TaskList({
    required this.tasks,
    required this.group,
    required this.hasAnyTask,
    required this.searching,
    required this.onToggle,
  });

  final List<TaskItem> tasks;
  final TaskGroup group;
  final bool hasAnyTask;
  final bool searching;
  final void Function(TaskItem, bool) onToggle;

  @override
  Widget build(BuildContext context) {
    if (tasks.isEmpty) {
      if (!hasAnyTask) {
        return EmptyState(
          icon: Icons.task_alt_rounded,
          title: context.tr('ยังไม่มีงาน', 'No tasks yet'),
          message: context.tr('เพิ่มสิ่งที่ต้องทำ พร้อมกำหนดส่งและการแจ้งเตือน', 'Add to-dos with due dates and reminders'),
          actionLabel: context.tr('เพิ่มงาน', 'Add task'),
          onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const TaskFormScreen())),
        );
      }
      final (IconData icon, String title) = searching
          ? (Icons.search_off_rounded, context.tr('ไม่พบงานที่ค้นหา', 'No matching tasks'))
          : switch (group) {
              TaskGroup.today => (Icons.wb_sunny_outlined, context.tr('ไม่มีงานวันนี้', 'Nothing due today')),
              TaskGroup.upcoming => (Icons.upcoming_outlined, context.tr('ไม่มีงานที่กำลังจะมาถึง', 'No upcoming tasks')),
              TaskGroup.overdue => (Icons.verified_outlined, context.tr('ไม่มีงานที่เลยกำหนด', 'Nothing overdue')),
              TaskGroup.completed => (Icons.done_all_rounded, context.tr('ยังไม่มีงานที่เสร็จ', 'No completed tasks yet')),
            };
      return EmptyState(icon: icon, title: title, compact: true);
    }
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 96),
      itemCount: tasks.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (context, i) {
        final t = tasks[i];
        return TaskTile(
          key: ValueKey(t.id),
          task: t,
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskFormScreen(task: t))),
          onToggle: (v) => onToggle(t, v),
        );
      },
    );
  }
}
