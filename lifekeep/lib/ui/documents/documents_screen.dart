import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../data/models/document_item.dart';
import '../../data/repositories/document_repository.dart';
import '../../data/repositories/settings_repository.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../widgets/form_widgets.dart';
import '../widgets/item_tiles.dart';
import 'document_detail_screen.dart';
import 'document_form_screen.dart';

enum DocFilter { all, pinned, expiring, expired }

class DocumentsScreen extends StatefulWidget {
  const DocumentsScreen({super.key});

  @override
  State<DocumentsScreen> createState() => _DocumentsScreenState();
}

class _DocumentsScreenState extends State<DocumentsScreen> {
  final _search = TextEditingController();
  String _query = '';
  DocumentSort _sort = DocumentSort.updatedDesc;
  DocFilter _filter = DocFilter.all;
  String? _categoryId;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  List<DocumentItem> _apply(AppController c, List<DocumentItem> all) {
    final now = DateTime.now();
    final q = _query.trim().toLowerCase();
    return all.where((d) {
      if (_categoryId != null && d.categoryId != _categoryId) return false;
      final st = ExpiryCalc.status(d.expiryDate, now);
      switch (_filter) {
        case DocFilter.all:
          break;
        case DocFilter.pinned:
          if (!d.isPinned) return false;
        case DocFilter.expiring:
          if (st != ExpiryStatus.expiringSoon &&
              st != ExpiryStatus.expiresToday) {
            return false;
          }
        case DocFilter.expired:
          if (st != ExpiryStatus.expired) return false;
      }
      if (q.isEmpty) return true;
      final cat = c.categoryById(d.categoryId).name.toLowerCase();
      return d.title.toLowerCase().contains(q) ||
          d.description.toLowerCase().contains(q) ||
          d.notes.toLowerCase().contains(q) ||
          cat.contains(q);
    }).toList();
  }

  String _sortLabel(BuildContext context, DocumentSort s) => switch (s) {
    DocumentSort.updatedDesc => context.tr('แก้ไขล่าสุด', 'Recently updated'),
    DocumentSort.createdDesc => context.tr('เพิ่มล่าสุด', 'Recently added'),
    DocumentSort.titleAsc => context.tr('ชื่อ ก–ฮ / A–Z', 'Title A–Z'),
    DocumentSort.expiryAsc => context.tr('ใกล้หมดอายุก่อน', 'Expiring first'),
  };

  void _open(DocumentItem d) => Navigator.push(
    context,
    MaterialPageRoute(builder: (_) => DocumentDetailScreen(documentId: d.id)),
  );

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final grid = c.settings[SettingKeys.documentsView] == 'grid';
    final scheme = Theme.of(context).colorScheme;
    final cat = c.maybeCategory(_categoryId);

    Widget chip(
      String label,
      bool selected,
      VoidCallback onTap, {
      IconData? icon,
    }) => Padding(
      padding: const EdgeInsets.only(right: 8),
      child: FilterChip(
        label: Text(label, style: TextStyle(color: selected ? scheme.onPrimary : scheme.onSurface)),
        avatar: icon == null
            ? null
            : Icon(
                icon,
                size: 18,
                color: selected ? scheme.onPrimary : scheme.primary,
              ),
        selected: selected,
        onSelected: (_) => onTap(),
      ),
    );

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        toolbarHeight: 96,
        title: TabTitle(
          icon: Icons.folder_rounded,
          title: context.tr('เอกสาร', 'Documents'),
          subtitle: context.tr(
            'จัดการไฟล์ของคุณได้อย่างง่ายดาย',
            'Your important papers in one place',
          ),
        ),
        actions: [
          IconButton(
            style: lkHeaderButtonStyle(context),
            tooltip: grid
                ? context.tr('มุมมองรายการ', 'List view')
                : context.tr('มุมมองตาราง', 'Grid view'),
            icon: AnimatedSwitcher(
              duration: Motion.fast,
              child: Icon(
                grid ? Icons.view_agenda_outlined : Icons.grid_view_rounded,
                key: ValueKey(grid),
              ),
            ),
            onPressed: () =>
                c.setSetting(SettingKeys.documentsView, grid ? 'list' : 'grid'),
          ),
          const SizedBox(width: Space.sm),
          PopupMenuButton<DocumentSort>(
            style: lkHeaderButtonStyle(context),
            tooltip: context.tr('เรียงลำดับ', 'Sort'),
            icon: const Icon(Icons.swap_vert_rounded),
            initialValue: _sort,
            onSelected: (s) => setState(() => _sort = s),
            itemBuilder: (context) => [
              for (final s in DocumentSort.values)
                CheckedPopupMenuItem(
                  value: s,
                  checked: s == _sort,
                  child: Text(_sortLabel(context, s)),
                ),
            ],
          ),
          const SizedBox(width: Space.page),
        ],
      ),
      floatingActionButton: GradientFab(
        tooltip: context.tr('เพิ่มเอกสาร', 'Add document'),
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const DocumentFormScreen()),
        ),
      ),
      body: ContentWidth(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(
                Space.page,
                0,
                Space.page,
                Space.sm,
              ),
              child: SearchField(
                controller: _search,
                hint: context.tr('ค้นหาเอกสาร', 'Search documents'),
                onChanged: (v) => setState(() => _query = v),
              ),
            ),
            SizedBox(
              height: 48,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: Space.page),
                children: [
                  // Category picker chip (one chip instead of a row per category).
                  Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: InputChip(
                      avatar: Icon(
                        cat?.icon ?? Icons.label_outline_rounded,
                        size: 16,
                        color: cat?.color ?? scheme.onSurfaceVariant,
                      ),
                      label: Text(
                        cat?.name ??
                            context.tr('ทุกหมวดหมู่', 'All categories'),
                        style: TextStyle(
                          color: cat != null ? scheme.onPrimary : scheme.onSurface,
                        ),
                      ),
                      selected: cat != null,
                      deleteIcon: Icon(
                        cat == null
                            ? Icons.expand_more_rounded
                            : Icons.close_rounded,
                        size: 16,
                      ),
                      onDeleted: cat == null
                          ? null
                          : () => setState(() => _categoryId = null),
                      onPressed: () async {
                        final r = await showCategorySheet(
                          context,
                          selectedId: _categoryId,
                          allowNone: true,
                          title: context.tr(
                            'กรองตามหมวดหมู่',
                            'Filter by category',
                          ),
                        );
                        if (r != null) {
                          setState(() => _categoryId = r.isEmpty ? null : r);
                        }
                      },
                    ),
                  ),
                  Container(
                    width: 1,
                    margin: const EdgeInsets.symmetric(
                      vertical: 10,
                      horizontal: 4,
                    ),
                    color: scheme.outlineVariant,
                  ),
                  const SizedBox(width: 4),
                  chip(
                    context.tr('ทั้งหมด', 'All'),
                    _filter == DocFilter.all,
                    () => setState(() => _filter = DocFilter.all),
                    icon: Icons.description_rounded,
                  ),
                  chip(
                    context.tr('ปักหมุด', 'Pinned'),
                    _filter == DocFilter.pinned,
                    () => setState(() => _filter = DocFilter.pinned),
                    icon: Icons.push_pin_rounded,
                  ),
                  chip(
                    context.tr('ใกล้หมดอายุ', 'Expiring'),
                    _filter == DocFilter.expiring,
                    () => setState(() => _filter = DocFilter.expiring),
                    icon: Icons.hourglass_bottom_rounded,
                  ),
                  chip(
                    context.tr('หมดอายุแล้ว', 'Expired'),
                    _filter == DocFilter.expired,
                    () => setState(() => _filter = DocFilter.expired),
                    icon: Icons.event_busy_rounded,
                  ),
                ],
              ),
            ),
            Expanded(
              child: DataBuilder<List<DocumentItem>>(
                deps: [_sort],
                load: (c) => c.s.documents.all(sort: _sort),
                builder: (context, all) {
                  if (all.isEmpty) {
                    return EmptyState(
                      illustration: 'assets/illustrations/empty_documents.png',
                      icon: Icons.folder_open_rounded,
                      title: context.tr('ยังไม่มีเอกสาร', 'No documents yet'),
                      message: context.tr(
                        'เก็บบัตร ใบอนุญาต กรมธรรม์ และเอกสารสำคัญไว้ที่นี่',
                        'Keep IDs, licences, policies and other papers here',
                      ),
                      actionLabel: context.tr('เพิ่มเอกสาร', 'Add document'),
                      onAction: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => const DocumentFormScreen(),
                        ),
                      ),
                    );
                  }
                  final list = _apply(c, all);
                  if (list.isEmpty) {
                    return EmptyState(
                      icon: Icons.search_off_rounded,
                      title: context.tr('ไม่พบเอกสาร', 'No matching documents'),
                      message: context.tr(
                        'ลองเปลี่ยนคำค้นหาหรือตัวกรอง',
                        'Try another search or filter',
                      ),
                      compact: true,
                    );
                  }
                  final showPinnedSection =
                      _filter == DocFilter.all &&
                      _query.isEmpty &&
                      list.any((d) => d.isPinned) &&
                      list.any((d) => !d.isPinned);
                  final pinned = showPinnedSection
                      ? list.where((d) => d.isPinned).toList()
                      : <DocumentItem>[];
                  final rest = showPinnedSection
                      ? list.where((d) => !d.isPinned).toList()
                      : list;

                  Widget section(List<DocumentItem> docs) => grid
                      ? GridView.builder(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          gridDelegate:
                              const SliverGridDelegateWithMaxCrossAxisExtent(
                                maxCrossAxisExtent: 220,
                                mainAxisSpacing: Space.md,
                                crossAxisSpacing: Space.md,
                                childAspectRatio: 0.86,
                              ),
                          itemCount: docs.length,
                          itemBuilder: (context, i) => FadeSlideIn(
                            index: i,
                            child: DocumentGridCard(
                              doc: docs[i],
                              onTap: () => _open(docs[i]),
                            ),
                          ),
                        )
                      : LkGroup(
                          dividerIndent: 72,
                          children: [
                            for (final d in docs)
                              DocumentRow(
                                doc: d,
                                highlight: _query,
                                onTap: () => _open(d),
                              ),
                          ],
                        );

                  return ListView(
                    padding: const EdgeInsets.fromLTRB(
                      Space.page,
                      Space.xs,
                      Space.page,
                      96,
                    ),
                    children: [
                      if (pinned.isNotEmpty) ...[
                        LkSectionHeader(
                          title: context.tr('ปักหมุด', 'Pinned'),
                          count: pinned.length,
                          padding: const EdgeInsets.fromLTRB(
                            Space.xs,
                            Space.sm,
                            Space.xs,
                            Space.sm,
                          ),
                        ),
                        section(pinned),
                      ],
                      LkSectionHeader(
                        title: showPinnedSection
                            ? context.tr('เอกสารอื่น ๆ', 'Other documents')
                            : context.tr(
                                '${list.length} รายการ',
                                '${list.length} items',
                              ),
                        count: showPinnedSection ? rest.length : null,
                        padding: EdgeInsets.fromLTRB(
                          Space.xs,
                          pinned.isEmpty ? Space.sm : Space.xl,
                          Space.xs,
                          Space.sm,
                        ),
                      ),
                      FadeSlideIn(child: section(rest)),
                    ],
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
