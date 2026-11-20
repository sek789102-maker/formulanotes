import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/document_item.dart';
import '../../data/models/event_item.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../documents/document_detail_screen.dart';
import '../tasks/task_form_screen.dart';
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
  bool _weekMode = false;
  int _direction = 0;

  void _shift(int delta) {
    setState(() {
      _direction = delta;
      if (_weekMode) {
        _selected = DateTime(_selected.year, _selected.month, _selected.day + 7 * delta);
        _month = Dates.firstDayOfMonth(_selected);
      } else {
        _month = Dates.addMonths(_month, delta);
        final today = DateTime.now();
        _selected = (_month.year == today.year && _month.month == today.month) ? Dates.startOfDay(today) : _month;
      }
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
    // Load the visible grid plus the selected week (week mode may cross months).
    final grid = Dates.monthGrid(_month);
    final from = grid.first.isBefore(_selected) ? grid.first : _selected.subtract(const Duration(days: 7));
    final to = Dates.endOfDay(grid.last.isAfter(_selected) ? grid.last : _selected.add(const Duration(days: 7)));
    return _MonthData(
      await c.s.events.between(from, to),
      await c.s.tasks.dueBetween(from, to),
      await c.s.documents.expiringBetween(from, to),
    );
  }

  void _addMenu() async {
    final r = await showOptionSheet<int>(
      context,
      title: Fmt.fullDate(_selected),
      options: [
        SheetOption(value: 0, icon: Icons.event_available_outlined, label: context.tr('เพิ่มกิจกรรม', 'Add event')),
        SheetOption(value: 1, icon: Icons.add_task_rounded, label: context.tr('เพิ่มงาน', 'Add task')),
      ],
    );
    if (!mounted || r == null) return;
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => r == 0 ? EventFormScreen(initialDate: _selected) : TaskFormScreen(initialDate: _selected),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final isToday = Dates.isSameDay(_selected, DateTime.now());
    return Scaffold(
      appBar: AppBar(
        title: Text(context.tr('ปฏิทิน', 'Calendar')),
        actions: [
          if (!isToday || _month != Dates.firstDayOfMonth(DateTime.now()))
            TextButton.icon(
              onPressed: _goToday,
              icon: const Icon(Icons.today_rounded, size: 18),
              label: Text(context.tr('วันนี้', 'Today')),
            ),
          const SizedBox(width: Space.sm),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'fab_cal',
        tooltip: context.tr('เพิ่ม', 'Add'),
        onPressed: _addMenu,
        child: const Icon(Icons.add_rounded),
      ),
      body: ContentWidth(
        child: DataBuilder<_MonthData>(
          deps: [_month, _weekMode, _weekMode ? _selected : null],
          load: _load,
          builder: (context, data) {
            final events = data.eventsOn(_selected);
            final tasks = data.tasksOn(_selected);
            final docs = data.docsOn(_selected);
            final total = events.length + tasks.length + docs.length;
            final headerDate = _weekMode ? _selected : _month;
            return ListView(
              padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, 96),
              children: [
                LkPanel(
                  padding: const EdgeInsets.fromLTRB(Space.sm, Space.xs, Space.sm, Space.sm),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          IconButton(
                            tooltip: _weekMode
                                ? context.tr('สัปดาห์ก่อน', 'Previous week')
                                : context.tr('เดือนก่อน', 'Previous month'),
                            icon: const Icon(Icons.chevron_left_rounded),
                            onPressed: () => _shift(-1),
                          ),
                          Expanded(
                            child: AnimatedSwitcher(
                              duration: Motion.fast,
                              child: Text(
                                Fmt.monthYear(headerDate),
                                key: ValueKey(Fmt.monthYear(headerDate)),
                                textAlign: TextAlign.center,
                                style: t.titleMedium,
                              ),
                            ),
                          ),
                          IconButton(
                            tooltip: _weekMode
                                ? context.tr('สัปดาห์ถัดไป', 'Next week')
                                : context.tr('เดือนถัดไป', 'Next month'),
                            icon: const Icon(Icons.chevron_right_rounded),
                            onPressed: () => _shift(1),
                          ),
                        ],
                      ),
                      Row(
                        children: [
                          for (final wd in const [7, 1, 2, 3, 4, 5, 6])
                            Expanded(
                              child: Center(
                                child: Text(
                                  Fmt.weekdayShort(wd),
                                  style: t.labelSmall?.copyWith(
                                    color: wd == 7
                                        ? context.status.danger.withValues(alpha: 0.8)
                                        : scheme.onSurfaceVariant,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: Space.xs),
                      GestureDetector(
                        onHorizontalDragEnd: (d) {
                          final v = d.primaryVelocity ?? 0;
                          if (v < -200) _shift(1);
                          if (v > 200) _shift(-1);
                        },
                        onVerticalDragEnd: (d) {
                          final v = d.primaryVelocity ?? 0;
                          if (v < -250 && !_weekMode) setState(() => _weekMode = true);
                          if (v > 250 && _weekMode) setState(() => _weekMode = false);
                        },
                        child: AnimatedSize(
                          duration: Motion.medium,
                          curve: Motion.curve,
                          alignment: Alignment.topCenter,
                          child: AnimatedSwitcher(
                            duration: Motion.medium,
                            transitionBuilder: (child, anim) => FadeTransition(
                              opacity: anim,
                              child: SlideTransition(
                                position: Tween(begin: Offset(0.05 * _direction, 0), end: Offset.zero).animate(anim),
                                child: child,
                              ),
                            ),
                            child: _Grid(
                              key: ValueKey(
                                '${_month}_${_weekMode ? Dates.startOfDay(_selected).subtract(Duration(days: _selected.weekday % 7)) : ''}',
                              ),
                              month: _month,
                              selected: _selected,
                              weekMode: _weekMode,
                              data: data,
                              onSelect: (d) => setState(() {
                                _selected = d;
                                if (d.month != _month.month || d.year != _month.year) {
                                  _direction = d.isAfter(_month) ? 1 : -1;
                                  _month = Dates.firstDayOfMonth(d);
                                }
                              }),
                            ),
                          ),
                        ),
                      ),
                      // Month / week toggle handle
                      InkWell(
                        borderRadius: Radii.brSm,
                        onTap: () => setState(() => _weekMode = !_weekMode),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: Space.xs, horizontal: Space.md),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                _weekMode ? Icons.expand_more_rounded : Icons.expand_less_rounded,
                                size: 18,
                                color: scheme.onSurfaceVariant,
                              ),
                              const SizedBox(width: 4),
                              Text(
                                _weekMode
                                    ? context.tr('ดูทั้งเดือน', 'Show month')
                                    : context.tr('ดูเฉพาะสัปดาห์', 'Show week'),
                                style: t.labelSmall,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: Space.md),
                const _Legend(),
                Padding(
                  padding: const EdgeInsets.fromLTRB(Space.xs, Space.lg, Space.xs, Space.sm),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              Fmt.fullDate(_selected),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: t.titleSmall,
                            ),
                            Text(
                              total == 0
                                  ? context.tr('ไม่มีรายการ', 'Nothing scheduled')
                                  : context.tr('$total รายการ', '$total items'),
                              style: t.bodySmall,
                            ),
                          ],
                        ),
                      ),
                      if (isToday)
                        Pill(
                          label: context.tr('วันนี้', 'Today'),
                          color: scheme.primary,
                          background: scheme.primaryContainer,
                        ),
                    ],
                  ),
                ),
                if (total == 0)
                  LkPanel(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => EventFormScreen(initialDate: _selected)),
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: Space.lg),
                    child: Row(
                      children: [
                        IconBadge(icon: Icons.add_rounded, color: scheme.primary, size: 32),
                        const SizedBox(width: Space.md),
                        Expanded(
                          child: Text(
                            context.tr('เพิ่มกิจกรรมในวันนี้', 'Add an event on this day'),
                            style: t.bodyMedium,
                          ),
                        ),
                      ],
                    ),
                  )
                else
                  FadeSlideIn(
                    key: ValueKey(_selected),
                    child: Column(
                      children: [
                        if (events.isNotEmpty)
                          LkGroup(
                            dividerIndent: 80,
                            children: [
                              for (final e in events)
                                EventRow(
                                  event: e,
                                  showDate: false,
                                  onTap: () => Navigator.push(
                                    context,
                                    MaterialPageRoute(builder: (_) => EventFormScreen(event: e)),
                                  ),
                                ),
                            ],
                          ),
                        if (tasks.isNotEmpty) ...[
                          LkSectionHeader(
                            title: context.tr('งาน', 'Tasks'),
                            count: tasks.length,
                            padding: const EdgeInsets.fromLTRB(Space.xs, Space.md, Space.xs, Space.sm),
                          ),
                          LkGroup(
                            dividerIndent: 52,
                            children: [
                              for (final tk in tasks)
                                TaskRow(
                                  task: tk,
                                  showDate: false,
                                  onTap: () => Navigator.push(
                                    context,
                                    MaterialPageRoute(builder: (_) => TaskFormScreen(task: tk)),
                                  ),
                                  onToggle: (v) =>
                                      runAction(context, () => context.read<AppController>().setTaskCompleted(tk, v)),
                                ),
                            ],
                          ),
                        ],
                        if (docs.isNotEmpty) ...[
                          LkSectionHeader(
                            title: context.tr('เอกสารหมดอายุ', 'Documents expiring'),
                            count: docs.length,
                            padding: const EdgeInsets.fromLTRB(Space.xs, Space.md, Space.xs, Space.sm),
                          ),
                          LkGroup(
                            dividerIndent: 72,
                            children: [
                              for (final d in docs)
                                DocumentRow(
                                  doc: d,
                                  onTap: () => Navigator.push(
                                    context,
                                    MaterialPageRoute(builder: (_) => DocumentDetailScreen(documentId: d.id)),
                                  ),
                                ),
                            ],
                          ),
                        ],
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

class _Grid extends StatelessWidget {
  const _Grid({
    super.key,
    required this.month,
    required this.selected,
    required this.weekMode,
    required this.data,
    required this.onSelect,
  });
  final DateTime month;
  final DateTime selected;
  final bool weekMode;
  final _MonthData data;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    List<DateTime> days;
    if (weekMode) {
      final start = DateTime(selected.year, selected.month, selected.day - selected.weekday % 7);
      days = List.generate(7, (i) => DateTime(start.year, start.month, start.day + i));
    } else {
      days = Dates.monthGrid(month);
      if (days.sublist(35).every((d) => d.month != month.month)) days = days.sublist(0, 35);
    }
    final weeks = days.length ~/ 7;
    return Column(
      children: [
        for (var w = 0; w < weeks; w++)
          Row(
            children: [
              for (final d in days.sublist(w * 7, w * 7 + 7))
                Expanded(
                  child: _DayCell(
                    day: d,
                    inMonth: weekMode || d.month == month.month,
                    isToday: Dates.isSameDay(d, today),
                    isSelected: Dates.isSameDay(d, selected),
                    hasEvent: data.events.any((e) => e.occursOn(d)),
                    hasTask: data.tasks.any((t) => !t.isCompleted && Dates.isSameDay(t.dueAt!, d)),
                    hasDoc: data.docs.any((x) => Dates.isSameDay(x.expiryDate!, d)),
                    onTap: () => onSelect(d),
                  ),
                ),
            ],
          ),
      ],
    );
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
  });

  final DateTime day;
  final bool inMonth, isToday, isSelected, hasEvent, hasTask, hasDoc;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final status = context.status;
    final Color bg = isSelected ? scheme.primary : (isToday ? scheme.primaryContainer : Colors.transparent);
    final Color fg = isSelected
        ? scheme.onPrimary
        : (!inMonth
              ? scheme.onSurfaceVariant.withValues(alpha: 0.4)
              : (isToday ? scheme.onPrimaryContainer : scheme.onSurface));
    Widget dot(Color c) => Container(
      width: 5,
      height: 5,
      margin: const EdgeInsets.symmetric(horizontal: 1.2),
      decoration: BoxDecoration(
        color: isSelected ? scheme.onPrimary : (inMonth ? c : c.withValues(alpha: 0.4)),
        shape: BoxShape.circle,
      ),
    );
    return Semantics(
      button: true,
      selected: isSelected,
      label: Fmt.fullDate(day),
      child: Padding(
        padding: const EdgeInsets.all(2),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: onTap,
            borderRadius: Radii.brSm,
            child: AnimatedContainer(
              duration: Motion.fast,
              // Min height (not fixed) so cells grow with large system text.
              constraints: const BoxConstraints(minHeight: 46),
              padding: const EdgeInsets.symmetric(vertical: 5),
              decoration: BoxDecoration(color: bg, borderRadius: Radii.brSm),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    '${day.day}',
                    style: TextStyle(
                      fontFamily: AppTheme.fontFamily,
                      color: fg,
                      fontWeight: isToday || isSelected ? FontWeight.w700 : FontWeight.w500,
                      fontSize: 14.5,
                      height: 1.3,
                    ),
                  ),
                  const SizedBox(height: 2),
                  SizedBox(
                    height: 5,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        if (hasEvent) dot(scheme.primary),
                        if (hasTask) dot(status.warning),
                        if (hasDoc) dot(status.danger),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final s = context.status;
    final t = Theme.of(context).textTheme;
    Widget item(Color c, String label) => Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        StatusDot(color: c, size: 7),
        const SizedBox(width: 5),
        Text(label, style: t.labelSmall),
      ],
    );
    return Center(
      child: Wrap(
        spacing: Space.lg,
        runSpacing: 4,
        children: [
          item(scheme.primary, context.tr('กิจกรรม', 'Event')),
          item(s.warning, context.tr('งาน', 'Task')),
          item(s.danger, context.tr('เอกสารหมดอายุ', 'Document expiry')),
        ],
      ),
    );
  }
}
