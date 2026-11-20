import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/ids.dart';
import '../../core/utils/validators.dart';
import '../../data/models/category.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';

class CategoriesScreen extends StatelessWidget {
  const CategoriesScreen({super.key});

  Future<void> _edit(BuildContext context, {ItemCategory? cat}) async {
    final c = context.read<AppController>();
    final result = await showDialog<ItemCategory>(
      context: context,
      builder: (_) => _CategoryDialog(category: cat),
    );
    if (result == null || !context.mounted) return;
    var toSave = result;
    if (cat == null) toSave = result.copyWith(sortOrder: await c.s.categories.nextSortOrder());
    if (!context.mounted) return;
    await runAction(
      context,
      () => c.saveCategory(toSave),
      success: cat == null ? context.tr('เพิ่มหมวดหมู่แล้ว', 'Category added') : context.tr('บันทึกแล้ว', 'Saved'),
    );
  }

  Future<void> _delete(BuildContext context, ItemCategory cat, int usage) async {
    final c = context.read<AppController>();
    final other = c.categoryById(ItemCategory.otherId).name;
    final ok = await confirmDialog(
      context,
      title: context.tr('ลบหมวดหมู่ “${cat.name}”?', 'Delete “${cat.name}”?'),
      message: usage == 0
          ? context.tr('หมวดหมู่นี้ยังไม่มีรายการ', 'This category has no items.')
          : context.tr(
              'มี $usage รายการในหมวดหมู่นี้ เอกสารจะถูกย้ายไป “$other” และงานจะไม่มีหมวดหมู่ ข้อมูลจะไม่ถูกลบ',
              '$usage item(s) use this category. Documents move to “$other”, tasks become uncategorised. No items are deleted.',
            ),
      confirmLabel: context.tr('ลบหมวดหมู่', 'Delete category'),
      destructive: true,
    );
    if (!ok || !context.mounted) return;
    await runAction(context, () => c.deleteCategory(cat), success: context.tr('ลบหมวดหมู่แล้ว', 'Category deleted'));
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('จัดการหมวดหมู่', 'Manage categories'))),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _edit(context),
        icon: const Icon(Icons.add_rounded),
        label: Text(context.tr('เพิ่มหมวดหมู่', 'Add category')),
      ),
      body: DataBuilder<Map<String, int>>(
        load: (c) => c.s.categories.usageCounts(),
        builder: (context, usage) => ContentWidth(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(Space.page, Space.xs, Space.page, 96),
            children: [
              LkGroup(
                dividerIndent: 64,
                children: [
                  for (final cat in c.categories)
                    LkRow(
                      leading: IconBadge(icon: cat.icon, color: cat.color),
                      title: cat.name,
                      subtitle: Text(context.tr('${usage[cat.id] ?? 0} รายการ', '${usage[cat.id] ?? 0} item(s)')),
                      onTap: () => _edit(context, cat: cat),
                      trailing: cat.id == ItemCategory.otherId
                          ? Pill(
                              label: context.tr('ค่าเริ่มต้น', 'Default'),
                              color: Theme.of(context).colorScheme.onSurfaceVariant,
                              background: Theme.of(context).colorScheme.surfaceContainerHigh,
                              dense: true,
                            )
                          : IconButton(
                              tooltip: context.tr('ลบ', 'Delete'),
                              icon: const Icon(Icons.delete_outline_rounded, size: 21),
                              onPressed: () => _delete(context, cat, usage[cat.id] ?? 0),
                            ),
                    ),
                ],
              ),
              const SizedBox(height: Space.md),
              Text(
                context.tr(
                  'แตะเพื่อแก้ไขชื่อ สี หรือไอคอน · ลบหมวดหมู่แล้วข้อมูลจะไม่ถูกลบ',
                  'Tap to rename or change colour/icon · deleting a category never deletes items',
                ),
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.labelSmall,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CategoryDialog extends StatefulWidget {
  const _CategoryDialog({this.category});
  final ItemCategory? category;

  @override
  State<_CategoryDialog> createState() => _CategoryDialogState();
}

class _CategoryDialogState extends State<_CategoryDialog> {
  final _form = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.category?.name);
  late int _color = widget.category?.colorValue ?? AppColors.categoryPalette[1].toARGB32();
  late String _icon = widget.category?.iconKey ?? 'folder';

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return AlertDialog(
      title: Text(
        widget.category == null
            ? context.tr('เพิ่มหมวดหมู่', 'Add category')
            : context.tr('แก้ไขหมวดหมู่', 'Edit category'),
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _form,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextFormField(
                controller: _name,
                autofocus: true,
                decoration: InputDecoration(labelText: context.tr('ชื่อหมวดหมู่', 'Name')),
                validator: Validators.categoryName,
              ),
              const SizedBox(height: 16),
              Text(context.tr('สี', 'Colour'), style: Theme.of(context).textTheme.labelLarge),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final col in AppColors.categoryPalette)
                    GestureDetector(
                      onTap: () => setState(() => _color = col.toARGB32()),
                      child: Container(
                        width: 30,
                        height: 30,
                        decoration: BoxDecoration(
                          color: col,
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: _color == col.toARGB32() ? scheme.onSurface : Colors.transparent,
                            width: 2.5,
                          ),
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 16),
              Text(context.tr('ไอคอน', 'Icon'), style: Theme.of(context).textTheme.labelLarge),
              const SizedBox(height: 8),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  for (final e in CategoryIcons.all.entries)
                    InkWell(
                      borderRadius: BorderRadius.circular(10),
                      onTap: () => setState(() => _icon = e.key),
                      child: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: _icon == e.key ? Color(_color).withValues(alpha: 0.15) : null,
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: _icon == e.key ? Color(_color) : scheme.outlineVariant),
                        ),
                        child: Icon(e.value, size: 20, color: _icon == e.key ? Color(_color) : scheme.onSurfaceVariant),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text(context.tr('ยกเลิก', 'Cancel'))),
        FilledButton(
          onPressed: () {
            if (!_form.currentState!.validate()) return;
            final base = widget.category;
            Navigator.pop(
              context,
              base == null
                  ? ItemCategory(
                      id: newId(),
                      name: _name.text.trim(),
                      colorValue: _color,
                      iconKey: _icon,
                      createdAt: DateTime.now(),
                    )
                  : base.copyWith(name: _name.text.trim(), colorValue: _color, iconKey: _icon),
            );
          },
          child: Text(context.tr('บันทึก', 'Save')),
        ),
      ],
    );
  }
}
