import 'package:flutter/services.dart';

/// Toggles Android FLAG_SECURE so app content is hidden in Recent Apps
/// (and screenshots are blocked) while App Lock is enabled.
class SecureWindow {
  SecureWindow._();

  static const _channel = MethodChannel('lifekeep/secure_window');

  static Future<void> set(bool enabled) async {
    try {
      await _channel.invokeMethod('setSecure', {'enabled': enabled});
    } catch (_) {
      // Not available on this platform; nothing else to do.
    }
  }
}
