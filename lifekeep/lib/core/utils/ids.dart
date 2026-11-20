import 'dart:math';

final Random _rng = Random.secure();

/// Random 128-bit identifier (hex). Stable across backup/restore so that
/// merging backups can detect duplicates.
String newId() {
  final b = List<int>.generate(16, (_) => _rng.nextInt(256));
  return b.map((e) => e.toRadixString(16).padLeft(2, '0')).join();
}
