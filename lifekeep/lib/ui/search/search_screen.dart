import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../data/models/document_item.dart';
import '../../data/models/event_item.dart';
import '../../data/models/task_item.dart';
import '../../data/repositories/settings_repository.dart';
import '../../state/app_controller.dart';
import '../calendar/event_form_screen.dart';
import '../design/design.dart';
import '../documents/document_detail_screen.dart';
import '../tasks/task_form_screen.dart';
import '../widgets/item_tiles.dart';

class _Results {
  _Results(this.docs, this.tasks, this.events);
  final List<DocumentItem> docs;
  final List<TaskItem> tasks;
  final List<EventItem> events;
  int get total => docs.length + tasks.length + events.length;
}

enum _Scope { all, documents, tasks, events }

/// Real-time search over documents, tasks and events (title, details,
/// category, notes, location) using SQLite LIKE queries.
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final _ctrl = TextEditingController();
  String _q = '';
  _Scope _scope = _Scope.all;
  Timer? _saveTimer;

  @override
  void dispose() {
    _saveTimer?.cancel();
    _ctrl.dispose();
    super.dispose();
  }

  List<String> _recent(AppController c) {
    try {
      final raw = c.settings[SettingKeys.recentSearches];
      if (raw == null) return [];
      return (jsonDecode(raw) as List).whereType<String>().toList();
    } catch (_) {
      return [];
    }
  }

  void _remember(String q) {
    final term = q.trim();
    if (term.length < 2) return;
    final c = context.read<AppController>();
    final list = _recent(c)..remove(term);
    list.insert(0, term);
    c.setSetting(SettingKeys.recentSearches, jsonEncode(list.take(8).toList()));
  }

  void _onChanged(String v) {
    setState(() => _q = v);
    // Remember a term once the user pauses typing.
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 1500), () {
      if (mounted) _remember(v);
    });
  }

  Future<_Results> _search(AppController c) async {
    if (_q.trim().isEmpty) return _Results(const [], const [], const []);
    return _Results(await c.s.documents.search(_q), await c.s.tasks.search(_q), await c.s.events.search(_q));
  }

  void _open(Widget page) {
    _remember(_q);
    Navigator.push(context, MaterialPageRoute(builder: (_) => page));
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final t = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: Padding(
          padding: const EdgeInsets.only(right: Space.page),
          child: SearchField(
            controller: _ctrl,
            autofocus: true,
            hint: context.tr('ค้นหาเอกสาร งาน กิจกรรม', 'Search documents, tasks, events'),
            onChanged: _onChanged,
            onSubmitted: _remember,
          ),
        ),
      ),
      body: ContentWidth(
        child: DataBuilder<_Results>(
          deps: [_q],
          load: _search,
          builder: (context, r) {
            if (_q.trim().isEmpty) {
              final recent = _recent(c);
              return ListView(
                padding: const EdgeInsets.fromLTRB(Space.page, Space.sm, Space.page, Space.xl),
                children: [
                  if (recent.isNotEmpty) ...[
                    LkSectionHeader(
                      title: context.tr('ค้นหาล่าสุด', 'Recent'),
                      actionLabel: context.tr('ล้าง', 'Clear'),
                      onAction: () => c.setSetting(SettingKeys.recentSearches, null),
                      padding: const EdgeInsets.fromLTRB(Space.xs, Space.xs, Space.xs, Space.sm),
                    ),
                    Wrap(
                      spacing: Space.sm,
                      runSpacing: Space.sm,
                      children: [
                        for (final q in recent)
                          ActionChip(
                            avatar: const Icon(Icons.history_rounded, size: 16),
                            label: Text(q),
                            onPressed: () {
                              _ctrl.text = q;
                              setState(() => _q = q);
                            },
                          ),
                      ],
                    ),
                  ],
                  EmptyState(
                    icon: Icons.manage_search_rounded,
                    title: context.tr('ค้นหาทุกอย่างในเครื่อง', 'Search everything on this device'),
                    message: context.tr(
                      'ชื่อ รายละเอียด หมวดหมู่ หมายเหตุ และสถานที่',
                      'Titles, details, categories, notes and places',
                    ),
                    compact: recent.isNotEmpty,
                  ),
                ],
              );
            }
            if (r.total == 0) {
              return EmptyState(
                icon: Icons.search_off_rounded,
                title: context.tr('ไม่พบผลลัพธ์', 'No results'),
                message: context.tr('ไม่พบรายการที่ตรงกับ “$_q”', 'Nothing matches “$_q”'),
              );
            }
            Widget scopeChip(_Scope s, String label, int n) => Padding(
              padding: const EdgeInsets.only(right: 6),
              child: ChoiceChip(
                label: Text(n > 0 || s == _Scope.all ? '$label  $n' : label),
                selected: _scope == s,
                onSelected: n == 0 && s != _Scope.all ? null : (_) => setState(() => _scope = s),
              ),
            );
            final showDocs = (_scope == _Scope.all || _scope == _Scope.documents) && r.docs.isNotEmpty;
            final showTasks = (_scope == _Scope.all || _scope == _Scope.tasks) && r.tasks.isNotEmpty;
            final showEvents = (_scope == _Scope.all || _scope == _Scope.events) && r.events.isNotEmpty;
            return Column(
              children: [
                SizedBox(
                  height: 44,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: Space.page),
                    children: [
                      scopeChip(_Scope.all, context.tr('ทั้งหมด', 'All'), r.total),
                      scopeChip(_Scope.documents, context.tr('เอกสาร', 'Documents'), r.docs.length),
                      scopeChip(_Scope.tasks, context.tr('งาน', 'Tasks'), r.tasks.length),
                      scopeChip(_Scope.events, context.tr('กิจกรรม', 'Events'), r.events.length),
                    ],
                  ),
                ),
                Expanded(
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
                    children: [
                      if (showDocs) ...[
                        LkSectionHeader(
                          title: context.tr('เอกสาร', 'Documents'),
                          count: r.docs.length,
                          padding: const EdgeInsets.fromLTRB(Space.xs, Space.md, Space.xs, Space.sm),
                        ),
                        LkGroup(
                          dividerIndent: 72,
                          children: [
                            for (final d in r.docs)
                              DocumentRow(
                                doc: d,
                                highlight: _q,
                                onTap: () => _open(DocumentDetailScreen(documentId: d.id)),
                              ),
                          ],
                        ),
                      ],
                      if (showTasks) ...[
                        LkSectionHeader(title: context.tr('งาน', 'Tasks'), count: r.tasks.length),
                        LkGroup(
                          dividerIndent: 52,
                          children: [
                            for (final tk in r.tasks)
                              TaskRow(
                                task: tk,
                                highlight: _q,
                                onTap: () => _open(TaskFormScreen(task: tk)),
                                onToggle: (v) => runAction(context, () => c.setTaskCompleted(tk, v)),
                              ),
                          ],
                        ),
                      ],
                      if (showEvents) ...[
                        LkSectionHeader(title: context.tr('กิจกรรม', 'Events'), count: r.events.length),
                        LkGroup(
                          dividerIndent: 80,
                          children: [
                            for (final e in r.events)
                              EventRow(
                                event: e,
                                highlight: _q,
                                onTap: () => _open(EventFormScreen(event: e)),
                              ),
                          ],
                        ),
                      ],
                      const SizedBox(height: Space.md),
                      Center(
                        child: Text(context.tr('พบ ${r.total} รายการ', '${r.total} results'), style: t.labelSmall),
                      ),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
