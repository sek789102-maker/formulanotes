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
import '../documents/document_detail_screen.dart';
import '../tasks/task_form_screen.dart';
import '../widgets/common.dart';
import '../widgets/item_tiles.dart';
import 'event_form_screen.dart';

class _MonthData {
  _MonthData(this.events, this.tasks, this.docs);
  final List<EventItem> events;
  final List<TaskItem> tasks;
  final List<DocumentItem> docs;

  List<EventItem> eventsOn(DateTime d) => events.where((e) => e.occursOn(d)).toList();
  List<TaskItem> tasksOn(DateTime d) => tasks.where((t) => Dates.isSameDay(t.dueAt!, d)).toList();
  List<DocumentItem> docsOn(DateTime d) => docs.where((x) => Dates.isSameDay(x.expiryDate!, d)).toList();
}

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  DateTime _month = Dates.firstDayOfMonth(DateTime.now());
  DateTime _selected = Dates.startOfDay(DateTime.now());
  int _direction = 0;

  void _changeMonth(int delta) {
    setState(() {
      _direction = delta;
      _month = Dates.addMonths(_month, delta);
      final today = DateTime.now();
      _selected = (_month.year == today.year && _month.month == today.month) ? Dates.startOfDay(today) : _month;
    });
  }

  void _goToday() {
    final now = DateTime.now();
    setState(() {
      _direction = 0;
      _month = Dates.firstDayOfMonth(now);
      _selected = Dates.startOfDay(now);
    });
  }

  Future<_MonthData> _load(AppController c) async {
    final grid = Dates.monthGrid(_month);
    final from = grid.first;
    final to = Dates.endOfDay(grid.last);
    return _MonthData(
      await c.s.events.between(from, to),
      await c.s.tasks.dueBetween(from, to),
      await c.s.documents.expiringBetween(from, to),
    );
  }

  void _addMenu() {
    showModalBottomSheet<void>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 8),
            child: Text(Fmt.fullDate(_selected), style: Theme.of(ctx).textTheme.titleMedium),
          ),
          ListTile(
            leading: const Icon(Icons.event_available_outlined),
            title: Text(ctx.tr('เพิ่มกิจกรรม', 'Add event')),
            onTap: () {
              Navigator.pop(ctx);
              Navigator.push(context, MaterialPageRoute(builder: (_) => EventFormScreen(initialDate: _selected)));
            },
          ),
          ListTile(
            leading: const Icon(Icons.add_task_rounded),
            title: Text(ctx.tr('เพิ่มงาน', 'Add task')),
            onTap: () {
              Navigator.pop(ctx);
              Navigator.push(context, MaterialPageRoute(builder: (_) => TaskFormScreen(initialDate: _selected)));
            },
          ),
          const SizedBox(height: 8),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        title: Text(context.tr('ปฏิทิน', 'Calendar')),
        actions: [
          TextButton.icon(
            onPressed: _goToday,
            icon: const Icon(Icons.today_rounded, size: 18),
            label: Text(context.tr('วันนี้', 'Today')),
          ),
          const SizedBox(width: 8),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'fab_cal',
        tooltip: context.tr('เพิ่ม', 'Add'),
        onPressed: _addMenu,
        child: const Icon(Icons.add_rounded),
      ),
      body: DataBuilder<_MonthData>(
        deps: [_month],
        load: _load,
        builder: (context, data) {
          final events = data.eventsOn(_selected);
          final tasks = data.tasksOn(_selected);
          final docs = data.docsOn(_selected);
          final empty = events.isEmpty && tasks.isEmpty && docs.isEmpty;
          return CustomScrollView(slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(12, 0, 12, 0),
              sliver: SliverToBoxAdapter(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(8, 8, 8, 12),
                    child: Column(children: [
                      Row(children: [
                        IconButton(
                          tooltip: context.tr('เดือนก่อน', 'Previous month'),
                          icon: const Icon(Icons.chevron_left_rounded),
                          onPressed: () => _changeMonth(-1),
                        ),
                        Expanded(
                          child: Text(Fmt.monthYear(_month), textAlign: TextAlign.center, style: text.titleMedium),
                        ),
                        IconButton(
                          tooltip: context.tr('เดือนถัดไป', 'Next month'),
                          icon: const Icon(Icons.chevron_right_rounded),
                          onPressed: () => _changeMonth(1),
                        ),
                      ]),
                      const SizedBox(height: 4),
                      Row(children: [
                        for (final wd in const [7, 1, 2, 3, 4, 5, 6])
                          Expanded(
                            child: Center(
                              child: Text(Fmt.weekdayShort(wd),
                                  style: text.labelSmall?.copyWith(
                                      color: wd == 7 ? context.status.danger : scheme.onSurfaceVariant,
                                      fontWeight: FontWeight.w600)),
                            ),
                          ),
                      ]),
                      const SizedBox(height: 6),
                      GestureDetector(
                        onHorizontalDragEnd: (d) {
                          final v = d.primaryVelocity ?? 0;
                          if (v < -200) _changeMonth(1);
                          if (v > 200) _changeMonth(-1);
                        },
                        child: AnimatedSwitcher(
                          duration: const Duration(milliseconds: 220),
                          transitionBuilder: (child, anim) => FadeTransition(
                            opacity: anim,
                            child: SlideTransition(
                              position: Tween(begin: Offset(0.04 * _direction, 0), end: Offset.zero).animate(anim),
                              child: child,
                            ),
                          ),
                          child: _MonthGrid(
                            key: ValueKey(_month),
                            month: _month,
                            selected: _selected,
                            data: data,
                            onSelect: (d) => setState(() => _selected = d),
                          ),
                        ),
                      ),
                    ]),
                  ),
                ),
              ),
            ),
            const SliverPadding(padding: EdgeInsets.fromLTRB(20, 10, 20, 0), sliver: SliverToBoxAdapter(child: _Legend())),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
              sliver: SliverToBoxAdapter(child: Text(Fmt.fullDate(_selected), style: text.titleMedium)),
            ),
            if (empty)
              SliverToBoxAdapter(
                child: EmptyState(
                  icon: Icons.event_note_outlined,
                  title: context.tr('ไม่มีรายการในวันนี้', 'Nothing on this day'),
                  actionLabel: context.tr('เพิ่มกิจกรรม', 'Add event'),
                  onAction: () => Navigator.push(
                      context, MaterialPageRoute(builder: (_) => EventFormScreen(initialDate: _selected))),
                  compact: true,
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 96),
                sliver: SliverList.list(children: [
                  for (final e in events)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: EventTile(
                        event: e,
                        showDate: false,
                        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => EventFormScreen(event: e))),
                      ),
                    ),
                  for (final t in tasks)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: TaskTile(
                        task: t,
                        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskFormScreen(task: t))),
                        onToggle: (v) => runAction(context, () => context.read<AppController>().setTaskCompleted(t, v)),
                      ),
                    ),
                  for (final d in docs)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: DocumentCard(
                        doc: d,
                        onTap: () => Navigator.push(
                            context, MaterialPageRoute(builder: (_) => DocumentDetailScreen(documentId: d.id))),
                      ),
                    ),
                ]),
              ),
          ]);
        },
      ),
    );
  }
}

class _MonthGrid extends StatelessWidget {
  const _MonthGrid({super.key, required this.month, required this.selected, required this.data, required this.onSelect});
  final DateTime month;
  final DateTime selected;
  final _MonthData data;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final days = Dates.monthGrid(month);
    final today = DateTime.now();
    // Drop the 6th week when it belongs entirely to the next month.
    final weeks = days.sublist(35).every((d) => d.month != month.month) ? 5 : 6;
    return Column(children: [
      for (var w = 0; w < weeks; w++)
        Row(children: [
          for (final d in days.sublist(w * 7, w * 7 + 7))
            Expanded(
              child: _DayCell(
                day: d,
                inMonth: d.month == month.month,
                isToday: Dates.isSameDay(d, today),
                isSelected: Dates.isSameDay(d, selected),
                hasEvent: data.events.any((e) => e.occursOn(d)),
                hasTask: data.tasks.any((t) => !t.isCompleted && Dates.isSameDay(t.dueAt!, d)),
                hasDoc: data.docs.any((x) => Dates.isSameDay(x.expiryDate!, d)),
                onTap: () => onSelect(d),
                scheme: scheme,
              ),
            ),
        ]),
    ]);
  }
}

class _DayCell extends StatelessWidget {
  const _DayCell({
    required this.day,
    required this.inMonth,
    required this.isToday,
    required this.isSelected,
    required this.hasEvent,
    required this.hasTask,
    required this.hasDoc,
    required this.onTap,
    required this.scheme,
  });

  final DateTime day;
  final bool inMonth, isToday, isSelected, hasEvent, hasTask, hasDoc;
  final VoidCallback onTap;
  final ColorScheme scheme;

  @override
  Widget build(BuildContext context) {
    final status = context.status;
    final fg = isSelected
        ? scheme.onPrimary
        : (!inMonth ? scheme.onSurfaceVariant.withValues(alpha: 0.45) : (isToday ? scheme.primary : scheme.onSurface));
    return Semantics(
      button: true,
      selected: isSelected,
      label: Fmt.fullDate(day),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: SizedBox(
          height: 48,
          child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 34,
              height: 34,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: isSelected ? scheme.primary : Colors.transparent,
                shape: BoxShape.circle,
                border: isToday && !isSelected ? Border.all(color: scheme.primary, width: 1.5) : null,
              ),
              child: Text(
                '${day.day}',
                style: TextStyle(
                  color: fg,
                  fontWeight: isToday || isSelected ? FontWeight.w700 : FontWeight.w500,
                  fontSize: 14,
                ),
              ),
            ),
            const SizedBox(height: 3),
            SizedBox(
              height: 5,
              child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                if (hasEvent) _dot(scheme.primary),
                if (hasTask) _dot(status.warning),
                if (hasDoc) _dot(status.danger),
              ]),
            ),
          ]),
        ),
      ),
    );
  }

  Widget _dot(Color c) => Container(
        width: 5,
        height: 5,
        margin: const EdgeInsets.symmetric(horizontal: 1.5),
        decoration: BoxDecoration(color: inMonth ? c : c.withValues(alpha: 0.4), shape: BoxShape.circle),
      );
}

class _Legend extends StatelessWidget {
  const _Legend();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final s = context.status;
    Widget item(Color c, String label) => Row(mainAxisSize: MainAxisSize.min, children: [
          Container(width: 8, height: 8, decoration: BoxDecoration(color: c, shape: BoxShape.circle)),
          const SizedBox(width: 6),
          Text(label, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: scheme.onSurfaceVariant)),
        ]);
    return Wrap(spacing: 16, runSpacing: 4, children: [
      item(scheme.primary, context.tr('กิจกรรม', 'Event')),
      item(s.warning, context.tr('งาน', 'Task')),
      item(s.danger, context.tr('เอกสารหมดอายุ', 'Document expiry')),
    ]);
  }
}
