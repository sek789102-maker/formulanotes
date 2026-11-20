import 'package:sqflite/sqflite.dart';

import '../models/event_item.dart';
import 'sql_utils.dart';

class EventRepository {
  EventRepository(this.db);
  final Database db;

  Future<List<EventItem>> all() async {
    final rows = await db.query('events', orderBy: 'start_at ASC');
    return rows.map(EventItem.fromMap).toList();
  }

  Future<EventItem?> byId(String id) async {
    final rows = await db.query('events', where: 'id = ?', whereArgs: [id], limit: 1);
    return rows.isEmpty ? null : EventItem.fromMap(rows.first);
  }

  Future<void> upsert(EventItem e) =>
      db.insert('events', e.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);

  Future<void> delete(String id) => db.delete('events', where: 'id = ?', whereArgs: [id]);

  /// Events overlapping [from, to].
  Future<List<EventItem>> between(DateTime from, DateTime to) async {
    final f = from.millisecondsSinceEpoch;
    final t = to.millisecondsSinceEpoch;
    final rows = await db.query(
      'events',
      where: 'start_at <= ? AND COALESCE(end_at, start_at) >= ?',
      whereArgs: [t, f],
      orderBy: 'all_day DESC, start_at ASC',
    );
    return rows.map(EventItem.fromMap).toList();
  }

  Future<List<EventItem>> search(String query) async {
    final q = query.trim();
    if (q.isEmpty) return [];
    final like = likePattern(q);
    final rows = await db.rawQuery('''
      SELECT * FROM events
      WHERE title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\' OR location LIKE ? ESCAPE '\\'
      ORDER BY start_at DESC
      LIMIT 200''', [like, like, like]);
    return rows.map(EventItem.fromMap).toList();
  }
}
