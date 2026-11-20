import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../core/utils/errors.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/ids.dart';
import '../../core/utils/validators.dart';
import '../../data/models/category.dart';
import '../../data/models/document_item.dart';
import '../../data/models/reminder_config.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../lock/lock_gate.dart';
import '../widgets/form_widgets.dart';
import '../widgets/item_tiles.dart';
import 'image_viewer_screen.dart';

enum _ImageSource { gallery, camera, files }

class DocumentFormScreen extends StatefulWidget {
  const DocumentFormScreen({super.key, this.document});
  final DocumentItem? document;

  @override
  State<DocumentFormScreen> createState() => _DocumentFormScreenState();
}

class _DocumentFormScreenState extends State<DocumentFormScreen> {
  final _form = GlobalKey<FormState>();
  late final _title = TextEditingController(text: widget.document?.title);
  late final _desc = TextEditingController(text: widget.document?.description);
  late final _notes = TextEditingController(text: widget.document?.notes);
  late String _categoryId = widget.document?.categoryId ?? ItemCategory.otherId;
  late DateTime? _expiry = widget.document?.expiryDate;
  late ReminderConfig _reminder =
      widget.document?.reminder ?? const ReminderConfig([ReminderCalc.oneDay, ReminderCalc.sevenDays]);
  late bool _pinned = widget.document?.isPinned ?? false;
  late final List<String> _attachments = [...?widget.document?.attachmentPaths];

  /// Files copied during this session (deleted again if the user discards).
  final Set<String> _added = {};

  /// Existing files the user removed (deleted only after a successful save).
  final Set<String> _removed = {};

  bool _dirty = false;
  bool _saving = false;
  bool _busyPicking = false;

  bool get _isEdit => widget.document != null;

  @override
  void initState() {
    super.initState();
    // Only real text edits count (focus/cursor changes also notify).
    for (final c in [_title, _desc, _notes]) {
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
    _notes.dispose();
    super.dispose();
  }

  void _mark(VoidCallback f) => setState(() {
    f();
    _dirty = true;
  });

  void _addName(String name) => _mark(() {
    _attachments.add(name);
    _added.add(name);
  });

  Future<void> _addImages() async {
    final src = await showOptionSheet<_ImageSource>(
      context,
      title: context.tr('เพิ่มรูปภาพ', 'Add images'),
      options: [
        SheetOption(
          value: _ImageSource.gallery,
          icon: Icons.photo_library_outlined,
          label: context.tr('เลือกจากแกลเลอรี', 'Choose from gallery'),
          subtitle: context.tr('เลือกได้หลายรูป', 'Multiple images'),
        ),
        SheetOption(
          value: _ImageSource.camera,
          icon: Icons.photo_camera_outlined,
          label: context.tr('ถ่ายรูป', 'Take a photo'),
        ),
        SheetOption(
          value: _ImageSource.files,
          icon: Icons.folder_open_outlined,
          label: context.tr('เลือกจากไฟล์', 'Choose image files'),
        ),
      ],
    );
    if (src == null || !mounted) return;
    final c = context.read<AppController>();
    await _withPicking(() async {
      switch (src) {
        case _ImageSource.gallery:
          final files = await LockGuard.external(
            () => ImagePicker().pickMultiImage(
              maxWidth: 3000,
              maxHeight: 3000,
              imageQuality: 90,
              requestFullMetadata: false,
            ),
          );
          for (final f in files) {
            _addName(await c.s.attachments.importFile(f.path));
          }
        case _ImageSource.camera:
          final f = await LockGuard.external(
            () =>
                ImagePicker().pickImage(source: ImageSource.camera, maxWidth: 3000, maxHeight: 3000, imageQuality: 90),
          );
          if (f != null) _addName(await c.s.attachments.importFile(f.path));
        case _ImageSource.files:
          final files = await LockGuard.external(() => FilePicker.pickFiles(type: FileType.image));
          for (final f in files) {
            final path = f.path;
            _addName(
              path != null
                  ? await c.s.attachments.importFile(path)
                  : await c.s.attachments.importStream(f.xFile.openRead(), f.name),
            );
          }
      }
    });
  }

  Future<void> _withPicking(Future<void> Function() f) async {
    if (_busyPicking) return;
    setState(() => _busyPicking = true);
    try {
      await f();
    } catch (e) {
      if (!mounted) return;
      final msg = e is AppException
          ? e.userMessage
          : context.tr(
              'ไม่สามารถเลือกรูปภาพได้ กรุณาตรวจสอบสิทธิ์การเข้าถึงรูปภาพ',
              'Could not pick images. Please check photo permission.',
            );
      showSnack(context, msg, error: true);
    } finally {
      if (mounted) setState(() => _busyPicking = false);
    }
  }

  void _removeAttachment(String name) {
    _mark(() {
      _attachments.remove(name);
      if (_added.remove(name)) {
        context.read<AppController>().s.attachments.delete(name);
      } else {
        _removed.add(name);
      }
    });
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final c = context.read<AppController>();
    final nav = Navigator.of(context);
    setState(() => _saving = true);
    final now = DateTime.now();
    final reminder = _expiry == null ? const ReminderConfig.none() : _reminder;
    final doc = DocumentItem(
      id: widget.document?.id ?? newId(),
      title: _title.text.trim(),
      categoryId: _categoryId,
      description: _desc.text.trim(),
      notes: _notes.text.trim(),
      createdAt: widget.document?.createdAt ?? now,
      updatedAt: now,
      expiryDate: _expiry,
      reminder: reminder,
      attachmentPaths: List.of(_attachments),
      isPinned: _pinned,
    );
    if (reminder.isEnabled) await c.ensureNotificationPermission();
    if (!mounted) return;
    var reminderOk = true;
    final ok = await runAction(context, () async {
      reminderOk = await c.saveDocument(doc, removedAttachments: _removed);
    });
    if (!mounted) return;
    setState(() => _saving = false);
    if (!ok) return;
    _added.clear();
    showSnack(
      context,
      reminderOk
          ? (_isEdit
                ? context.tr('บันทึกการแก้ไขแล้ว', 'Changes saved')
                : context.tr('เพิ่มเอกสารแล้ว', 'Document added'))
          : context.tr('บันทึกแล้ว แต่ตั้งการแจ้งเตือนไม่สำเร็จ', 'Saved, but reminders could not be scheduled'),
      error: !reminderOk,
    );
    nav.pop();
  }

  Future<bool> _confirmDiscard() async {
    if (!_dirty) return true;
    return confirmDialog(
      context,
      title: context.tr('ยกเลิกการแก้ไข?', 'Discard changes?'),
      message: context.tr('การเปลี่ยนแปลงที่ยังไม่บันทึกจะหายไป', 'Unsaved changes will be lost.'),
      confirmLabel: context.tr('ยกเลิกการแก้ไข', 'Discard'),
      destructive: true,
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final expiryDays = ExpiryCalc.daysLeft(_expiry, DateTime.now());
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (didPop) return;
        final nav = Navigator.of(context);
        final atts = context.read<AppController>().s.attachments;
        if (await _confirmDiscard()) {
          // Remove files copied for a form that was never saved.
          await atts.deleteMany(_added);
          _added.clear();
          nav.pop();
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text(_isEdit ? context.tr('แก้ไขเอกสาร', 'Edit document') : context.tr('เพิ่มเอกสาร', 'New document')),
          actions: [
            TextButton(onPressed: _saving ? null : _save, child: Text(context.tr('บันทึก', 'Save'))),
            const SizedBox(width: Space.sm),
          ],
        ),
        bottomNavigationBar: SaveBar(
          label: _isEdit ? context.tr('บันทึกการแก้ไข', 'Save changes') : context.tr('บันทึกเอกสาร', 'Save document'),
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
                  title: context.tr('ข้อมูลเอกสาร', 'Document'),
                  icon: Icons.description_outlined,
                  children: [
                    TextFormField(
                      controller: _title,
                      textInputAction: TextInputAction.next,
                      maxLength: Validators.maxTitle,
                      style: t.bodyLarge?.copyWith(fontWeight: FontWeight.w500),
                      decoration: InputDecoration(
                        labelText: context.tr('ชื่อเอกสาร *', 'Title *'),
                        hintText: context.tr('เช่น บัตรประชาชน, ใบขับขี่', 'e.g. ID card, Driving licence'),
                        counterText: '',
                      ),
                      validator: Validators.title,
                    ),
                    CategoryPickerField(
                      selectedId: _categoryId,
                      onChanged: (v) => _mark(() => _categoryId = v ?? ItemCategory.otherId),
                    ),
                  ],
                ),
                FormBlock(
                  title: context.tr('วันหมดอายุและการแจ้งเตือน', 'Expiry & reminders'),
                  icon: Icons.event_outlined,
                  trailing: expiryDays == null
                      ? null
                      : Text(
                          Fmt.relativeDays(expiryDays),
                          style: t.labelMedium?.copyWith(
                            color: expiryDays < 0 ? context.status.danger : scheme.primary,
                          ),
                        ),
                  children: [
                    PickerField(
                      icon: Icons.event_busy_outlined,
                      label: context.tr('วันหมดอายุ', 'Expiry date'),
                      value: _expiry == null ? null : Fmt.fullDate(_expiry!),
                      onTap: () async {
                        final d = await pickDate(
                          context,
                          initial: _expiry ?? DateTime.now().add(const Duration(days: 365)),
                        );
                        if (d != null) _mark(() => _expiry = d);
                      },
                      onClear: () => _mark(() => _expiry = null),
                    ),
                    ReminderPicker(
                      config: _reminder,
                      enabled: _expiry != null,
                      forDocument: true,
                      disabledHint: context.tr(
                        'ใส่วันหมดอายุเพื่อเปิดการแจ้งเตือน',
                        'Add an expiry date to enable reminders',
                      ),
                      onChanged: (r) => _mark(() => _reminder = r),
                    ),
                  ],
                ),
                FormBlock(
                  title: context.tr('รูปภาพแนบ', 'Images'),
                  icon: Icons.photo_library_outlined,
                  trailing: _attachments.isEmpty
                      ? null
                      : Text('${_attachments.length}', style: t.labelMedium?.copyWith(color: scheme.onSurfaceVariant)),
                  children: [
                    SizedBox(
                      height: 92,
                      child: ListView(
                        scrollDirection: Axis.horizontal,
                        children: [
                          _AddImageTile(busy: _busyPicking, onTap: _busyPicking ? null : _addImages),
                          for (var i = 0; i < _attachments.length; i++)
                            Padding(
                              padding: const EdgeInsets.only(left: Space.sm),
                              child: _ThumbWithRemove(
                                name: _attachments[i],
                                onOpen: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => ImageViewerScreen(names: List.of(_attachments), initialIndex: i),
                                  ),
                                ),
                                onRemove: () => _removeAttachment(_attachments[i]),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
                FormBlock(
                  title: context.tr('รายละเอียดเพิ่มเติม', 'More details'),
                  icon: Icons.notes_rounded,
                  children: [
                    TextFormField(
                      controller: _desc,
                      minLines: 1,
                      maxLines: 3,
                      decoration: InputDecoration(labelText: context.tr('คำอธิบายสั้น', 'Short description')),
                      validator: Validators.longText,
                    ),
                    TextFormField(
                      controller: _notes,
                      minLines: 2,
                      maxLines: 6,
                      decoration: InputDecoration(
                        labelText: context.tr('หมายเหตุ', 'Notes'),
                        hintText: context.tr(
                          'เช่น เลขที่เอกสาร ที่เก็บตัวจริง',
                          'e.g. number, where the original is kept',
                        ),
                        alignLabelWithHint: true,
                      ),
                      validator: Validators.longText,
                    ),
                  ],
                ),
                LkGroup(
                  children: [
                    LkSwitchRow(
                      leading: Icon(
                        _pinned ? Icons.push_pin_rounded : Icons.push_pin_outlined,
                        color: scheme.primary,
                        size: 22,
                      ),
                      title: context.tr('ปักหมุดเอกสารสำคัญ', 'Pin as important'),
                      subtitle: context.tr('แสดงไว้บนสุดของรายการ', 'Shown at the top of the list'),
                      value: _pinned,
                      onChanged: (v) => _mark(() => _pinned = v),
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

class _AddImageTile extends StatelessWidget {
  const _AddImageTile({required this.onTap, required this.busy});
  final VoidCallback? onTap;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.primaryContainer.withValues(alpha: 0.55),
      borderRadius: Radii.brMd,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          width: 92,
          height: 92,
          child: busy
              ? const Center(child: SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.4)))
              : Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.add_photo_alternate_outlined, color: scheme.primary, size: 26),
                    const SizedBox(height: Space.xs),
                    Text(
                      context.tr('เพิ่มรูป', 'Add'),
                      style: Theme.of(context).textTheme.labelMedium?.copyWith(color: scheme.primary),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}

class _ThumbWithRemove extends StatelessWidget {
  const _ThumbWithRemove({required this.name, required this.onOpen, required this.onRemove});
  final String name;
  final VoidCallback onOpen;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return FadeSlideIn(
      child: Stack(
        children: [
          GestureDetector(
            onTap: onOpen,
            child: AttachmentThumb(name: name, size: 92, radius: Radii.md),
          ),
          Positioned(
            top: 4,
            right: 4,
            child: Material(
              color: Colors.black.withValues(alpha: 0.55),
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: onRemove,
                child: Padding(
                  padding: const EdgeInsets.all(4),
                  child: Icon(
                    Icons.close_rounded,
                    size: 15,
                    color: Colors.white,
                    semanticLabel: context.tr('ลบรูป', 'Remove image'),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
