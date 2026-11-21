import 'package:flutter/services.dart';

/// Opens Android system settings pages relevant to reminder delivery.
class SystemSettings {
  SystemSettings._();
  static const _ch = MethodChannel('lifekeep/system');

  static Future<bool> _call(String m) async {
    try {
      return await _ch.invokeMethod<bool>(m) ?? false;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> openNotificationSettings() => _call('openNotificationSettings');
  static Future<bool> openExactAlarmSettings() => _call('openExactAlarmSettings');
  static Future<bool> openBatterySettings() => _call('openBatterySettings');
  static Future<bool> openAppDetails() => _call('openAppDetails');

  /// null when unknown (e.g. tests / non-Android).
  static Future<bool?> isIgnoringBatteryOptimizations() async {
    try {
      return await _ch.invokeMethod<bool>('isIgnoringBatteryOptimizations');
    } catch (_) {
      return null;
    }
  }

  static Future<String> manufacturer() async {
    try {
      return (await _ch.invokeMethod<String>('manufacturer') ?? '').toLowerCase();
    } catch (_) {
      return '';
    }
  }

  /// Brands known to add their own background restrictions (autostart etc.).
  static bool isAggressiveOem(String m) => const [
    'xiaomi',
    'redmi',
    'poco',
    'oppo',
    'realme',
    'vivo',
    'huawei',
    'honor',
    'oneplus',
    'samsung',
    'infinix',
    'tecno',
  ].any(m.contains);
}
