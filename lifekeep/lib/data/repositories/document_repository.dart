import 'package:sqflite/sqflite.dart';

import '../models/document_item.dart';
import 'sql_utils.dart';

enum DocumentSort { updatedDesc, createdDesc, titleAsc, expiryAsc }

class DocumentRepository {
  DocumentRepository(this.db);
  final Database db;

  static String _order(DocumentSort s) {
    switch (s) {
      case DocumentSort.updatedDesc:
        return 'is_pinned DESC, updated_at DESC';
      case DocumentSort.createdDesc:
        return 'is_pinned DESC, created_at DESC';
      case DocumentSort.titleAsc:
        return 'is_pinned DESC, title COLLATE NOCASE ASC';
      case DocumentSort.expiryAsc:
        // Documents without expiry go last.
        return 'is_pinned DESC, expiry_date IS NULL, expiry_date ASC';
    }
  }

  Future<List<DocumentItem>> all({DocumentSort sort = DocumentSort.updatedDesc, String? categoryId}) async {
    final rows = await db.query(
      'documents',
      where: categoryId == null ? null : 'category_id = ?',
      whereArgs: categoryId == null ? null : [categoryId],
      orderBy: _order(sort),
    );
    return rows.map(DocumentItem.fromMap).toList();
  }

  Future<DocumentItem?> byId(String id) async {
    final rows = await db.query('documents', where: 'id = ?', whereArgs: [id], limit: 1);
    return rows.isEmpty ? null : DocumentItem.fromMap(rows.first);
  }

  Future<void> upsert(DocumentItem d) =>
      db.insert('documents', d.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);

  Future<void> delete(String id) => db.delete('documents', where: 'id = ?', whereArgs: [id]);

  Future<void> setPinned(String id, bool pinned) => db.update(
        'documents',
        {'is_pinned': pinned ? 1 : 0},
        where: 'id = ?',
        whereArgs: [id],
      );

  Future<int> count() async =>
      Sqflite.firstIntValue(await db.rawQuery('SELECT COUNT(*) FROM documents')) ?? 0;

  /// Documents with an expiry date up to [until] (includes already expired).
  Future<List<DocumentItem>> expiringBefore(DateTime until) async {
    final rows = await db.query(
      'documents',
      where: 'expiry_date IS NOT NULL AND expiry_date <= ?',
      whereArgs: [until.millisecondsSinceEpoch],
      orderBy: 'expiry_date ASC',
    );
    return rows.map(DocumentItem.fromMap).toList();
  }

  Future<List<DocumentItem>> expiringBetween(DateTime from, DateTime to) async {
    final rows = await db.query(
      'documents',
      where: 'expiry_date IS NOT NULL AND expiry_date >= ? AND expiry_date <= ?',
      whereArgs: [from.millisecondsSinceEpoch, to.millisecondsSinceEpoch],
      orderBy: 'expiry_date ASC',
    );
    return rows.map(DocumentItem.fromMap).toList();
  }

  /// Search title, description, notes and category name.
  Future<List<DocumentItem>> search(String query) async {
    final q = query.trim();
    if (q.isEmpty) return [];
    final like = likePattern(q);
    final rows = await db.rawQuery('''
      SELECT d.* FROM documents d
      LEFT JOIN categories c ON c.id = d.category_id
      WHERE d.title LIKE ? ESCAPE '\\' OR d.description LIKE ? ESCAPE '\\'
         OR d.notes LIKE ? ESCAPE '\\' OR c.name LIKE ? ESCAPE '\\'
      ORDER BY d.is_pinned DESC, d.updated_at DESC
      LIMIT 200''', [like, like, like, like]);
    return rows.map(DocumentItem.fromMap).toList();
  }
}
