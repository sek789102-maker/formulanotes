import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/errors.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/ids.dart';
import '../../core/utils/validators.dart';
import '../../data/models/category.dart';
import '../../data/models/document_item.dart';
import '../../data/models/reminder_config.dart';
import '../../core/utils/dates.dart';
import '../../state/app_controller.dart';
import '../lock/lock_gate.dart';
import '../widgets/common.dart';
import '../widgets/form_widgets.dart';
import '../widgets/item_tiles.dart';
import 'image_viewer_screen.dart';

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
  late ReminderConfig _reminder = widget.document?.reminder ??
      const ReminderConfig([ReminderCalc.oneDay, ReminderCalc.sevenDays]);
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
    for (final c in [_title, _desc, _notes]) {
      c.addListener(() => _dirty = true);
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

  Future<void> _pickGallery() async {
    final c = context.read<AppController>();
    await _withPicking(() async {
      final files = await LockGuard.external(
        () => ImagePicker().pickMultiImage(maxWidth: 3000, maxHeight: 3000, imageQuality: 90, requestFullMetadata: false),
      );
      for (final f in files) {
        final name = await c.s.attachments.importFile(f.path);
        _mark(() {
          _attachments.add(name);
          _added.add(name);
        });
      }
    });
  }

  Future<void> _pickCamera() async {
    final c = context.read<AppController>();
    await _withPicking(() async {
      final f = await LockGuard.external(
        () => ImagePicker().pickImage(source: ImageSource.camera, maxWidth: 3000, maxHeight: 3000, imageQuality: 90),
      );
      if (f == null) return;
      final name = await c.s.attachments.importFile(f.path);
      _mark(() {
        _attachments.add(name);
        _added.add(name);
      });
    });
  }

  Future<void> _pickFiles() async {
    final c = context.read<AppController>();
    await _withPicking(() async {
      final files = await LockGuard.external(() => FilePicker.pickFiles(type: FileType.image));
      for (final f in files) {
        final path = f.path;
        final name = path != null
            ? await c.s.attachments.importFile(path)
            : await c.s.attachments.importStream(f.xFile.openRead(), f.name);
        _mark(() {
          _attachments.add(name);
          _added.add(name);
        });
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
          : context.tr('ไม่สามารถเลือกรูปภาพได้ กรุณาตรวจสอบสิทธิ์การเข้าถึงรูปภาพ',
              'Could not pick images. Please check photo access permission.');
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
          ? (_isEdit ? context.tr('บันทึกการแก้ไขแล้ว', 'Changes saved') : context.tr('เพิ่มเอกสารแล้ว', 'Document added'))
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
          title: Text(_isEdit ? context.tr('แก้ไขเอกสาร', 'Edit document') : context.tr('เพิ่มเอกสาร', 'Add document')),
          actions: [
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: TextButton(
                onPressed: _saving ? null : _save,
                child: Text(context.tr('บันทึก', 'Save')),
              ),
            ),
          ],
        ),
        body: Form(
          key: _form,
          child: ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 40), children: [
            TextFormField(
              controller: _title,
              textInputAction: TextInputAction.next,
              maxLength: Validators.maxTitle,
              decoration: InputDecoration(
                labelText: context.tr('ชื่อเอกสาร *', 'Title *'),
                hintText: context.tr('เช่น บัตรประชาชน, ใบขับขี่', 'e.g. ID card, Driving licence'),
                counterText: '',
              ),
              validator: Validators.title,
            ),
            FormSection(
              title: context.tr('หมวดหมู่', 'Category'),
              icon: Icons.label_outline_rounded,
              child: CategorySelector(selectedId: _categoryId, onChanged: (v) => _mark(() => _categoryId = v ?? ItemCategory.otherId)),
            ),
            FormSection(
              title: context.tr('รายละเอียด', 'Details'),
              icon: Icons.notes_rounded,
              child: Column(children: [
                TextFormField(
                  controller: _desc,
                  minLines: 1,
                  maxLines: 3,
                  decoration: InputDecoration(labelText: context.tr('คำอธิบาย', 'Description')),
                  validator: Validators.longText,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _notes,
                  minLines: 2,
                  maxLines: 6,
                  decoration: InputDecoration(
                    labelText: context.tr('หมายเหตุ', 'Notes'),
                    hintText: context.tr('เช่น เลขที่เอกสาร สถานที่เก็บตัวจริง', 'e.g. document number, where the original is kept'),
                  ),
                  validator: Validators.longText,
                ),
              ]),
            ),
            FormSection(
              title: context.tr('วันหมดอายุและการแจ้งเตือน', 'Expiry & reminders'),
              icon: Icons.event_outlined,
              child: Column(children: [
                PickerTile(
                  icon: Icons.event_busy_outlined,
                  label: context.tr('วันหมดอายุ', 'Expiry date'),
                  value: _expiry == null ? null : Fmt.date(_expiry!),
                  onTap: () async {
                    final d = await pickDate(context, initial: _expiry ?? DateTime.now().add(const Duration(days: 365)));
                    if (d != null) _mark(() => _expiry = d);
                  },
                  onClear: () => _mark(() => _expiry = null),
                ),
                const SizedBox(height: 12),
                ReminderPicker(
                  config: _reminder,
                  enabled: _expiry != null,
                  forDocument: true,
                  disabledHint: context.tr('กำหนดวันหมดอายุเพื่อตั้งการแจ้งเตือน', 'Set an expiry date to add reminders'),
                  onChanged: (r) => _mark(() => _reminder = r),
                ),
              ]),
            ),
            FormSection(
              title: context.tr('รูปภาพแนบ', 'Attachments'),
              icon: Icons.photo_library_outlined,
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                if (_attachments.isNotEmpty)
                  Wrap(spacing: 8, runSpacing: 8, children: [
                    for (var i = 0; i < _attachments.length; i++)
                      Stack(clipBehavior: Clip.none, children: [
                        GestureDetector(
                          onTap: () => Navigator.push(
                            context,
                            MaterialPageRoute(builder: (_) => ImageViewerScreen(names: List.of(_attachments), initialIndex: i)),
                          ),
                          child: AttachmentThumb(name: _attachments[i], size: 88),
                        ),
                        Positioned(
                          top: -6,
                          right: -6,
                          child: Material(
                            color: scheme.surface,
                            shape: const CircleBorder(),
                            elevation: 1,
                            child: InkWell(
                              customBorder: const CircleBorder(),
                              onTap: () => _removeAttachment(_attachments[i]),
                              child: Padding(
                                padding: const EdgeInsets.all(4),
                                child: Icon(Icons.close_rounded, size: 16, color: scheme.error),
                              ),
                            ),
                          ),
                        ),
                      ]),
                  ]),
                if (_attachments.isNotEmpty) const SizedBox(height: 12),
                if (_busyPicking) const LinearProgressIndicator(),
                Wrap(spacing: 8, runSpacing: 8, children: [
                  OutlinedButton.icon(
                    onPressed: _busyPicking ? null : _pickGallery,
                    icon: const Icon(Icons.photo_outlined),
                    label: Text(context.tr('แกลเลอรี', 'Gallery')),
                  ),
                  OutlinedButton.icon(
                    onPressed: _busyPicking ? null : _pickCamera,
                    icon: const Icon(Icons.photo_camera_outlined),
                    label: Text(context.tr('ถ่ายรูป', 'Camera')),
                  ),
                  OutlinedButton.icon(
                    onPressed: _busyPicking ? null : _pickFiles,
                    icon: const Icon(Icons.folder_open_outlined),
                    label: Text(context.tr('เลือกไฟล์รูป', 'Image files')),
                  ),
                ]),
              ]),
            ),
            const SizedBox(height: 12),
            SwitchListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 4),
              title: Text(context.tr('ปักหมุดเอกสารสำคัญ', 'Pin as important')),
              subtitle: Text(context.tr('แสดงไว้บนสุดของรายการ', 'Shown at the top of the list')),
              secondary: const Icon(Icons.push_pin_outlined),
              value: _pinned,
              onChanged: (v) => _mark(() => _pinned = v),
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _saving ? null : _save,
              icon: _saving
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.check_rounded),
              label: Text(context.tr('บันทึกเอกสาร', 'Save document')),
            ),
          ]),
        ),
      ),
    );
  }
}
