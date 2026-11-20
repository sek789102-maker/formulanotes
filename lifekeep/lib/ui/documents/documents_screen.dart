import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../data/models/document_item.dart';
import '../../data/repositories/document_repository.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';
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
          if (st != ExpiryStatus.expiringSoon && st != ExpiryStatus.expiresToday) return false;
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

  String _sortLabel(BuildContext context, DocumentSort s) {
    switch (s) {
      case DocumentSort.updatedDesc:
        return context.tr('แก้ไขล่าสุด', 'Recently updated');
      case DocumentSort.createdDesc:
        return context.tr('เพิ่มล่าสุด', 'Recently added');
      case DocumentSort.titleAsc:
        return context.tr('ชื่อ ก–ฮ / A–Z', 'Title A–Z');
      case DocumentSort.expiryAsc:
        return context.tr('ใกล้หมดอายุก่อน', 'Expiring first');
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    return Scaffold(
      appBar: AppBar(
        title: Text(context.tr('เอกสาร', 'Documents')),
        actions: [
          PopupMenuButton<DocumentSort>(
            tooltip: context.tr('เรียงลำดับ', 'Sort'),
            icon: const Icon(Icons.sort_rounded),
            initialValue: _sort,
            onSelected: (s) => setState(() => _sort = s),
            itemBuilder: (context) => [
              for (final s in DocumentSort.values)
                CheckedPopupMenuItem(value: s, checked: s == _sort, child: Text(_sortLabel(context, s))),
            ],
          ),
          const SizedBox(width: 4),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'fab_docs',
        tooltip: context.tr('เพิ่มเอกสาร', 'Add document'),
        onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const DocumentFormScreen())),
        child: const Icon(Icons.add_rounded),
      ),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
          child: SearchField(
            controller: _search,
            hint: context.tr('ค้นหาชื่อ รายละเอียด หมวดหมู่ หมายเหตุ', 'Search title, details, category, notes'),
            onChanged: (v) => setState(() => _query = v),
          ),
        ),
        SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              for (final f in DocFilter.values)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(switch (f) {
                      DocFilter.all => context.tr('ทั้งหมด', 'All'),
                      DocFilter.pinned => context.tr('ปักหมุด', 'Pinned'),
                      DocFilter.expiring => context.tr('ใกล้หมดอายุ', 'Expiring'),
                      DocFilter.expired => context.tr('หมดอายุแล้ว', 'Expired'),
                    }),
                    selected: _filter == f,
                    onSelected: (_) => setState(() => _filter = f),
                  ),
                ),
            ],
          ),
        ),
        SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: FilterChip(
                  label: Text(context.tr('ทุกหมวดหมู่', 'All categories')),
                  selected: _categoryId == null,
                  onSelected: (_) => setState(() => _categoryId = null),
                ),
              ),
              for (final cat in c.categories)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    avatar: Icon(cat.icon, size: 16, color: cat.color),
                    label: Text(cat.name),
                    selected: _categoryId == cat.id,
                    onSelected: (v) => setState(() => _categoryId = v ? cat.id : null),
                  ),
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
                  icon: Icons.folder_open_rounded,
                  title: context.tr('ยังไม่มีเอกสาร', 'No documents yet'),
                  message: context.tr('เพิ่มบัตร ใบอนุญาต กรมธรรม์ หรือเอกสารสำคัญอื่น ๆ',
                      'Add ID cards, licences, policies or other important papers'),
                  actionLabel: context.tr('เพิ่มเอกสาร', 'Add document'),
                  onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const DocumentFormScreen())),
                );
              }
              final list = _apply(c, all);
              if (list.isEmpty) {
                return EmptyState(
                  icon: Icons.search_off_rounded,
                  title: context.tr('ไม่พบเอกสาร', 'No matching documents'),
                  message: context.tr('ลองเปลี่ยนคำค้นหาหรือตัวกรอง', 'Try another search or filter'),
                  compact: true,
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
                itemCount: list.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, i) => DocumentCard(
                  doc: list[i],
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => DocumentDetailScreen(documentId: list[i].id)),
                  ),
                ),
              );
            },
          ),
        ),
      ]),
    );
  }
}
