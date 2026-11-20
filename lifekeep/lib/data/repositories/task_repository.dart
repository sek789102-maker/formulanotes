import 'package:sqflite/sqflite.dart';

import '../models/task_item.dart';
import 'sql_utils.dart';

class TaskRepository {
  TaskRepository(this.db);
  final Database db;

  Future<List<TaskItem>> all() async {
    final rows = await db.query(
      'tasks',
      orderBy: 'is_completed ASC, due_at IS NULL, due_at ASC, priority DESC, created_at DESC',
    );
    return rows.map(TaskItem.fromMap).toList();
  }

  Future<TaskItem?> byId(String id) async {
    final rows = await db.query('tasks', where: 'id = ?', whereArgs: [id], limit: 1);
    return rows.isEmpty ? null : TaskItem.fromMap(rows.first);
  }

  Future<void> upsert(TaskItem t) =>
      db.insert('tasks', t.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);

  Future<void> delete(String id) => db.delete('tasks', where: 'id = ?', whereArgs: [id]);

  Future<TaskItem?> setCompleted(String id, bool completed, {DateTime? now}) async {
    final ts = (now ?? DateTime.now()).millisecondsSinceEpoch;
    await db.update(
      'tasks',
      {
        'is_completed': completed ? 1 : 0,
        'completed_at': completed ? ts : null,
        'updated_at': ts,
      },
      where: 'id = ?',
      whereArgs: [id],
    );
    return byId(id);
  }

  Future<int> pendingCount() async =>
      Sqflite.firstIntValue(await db.rawQuery('SELECT COUNT(*) FROM tasks WHERE is_completed = 0')) ?? 0;

  Future<List<TaskItem>> pendingDueBefore(DateTime until) async {
    final rows = await db.query(
      'tasks',
      where: 'is_completed = 0 AND due_at IS NOT NULL AND due_at <= ?',
      whereArgs: [until.millisecondsSinceEpoch],
      orderBy: 'due_at ASC, priority DESC',
    );
    return rows.map(TaskItem.fromMap).toList();
  }

  Future<List<TaskItem>> dueBetween(DateTime from, DateTime to) async {
    final rows = await db.query(
      'tasks',
      where: 'due_at IS NOT NULL AND due_at >= ? AND due_at <= ?',
      whereArgs: [from.millisecondsSinceEpoch, to.millisecondsSinceEpoch],
      orderBy: 'due_at ASC',
    );
    return rows.map(TaskItem.fromMap).toList();
  }

  Future<List<TaskItem>> search(String query) async {
    final q = query.trim();
    if (q.isEmpty) return [];
    final like = likePattern(q);
    final rows = await db.rawQuery('''
      SELECT t.* FROM tasks t
      LEFT JOIN categories c ON c.id = t.category_id
      WHERE t.title LIKE ? ESCAPE '\\' OR t.description LIKE ? ESCAPE '\\'
         OR c.name LIKE ? ESCAPE '\\'
      ORDER BY t.is_completed ASC, t.due_at IS NULL, t.due_at ASC
      LIMIT 200''', [like, like, like]);
    return rows.map(TaskItem.fromMap).toList();
  }
}
