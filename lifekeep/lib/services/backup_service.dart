import 'dart:convert';
import 'dart:io';

import 'package:archive/archive_io.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import '../core/l10n/lang.dart';
import '../core/utils/errors.dart';
import '../data/database/app_database.dart';
import '../data/models/category.dart';
import '../data/models/document_item.dart';
import '../data/models/event_item.dart';
import '../data/models/task_item.dart';
import '../data/repositories/settings_repository.dart';
import 'attachment_service.dart';

/// LIFEKEEP backup file format (ZIP container):
///
///   manifest.json   – format id, format version, created date, counts,
///                     SHA-256 of data.json and of every attachment
///   data.json       – documents, tasks, events, categories, settings
///   attachments/*   – optional image files
///
/// The format is versioned; newer app versions must keep reading older
/// format versions. Unknown extra fields are ignored, missing fields get
/// defaults, so backups stay restorable for the long term.
class BackupFormat {
  BackupFormat._();
  static const id = 'lifekeep-backup';
  static const version = 1;
  static const manifestName = 'manifest.json';
  static const dataName = 'data.json';
  static const attachmentsPrefix = 'attachments/';
}

enum RestoreMode { replace, merge }

class BackupFileInfo {
  const BackupFileInfo({required this.path, required this.size, required this.modified, required this.isAuto});
  final String path;
  final int size;
  final DateTime modified;
  final bool isAuto;
  String get name => p.basename(path);
}

class BackupCounts {
  const BackupCounts({this.documents = 0, this.tasks = 0, this.events = 0, this.categories = 0, this.attachments = 0});
  final int documents;
  final int tasks;
  final int events;
  final int categories;
  final int attachments;

  Map<String, int> toJson() => {
    'documents': documents,
    'tasks': tasks,
    'events': events,
    'categories': categories,
    'attachments': attachments,
  };

  static BackupCounts fromJson(Object? m) {
    if (m is! Map) return const BackupCounts();
    int g(String k) => (m[k] as num?)?.toInt() ?? 0;
    return BackupCounts(
      documents: g('documents'),
      tasks: g('tasks'),
      events: g('events'),
      categories: g('categories'),
      attachments: g('attachments'),
    );
  }
}

/// Result of validating a backup file before restore.
class BackupInspection {
  BackupInspection({
    required this.path,
    this.formatVersion,
    this.createdAt,
    this.appVersion,
    this.counts = const BackupCounts(),
    this.includesAttachments = false,
    List<String>? warnings,
    this.fatal,
    this.data,
  }) : warnings = warnings ?? [];

  final String path;
  final int? formatVersion;
  final DateTime? createdAt;
  final String? appVersion;
  final BackupCounts counts;
  final bool includesAttachments;

  /// Non-fatal issues (e.g. some records skipped, missing attachments).
  final List<String> warnings;

  /// If set, the backup must not be restored.
  final String? fatal;

  /// Parsed + normalised content (only when not fatal).
  final ValidatedData? data;

  bool get canRestore => fatal == null && data != null;
}

class ValidatedData {
  ValidatedData({
    required this.documents,
    required this.tasks,
    required this.events,
    required this.categories,
    required this.settings,
    required this.attachments,
  });
  final List<DocumentItem> documents;
  final List<TaskItem> tasks;
  final List<EventItem> events;
  final List<ItemCategory> categories;
  final Map<String, String> settings;

  /// attachment name -> expected sha256 (hex)
  final Map<String, String> attachments;
}

class RestoreResult {
  const RestoreResult({required this.counts, required this.skippedDuplicates, this.safetyBackupPath});
  final BackupCounts counts;
  final int skippedDuplicates;
  final String? safetyBackupPath;
}

class BackupService {
  BackupService({
    required this.db,
    required this.attachments,
    required this.settings,
    required this.backupDir,
    required this.appVersion,
  });

  final Database db;
  final AttachmentService attachments;
  final SettingsRepository settings;
  final Directory backupDir;
  final String appVersion;

  static const autoPrefix = 'LIFEKEEP-auto-';
  static const manualPrefix = 'LIFEKEEP-backup-';
  static const safetyPrefix = 'LIFEKEEP-before-restore-';

  static String _stamp(DateTime d) {
    String two(int v) => v.toString().padLeft(2, '0');
    return '${d.year}${two(d.month)}${two(d.day)}-${two(d.hour)}${two(d.minute)}${two(d.second)}';
  }

  static String fileNameFor(String prefix, DateTime d) => '$prefix${_stamp(d)}.zip';

  Future<void> _ensureDir() async {
    if (!await backupDir.exists()) await backupDir.create(recursive: true);
  }

  // ---------------------------------------------------------------- create

  /// Builds the JSON payload from a consistent database snapshot.
  Future<Map<String, Object?>> exportData() async {
    return db.transaction((txn) async {
      final s = await txn.query('settings');
      return {
        'documents': await txn.query('documents'),
        'tasks': await txn.query('tasks'),
        'events': await txn.query('events'),
        'categories': await txn.query('categories'),
        'settings': {
          for (final r in s)
            if (SettingKeys.backupable.contains(r['key']) && r['value'] != null) r['key'] as String: r['value'],
        },
      };
    });
  }

  /// Creates a backup ZIP in the local backup folder. The file is written to
  /// a temporary name, verified, and only then renamed — a half-written
  /// backup can never replace a good one.
  Future<BackupFileInfo> createBackup({
    required bool includeAttachments,
    String prefix = manualPrefix,
    DateTime? now,
  }) async {
    final n = now ?? DateTime.now();
    await _ensureDir();
    var finalPath = p.join(backupDir.path, fileNameFor(prefix, n));
    var k = 1;
    while (await File(finalPath).exists()) {
      finalPath = p.join(backupDir.path, '${p.basenameWithoutExtension(fileNameFor(prefix, n))}-$k.zip');
      k++;
    }
    final tmpPath = '$finalPath.partial';
    try {
      final data = await exportData();
      final dataJson = utf8.encode(jsonEncode(data));
      final docs = (data['documents'] as List).cast<Map<String, Object?>>();

      // Collect attachments referenced by documents that actually exist.
      final attachmentHashes = <String, String>{};
      final attachmentFiles = <String, File>{};
      if (includeAttachments) {
        for (final d in docs) {
          for (final name in DocumentItem.fromMap(d).attachmentPaths) {
            if (attachmentFiles.containsKey(name) || !AttachmentService.isSafeName(name)) continue;
            final f = attachments.fileFor(name);
            if (await f.exists()) {
              attachmentFiles[name] = f;
              attachmentHashes[name] = (await sha256.bind(f.openRead()).first).toString();
            }
          }
        }
      }

      final manifest = {
        'format': BackupFormat.id,
        'formatVersion': BackupFormat.version,
        'schemaVersion': AppDatabase.schemaVersion,
        'appVersion': appVersion,
        'createdAt': n.toUtc().toIso8601String(),
        'includesAttachments': includeAttachments,
        'counts': BackupCounts(
          documents: docs.length,
          tasks: (data['tasks'] as List).length,
          events: (data['events'] as List).length,
          categories: (data['categories'] as List).length,
          attachments: attachmentFiles.length,
        ).toJson(),
        'dataSha256': sha256.convert(dataJson).toString(),
        'attachments': attachmentHashes,
      };

      final enc = ZipFileEncoder();
      enc.create(tmpPath);
      enc.addArchiveFile(
        ArchiveFile.string(BackupFormat.manifestName, const JsonEncoder.withIndent('  ').convert(manifest)),
      );
      enc.addArchiveFile(ArchiveFile.bytes(BackupFormat.dataName, dataJson));
      for (final e in attachmentFiles.entries) {
        // Streams from disk; images are not loaded fully into memory.
        await enc.addFile(e.value, '${BackupFormat.attachmentsPrefix}${e.key}');
      }
      await enc.close();

      final check = await inspect(tmpPath, verifyAttachments: true);
      if (!check.canRestore || check.warnings.isNotEmpty) {
        throw AppException(
          ErrorKind.backup,
          Lang.t('ตรวจสอบไฟล์สำรองข้อมูลไม่ผ่าน', 'Backup verification failed'),
          check.fatal ?? check.warnings.join('; '),
        );
      }
      await File(tmpPath).rename(finalPath);
      final f = File(finalPath);
      return BackupFileInfo(
        path: finalPath,
        size: await f.length(),
        modified: await f.lastModified(),
        isAuto: prefix == autoPrefix,
      );
    } catch (e) {
      try {
        final t = File(tmpPath);
        if (await t.exists()) await t.delete();
      } catch (_) {}
      if (e is AppException) rethrow;
      throw AppException(
        ErrorKind.backup,
        Lang.t('สำรองข้อมูลไม่สำเร็จ พื้นที่เก็บข้อมูลอาจไม่พอ', 'Backup failed. Storage may be full.'),
        e,
      );
    }
  }

  Future<List<BackupFileInfo>> listBackups() async {
    if (!await backupDir.exists()) return [];
    final list = <BackupFileInfo>[];
    await for (final e in backupDir.list()) {
      if (e is File && e.path.endsWith('.zip')) {
        final st = await e.stat();
        list.add(
          BackupFileInfo(
            path: e.path,
            size: st.size,
            modified: st.modified,
            isAuto: p.basename(e.path).startsWith(autoPrefix),
          ),
        );
      }
    }
    list.sort((a, b) => b.modified.compareTo(a.modified));
    return list;
  }

  Future<void> deleteBackup(String path) async {
    if (!p.isWithin(backupDir.path, path)) return;
    final f = File(path);
    if (await f.exists()) await f.delete();
  }

  /// Keeps the newest [keep] automatic backups and trims older ones while
  /// the total exceeds [maxBytes]. The newest backup is never deleted.
  /// Must only be called after a new backup was created and verified.
  Future<int> pruneAutoBackups({required int keep, required int maxBytes}) async {
    final autos = (await listBackups()).where((b) => b.isAuto).toList();
    var removed = 0;
    var total = autos.fold<int>(0, (s, b) => s + b.size);
    for (var i = autos.length - 1; i >= 1; i--) {
      final tooMany = i >= keep;
      final tooBig = total > maxBytes;
      if (!tooMany && !tooBig) continue;
      await deleteBackup(autos[i].path);
      total -= autos[i].size;
      removed++;
    }
    return removed;
  }

  // --------------------------------------------------------------- inspect

  /// Validates structure, version and checksums without touching user data.
  Future<BackupInspection> inspect(String path, {bool verifyAttachments = false}) async {
    String t(String th, String en) => Lang.t(th, en);
    final file = File(path);
    if (!await file.exists()) {
      return BackupInspection(path: path, fatal: t('ไม่พบไฟล์สำรองข้อมูล', 'Backup file not found'));
    }
    InputFileStream? input;
    try {
      input = InputFileStream(path);
      final Archive archive;
      try {
        archive = ZipDecoder().decodeStream(input);
      } catch (e) {
        return BackupInspection(
          path: path,
          fatal: t(
            'ไฟล์ไม่ใช่ไฟล์สำรองข้อมูล LIFEKEEP หรือไฟล์เสียหาย',
            'Not a LIFEKEEP backup or the file is damaged',
          ),
        );
      }
      final mf = archive.findFile(BackupFormat.manifestName);
      final df = archive.findFile(BackupFormat.dataName);
      if (mf == null || df == null) {
        return BackupInspection(
          path: path,
          fatal: t('ไฟล์ไม่ใช่ไฟล์สำรองข้อมูล LIFEKEEP', 'This is not a LIFEKEEP backup file'),
        );
      }
      final Map<String, dynamic> manifest;
      try {
        manifest = jsonDecode(utf8.decode(mf.readBytes() ?? const [])) as Map<String, dynamic>;
      } catch (_) {
        return BackupInspection(path: path, fatal: t('ข้อมูลกำกับไฟล์เสียหาย', 'Backup manifest is damaged'));
      }
      if (manifest['format'] != BackupFormat.id) {
        return BackupInspection(
          path: path,
          fatal: t('ไฟล์ไม่ใช่ไฟล์สำรองข้อมูล LIFEKEEP', 'This is not a LIFEKEEP backup file'),
        );
      }
      final fv = (manifest['formatVersion'] as num?)?.toInt();
      if (fv == null || fv < 1) {
        return BackupInspection(path: path, fatal: t('ไม่ทราบเวอร์ชันของไฟล์สำรอง', 'Unknown backup version'));
      }
      if (fv > BackupFormat.version) {
        return BackupInspection(
          path: path,
          formatVersion: fv,
          fatal: t(
            'ไฟล์นี้สร้างจาก LIFEKEEP เวอร์ชันที่ใหม่กว่า กรุณาอัปเดตแอปก่อนกู้คืน',
            'This backup was made by a newer LIFEKEEP version. Please update the app first.',
          ),
        );
      }
      final createdAt = DateTime.tryParse(manifest['createdAt'] as String? ?? '')?.toLocal();
      final counts = BackupCounts.fromJson(manifest['counts']);
      final includesAtt = manifest['includesAttachments'] == true;

      final dataBytes = df.readBytes() ?? Uint8List(0);
      final expectedSha = manifest['dataSha256'] as String?;
      if (expectedSha == null || sha256.convert(dataBytes).toString() != expectedSha) {
        return BackupInspection(
          path: path,
          formatVersion: fv,
          createdAt: createdAt,
          fatal: t('ข้อมูลในไฟล์สำรองเสียหาย (checksum ไม่ตรงกัน)', 'Backup data is corrupted (checksum mismatch)'),
        );
      }

      final warnings = <String>[];
      final Map<String, dynamic> raw;
      try {
        raw = jsonDecode(utf8.decode(dataBytes)) as Map<String, dynamic>;
      } catch (_) {
        return BackupInspection(path: path, fatal: t('รูปแบบข้อมูลไม่ถูกต้อง', 'Invalid data format'));
      }

      final data = validateData(raw, warnings);

      // Attachments: listed in manifest and present in the archive.
      final attHashes = <String, String>{};
      final listed = manifest['attachments'];
      if (listed is Map) {
        var missing = 0;
        var bad = 0;
        for (final e in listed.entries) {
          final name = e.key.toString();
          final hash = e.value.toString();
          if (!AttachmentService.isSafeName(name)) {
            bad++;
            continue;
          }
          final af = archive.findFile('${BackupFormat.attachmentsPrefix}$name');
          if (af == null) {
            missing++;
            continue;
          }
          if (verifyAttachments) {
            final bytes = af.readBytes();
            if (bytes == null || sha256.convert(bytes).toString() != hash) {
              bad++;
              continue;
            }
          }
          attHashes[name] = hash;
        }
        if (missing > 0) warnings.add(t('ไฟล์แนบหายไป $missing ไฟล์', '$missing attachment(s) missing'));
        if (bad > 0) warnings.add(t('ไฟล์แนบเสียหาย $bad ไฟล์', '$bad attachment(s) damaged'));
      }

      return BackupInspection(
        path: path,
        formatVersion: fv,
        createdAt: createdAt,
        appVersion: manifest['appVersion'] as String?,
        counts: counts,
        includesAttachments: includesAtt,
        warnings: warnings,
        data: ValidatedData(
          documents: data.documents,
          tasks: data.tasks,
          events: data.events,
          categories: data.categories,
          settings: data.settings,
          attachments: attHashes,
        ),
      );
    } catch (e) {
      if (kDebugMode) debugPrint('inspect failed: $e');
      return BackupInspection(path: path, fatal: t('อ่านไฟล์สำรองข้อมูลไม่ได้', 'Could not read the backup file'));
    } finally {
      try {
        input?.closeSync();
      } catch (_) {}
    }
  }

  /// Normalises raw JSON into models. Invalid records are skipped and
  /// reported in [warnings]; valid ones are kept.
  static ValidatedData validateData(Map<String, dynamic> raw, List<String> warnings) {
    var invalid = 0;
    List<Map<String, Object?>> rows(String key) {
      final v = raw[key];
      if (v == null) return const [];
      if (v is! List) {
        invalid++;
        return const [];
      }
      final out = <Map<String, Object?>>[];
      for (final r in v) {
        if (r is Map) {
          out.add(r.map((k, v) => MapEntry(k.toString(), v as Object?)));
        } else {
          invalid++;
        }
      }
      return out;
    }

    bool validId(Object? id) => id is String && id.isNotEmpty && id.length <= 64;
    bool validTitle(Object? s) => s is String && s.trim().isNotEmpty;
    bool isNum(Object? v) => v is num;
    bool numOrNull(Object? v) => v == null || v is num;

    T? safe<T>(T Function() f) {
      try {
        return f();
      } catch (_) {
        return null;
      }
    }

    final categories = <ItemCategory>[];
    for (final r in rows('categories')) {
      final c = validId(r['id']) && validTitle(r['name']) ? safe(() => ItemCategory.fromMap(r)) : null;
      c == null ? invalid++ : categories.add(c);
    }
    final documents = <DocumentItem>[];
    for (final r in rows('documents')) {
      final ok = validId(r['id']) && validTitle(r['title']) && isNum(r['created_at']) && numOrNull(r['expiry_date']);
      final d = ok ? safe(() => DocumentItem.fromMap(r)) : null;
      d == null ? invalid++ : documents.add(d);
    }
    final tasks = <TaskItem>[];
    for (final r in rows('tasks')) {
      final ok = validId(r['id']) && validTitle(r['title']) && isNum(r['created_at']) && numOrNull(r['due_at']);
      final t = ok ? safe(() => TaskItem.fromMap(r)) : null;
      t == null ? invalid++ : tasks.add(t);
    }
    final events = <EventItem>[];
    for (final r in rows('events')) {
      final ok = validId(r['id']) && validTitle(r['title']) && isNum(r['start_at']) && numOrNull(r['end_at']);
      final e = ok ? safe(() => EventItem.fromMap(r)) : null;
      e == null ? invalid++ : events.add(e);
    }
    final settings = <String, String>{};
    final s = raw['settings'];
    if (s is Map) {
      for (final e in s.entries) {
        if (SettingKeys.backupable.contains(e.key) && e.value is String) settings[e.key as String] = e.value as String;
      }
    }
    if (invalid > 0) {
      warnings.add(Lang.t('ข้อมูลไม่สมบูรณ์ $invalid รายการ จะถูกข้าม', '$invalid invalid record(s) will be skipped'));
    }
    return ValidatedData(
      documents: documents,
      tasks: tasks,
      events: events,
      categories: categories,
      settings: settings,
      attachments: const {},
    );
  }

  // --------------------------------------------------------------- restore

  /// Restores a previously [inspect]ed backup.
  ///
  /// Safety: a backup of the current data is made first; attachments are
  /// extracted to a staging folder; all database writes happen in ONE
  /// transaction, so any failure rolls back and the current data stays
  /// untouched. Old files are only removed after the commit succeeded.
  Future<RestoreResult> restore(BackupInspection inspection, RestoreMode mode) async {
    final data = inspection.data;
    if (!inspection.canRestore || data == null) {
      throw AppException(
        ErrorKind.restore,
        inspection.fatal ?? Lang.t('ไฟล์สำรองข้อมูลไม่ถูกต้อง', 'Invalid backup file'),
      );
    }

    // 1. Safety backup of current data (so the user can always go back).
    String? safetyPath;
    try {
      final hasData =
          ((await db.rawQuery(
                    'SELECT (SELECT COUNT(*) FROM documents)+(SELECT COUNT(*) FROM tasks)+(SELECT COUNT(*) FROM events) AS n',
                  )).first['n']
                  as num)
              .toInt() >
          0;
      if (hasData) {
        safetyPath = (await createBackup(includeAttachments: true, prefix: safetyPrefix)).path;
      }
    } catch (e) {
      throw AppException(
        ErrorKind.restore,
        Lang.t(
          'ไม่สามารถสำรองข้อมูลปัจจุบันก่อนกู้คืนได้ จึงยกเลิกการกู้คืนเพื่อความปลอดภัย',
          'Could not back up current data before restoring, so the restore was cancelled for safety.',
        ),
        e,
      );
    }

    // 2. Stage attachments.
    final staging = Directory(p.join(attachments.root.path, 'restore_staging'));
    final staged = <String>{};
    InputFileStream? input;
    try {
      if (await staging.exists()) await staging.delete(recursive: true);
      await staging.create(recursive: true);
      if (data.attachments.isNotEmpty) {
        input = InputFileStream(inspection.path);
        final archive = ZipDecoder().decodeStream(input);
        for (final e in data.attachments.entries) {
          final af = archive.findFile('${BackupFormat.attachmentsPrefix}${e.key}');
          if (af == null) continue;
          final outPath = p.join(staging.path, e.key);
          final out = OutputFileStream(outPath);
          af.writeContent(out);
          await out.close();
          final h = (await sha256.bind(File(outPath).openRead()).first).toString();
          if (h == e.value) {
            staged.add(e.key);
          } else {
            await File(outPath).delete();
          }
        }
      }
    } catch (e) {
      await _safeDelete(staging);
      throw AppException(
        ErrorKind.restore,
        Lang.t(
          'แตกไฟล์แนบไม่สำเร็จ ข้อมูลเดิมไม่ถูกเปลี่ยนแปลง',
          'Could not extract attachments. Your data was not changed.',
        ),
        e,
      );
    } finally {
      try {
        input?.closeSync();
      } catch (_) {}
    }

    // 3. Single transaction for all database changes.
    var skipped = 0;
    try {
      await db.transaction((txn) async {
        final conflict = mode == RestoreMode.replace ? ConflictAlgorithm.replace : ConflictAlgorithm.ignore;
        if (mode == RestoreMode.replace) {
          for (final tbl in AppDatabase.contentTables) {
            await txn.delete(tbl);
          }
        }
        Future<void> put(String table, Map<String, Object?> row) async {
          if (mode == RestoreMode.merge) {
            final exists = await txn.query(table, columns: ['id'], where: 'id = ?', whereArgs: [row['id']], limit: 1);
            if (exists.isNotEmpty) {
              skipped++; // duplicate: keep the existing record
              return;
            }
          }
          await txn.insert(table, row, conflictAlgorithm: conflict);
        }

        for (final c in data.categories) {
          await put('categories', c.toMap());
        }
        // Guarantee the fallback category exists.
        final other = await txn.query('categories', where: 'id = ?', whereArgs: [ItemCategory.otherId]);
        if (other.isEmpty) {
          await txn.insert(
            'categories',
            DefaultCategories.build(Lang.isEn).firstWhere((c) => c.id == ItemCategory.otherId).toMap(),
          );
        }
        final catIds = (await txn.query('categories', columns: ['id'])).map((r) => r['id'] as String).toSet();

        for (final d in data.documents) {
          final fixed = catIds.contains(d.categoryId) ? d : d.copyWith(categoryId: ItemCategory.otherId);
          await put('documents', fixed.toMap());
        }
        for (final t in data.tasks) {
          final fixed = t.categoryId == null || catIds.contains(t.categoryId) ? t : t.copyWith(clearCategory: true);
          await put('tasks', fixed.toMap());
        }
        for (final e in data.events) {
          await put('events', e.toMap());
        }
        if (mode == RestoreMode.replace) {
          for (final s in data.settings.entries) {
            await txn.insert('settings', {
              'key': s.key,
              'value': s.value,
            }, conflictAlgorithm: ConflictAlgorithm.replace);
          }
        }
        // Verify inside the transaction before committing.
        final check = await txn.rawQuery('PRAGMA integrity_check');
        final status = check.isEmpty ? 'ok' : check.first.values.first.toString();
        if (status != 'ok') throw StateError('integrity_check: $status');
      });
    } catch (e) {
      await _safeDelete(staging);
      throw AppException(
        ErrorKind.restore,
        Lang.t(
          'กู้คืนข้อมูลไม่สำเร็จ ระบบได้ยกเลิกการเปลี่ยนแปลงทั้งหมดแล้ว ข้อมูลเดิมยังอยู่ครบ',
          'Restore failed. All changes were rolled back and your existing data is intact.',
        ),
        e,
      );
    }

    // 4. Commit succeeded: move staged files into place.
    try {
      await attachments.ensureDir();
      for (final name in staged) {
        final dest = File(p.join(attachments.dir.path, name));
        if (await dest.exists()) continue; // same random name = same file
        await File(p.join(staging.path, name)).rename(dest.path);
      }
      if (mode == RestoreMode.replace) {
        final referenced = <String>{};
        for (final r in await db.query('documents', columns: ['attachments'])) {
          referenced.addAll(DocumentItem.fromMap({'id': '', ...r}).attachmentPaths);
        }
        await attachments.cleanupOrphans(referenced);
      }
    } catch (e) {
      if (kDebugMode) debugPrint('post-restore file step failed: $e');
    } finally {
      await _safeDelete(staging);
    }

    return RestoreResult(
      counts: BackupCounts(
        documents: data.documents.length,
        tasks: data.tasks.length,
        events: data.events.length,
        categories: data.categories.length,
        attachments: staged.length,
      ),
      skippedDuplicates: skipped,
      safetyBackupPath: safetyPath,
    );
  }

  static Future<void> _safeDelete(Directory d) async {
    try {
      if (await d.exists()) await d.delete(recursive: true);
    } catch (_) {}
  }
}

/// Automatic local backups (daily / weekly), checked whenever the app
/// starts or returns to the foreground.
class AutoBackupService {
  AutoBackupService({required this.backup, required this.settings});

  final BackupService backup;
  final SettingsRepository settings;

  static Duration intervalFor(String? frequency) =>
      frequency == 'weekly' ? const Duration(days: 7) : const Duration(days: 1);

  static bool isDue({required DateTime? last, required String? frequency, required DateTime now}) {
    if (last == null) return true;
    return now.difference(last) >= intervalFor(frequency) - const Duration(minutes: 30);
  }

  /// Returns null when nothing was due, true on success, false on failure.
  Future<bool?> runIfDue({DateTime? now, bool force = false}) async {
    final n = now ?? DateTime.now();
    final s = await settings.all();
    if (!force && s[SettingKeys.autoBackupEnabled] != 'true') return null;
    final last = int.tryParse(s[SettingKeys.lastAutoBackupAt] ?? '');
    final lastAt = last == null ? null : DateTime.fromMillisecondsSinceEpoch(last);
    if (!force && !isDue(last: lastAt, frequency: s[SettingKeys.autoBackupFrequency], now: n)) return null;

    try {
      await backup.createBackup(
        includeAttachments: s[SettingKeys.autoBackupIncludeAttachments] != 'false',
        prefix: BackupService.autoPrefix,
        now: n,
      );
      // Only after the new backup exists and is verified, trim old ones.
      final keep = int.tryParse(s[SettingKeys.autoBackupKeep] ?? '')?.clamp(1, 30) ?? 5;
      final maxMb = int.tryParse(s[SettingKeys.autoBackupMaxMb] ?? '')?.clamp(10, 10000) ?? 500;
      await backup.pruneAutoBackups(keep: keep, maxBytes: maxMb * 1024 * 1024);
      await settings.set(SettingKeys.lastAutoBackupAt, '${n.millisecondsSinceEpoch}');
      await settings.set(SettingKeys.lastAutoBackupError, null);
      return true;
    } catch (e) {
      await settings.set(SettingKeys.lastAutoBackupError, '${n.millisecondsSinceEpoch}|${friendlyError(e)}');
      return false;
    }
  }
}
