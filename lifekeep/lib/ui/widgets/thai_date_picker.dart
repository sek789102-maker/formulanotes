import 'package:flutter/material.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../design/design.dart';

/// Date picker that shows Thai months and Buddhist Era years (พ.ศ.), in the
/// app's blue style. In English it shows Gregorian years.
Future<DateTime?> showThaiDatePicker(
  BuildContext context, {
  required DateTime initial,
  required DateTime first,
  required DateTime last,
}) {
  return showDialog<DateTime>(
    context: context,
    builder: (_) => _ThaiDatePicker(initial: Dates.startOfDay(initial), first: first, last: last),
  );
}

class _ThaiDatePicker extends StatefulWidget {
  const _ThaiDatePicker({required this.initial, required this.first, required this.last});
  final DateTime initial;
  final DateTime first;
  final DateTime last;

  @override
  State<_ThaiDatePicker> createState() => _ThaiDatePickerState();
}

class _ThaiDatePickerState extends State<_ThaiDatePicker> {
  late DateTime _selected = widget.initial;
  late DateTime _month = Dates.firstDayOfMonth(widget.initial);
  bool _years = false;

  bool _inRange(DateTime d) =>
      !d.isBefore(Dates.startOfDay(widget.first)) && !d.isAfter(Dates.startOfDay(widget.last));

  void _select(DateTime d) {
    if (!_inRange(d)) return;
    setState(() {
      _selected = d;
      _month = Dates.firstDayOfMonth(d);
    });
  }

  void _shiftMonth(int by) {
    final m = Dates.addMonths(_month, by);
    if (Dates.addMonths(m, 1).isBefore(widget.first) || m.isAfter(widget.last)) return;
    setState(() => _month = m);
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final today = Dates.startOfDay(DateTime.now());
    return Dialog(
      insetPadding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: Space.xl),
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Radii.xl)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        // Scrolls on short screens / large text instead of overflowing.
        child: SingleChildScrollView(
          child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Header: chosen date + quick picks
            Container(
              decoration: const BoxDecoration(gradient: lkGradient),
              padding: const EdgeInsets.fromLTRB(Space.xl, Space.lg, Space.xl, Space.md),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    context.tr('เลือกวันที่', 'Select date'),
                    style: t.labelLarge?.copyWith(color: Colors.white.withValues(alpha: 0.85)),
                  ),
                  const SizedBox(height: Space.xs),
                  FittedBox(
                    fit: BoxFit.scaleDown,
                    alignment: Alignment.centerLeft,
                    child: Text(
                      '${Fmt.weekdayShort(_selected.weekday)}. ${Fmt.date(_selected)}',
                      key: const Key('thai_picker_selected'),
                      style: t.headlineMedium?.copyWith(color: Colors.white, fontWeight: FontWeight.w800),
                    ),
                  ),
                  const SizedBox(height: Space.sm),
                  Wrap(
                    spacing: Space.sm,
                    runSpacing: Space.xs,
                    children: [
                      _QuickChip(label: context.tr('วันนี้', 'Today'), onTap: () => _select(today)),
                      _QuickChip(label: context.tr('+6 เดือน', '+6 months'), onTap: () => _select(Dates.addMonths(today, 6))),
                      _QuickChip(label: context.tr('+1 ปี', '+1 year'), onTap: () => _select(Dates.addMonths(today, 12))),
                    ],
                  ),
                ],
              ),
            ),
            // Month bar
            Padding(
              padding: const EdgeInsets.fromLTRB(Space.md, Space.sm, Space.sm, 0),
              child: Row(
                children: [
                  TextButton.icon(
                    key: const Key('thai_picker_month'),
                    onPressed: () => setState(() => _years = !_years),
                    iconAlignment: IconAlignment.end,
                    icon: Icon(_years ? Icons.arrow_drop_up_rounded : Icons.arrow_drop_down_rounded),
                    label: Text(Fmt.monthYear(_month), style: t.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
                  ),
                  const Spacer(),
                  if (!_years) ...[
                    IconButton(
                      tooltip: context.tr('เดือนก่อน', 'Previous month'),
                      onPressed: () => _shiftMonth(-1),
                      icon: const Icon(Icons.chevron_left_rounded),
                    ),
                    IconButton(
                      tooltip: context.tr('เดือนถัดไป', 'Next month'),
                      onPressed: () => _shiftMonth(1),
                      icon: const Icon(Icons.chevron_right_rounded),
                    ),
                  ],
                ],
              ),
            ),
            SizedBox(
              height: 7 * 44.0,
              child: _years ? _yearGrid(context) : _dayGrid(context, today),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(Space.md, 0, Space.md, Space.md),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    key: const Key('thai_picker_cancel'),
                    onPressed: () => Navigator.pop(context),
                    child: Text(context.tr('ยกเลิก', 'Cancel')),
                  ),
                  const SizedBox(width: Space.sm),
                  FilledButton(
                    key: const Key('thai_picker_ok'),
                    onPressed: () => Navigator.pop(context, _selected),
                    child: Text(context.tr('ตกลง', 'OK')),
                  ),
                ],
              ),
            ),
          ],
        ),
        ),
      ),
    );
  }

  Widget _dayGrid(BuildContext context, DateTime today) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final days = Dates.monthGrid(_month);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Space.md),
      child: Column(
        children: [
          SizedBox(
            height: 36,
            child: Row(
              children: [
                for (final wd in const [7, 1, 2, 3, 4, 5, 6])
                  Expanded(
                    child: Center(
                      child: Text(
                        Fmt.weekdayShort(wd),
                        style: t.labelMedium?.copyWith(color: wd == 7 ? context.status.danger : scheme.onSurfaceVariant),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          for (var w = 0; w < 6; w++)
            Expanded(
              child: Row(
                children: [
                  for (final d in days.sublist(w * 7, w * 7 + 7))
                    Expanded(child: _day(context, d, today)),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _day(BuildContext context, DateTime d, DateTime today) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final inMonth = d.month == _month.month;
    final selected = Dates.isSameDay(d, _selected);
    final isToday = Dates.isSameDay(d, today);
    final enabled = _inRange(d);
    final color = selected
        ? Colors.white
        : !enabled
            ? scheme.onSurfaceVariant.withValues(alpha: 0.3)
            : inMonth
                ? scheme.onSurface
                : scheme.onSurfaceVariant.withValues(alpha: 0.45);
    return Center(
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: enabled ? () => _select(d) : null,
        child: Container(
          width: 40,
          height: 40,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: selected ? lkGradient : null,
            border: isToday && !selected ? Border.all(color: scheme.primary, width: 1.5) : null,
          ),
          child: Text(
            '${d.day}',
            style: t.bodyLarge?.copyWith(color: color, fontWeight: selected || isToday ? FontWeight.w700 : FontWeight.w500),
          ),
        ),
      ),
    );
  }

  Widget _yearGrid(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final years = [for (var y = widget.first.year; y <= widget.last.year; y++) y];
    final row = (years.indexOf(_month.year) ~/ 3 - 2).clamp(0, 1 << 20);
    return GridView.builder(
      controller: ScrollController(initialScrollOffset: row * 56.0),
      padding: const EdgeInsets.symmetric(horizontal: Space.lg, vertical: Space.sm),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, mainAxisExtent: 56),
      itemCount: years.length,
      itemBuilder: (context, i) {
        final y = years[i];
        final on = y == _month.year;
        final now = y == DateTime.now().year;
        return Padding(
          padding: const EdgeInsets.all(6),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              borderRadius: BorderRadius.circular(Radii.pill),
              onTap: () => setState(() {
                final day = _selected.day.clamp(1, Dates.daysInMonth(y, _selected.month));
                final d = DateTime(y, _selected.month, day);
                _month = DateTime(y, _month.month);
                if (_inRange(d)) _selected = d;
                _years = false;
              }),
              child: Ink(
                decoration: BoxDecoration(
                  gradient: on ? lkGradient : null,
                  borderRadius: BorderRadius.circular(Radii.pill),
                  border: now && !on ? Border.all(color: scheme.primary, width: 1.5) : null,
                ),
                child: Center(
                  child: Text(
                    Fmt.year(y),
                    style: t.titleMedium?.copyWith(color: on ? Colors.white : scheme.onSurface, fontWeight: on ? FontWeight.w700 : FontWeight.w500),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

class _QuickChip extends StatelessWidget {
  const _QuickChip({required this.label, required this.onTap});
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Material(
    color: Colors.white.withValues(alpha: 0.18),
    shape: const StadiumBorder(),
    child: InkWell(
      customBorder: const StadiumBorder(),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Space.md, vertical: 6),
        child: Text(label, style: Theme.of(context).textTheme.labelMedium?.copyWith(color: Colors.white)),
      ),
    ),
  );
}
