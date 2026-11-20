import 'package:sqflite/sqflite.dart';

import '../models/category.dart';

class CategoryRepository {
  CategoryRepository(this.db);
  final Database db;

  Future<List<ItemCategory>> all() async {
    final rows = await db.query('categories', orderBy: 'sort_order ASC, created_at ASC');
    return rows.map(ItemCategory.fromMap).toList();
  }

  Future<ItemCategory?> byId(String id) async {
    final rows = await db.query('categories', where: 'id = ?', whereArgs: [id], limit: 1);
    return rows.isEmpty ? null : ItemCategory.fromMap(rows.first);
  }

  Future<void> upsert(ItemCategory c) =>
      db.insert('categories', c.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);

  Future<int> nextSortOrder() async {
    final r = await db.rawQuery('SELECT MAX(sort_order) AS m FROM categories');
    return ((r.first['m'] as num?)?.toInt() ?? 0) + 1;
  }

  /// Deletes a category atomically; documents move to "Other" and tasks
  /// lose their category. The "Other" category itself cannot be deleted.
  Future<void> delete(String id) async {
    if (id == ItemCategory.otherId) return;
    await db.transaction((txn) async {
      await txn.update('documents', {'category_id': ItemCategory.otherId},
          where: 'category_id = ?', whereArgs: [id]);
      await txn.update('tasks', {'category_id': null}, where: 'category_id = ?', whereArgs: [id]);
      await txn.delete('categories', where: 'id = ?', whereArgs: [id]);
    });
  }

  Future<Map<String, int>> usageCounts() async {
    final rows = await db.rawQuery('''
      SELECT c.id AS id,
        (SELECT COUNT(*) FROM documents d WHERE d.category_id = c.id) +
        (SELECT COUNT(*) FROM tasks t WHERE t.category_id = c.id) AS n
      FROM categories c''');
    return {for (final r in rows) r['id'] as String: (r['n'] as num).toInt()};
  }
}
