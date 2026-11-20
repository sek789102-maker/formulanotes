import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/validators.dart';
import '../../data/models/category.dart';
import '../../data/models/reminder_config.dart';
import '../../state/app_controller.dart';

class FormSection extends StatelessWidget {
  const FormSection({super.key, required this.title, required this.child, this.icon});
  final String title;
  final Widget child;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(top: 20),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Row(children: [
            if (icon != null) ...[Icon(icon, size: 18, color: scheme.onSurfaceVariant), const SizedBox(width: 6)],
            Text(title, style: Theme.of(context).textTheme.labelLarge?.copyWith(color: scheme.onSurfaceVariant)),
          ]),
        ),
        child,
      ]),
    );
  }
}

Future<DateTime?> pickDate(BuildContext context, {DateTime? initial, DateTime? first, DateTime? last}) {
  final now = DateTime.now();
  final init = initial ?? now;
  final f = first ?? DateTime(now.year - 50);
  final l = last ?? DateTime(now.year + 50);
  return showDatePicker(
    context: context,
    initialDate: init.isBefore(f) ? f : (init.isAfter(l) ? l : init),
    firstDate: f,
    lastDate: l,
  );
}

Future<TimeOfDay?> pickTime(BuildContext context, {TimeOfDay? initial}) {
  return showTimePicker(
    context: context,
    initialTime: initial ?? const TimeOfDay(hour: 9, minute: 0),
    builder: (context, child) => MediaQuery(
      data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
      child: child!,
    ),
  );
}

/// Tappable field showing a date (and optionally a clear button).
class PickerTile extends StatelessWidget {
  const PickerTile({
    super.key,
    required this.icon,
    required this.label,
    required this.value,
    required this.onTap,
    this.onClear,
  });

  final IconData icon;
  final String label;
  final String? value;
  final VoidCallback onTap;
  final VoidCallback? onClear;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: scheme.outlineVariant),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(children: [
            Icon(icon, color: scheme.onSurfaceVariant, size: 20),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(label, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: scheme.onSurfaceVariant)),
                const SizedBox(height: 2),
                Text(
                  value ?? context.tr('ไม่ระบุ', 'Not set'),
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: value == null ? scheme.onSurfaceVariant : scheme.onSurface,
                      ),
                ),
              ]),
            ),
            if (value != null && onClear != null)
              IconButton(
                tooltip: context.tr('ล้าง', 'Clear'),
                icon: const Icon(Icons.close_rounded, size: 20),
                onPressed: onClear,
              )
            else
              Icon(Icons.chevron_right_rounded, color: scheme.onSurfaceVariant),
          ]),
        ),
      ),
    );
  }
}

/// Lets the user pick reminder presets (1/3/7 days, at time) or a custom
/// offset. Disabled with an explanation when there is no date to anchor to.
class ReminderPicker extends StatelessWidget {
  const ReminderPicker({
    super.key,
    required this.config,
    required this.onChanged,
    required this.enabled,
    this.forDocument = false,
    this.disabledHint,
  });

  final ReminderConfig config;
  final ValueChanged<ReminderConfig> onChanged;
  final bool enabled;
  final bool forDocument;
  final String? disabledHint;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final presets = forDocument ? ReminderConfig.presetsDocument : ReminderConfig.presetsItem;
    final custom = config.offsetsMinutes.where((o) => !presets.contains(o)).toList();
    final prefs = context.watch<AppController>().reminderPrefs;

    if (!enabled) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: scheme.surfaceContainerHigh.withValues(alpha: 0.6),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(children: [
          Icon(Icons.info_outline_rounded, size: 18, color: scheme.onSurfaceVariant),
          const SizedBox(width: 10),
          Expanded(
            child: Text(disabledHint ?? context.tr('กำหนดวันที่ก่อนเพื่อตั้งการแจ้งเตือน', 'Set a date first to add reminders'),
                style: TextStyle(color: scheme.onSurfaceVariant)),
          ),
        ]),
      );
    }

    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Wrap(spacing: 8, runSpacing: 8, children: [
        for (final p in presets)
          FilterChip(
            label: Text(Fmt.reminderOffset(p)),
            selected: config.offsetsMinutes.contains(p),
            onSelected: (_) => onChanged(config.toggle(p)),
          ),
        for (final cOff in custom)
          InputChip(
            label: Text(Fmt.reminderOffset(cOff)),
            selected: true,
            onDeleted: () => onChanged(config.toggle(cOff)),
            onPressed: () => onChanged(config.toggle(cOff)),
          ),
        ActionChip(
          avatar: const Icon(Icons.add_rounded, size: 18),
          label: Text(context.tr('กำหนดเอง', 'Custom')),
          onPressed: () async {
            final v = await showDialog<int>(
              context: context,
              builder: (_) => _CustomReminderDialog(daysOnly: forDocument),
            );
            if (v != null && !config.offsetsMinutes.contains(v)) onChanged(config.toggle(v));
          },
        ),
      ]),
      if (forDocument && config.isEnabled)
        Padding(
          padding: const EdgeInsets.only(top: 8, left: 4),
          child: Text(
            context.tr(
              'แจ้งเตือนเวลา ${prefs.hour.toString().padLeft(2, '0')}:${prefs.minute.toString().padLeft(2, '0')} น. (เปลี่ยนได้ในตั้งค่า)',
              'Reminds at ${prefs.hour.toString().padLeft(2, '0')}:${prefs.minute.toString().padLeft(2, '0')} (change in Settings)',
            ),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ),
    ]);
  }
}

class _CustomReminderDialog extends StatefulWidget {
  const _CustomReminderDialog({required this.daysOnly});
  final bool daysOnly;

  @override
  State<_CustomReminderDialog> createState() => _CustomReminderDialogState();
}

class _CustomReminderDialogState extends State<_CustomReminderDialog> {
  final _ctrl = TextEditingController();
  final _form = GlobalKey<FormState>();
  late int _unit = widget.daysOnly ? ReminderCalc.day : ReminderCalc.hour;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final units = <int, String>{
      if (!widget.daysOnly) ReminderCalc.minute: context.tr('นาที', 'minutes'),
      if (!widget.daysOnly) ReminderCalc.hour: context.tr('ชั่วโมง', 'hours'),
      ReminderCalc.day: context.tr('วัน', 'days'),
    };
    return AlertDialog(
      title: Text(context.tr('แจ้งเตือนล่วงหน้า', 'Remind before')),
      content: Form(
        key: _form,
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            child: TextFormField(
              controller: _ctrl,
              autofocus: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(6)],
              decoration: InputDecoration(labelText: context.tr('จำนวน', 'Amount')),
              validator: (v) => Validators.customReminder(v, maxMinutes: ReminderCalc.maxOffset, unitMinutes: _unit),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: DropdownButtonFormField<int>(
              initialValue: _unit,
              items: [for (final e in units.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
              onChanged: (v) => setState(() => _unit = v ?? _unit),
            ),
          ),
        ]),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ยกเลิก', 'Cancel'))),
        FilledButton(
          onPressed: () {
            if (!_form.currentState!.validate()) return;
            Navigator.pop(context, int.parse(_ctrl.text) * _unit);
          },
          child: Text(context.tr('เพิ่ม', 'Add')),
        ),
      ],
    );
  }
}

/// Horizontal category selector.
class CategorySelector extends StatelessWidget {
  const CategorySelector({super.key, required this.selectedId, required this.onChanged, this.allowNone = false});
  final String? selectedId;
  final ValueChanged<String?> onChanged;
  final bool allowNone;

  @override
  Widget build(BuildContext context) {
    final cats = context.watch<AppController>().categories;
    return Wrap(spacing: 8, runSpacing: 8, children: [
      if (allowNone)
        ChoiceChip(
          label: Text(context.tr('ไม่ระบุ', 'None')),
          selected: selectedId == null,
          onSelected: (_) => onChanged(null),
        ),
      for (final ItemCategory c in cats)
        ChoiceChip(
          showCheckmark: false,
          avatar: Icon(c.icon, size: 18, color: c.color),
          label: Text(c.name),
          selected: selectedId == c.id,
          onSelected: (_) => onChanged(c.id),
        ),
    ]);
  }
}
