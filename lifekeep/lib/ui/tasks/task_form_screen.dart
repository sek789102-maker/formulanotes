import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/ids.dart';
import '../../core/utils/validators.dart';
import '../../data/models/reminder_config.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../widgets/form_widgets.dart';
import '../widgets/reminder_permission.dart';

class TaskFormScreen extends StatefulWidget {
  const TaskFormScreen({super.key, this.task, this.initialDate});
  final TaskItem? task;
  final DateTime? initialDate;

  @override
  State<TaskFormScreen> createState() => _TaskFormScreenState();
}

class _TaskFormScreenState extends State<TaskFormScreen> {
  final _form = GlobalKey<FormState>();
  late final _title = TextEditingController(text: widget.task?.title);
  late final _desc = TextEditingController(text: widget.task?.description);
  late String? _categoryId = widget.task?.categoryId;
  late DateTime? _date = widget.task?.dueAt ?? widget.initialDate;
  late TimeOfDay? _time = (widget.task?.hasTime ?? false) && widget.task?.dueAt != null
      ? TimeOfDay.fromDateTime(widget.task!.dueAt!)
      : null;
  late TaskPriority _priority = widget.task?.priority ?? TaskPriority.normal;
  late ReminderConfig _reminder = widget.task?.reminder ?? const ReminderConfig([ReminderCalc.atTime]);
  late bool _completed = widget.task?.isCompleted ?? false;
  bool _saving = false;
  bool _dirty = false;

  bool get _isEdit => widget.task != null;

  @override
  void initState() {
    super.initState();
    // Only real text edits count (focus/cursor changes also notify).
    for (final c in [_title, _desc]) {
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
    super.dispose();
  }

  void _mark(VoidCallback f) => setState(() {
    f();
    _dirty = true;
  });

  DateTime? get _dueAt {
    final d = _date;
    if (d == null) return null;
    final t = _time;
    return t == null ? DateTime(d.year, d.month, d.day) : DateTime(d.year, d.month, d.day, t.hour, t.minute);
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    final now = DateTime.now();
    final old = widget.task;
    final reminder = _dueAt == null ? const ReminderConfig.none() : _reminder;
    final task = TaskItem(
      id: old?.id ?? newId(),
      title: _title.text.trim(),
      description: _desc.text.trim(),
      categoryId: _categoryId,
      dueAt: _dueAt,
      hasTime: _time != null && _date != null,
      priority: _priority,
      isCompleted: _completed,
      completedAt: _completed ? (old?.completedAt ?? now) : null,
      reminder: reminder,
      createdAt: old?.createdAt ?? now,
      updatedAt: now,
    );
    setState(() => _saving = true);
    if (reminder.isEnabled && !_completed) await ensureReminderReady(context);
    if (!mounted) return;
    var reminderOk = true;
    final ok = await runAction(context, () async => reminderOk = await c.saveTask(task));
    if (!mounted) return;
    setState(() => _saving = false);
    if (!ok) return;
    showSnack(
      context,
      reminderOk
          ? (_isEdit ? context.tr('บันทึกการแก้ไขแล้ว', 'Changes saved') : context.tr('เพิ่มงานแล้ว', 'Task added'))
          : context.tr('บันทึกแล้ว แต่ตั้งการแจ้งเตือนไม่สำเร็จ', 'Saved, but reminders could not be scheduled'),
      error: !reminderOk,
    );
    nav.pop();
  }

  Future<void> _delete() async {
    final t = widget.task!;
    final ok = await confirmDialog(
      context,
      title: context.tr('ลบงานนี้?', 'Delete this task?'),
      message: context.tr('“${t.title}” จะถูกลบถาวร', '“${t.title}” will be permanently deleted.'),
      confirmLabel: context.tr('ลบ', 'Delete'),
      destructive: true,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok || !mounted) return;
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    final done = await runAction(context, () => c.deleteTask(t), success: context.tr('ลบงานแล้ว', 'Task deleted'));
    if (done) nav.pop();
  }

  Widget _priorityOption(TaskPriority p, String label) {
    final scheme = Theme.of(context).colorScheme;
    final sel = _priority == p;
    final color = priorityColor(context, p.index);
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 3),
        child: Material(
          color: sel ? color.withValues(alpha: 0.12) : scheme.surfaceContainerHigh,
          shape: RoundedRectangleBorder(
            borderRadius: Radii.brMd,
            side: BorderSide(color: sel ? color : Colors.transparent, width: 1.4),
          ),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: () => _mark(() => _priority = p),
            child: SizedBox(
              height: 42,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    p == TaskPriority.high ? Icons.flag_rounded : Icons.outlined_flag_rounded,
                    size: 17,
                    color: color,
                  ),
                  const SizedBox(width: 5),
                  Flexible(
                    child: Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.labelMedium?.copyWith(color: sel ? color : scheme.onSurface),
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

  @override
  Widget build(BuildContext context) {
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
          title: Text(_isEdit ? context.tr('แก้ไขงาน', 'Edit task') : context.tr('เพิ่มงาน', 'New task')),
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
          label: _isEdit ? context.tr('บันทึกการแก้ไข', 'Save changes') : context.tr('บันทึกงาน', 'Save task'),
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
                  title: context.tr('รายละเอียดงาน', 'Task'),
                  icon: Icons.check_circle_outline_rounded,
                  children: [
                    TextFormField(
                      controller: _title,
                      autofocus: !_isEdit,
                      maxLength: Validators.maxTitle,
                      textInputAction: TextInputAction.next,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.w500),
                      decoration: InputDecoration(labelText: context.tr('ชื่องาน *', 'Task *'), counterText: ''),
                      validator: Validators.title,
                    ),
                    TextFormField(
                      controller: _desc,
                      minLines: 1,
                      maxLines: 5,
                      decoration: InputDecoration(
                        labelText: context.tr('รายละเอียด', 'Details'),
                        alignLabelWithHint: true,
                      ),
                      validator: Validators.longText,
                    ),
                    CategoryPickerField(
                      selectedId: _categoryId,
                      allowNone: true,
                      onChanged: (v) => _mark(() => _categoryId = v),
                    ),
                  ],
                ),
                FormBlock(
                  title: context.tr('กำหนดส่ง', 'Due'),
                  icon: Icons.event_outlined,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          flex: 3,
                          child: PickerField(
                            icon: Icons.calendar_today_outlined,
                            label: context.tr('วันที่', 'Date'),
                            value: _date == null ? null : Fmt.date(_date!),
                            onTap: () async {
                              final d = await pickDate(context, initial: _date ?? DateTime.now());
                              if (d != null) _mark(() => _date = d);
                            },
                            onClear: () => _mark(() {
                              _date = null;
                              _time = null;
                            }),
                          ),
                        ),
                        const SizedBox(width: Space.sm),
                        Expanded(
                          flex: 2,
                          child: PickerField(
                            icon: Icons.schedule_rounded,
                            label: context.tr('เวลา', 'Time'),
                            value: _time == null
                                ? (_date == null ? null : context.tr('ทั้งวัน', 'All day'))
                                : '${_time!.hour.toString().padLeft(2, '0')}:${_time!.minute.toString().padLeft(2, '0')}',
                            onTap: () async {
                              final t = await pickTime(context, initial: _time);
                              if (t != null) {
                                _mark(() {
                                  _time = t;
                                  _date ??= DateTime.now();
                                });
                              }
                            },
                            onClear: _time == null ? null : () => _mark(() => _time = null),
                          ),
                        ),
                      ],
                    ),
                    ReminderPicker(
                      config: _reminder,
                      enabled: _date != null,
                      disabledHint: context.tr(
                        'เลือกวันส่งเพื่อเปิดการแจ้งเตือน',
                        'Pick a due date to enable reminders',
                      ),
                      onChanged: (r) => _mark(() => _reminder = r),
                    ),
                  ],
                ),
                FormBlock(
                  title: context.tr('ความสำคัญ', 'Priority'),
                  icon: Icons.flag_outlined,
                  children: [
                    Row(
                      children: [
                        _priorityOption(TaskPriority.low, context.tr('ต่ำ', 'Low')),
                        _priorityOption(TaskPriority.normal, context.tr('ปกติ', 'Normal')),
                        _priorityOption(TaskPriority.high, context.tr('สูง', 'High')),
                      ],
                    ),
                  ],
                ),
                if (_isEdit)
                  LkGroup(
                    children: [
                      LkSwitchRow(
                        leading: Icon(Icons.task_alt_rounded, color: context.status.success),
                        title: context.tr('ทำเสร็จแล้ว', 'Completed'),
                        value: _completed,
                        onChanged: (v) => _mark(() => _completed = v),
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
