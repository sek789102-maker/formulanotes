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
import '../design/design.dart';
import 'thai_date_picker.dart';

Future<DateTime?> pickDate(BuildContext context, {DateTime? initial, DateTime? first, DateTime? last}) {
  final now = DateTime.now();
  final init = initial ?? now;
  final f = first ?? DateTime(now.year - 50);
  final l = last ?? DateTime(now.year + 50);
  return showThaiDatePicker(
    context,
    initial: init.isBefore(f) ? f : (init.isAfter(l) ? l : init),
    first: f,
    last: l,
  );
}

Future<TimeOfDay?> pickTime(BuildContext context, {TimeOfDay? initial}) {
  return showTimePicker(
    context: context,
    initialTime: initial ?? const TimeOfDay(hour: 9, minute: 0),
    builder: (context, child) =>
        MediaQuery(data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true), child: child!),
  );
}

/// Reminder presets (1/3/7 days, at time) plus custom offsets, as compact chips.
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
    final t = Theme.of(context).textTheme;
    final presets = forDocument ? ReminderConfig.presetsDocument : ReminderConfig.presetsItem;
    final custom = config.offsetsMinutes.where((o) => !presets.contains(o)).toList();
    final prefs = context.watch<AppController>().reminderPrefs;

    if (!enabled) {
      return Row(
        children: [
          Icon(Icons.notifications_off_outlined, size: 18, color: scheme.onSurfaceVariant),
          const SizedBox(width: Space.sm),
          Expanded(
            child: Text(
              disabledHint ?? context.tr('กำหนดวันที่ก่อนเพื่อตั้งการแจ้งเตือน', 'Set a date first to add reminders'),
              style: t.bodySmall,
            ),
          ),
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(Icons.notifications_none_rounded, size: 18, color: scheme.onSurfaceVariant),
            const SizedBox(width: Space.sm),
            Expanded(child: Text(context.tr('แจ้งเตือนล่วงหน้า', 'Remind me'), style: t.labelMedium)),
            if (config.isEnabled)
              Text(
                context.tr('${config.offsetsMinutes.length} ครั้ง', '${config.offsetsMinutes.length}×'),
                style: t.labelSmall?.copyWith(color: scheme.primary),
              ),
          ],
        ),
        const SizedBox(height: Space.sm),
        Wrap(
          spacing: 6,
          runSpacing: 6,
          children: [
            for (final p in presets)
              FilterChip(
                label: Text(Fmt.reminderOffset(p)),
                selected: config.offsetsMinutes.contains(p),
                showCheckmark: true,
                onSelected: (_) {
                  HapticFeedback.selectionClick();
                  onChanged(config.toggle(p));
                },
              ),
            for (final cOff in custom)
              InputChip(
                label: Text(Fmt.reminderOffset(cOff)),
                selected: true,
                onDeleted: () => onChanged(config.toggle(cOff)),
                onPressed: () => onChanged(config.toggle(cOff)),
              ),
            ActionChip(
              avatar: Icon(Icons.add_rounded, size: 16, color: scheme.primary),
              label: Text(context.tr('กำหนดเอง', 'Custom')),
              onPressed: () async {
                final v = await showDialog<int>(
                  context: context,
                  builder: (_) => _CustomReminderDialog(daysOnly: forDocument),
                );
                if (v != null && !config.offsetsMinutes.contains(v)) onChanged(config.toggle(v));
              },
            ),
          ],
        ),
        if (forDocument && config.isEnabled) ...[
          const SizedBox(height: Space.sm),
          Text(
            context.tr(
              'แจ้งเวลา ${prefs.hour.toString().padLeft(2, '0')}:${prefs.minute.toString().padLeft(2, '0')} น. · เปลี่ยนได้ในตั้งค่า',
              'At ${prefs.hour.toString().padLeft(2, '0')}:${prefs.minute.toString().padLeft(2, '0')} · change in Settings',
            ),
            style: t.bodySmall,
          ),
        ],
      ],
    );
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
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: TextFormField(
                controller: _ctrl,
                autofocus: true,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(6)],
                decoration: InputDecoration(hintText: context.tr('จำนวน', 'Amount')),
                validator: (v) => Validators.customReminder(v, maxMinutes: ReminderCalc.maxOffset, unitMinutes: _unit),
              ),
            ),
            const SizedBox(width: Space.md),
            Expanded(
              child: DropdownButtonFormField<int>(
                initialValue: _unit,
                borderRadius: Radii.brMd,
                items: [for (final e in units.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
                onChanged: (v) => setState(() => _unit = v ?? _unit),
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ยกเลิก', 'Cancel'))),
        FilledButton(
          style: FilledButton.styleFrom(minimumSize: const Size(0, Sizes.buttonCompact)),
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

/// Field showing the chosen category; opens a grid sheet to change it.
class CategoryPickerField extends StatelessWidget {
  const CategoryPickerField({super.key, required this.selectedId, required this.onChanged, this.allowNone = false});
  final String? selectedId;
  final ValueChanged<String?> onChanged;
  final bool allowNone;

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final cat = c.maybeCategory(selectedId);
    return PickerField(
      icon: Icons.label_outline_rounded,
      leading: cat == null ? null : IconBadge(icon: cat.icon, color: cat.color, size: 28),
      label: context.tr('หมวดหมู่', 'Category'),
      value: cat?.name,
      onTap: () async {
        final r = await showCategorySheet(context, selectedId: selectedId, allowNone: allowNone);
        if (r != null) onChanged(r.isEmpty ? null : r);
      },
    );
  }
}

/// Returns the chosen category id, '' for "none", or null if dismissed.
Future<String?> showCategorySheet(BuildContext context, {String? selectedId, bool allowNone = false, String? title}) {
  return showModalBottomSheet<String>(
    context: context,
    isScrollControlled: true,
    builder: (ctx) {
      final cats = ctx.read<AppController>().categories;
      final t = Theme.of(ctx).textTheme;
      final scheme = Theme.of(ctx).colorScheme;
      Widget tile({required String id, required String name, required Widget icon}) {
        final sel = (selectedId ?? '') == id;
        return Material(
          color: sel ? scheme.primaryContainer : scheme.surfaceContainerHigh.withValues(alpha: 0.6),
          borderRadius: Radii.brMd,
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: () => Navigator.pop(ctx, id),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: Space.xs, vertical: Space.md),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  icon,
                  const SizedBox(height: Space.sm),
                  Text(
                    name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: t.labelMedium?.copyWith(color: sel ? scheme.onPrimaryContainer : scheme.onSurface),
                  ),
                ],
              ),
            ),
          ),
        );
      }

      return SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(ctx).height * 0.75),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title ?? ctx.tr('เลือกหมวดหมู่', 'Choose category'), style: t.titleMedium),
                const SizedBox(height: Space.md),
                // Wrap (not a fixed-ratio grid) so tiles grow with large text sizes.
                LayoutBuilder(
                  builder: (context, box) {
                    final cols = box.maxWidth > 520 ? 5 : 3;
                    final w = (box.maxWidth - Space.sm * (cols - 1)) / cols;
                    return Wrap(
                      spacing: Space.sm,
                      runSpacing: Space.sm,
                      children: [
                        if (allowNone)
                          SizedBox(
                            width: w,
                            child: tile(
                              id: '',
                              name: ctx.tr('ไม่ระบุ', 'None'),
                              icon: IconBadge(icon: Icons.block_rounded, color: scheme.secondary),
                            ),
                          ),
                        for (final ItemCategory c in cats)
                          SizedBox(
                            width: w,
                            child: tile(
                              id: c.id,
                              name: c.name,
                              icon: IconBadge(icon: c.icon, color: c.color),
                            ),
                          ),
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      );
    },
  );
}

/// Sticky bottom bar with the primary save button.
class SaveBar extends StatelessWidget {
  const SaveBar({super.key, required this.label, required this.onPressed, this.busy = false});
  final String label;
  final VoidCallback? onPressed;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(Space.page, Space.sm, Space.page, Space.md),
          child: SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: busy ? null : onPressed,
              child: AnimatedSwitcher(
                duration: Motion.fast,
                child: busy
                    ? SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2.2, color: scheme.onPrimary),
                      )
                    : Text(label),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
