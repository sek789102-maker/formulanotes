import 'dart:io';

import 'package:lifekeep/data/database/app_database.dart';
import 'package:lifekeep/services/attachment_service.dart';
import 'package:lifekeep/services/notification_gateway.dart';
import 'package:lifekeep/services/security_service.dart';
import 'package:lifekeep/state/app_controller.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

/// Records notification calls instead of talking to Android.
class FakeGateway implements NotificationGateway {
  final Map<int, ({String title, String body, DateTime at})> scheduled = {};
  final List<int> cancelled = [];
  final List<String> shown = [];
  bool failSchedule = false;
  bool? notificationsAllowed = true;
  bool? exactAlarmsAllowed = true;

  @override
  Future<void> init() async {}

  @override
  Future<void> schedule({
    required int id,
    required String title,
    required String body,
    required DateTime at,
    String? payload,
  }) async {
    if (failSchedule) throw Exception('boom');
    if (scheduled.containsKey(id)) throw StateError('duplicate id $id');
    scheduled[id] = (title: title, body: body, at: at);
  }

  @override
  Future<void> showNow({required int id, required String title, required String body}) async => shown.add(title);

  @override
  Future<void> cancel(int id) async {
    cancelled.add(id);
    scheduled.remove(id);
  }

  @override
  Future<List<int>> pendingIds() async => scheduled.keys.toList();

  @override
  Future<NotificationPermissionState> permissionState() async =>
      NotificationPermissionState(notificationsAllowed: notificationsAllowed, exactAlarmsAllowed: exactAlarmsAllowed);

  @override
  Future<bool> requestExactAlarmPermission() async => true;

  @override
  Future<bool> requestNotificationPermission() async => true;

  @override
  String get timezoneName => 'Asia/Bangkok';
}

bool _ffiReady = false;

void initFfi() {
  if (_ffiReady) return;
  sqfliteFfiInit();
  _ffiReady = true;
}

Future<Database> openTestDb([String? path]) async {
  initFfi();
  return AppDatabase.open(factory: databaseFactoryFfiNoIsolate, path: path ?? inMemoryDatabasePath);
}

class TestEnv {
  TestEnv._(this.dir, this.db, this.gateway, this.services, this.controller);

  final Directory dir;
  final Database db;
  final FakeGateway gateway;
  final AppServices services;
  final AppController controller;

  static Future<TestEnv> create({String? dbPath}) async {
    final dir = await Directory.systemTemp.createTemp('lifekeep_test_');
    // Each environment gets its own file: sqflite shares one instance per path,
    // so two ':memory:' opens would silently be the same database.
    final db = await openTestDb(dbPath ?? '${dir.path}/lifekeep_test.db');
    final gw = FakeGateway();
    final s = AppServices(
      db: db,
      gateway: gw,
      security: SecurityService(MemorySecureKV(), iterations: 1000),
      attachments: AttachmentService(dir),
      backupDir: Directory('${dir.path}/backups'),
      appVersion: '1.0.0-test',
    );
    final c = AppController(s);
    await c.load();
    return TestEnv._(dir, db, gw, s, c);
  }

  Future<void> dispose() async {
    await db.close();
    if (await dir.exists()) await dir.delete(recursive: true);
  }
}
