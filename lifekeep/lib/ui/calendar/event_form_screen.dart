import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/ids.dart';
import '../../core/utils/validators.dart';
import '../../data/models/event_item.dart';
import '../../data/models/reminder_config.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';
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
  late DateTime? _end = widget.event?.endAt ?? (widget.event == null ? _defaultStart().add(const Duration(hours: 1)) : null);
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
    for (final c in [_title, _desc, _location]) {
      c.addListener(() => _dirty = true);
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
          ? (_isEdit ? context.tr('บันทึกการแก้ไขแล้ว', 'Changes saved') : context.tr('เพิ่มกิจกรรมแล้ว', 'Event added'))
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
    final done = await runAction(context, () => c.deleteEvent(e), success: context.tr('ลบกิจกรรมแล้ว', 'Event deleted'));
    if (done) nav.pop();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
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
          title: Text(_isEdit ? context.tr('แก้ไขกิจกรรม', 'Edit event') : context.tr('เพิ่มกิจกรรม', 'Add event')),
          actions: [
            if (_isEdit)
              IconButton(tooltip: context.tr('ลบ', 'Delete'), icon: const Icon(Icons.delete_outline_rounded), onPressed: _delete),
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: TextButton(onPressed: _saving ? null : _save, child: Text(context.tr('บันทึก', 'Save'))),
            ),
          ],
        ),
        body: Form(
          key: _form,
          child: ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 40), children: [
            TextFormField(
              controller: _title,
              autofocus: !_isEdit,
              maxLength: Validators.maxTitle,
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(labelText: context.tr('ชื่อกิจกรรม *', 'Event title *'), counterText: ''),
              validator: Validators.title,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _location,
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(
                labelText: context.tr('สถานที่', 'Location'),
                prefixIcon: const Icon(Icons.place_outlined),
              ),
            ),
            FormSection(
              title: context.tr('วันและเวลา', 'Date & time'),
              icon: Icons.schedule_rounded,
              child: Column(children: [
                SwitchListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 4),
                  title: Text(context.tr('ทั้งวัน', 'All day')),
                  value: _allDay,
                  onChanged: (v) => _mark(() => _allDay = v),
                ),
                PickerTile(
                  icon: Icons.play_circle_outline_rounded,
                  label: context.tr('เริ่ม', 'Starts'),
                  value: _allDay ? Fmt.date(_start) : Fmt.dateTime(_start),
                  onTap: _pickStart,
                ),
                const SizedBox(height: 10),
                PickerTile(
                  icon: Icons.stop_circle_outlined,
                  label: context.tr('สิ้นสุด (ไม่บังคับ)', 'Ends (optional)'),
                  value: _end == null ? null : (_allDay ? Fmt.date(_end!) : Fmt.dateTime(_end!)),
                  onTap: _pickEnd,
                  onClear: () => _mark(() => _end = null),
                ),
                if (_rangeError != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(_rangeError!, style: TextStyle(color: scheme.error)),
                  ),
              ]),
            ),
            FormSection(
              title: context.tr('การแจ้งเตือน', 'Reminders'),
              icon: Icons.notifications_none_rounded,
              child: ReminderPicker(config: _reminder, enabled: true, onChanged: (r) => _mark(() => _reminder = r)),
            ),
            FormSection(
              title: context.tr('สี', 'Colour'),
              icon: Icons.palette_outlined,
              child: Wrap(spacing: 10, runSpacing: 10, children: [
                for (final col in AppColors.categoryPalette)
                  GestureDetector(
                    onTap: () => _mark(() => _color = col.toARGB32()),
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 150),
                      width: 34,
                      height: 34,
                      decoration: BoxDecoration(
                        color: col,
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: (_color ?? AppColors.primary.toARGB32()) == col.toARGB32() ? scheme.onSurface : Colors.transparent,
                          width: 2.5,
                        ),
                      ),
                    ),
                  ),
              ]),
            ),
            FormSection(
              title: context.tr('รายละเอียด', 'Details'),
              icon: Icons.notes_rounded,
              child: TextFormField(
                controller: _desc,
                minLines: 2,
                maxLines: 6,
                decoration: InputDecoration(labelText: context.tr('รายละเอียด', 'Details')),
                validator: Validators.longText,
              ),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: _saving ? null : _save,
              icon: const Icon(Icons.check_rounded),
              label: Text(context.tr('บันทึกกิจกรรม', 'Save event')),
            ),
          ]),
        ),
      ),
    );
  }
}
