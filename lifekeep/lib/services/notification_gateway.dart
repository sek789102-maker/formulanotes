import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart' show Color;
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../core/l10n/lang.dart';

/// Permission/capability snapshot used by the settings screen.
class NotificationPermissionState {
  const NotificationPermissionState({required this.notificationsAllowed, required this.exactAlarmsAllowed});
  final bool? notificationsAllowed;
  final bool? exactAlarmsAllowed;
}

/// Abstraction over the OS notification API so scheduling logic can be
/// unit-tested with a fake.
abstract class NotificationGateway {
  Future<void> init();
  Future<void> schedule({
    required int id,
    required String title,
    required String body,
    required DateTime at,
    String? payload,
  });
  Future<void> showNow({required int id, required String title, required String body});
  Future<void> cancel(int id);
  Future<List<int>> pendingIds();
  Future<NotificationPermissionState> permissionState();
  Future<bool> requestNotificationPermission();
  Future<bool> requestExactAlarmPermission();
  String get timezoneName;
}

class LocalNotificationGateway implements NotificationGateway {
  LocalNotificationGateway();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  bool _ready = false;
  String _tzName = 'UTC';

  static const reminderChannelId = 'lifekeep_reminders';
  static const systemChannelId = 'lifekeep_system';

  @override
  String get timezoneName => _tzName;

  AndroidFlutterLocalNotificationsPlugin? get _android =>
      _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();

  @override
  Future<void> init() async {
    if (_ready) return;
    tzdata.initializeTimeZones();
    try {
      final info = await FlutterTimezone.getLocalTimezone();
      _tzName = info.identifier;
      tz.setLocalLocation(tz.getLocation(_tzName));
    } catch (e) {
      // Unknown zone id: fall back to Bangkok if the device offset matches,
      // otherwise UTC. Reminders still fire at the right absolute instant
      // because we convert from the device's DateTime.
      final offset = DateTime.now().timeZoneOffset;
      _tzName = offset == const Duration(hours: 7) ? 'Asia/Bangkok' : 'UTC';
      tz.setLocalLocation(tz.getLocation(_tzName));
      if (kDebugMode) debugPrint('Timezone fallback: $e');
    }
    await _plugin.initialize(
      settings: const InitializationSettings(
        android: AndroidInitializationSettings('ic_stat_lifekeep'),
      ),
    );
    final android = _android;
    if (android != null) {
      await android.createNotificationChannel(AndroidNotificationChannel(
        reminderChannelId,
        Lang.t('การแจ้งเตือน', 'Reminders'),
        description: Lang.t('แจ้งเตือนงาน กิจกรรม และเอกสารใกล้หมดอายุ',
            'Task, event and document expiry reminders'),
        importance: Importance.high,
      ));
      await android.createNotificationChannel(AndroidNotificationChannel(
        systemChannelId,
        Lang.t('ระบบ', 'System'),
        description: Lang.t('สถานะการสำรองข้อมูล', 'Backup status'),
        importance: Importance.defaultImportance,
      ));
    }
    _ready = true;
  }

  NotificationDetails _details(String channel) => NotificationDetails(
        android: AndroidNotificationDetails(
          channel,
          channel == reminderChannelId ? Lang.t('การแจ้งเตือน', 'Reminders') : Lang.t('ระบบ', 'System'),
          importance: channel == reminderChannelId ? Importance.high : Importance.defaultImportance,
          priority: channel == reminderChannelId ? Priority.high : Priority.defaultPriority,
          icon: 'ic_stat_lifekeep',
          color: const Color(0xFF233B67),
          category: AndroidNotificationCategory.reminder,
          visibility: NotificationVisibility.private,
        ),
      );

  @override
  Future<void> schedule({
    required int id,
    required String title,
    required String body,
    required DateTime at,
    String? payload,
  }) async {
    await init();
    // Exact alarms need user consent on Android 12+/14+. Without it we fall
    // back to inexact alarms which Android may delay (Doze / battery saver).
    var mode = AndroidScheduleMode.inexactAllowWhileIdle;
    try {
      final exact = await _android?.canScheduleExactNotifications();
      if (exact == true) mode = AndroidScheduleMode.exactAllowWhileIdle;
    } catch (_) {}
    await _plugin.zonedSchedule(
      id: id,
      scheduledDate: tz.TZDateTime.from(at, tz.local),
      notificationDetails: _details(reminderChannelId),
      androidScheduleMode: mode,
      title: title,
      body: body,
      payload: payload,
    );
  }

  @override
  Future<void> showNow({required int id, required String title, required String body}) async {
    await init();
    await _plugin.show(id: id, title: title, body: body, notificationDetails: _details(systemChannelId));
  }

  @override
  Future<void> cancel(int id) async {
    await init();
    await _plugin.cancel(id: id);
  }

  @override
  Future<List<int>> pendingIds() async {
    await init();
    final list = await _plugin.pendingNotificationRequests();
    return list.map((e) => e.id).toList();
  }

  @override
  Future<NotificationPermissionState> permissionState() async {
    await init();
    final android = _android;
    if (android == null) {
      return const NotificationPermissionState(notificationsAllowed: null, exactAlarmsAllowed: null);
    }
    bool? n;
    bool? e;
    try {
      n = await android.areNotificationsEnabled();
    } catch (_) {}
    try {
      e = await android.canScheduleExactNotifications();
    } catch (_) {}
    return NotificationPermissionState(notificationsAllowed: n, exactAlarmsAllowed: e);
  }

  @override
  Future<bool> requestNotificationPermission() async {
    await init();
    final r = await _android?.requestNotificationsPermission();
    return r ?? false;
  }

  @override
  Future<bool> requestExactAlarmPermission() async {
    await init();
    final r = await _android?.requestExactAlarmsPermission();
    return r ?? false;
  }
}
