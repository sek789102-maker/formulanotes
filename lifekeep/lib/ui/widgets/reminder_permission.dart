import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../data/repositories/settings_repository.dart';
import '../../services/system_settings.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';

/// Called before saving an item that has reminders. Makes sure Android will
/// actually deliver them:
///  1. notification permission (system prompt, or settings if blocked);
///  2. "Alarms & reminders" (exact alarms) - explained once, then opens the
///     system page. Without it Android may deliver reminders very late.
Future<void> ensureReminderReady(BuildContext context) async {
  final c = context.read<AppController>();
  try {
    var st = await c.s.gateway.permissionState();
    if (st.notificationsAllowed == false) {
      await c.s.gateway.requestNotificationPermission();
      st = await c.s.gateway.permissionState();
      if (st.notificationsAllowed == false && context.mounted) {
        final open = await confirmDialog(
          context,
          icon: Icons.notifications_off_outlined,
          title: context.tr('การแจ้งเตือนถูกปิดอยู่', 'Notifications are off'),
          message: context.tr(
            'LIFEKEEP จะไม่สามารถแจ้งเตือนได้จนกว่าจะเปิดการแจ้งเตือนของแอปในการตั้งค่าของเครื่อง',
            'LIFEKEEP cannot remind you until notifications are enabled in system settings.',
          ),
          confirmLabel: context.tr('เปิดการตั้งค่า', 'Open settings'),
        );
        if (open) await SystemSettings.openNotificationSettings();
        return;
      }
    }
    if (st.exactAlarmsAllowed == false && c.settings[SettingKeys.exactAlarmPrompted] != 'true' && context.mounted) {
      await c.setSetting(SettingKeys.exactAlarmPrompted, 'true');
      if (!context.mounted) return;
      final open = await confirmDialog(
        context,
        icon: Icons.alarm_on_rounded,
        title: context.tr('ให้แจ้งเตือนตรงเวลา', 'Get reminders on time'),
        message: context.tr(
          'Android ปิดสิทธิ์ “การปลุกและการช่วยเตือน” ไว้เป็นค่าเริ่มต้น ทำให้การแจ้งเตือนอาจมาช้ามาก\n\nแตะ “อนุญาต” แล้วเปิดสวิตช์สำหรับ LIFEKEEP',
          'Android turns off "Alarms & reminders" by default, so reminders may arrive very late.\n\nTap "Allow" and switch it on for LIFEKEEP.',
        ),
        confirmLabel: context.tr('อนุญาต', 'Allow'),
      );
      if (open) {
        final ok = await c.s.gateway.requestExactAlarmPermission();
        if (!ok) await SystemSettings.openExactAlarmSettings();
      }
    }
  } catch (_) {
    // Never block saving because of a permission check.
  }
}
