import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/ids.dart';
import '../../core/utils/validators.dart';
import '../../data/models/event_item.dart';
import '../../data/models/reminder_config.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../widgets/form_widgets.dart';

class EventFormScreen extends StatefulWidget {
  const EventFormScreen({super.key, this.event, this.initialDate});
  final EventItem? event;
  final DateTime? initialDate;

  @override
  State<EventFormScreen> createState() => _EventFormScreenState();
}

class _EventFormScreenState extends State<EventFormScreen> {
  final _form = GlobalKey<FormState>();
  late final _title = TextEditingController(text: widget.event?.title);
  late final _desc = TextEditingController(text: widget.event?.description);
  late final _location = TextEditingController(text: widget.event?.location);
  late bool _allDay = widget.event?.allDay ?? false;
  late DateTime _start = widget.event?.startAt ?? _defaultStart();
  late DateTime? _end =
      widget.event?.endAt ?? (widget.event == null ? _defaultStart().add(const Duration(hours: 1)) : null);
  late ReminderConfig _reminder = widget.event?.reminder ?? const ReminderConfig([60]);
  late int? _color = widget.event?.colorValue;
  bool _saving = false;
  bool _dirty = false;
  String? _rangeError;

  bool get _isEdit => widget.event != null;

  DateTime _defaultStart() {
    final base = widget.initialDate ?? DateTime.now();
    final now = DateTime.now();
    final hour = Dates.isSameDay(base, now) ? (now.hour + 1).clamp(0, 23) : 9;
    return DateTime(base.year, base.month, base.day, hour);
  }

  @override
  void initState() {
    super.initState();
    // Only real text edits count (focus/cursor changes also notify).
    for (final c in [_title, _desc, _location]) {
      final initial = c.text;
      c.addListener(() {
        if (c.text != initial) _dirty = true;
      });
    }
  }

  @override
  void dispose() {
    _title.dispose();
    _desc.dispose();
    _location.dispose();
    super.dispose();
  }

  void _mark(VoidCallback f) => setState(() {
    f();
    _dirty = true;
    _rangeError = null;
  });

  Future<void> _pickStart() async {
    final d = await pickDate(context, initial: _start);
    if (d == null || !mounted) return;
    TimeOfDay? t;
    if (!_allDay) {
      t = await pickTime(context, initial: TimeOfDay.fromDateTime(_start));
      if (!mounted) return;
    }
    _mark(() {
      final duration = _end?.difference(_start);
      _start = DateTime(d.year, d.month, d.day, t?.hour ?? _start.hour, t?.minute ?? _start.minute);
      if (_end != null && duration != null && !duration.isNegative) _end = _start.add(duration);
    });
  }

  Future<void> _pickEnd() async {
    final d = await pickDate(context, initial: _end ?? _start, first: Dates.startOfDay(_start));
    if (d == null || !mounted) return;
    TimeOfDay? t;
    if (!_allDay) {
      t = await pickTime(context, initial: TimeOfDay.fromDateTime(_end ?? _start.add(const Duration(hours: 1))));
      if (!mounted) return;
    }
    _mark(() => _end = DateTime(d.year, d.month, d.day, t?.hour ?? 23, t?.minute ?? 59));
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final start = _allDay ? Dates.startOfDay(_start) : _start;
    final end = _end == null ? null : (_allDay ? Dates.endOfDay(_end!) : _end);
    final err = Validators.eventRange(start, end);
    if (err != null) {
      setState(() => _rangeError = err);
      return;
    }
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    final now = DateTime.now();
    final e = EventItem(
      id: widget.event?.id ?? newId(),
      title: _title.text.trim(),
      description: _desc.text.trim(),
      location: _location.text.trim(),
      startAt: start,
      endAt: end,
      allDay: _allDay,
      reminder: _reminder,
      colorValue: _color,
      createdAt: widget.event?.createdAt ?? now,
      updatedAt: now,
    );
    setState(() => _saving = true);
    if (_reminder.isEnabled) await c.ensureNotificationPermission();
    if (!mounted) return;
    var reminderOk = true;
    final ok = await runAction(context, () async => reminderOk = await c.saveEvent(e));
    if (!mounted) return;
    setState(() => _saving = false);
    if (!ok) return;
    showSnack(
      context,
      reminderOk
          ? (_isEdit
                ? context.tr('บันทึกการแก้ไขแล้ว', 'Changes saved')
                : context.tr('เพิ่มกิจกรรมแล้ว', 'Event added'))
          : context.tr('บันทึกแล้ว แต่ตั้งการแจ้งเตือนไม่สำเร็จ', 'Saved, but reminders could not be scheduled'),
      error: !reminderOk,
    );
    nav.pop();
  }

  Future<void> _delete() async {
    final e = widget.event!;
    final ok = await confirmDialog(
      context,
      title: context.tr('ลบกิจกรรมนี้?', 'Delete this event?'),
      message: context.tr('“${e.title}” จะถูกลบถาวร', '“${e.title}” will be permanently deleted.'),
      confirmLabel: context.tr('ลบ', 'Delete'),
      destructive: true,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok || !mounted) return;
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    final done = await runAction(
      context,
      () => c.deleteEvent(e),
      success: context.tr('ลบกิจกรรมแล้ว', 'Event deleted'),
    );
    if (done) nav.pop();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final selectedColor = _color ?? AppColors.primary.toARGB32();
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (didPop) return;
        final nav = Navigator.of(context);
        if (!_dirty ||
            await confirmDialog(
              context,
              title: context.tr('ยกเลิกการแก้ไข?', 'Discard changes?'),
              message: context.tr('การเปลี่ยนแปลงที่ยังไม่บันทึกจะหายไป', 'Unsaved changes will be lost.'),
              confirmLabel: context.tr('ยกเลิกการแก้ไข', 'Discard'),
              destructive: true,
            )) {
          nav.pop();
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text(_isEdit ? context.tr('แก้ไขกิจกรรม', 'Edit event') : context.tr('เพิ่มกิจกรรม', 'New event')),
          actions: [
            if (_isEdit)
              IconButton(
                tooltip: context.tr('ลบ', 'Delete'),
                icon: const Icon(Icons.delete_outline_rounded),
                onPressed: _delete,
              ),
            TextButton(onPressed: _saving ? null : _save, child: Text(context.tr('บันทึก', 'Save'))),
            const SizedBox(width: Space.sm),
          ],
        ),
        bottomNavigationBar: SaveBar(
          label: _isEdit ? context.tr('บันทึกการแก้ไข', 'Save changes') : context.tr('บันทึกกิจกรรม', 'Save event'),
          busy: _saving,
          onPressed: _save,
        ),
        body: Form(
          key: _form,
          child: ContentWidth(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(Space.page, Space.xs, Space.page, Space.xl),
              children: [
                FormBlock(
                  title: context.tr('กิจกรรม', 'Event'),
                  icon: Icons.event_note_outlined,
                  children: [
                    TextFormField(
                      controller: _title,
                      autofocus: !_isEdit,
                      maxLength: Validators.maxTitle,
                      textInputAction: TextInputAction.next,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.w500),
                      decoration: InputDecoration(
                        labelText: context.tr('ชื่อกิจกรรม *', 'Event title *'),
                        counterText: '',
                      ),
                      validator: Validators.title,
                    ),
                    TextFormField(
                      controller: _location,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(
                        labelText: context.tr('สถานที่', 'Location'),
                        prefixIcon: const Icon(Icons.place_outlined, size: 20),
                      ),
                    ),
                  ],
                ),
                FormBlock(
                  title: context.tr('วันและเวลา', 'Date & time'),
                  icon: Icons.schedule_rounded,
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(context.tr('ทั้งวัน', 'All day'), style: Theme.of(context).textTheme.labelMedium),
                      const SizedBox(width: Space.xs),
                      Transform.scale(
                        scale: 0.85,
                        child: Switch(value: _allDay, onChanged: (v) => _mark(() => _allDay = v)),
                      ),
                    ],
                  ),
                  children: [
                    PickerField(
                      icon: Icons.play_circle_outline_rounded,
                      label: context.tr('เริ่ม', 'Starts'),
                      value: _allDay ? Fmt.fullDate(_start) : Fmt.dateTime(_start),
                      onTap: _pickStart,
                    ),
                    PickerField(
                      icon: Icons.stop_circle_outlined,
                      label: context.tr('สิ้นสุด (ไม่บังคับ)', 'Ends (optional)'),
                      value: _end == null ? null : (_allDay ? Fmt.fullDate(_end!) : Fmt.dateTime(_end!)),
                      onTap: _pickEnd,
                      onClear: () => _mark(() => _end = null),
                    ),
                    if (_rangeError != null)
                      Text(_rangeError!, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.error)),
                    ReminderPicker(config: _reminder, enabled: true, onChanged: (r) => _mark(() => _reminder = r)),
                  ],
                ),
                FormBlock(
                  title: context.tr('สีและรายละเอียด', 'Colour & details'),
                  icon: Icons.palette_outlined,
                  children: [
                    Wrap(
                      spacing: Space.sm,
                      runSpacing: Space.sm,
                      children: [
                        for (final col in AppColors.categoryPalette)
                          Semantics(
                            selected: selectedColor == col.toARGB32(),
                            button: true,
                            child: GestureDetector(
                              onTap: () => _mark(() => _color = col.toARGB32()),
                              child: AnimatedContainer(
                                duration: Motion.fast,
                                width: 32,
                                height: 32,
                                decoration: BoxDecoration(
                                  color: col,
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: selectedColor == col.toARGB32() ? scheme.surface : Colors.transparent,
                                    width: 3,
                                  ),
                                  boxShadow: selectedColor == col.toARGB32()
                                      ? [BoxShadow(color: col, spreadRadius: 2)]
                                      : null,
                                ),
                                child: selectedColor == col.toARGB32()
                                    ? const Icon(Icons.check_rounded, size: 16, color: Colors.white)
                                    : null,
                              ),
                            ),
                          ),
                      ],
                    ),
                    TextFormField(
                      controller: _desc,
                      minLines: 2,
                      maxLines: 6,
                      decoration: InputDecoration(
                        labelText: context.tr('รายละเอียด', 'Details'),
                        alignLabelWithHint: true,
                      ),
                      validator: Validators.longText,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
