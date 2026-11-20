import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import '../../core/l10n/lang.dart';
import '../../core/utils/errors.dart';
import '../models/category.dart';

/// Owns the SQLite schema and migrations.
///
/// Rules for future versions (long-term compatibility):
///  * never drop or rename columns that hold user data;
///  * add new columns with defaults via a migration step in [_migrations];
///  * bump [schemaVersion] by one per change.
class AppDatabase {
  AppDatabase._();

  static const fileName = 'lifekeep.db';
  static const schemaVersion = 1;

  static Future<Database> open({
    DatabaseFactory? factory,
    String? path,
    bool englishDefaults = false,
  }) async {
    final f = factory ?? databaseFactory;
    final dbPath = path ?? p.join(await f.getDatabasesPath(), fileName);
    try {
      return await f.openDatabase(
        dbPath,
        options: OpenDatabaseOptions(
          version: schemaVersion,
          onConfigure: (db) async {
            // Keep data safe on crashes / power loss.
            await db.execute('PRAGMA foreign_keys = OFF');
          },
          onCreate: (db, version) async {
            final batch = db.batch();
            createSchema(batch);
            for (final c in DefaultCategories.build(englishDefaults)) {
              batch.insert('categories', c.toMap());
            }
            await batch.commit(noResult: true);
          },
          onUpgrade: (db, oldV, newV) async {
            // Runs inside a transaction: either every step succeeds or the
            // database stays at the old version.
            for (var v = oldV + 1; v <= newV; v++) {
              final step = _migrations[v];
              if (step != null) await step(db);
            }
          },
          onDowngrade: (db, oldV, newV) async {
            throw AppException(
              ErrorKind.database,
              Lang.t(
                'ฐานข้อมูลถูกสร้างโดย LIFEKEEP เวอร์ชันที่ใหม่กว่า กรุณาติดตั้งเวอร์ชันล่าสุด (ข้อมูลยังไม่ถูกแก้ไข)',
                'The database was created by a newer LIFEKEEP version. Please install the latest version (your data was not changed).',
              ),
            );
          },
        ),
      );
    } on AppException {
      rethrow;
    } catch (e) {
      throw AppException(
        ErrorKind.database,
        Lang.t('ไม่สามารถเปิดฐานข้อมูลได้', 'Could not open the database'),
        e,
      );
    }
  }

  /// Version-specific migration steps. Example for a future v2:
  ///   2: (db) async => db.execute("ALTER TABLE tasks ADD COLUMN tags TEXT NOT NULL DEFAULT ''"),
  static final Map<int, Future<void> Function(Database db)> _migrations = {};

  static void createSchema(Batch b) {
    b.execute('''
      CREATE TABLE categories (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        color INTEGER NOT NULL,
        icon TEXT NOT NULL,
        is_default INTEGER NOT NULL DEFAULT 0,
        sort_order INTEGER NOT NULL DEFAULT 0,
        created_at INTEGER NOT NULL
      )''');
    b.execute('''
      CREATE TABLE documents (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        category_id TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        notes TEXT NOT NULL DEFAULT '',
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL,
        expiry_date INTEGER,
        reminder TEXT NOT NULL DEFAULT '',
        attachments TEXT NOT NULL DEFAULT '[]',
        is_pinned INTEGER NOT NULL DEFAULT 0
      )''');
    b.execute('CREATE INDEX idx_documents_expiry ON documents(expiry_date)');
    b.execute('CREATE INDEX idx_documents_category ON documents(category_id)');
    b.execute('''
      CREATE TABLE tasks (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        category_id TEXT,
        due_at INTEGER,
        has_time INTEGER NOT NULL DEFAULT 0,
        priority INTEGER NOT NULL DEFAULT 1,
        is_completed INTEGER NOT NULL DEFAULT 0,
        completed_at INTEGER,
        reminder TEXT NOT NULL DEFAULT '',
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      )''');
    b.execute('CREATE INDEX idx_tasks_due ON tasks(due_at)');
    b.execute('''
      CREATE TABLE events (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        location TEXT NOT NULL DEFAULT '',
        start_at INTEGER NOT NULL,
        end_at INTEGER,
        all_day INTEGER NOT NULL DEFAULT 0,
        reminder TEXT NOT NULL DEFAULT '',
        color INTEGER,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      )''');
    b.execute('CREATE INDEX idx_events_start ON events(start_at)');
    b.execute('''
      CREATE TABLE settings (
        key TEXT PRIMARY KEY,
        value TEXT
      )''');
    // Tracks OS notification ids so they can be cancelled/replaced reliably.
    b.execute('''
      CREATE TABLE scheduled_notifications (
        notif_id INTEGER PRIMARY KEY,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        fire_at INTEGER NOT NULL
      )''');
    b.execute('CREATE INDEX idx_sched_entity ON scheduled_notifications(entity_type, entity_id)');
  }

  /// Tables holding user content (used by backup / delete-all).
  static const contentTables = ['documents', 'tasks', 'events', 'categories'];
}
