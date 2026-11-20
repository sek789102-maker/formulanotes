import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../data/models/document_item.dart';
import '../../data/models/event_item.dart';
import '../../data/models/task_item.dart';
import '../../state/app_controller.dart';
import '../calendar/event_form_screen.dart';
import '../documents/document_detail_screen.dart';
import '../tasks/task_form_screen.dart';
import '../widgets/common.dart';
import '../widgets/item_tiles.dart';

class _Results {
  _Results(this.docs, this.tasks, this.events);
  final List<DocumentItem> docs;
  final List<TaskItem> tasks;
  final List<EventItem> events;
  bool get isEmpty => docs.isEmpty && tasks.isEmpty && events.isEmpty;
}

/// Real-time search over documents, tasks and events (title, details,
/// category and notes) using SQLite LIKE queries.
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final _ctrl = TextEditingController();
  String _q = '';

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  Future<_Results> _search(AppController c) async {
    if (_q.trim().isEmpty) return _Results(const [], const [], const []);
    return _Results(
      await c.s.documents.search(_q),
      await c.s.tasks.search(_q),
      await c.s.events.search(_q),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: Padding(
          padding: const EdgeInsets.only(right: 16),
          child: SearchField(
            controller: _ctrl,
            autofocus: true,
            hint: context.tr('ค้นหาเอกสาร งาน กิจกรรม', 'Search documents, tasks, events'),
            onChanged: (v) => setState(() => _q = v),
          ),
        ),
      ),
      body: DataBuilder<_Results>(
        deps: [_q],
        load: _search,
        builder: (context, r) {
          if (_q.trim().isEmpty) {
            return EmptyState(
              icon: Icons.manage_search_rounded,
              title: context.tr('พิมพ์เพื่อค้นหา', 'Type to search'),
              message: context.tr('ค้นหาจากชื่อ รายละเอียด หมวดหมู่ และหมายเหตุ', 'Searches titles, details, categories and notes'),
            );
          }
          if (r.isEmpty) {
            return EmptyState(
              icon: Icons.search_off_rounded,
              title: context.tr('ไม่พบผลลัพธ์', 'No results'),
              message: context.tr('ไม่พบรายการที่ตรงกับ “$_q”', 'Nothing matches “$_q”'),
            );
          }
          return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), children: [
            if (r.docs.isNotEmpty) ...[
              SectionHeader(title: context.tr('เอกสาร (${r.docs.length})', 'Documents (${r.docs.length})')),
              for (final d in r.docs)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DocumentCard(
                    doc: d,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => DocumentDetailScreen(documentId: d.id))),
                  ),
                ),
            ],
            if (r.tasks.isNotEmpty) ...[
              SectionHeader(title: context.tr('งาน (${r.tasks.length})', 'Tasks (${r.tasks.length})')),
              for (final t in r.tasks)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: TaskTile(
                    task: t,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskFormScreen(task: t))),
                    onToggle: (v) => runAction(context, () => context.read<AppController>().setTaskCompleted(t, v)),
                  ),
                ),
            ],
            if (r.events.isNotEmpty) ...[
              SectionHeader(title: context.tr('กิจกรรม (${r.events.length})', 'Events (${r.events.length})')),
              for (final e in r.events)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: EventTile(
                    event: e,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => EventFormScreen(event: e))),
                  ),
                ),
            ],
          ]);
        },
      ),
    );
  }
}
