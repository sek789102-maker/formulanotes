import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../widgets/form_widgets.dart';
import '../widgets/item_tiles.dart';
import 'task_form_screen.dart';

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key});

  @override
  State<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends State<TasksScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 4, vsync: this);
  final _search = TextEditingController();
  bool _searching = false;
  String _query = '';
  String? _categoryId;

  @override
  void dispose() {
    _tabs.dispose();
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

  Future<void> _toggle(TaskItem t, bool v) async {
    final c = context.read<AppController>();
    final messenger = ScaffoldMessenger.of(context);
    final undo = context.tr('เลิกทำ', 'Undo');
    final msg = v
        ? context.tr('เสร็จแล้ว: ${t.title}', 'Done: ${t.title}')
        : context.tr('ย้ายกลับไปงานค้าง', 'Moved back to open tasks');
    final ok = await runAction(context, () => c.setTaskCompleted(t, v));
    if (!ok) return;
    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(
      SnackBar(
        content: Text(msg, maxLines: 1, overflow: TextOverflow.ellipsis),
        action: SnackBarAction(label: undo, onPressed: () => c.setTaskCompleted(t, !v)),
      ),
    );
  }

  Future<void> _delete(TaskItem t) async {
    final c = context.read<AppController>();
    final messenger = ScaffoldMessenger.of(context);
    final undo = context.tr('เลิกทำ', 'Undo');
    final msg = context.tr('ลบงานแล้ว', 'Task deleted');
    final ok = await runAction(context, () => c.deleteTask(t));
    if (!ok) return;
    messenger.hideCurrentSnackBar();
    // Undo re-saves the same task (same id), which also restores its reminders.
    messenger.showSnackBar(
      SnackBar(
        content: Text(msg),
        action: SnackBarAction(label: undo, onPressed: () => c.saveTask(t)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final cat = c.maybeCategory(_categoryId);
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: Text(context.tr('งาน', 'Tasks')),
        actions: [
          IconButton(
            tooltip: context.tr('ค้นหา', 'Search'),
            isSelected: _searching,
            icon: const Icon(Icons.search_rounded),
            selectedIcon: const Icon(Icons.search_off_rounded),
            onPressed: () => setState(() {
              _searching = !_searching;
              if (!_searching) {
                _search.clear();
                _query = '';
              }
            }),
          ),
          IconButton(
            tooltip: context.tr('กรองหมวดหมู่', 'Filter category'),
            icon: Badge(
              isLabelVisible: cat != null,
              smallSize: 8,
              backgroundColor: scheme.primary,
              child: const Icon(Icons.tune_rounded),
            ),
            onPressed: () async {
              final r = await showCategorySheet(
                context,
                selectedId: _categoryId,
                allowNone: true,
                title: context.tr('กรองตามหมวดหมู่', 'Filter by category'),
              );
              if (r != null) setState(() => _categoryId = r.isEmpty ? null : r);
            },
          ),
          const SizedBox(width: Space.xs),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'fab_tasks',
        tooltip: context.tr('เพิ่มงาน', 'Add task'),
        onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const TaskFormScreen())),
        child: const Icon(Icons.add_rounded),
      ),
      body: ContentWidth(
        child: DataBuilder<List<TaskItem>>(
          load: (c) => c.s.tasks.all(),
          builder: (context, all) {
            final now = DateTime.now();
            final filtered = all.where((t) => _matches(c, t)).toList();
            final groups = <TaskGroup, List<TaskItem>>{for (final g in TaskGroup.values) g: []};
            for (final t in filtered) {
              groups[TaskGrouping.classify(completed: t.isCompleted, dueAt: t.dueAt, hasTime: t.hasTime, now: now)]!
                  .add(t);
            }
            groups[TaskGroup.completed]!.sort(
              (a, b) => (b.completedAt ?? b.updatedAt).compareTo(a.completedAt ?? a.updatedAt),
            );
            final order = [TaskGroup.today, TaskGroup.upcoming, TaskGroup.overdue, TaskGroup.completed];
            return Column(
              children: [
                AnimatedSize(
                  duration: Motion.medium,
                  curve: Motion.curve,
                  child: _searching
                      ? Padding(
                          padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xs),
                          child: SearchField(
                            controller: _search,
                            autofocus: true,
                            hint: context.tr('ค้นหางาน', 'Search tasks'),
                            onChanged: (v) => setState(() => _query = v),
                          ),
                        )
                      : const SizedBox(width: double.infinity),
                ),
                if (cat != null)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xs),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: InputChip(
                        avatar: Icon(cat.icon, size: 16, color: cat.color),
                        label: Text(cat.name),
                        selected: true,
                        onDeleted: () => setState(() => _categoryId = null),
                      ),
                    ),
                  ),
                SegmentTabs(
                  controller: _tabs,
                  labels: [
                    context.tr('วันนี้', 'Today'),
                    context.tr('ถัดไป', 'Upcoming'),
                    context.tr('เลยกำหนด', 'Overdue'),
                    context.tr('เสร็จแล้ว', 'Done'),
                  ],
                  counts: [for (final g in order) groups[g]!.length],
                  alertIndex: 2,
                ),
                Expanded(
                  child: TabBarView(
                    controller: _tabs,
                    children: [
                      for (final g in order)
                        _TaskList(
                          tasks: groups[g]!,
                          group: g,
                          hasAnyTask: all.isNotEmpty,
                          filtering: _query.isNotEmpty || _categoryId != null,
                          query: _query,
                          onToggle: _toggle,
                          onDelete: _delete,
                        ),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _TaskList extends StatelessWidget {
  const _TaskList({
    required this.tasks,
    required this.group,
    required this.hasAnyTask,
    required this.filtering,
    required this.query,
    required this.onToggle,
    required this.onDelete,
  });

  final List<TaskItem> tasks;
  final TaskGroup group;
  final bool hasAnyTask;
  final bool filtering;
  final String query;
  final void Function(TaskItem, bool) onToggle;
  final void Function(TaskItem) onDelete;

  @override
  Widget build(BuildContext context) {
    if (tasks.isEmpty) {
      if (!hasAnyTask) {
        return EmptyState(
          icon: Icons.task_alt_rounded,
          title: context.tr('ยังไม่มีงาน', 'No tasks yet'),
          message: context.tr(
            'เพิ่มสิ่งที่ต้องทำ พร้อมกำหนดส่งและการแจ้งเตือน',
            'Add to-dos with due dates and reminders',
          ),
          actionLabel: context.tr('เพิ่มงาน', 'Add task'),
          onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const TaskFormScreen())),
        );
      }
      final (IconData icon, String title, EmptyTone tone) = filtering
          ? (Icons.search_off_rounded, context.tr('ไม่พบงานที่ค้นหา', 'No matching tasks'), EmptyTone.neutral)
          : switch (group) {
              TaskGroup.today => (
                Icons.wb_sunny_outlined,
                context.tr('ไม่มีงานวันนี้', 'Nothing due today'),
                EmptyTone.success,
              ),
              TaskGroup.upcoming => (
                Icons.upcoming_outlined,
                context.tr('ไม่มีงานที่กำลังจะมาถึง', 'No upcoming tasks'),
                EmptyTone.neutral,
              ),
              TaskGroup.overdue => (
                Icons.verified_outlined,
                context.tr('ไม่มีงานที่เลยกำหนด', 'Nothing overdue'),
                EmptyTone.success,
              ),
              TaskGroup.completed => (
                Icons.done_all_rounded,
                context.tr('ยังไม่มีงานที่เสร็จ', 'No completed tasks yet'),
                EmptyTone.neutral,
              ),
            };
      return EmptyState(icon: icon, title: title, compact: true, tone: tone);
    }

    final c = context.read<AppController>();
    Widget row(TaskItem t, {bool showDate = true}) => TaskRow(
      key: ValueKey(t.id),
      task: t,
      showDate: showDate,
      highlight: query,
      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskFormScreen(task: t))),
      onToggle: (v) => onToggle(t, v),
      onDelete: () => onDelete(t),
    );

    final children = <Widget>[];
    if (group == TaskGroup.upcoming) {
      // Group upcoming tasks by date; undated last.
      final now = DateTime.now();
      final byDay = <String, List<TaskItem>>{};
      final labels = <String, String>{};
      for (final t in tasks) {
        final key = t.dueAt == null ? 'zzz' : Dates.startOfDay(t.dueAt!).toIso8601String();
        byDay.putIfAbsent(key, () => []).add(t);
        labels[key] = dateGroupLabel(context, t.dueAt, now);
      }
      final keys = byDay.keys.toList()..sort();
      for (final k in keys) {
        children.add(
          LkSectionHeader(
            title: labels[k]!,
            count: byDay[k]!.length,
            padding: const EdgeInsets.fromLTRB(Space.xs, Space.md, Space.xs, Space.sm),
          ),
        );
        children.add(LkGroup(dividerIndent: 52, children: [for (final t in byDay[k]!) row(t, showDate: false)]));
      }
    } else {
      children.add(const SizedBox(height: Space.sm));
      children.add(
        LkGroup(dividerIndent: 52, children: [for (final t in tasks) row(t, showDate: group != TaskGroup.today)]),
      );
      if (group == TaskGroup.completed) {
        children.add(
          Padding(
            padding: const EdgeInsets.only(top: Space.md),
            child: Text(
              context.tr('ปัดขวาเพื่อเปิดงานอีกครั้ง · ปัดซ้ายเพื่อลบ', 'Swipe right to reopen · left to delete'),
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.labelSmall,
            ),
          ),
        );
      }
    }
    if (group != TaskGroup.completed && c.categories.isNotEmpty) {
      children.add(
        Padding(
          padding: const EdgeInsets.only(top: Space.md),
          child: Text(
            context.tr('ปัดขวาเพื่อทำเสร็จ · ปัดซ้ายเพื่อลบ', 'Swipe right to complete · left to delete'),
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.labelSmall,
          ),
        ),
      );
    }
    return ListView(
      padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, 96),
      children: [
        FadeSlideIn(
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: children),
        ),
      ],
    );
  }
}
