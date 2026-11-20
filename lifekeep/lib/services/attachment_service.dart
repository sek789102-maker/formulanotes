import 'dart:io';

import 'package:path/path.dart' as p;

import '../core/l10n/lang.dart';
import '../core/utils/errors.dart';
import '../core/utils/ids.dart';

/// Stores attachment files inside the app's private storage.
///
/// Files are copied with streaming I/O (never fully loaded into RAM) and
/// documents only keep the file *name*, so the data folder can move
/// (e.g. after restore) without breaking references.
class AttachmentService {
  AttachmentService(this.root);

  /// App support directory; attachments live in `<root>/attachments`.
  final Directory root;

  static const maxFileBytes = 50 * 1024 * 1024; // 50 MB per image
  static final _safeName = RegExp(r'^[A-Za-z0-9._-]+$');
  static const allowedExtensions = {'.jpg', '.jpeg', '.png', '.webp', '.heic', '.heif', '.gif', '.bmp'};

  Directory get dir => Directory(p.join(root.path, 'attachments'));

  Future<void> ensureDir() async {
    if (!await dir.exists()) await dir.create(recursive: true);
  }

  static bool isSafeName(String name) => _safeName.hasMatch(name) && !name.startsWith('.');

  /// Resolves a stored name to a file, refusing anything that could escape
  /// the attachments folder.
  File fileFor(String name) {
    if (!isSafeName(name)) {
      throw AppException(ErrorKind.file, Lang.t('ชื่อไฟล์แนบไม่ถูกต้อง', 'Invalid attachment name'));
    }
    return File(p.join(dir.path, name));
  }

  Future<bool> exists(String name) async {
    if (!isSafeName(name)) return false;
    return File(p.join(dir.path, name)).exists();
  }

  static String _ext(String path) {
    final e = p.extension(path).toLowerCase();
    return allowedExtensions.contains(e) ? e : '.jpg';
  }

  /// Copies a picked file (by path) into storage and returns its new name.
  Future<String> importFile(String sourcePath) async {
    final src = File(sourcePath);
    try {
      if (!await src.exists()) {
        throw AppException(
          ErrorKind.file,
          Lang.t(
            'ไม่พบไฟล์ที่เลือก ไฟล์อาจถูกย้ายหรือลบ',
            'The selected file was not found. It may have been moved or deleted.',
          ),
        );
      }
      final size = await src.length();
      if (size > maxFileBytes) {
        throw AppException(ErrorKind.file, Lang.t('ไฟล์มีขนาดใหญ่เกิน 50 MB', 'The file is larger than 50 MB'));
      }
      await ensureDir();
      final name = '${newId()}${_ext(sourcePath)}';
      await src.copy(p.join(dir.path, name));
      return name;
    } on AppException {
      rethrow;
    } catch (e) {
      throw fileError(e);
    }
  }

  /// Copies a stream (e.g. from a content:// URI) into storage.
  Future<String> importStream(Stream<List<int>> data, String originalName) async {
    await ensureDir();
    final name = '${newId()}${_ext(originalName)}';
    final out = File(p.join(dir.path, name));
    var written = 0;
    final sink = out.openWrite();
    try {
      await for (final chunk in data) {
        written += chunk.length;
        if (written > maxFileBytes) {
          throw AppException(ErrorKind.file, Lang.t('ไฟล์มีขนาดใหญ่เกิน 50 MB', 'The file is larger than 50 MB'));
        }
        sink.add(chunk);
      }
      await sink.flush();
      await sink.close();
      return name;
    } catch (e) {
      try {
        await sink.close();
      } catch (_) {}
      if (await out.exists()) await out.delete();
      if (e is AppException) rethrow;
      throw fileError(e);
    }
  }

  Future<void> delete(String name) async {
    try {
      if (!isSafeName(name)) return;
      final f = File(p.join(dir.path, name));
      if (await f.exists()) await f.delete();
    } catch (_) {
      // A file that cannot be deleted must not block the user's action.
    }
  }

  Future<void> deleteMany(Iterable<String> names) async {
    for (final n in names) {
      await delete(n);
    }
  }

  /// Removes files that no document references any more.
  Future<int> cleanupOrphans(Set<String> referenced) async {
    if (!await dir.exists()) return 0;
    var removed = 0;
    await for (final e in dir.list()) {
      if (e is File && !referenced.contains(p.basename(e.path))) {
        try {
          await e.delete();
          removed++;
        } catch (_) {}
      }
    }
    return removed;
  }

  Future<({int count, int bytes})> usage() async {
    if (!await dir.exists()) return (count: 0, bytes: 0);
    var count = 0;
    var bytes = 0;
    await for (final e in dir.list()) {
      if (e is File) {
        count++;
        bytes += await e.length();
      }
    }
    return (count: count, bytes: bytes);
  }

  Future<void> deleteAll() async {
    if (await dir.exists()) await dir.delete(recursive: true);
  }
}
