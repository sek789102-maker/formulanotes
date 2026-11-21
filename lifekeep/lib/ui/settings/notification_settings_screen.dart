import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../data/repositories/settings_repository.dart';
import '../../services/notification_gateway.dart';
import '../../services/system_settings.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import '../widgets/form_widgets.dart';

class NotificationSettingsScreen extends StatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  State<NotificationSettingsScreen> createState() => _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState extends State<NotificationSettingsScreen> with WidgetsBindingObserver {
  NotificationPermissionState? _perm;
  int _pending = 0;
  bool? _batteryUnrestricted;
  String _oem = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  // Coming back from a system settings page: show the new state.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final c = context.read<AppController>();
    try {
      await c.refreshPermissions(); // re-schedules reminders if permissions changed
      final p = c.permissions ?? await c.s.gateway.permissionState();
      final n = await c.s.scheduler.pendingCount();
      final b = await SystemSettings.isIgnoringBatteryOptimizations();
      final m = await SystemSettings.manufacturer();
      if (mounted) {
        setState(() {
          _perm = p;
          _pending = n;
          _batteryUnrestricted = b;
          _oem = m;
        });
      }
    } catch (_) {}
  }

  Future<void> _set(String key, bool v) async {
    final c = context.read<AppController>();
    await runAction(context, () => c.setSetting(key, v ? 'true' : 'false', reschedule: true));
    await _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final p = c.reminderPrefs;
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final status = context.status;

    Widget permRow(String label, String hint, bool? ok, VoidCallback onFix) => LkRow(
      leading: Icon(
        ok == true
            ? Icons.check_circle_rounded
            : (ok == false ? Icons.error_outline_rounded : Icons.help_outline_rounded),
        color: ok == true ? status.success : (ok == false ? status.warning : scheme.onSurfaceVariant),
      ),
      title: label,
      subtitle: Text(ok == true ? context.tr('อนุญาตแล้ว', 'Allowed') : hint),
      trailing: ok == true
          ? null
          : FilledButton.tonal(
              style: FilledButton.styleFrom(
                minimumSize: const Size(0, 36),
                padding: const EdgeInsets.symmetric(horizontal: Space.md),
              ),
              onPressed: onFix,
              child: Text(context.tr('อนุญาต', 'Allow')),
            ),
    );

    return Scaffold(
      appBar: AppBar(title: Text(context.tr('การแจ้งเตือน', 'Notifications'))),
      body: ContentWidth(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
          children: [
            LkGroup(
              children: [
                LkSwitchRow(
                  leading: IconBadge(
                    icon: Icons.notifications_active_outlined,
                    color: const Color(0xFFC0742A),
                    size: 32,
                    filled: true,
                  ),
                  title: context.tr('เปิดการแจ้งเตือน', 'Enable reminders'),
                  subtitle: context.tr('ปิดเพื่อหยุดการแจ้งเตือนทั้งหมดชั่วคราว', 'Turn off to pause all reminders'),
                  value: p.enabled,
                  onChanged: (v) => _set(SettingKeys.notificationsEnabled, v),
                ),
              ],
            ),
            LkSectionHeader(title: context.tr('ประเภท', 'Types')),
            AnimatedOpacity(
              duration: Motion.fast,
              opacity: p.enabled ? 1 : 0.5,
              child: LkGroup(
                children: [
                  LkSwitchRow(
                    leading: Icon(Icons.check_circle_outline_rounded, color: scheme.onSurfaceVariant),
                    title: context.tr('งาน', 'Tasks'),
                    value: p.tasks,
                    onChanged: p.enabled ? (v) => _set(SettingKeys.notifyTasks, v) : null,
                  ),
                  LkSwitchRow(
                    leading: Icon(Icons.event_outlined, color: scheme.onSurfaceVariant),
                    title: context.tr('กิจกรรม', 'Events'),
                    value: p.events,
                    onChanged: p.enabled ? (v) => _set(SettingKeys.notifyEvents, v) : null,
                  ),
                  LkSwitchRow(
                    leading: Icon(Icons.hourglass_bottom_rounded, color: scheme.onSurfaceVariant),
                    title: context.tr('เอกสารใกล้หมดอายุ', 'Expiring documents'),
                    value: p.documents,
                    onChanged: p.enabled ? (v) => _set(SettingKeys.notifyDocuments, v) : null,
                  ),
                  LkRow(
                    leading: Icon(Icons.schedule_rounded, color: scheme.onSurfaceVariant),
                    title: context.tr('เวลาแจ้งเตือนเริ่มต้น', 'Default reminder time'),
                    subtitle: Text(
                      context.tr('สำหรับเอกสาร และงาน/กิจกรรมที่ไม่ระบุเวลา', 'For documents and items without a time'),
                    ),
                    showChevron: true,
                    trailing: Text(
                      '${p.hour.toString().padLeft(2, '0')}:${p.minute.toString().padLeft(2, '0')}',
                      style: t.titleSmall?.copyWith(color: scheme.primary),
                    ),
                    onTap: () async {
                      final tm = await pickTime(
                        context,
                        initial: TimeOfDay(hour: p.hour, minute: p.minute),
                      );
                      if (tm == null || !context.mounted) return;
                      await runAction(context, () async {
                        await c.setSetting(SettingKeys.reminderHour, '${tm.hour}');
                        await c.setSetting(SettingKeys.reminderMinute, '${tm.minute}', reschedule: true);
                      });
                    },
                  ),
                ],
              ),
            ),
            LkSectionHeader(title: context.tr('สิทธิ์ของ Android', 'Android permissions')),
            LkGroup(
              dividerIndent: 56,
              children: [
                permRow(
                  context.tr('แสดงการแจ้งเตือน', 'Show notifications'),
                  context.tr('จำเป็นเพื่อให้การแจ้งเตือนแสดง', 'Required to show reminders'),
                  _perm?.notificationsAllowed,
                  () async {
                    final ok = await c.s.gateway.requestNotificationPermission();
                    // Blocked permanently: the system dialog no longer appears.
                    if (!ok) await SystemSettings.openNotificationSettings();
                    await _refresh();
                  },
                ),
                permRow(
                  context.tr('ตั้งเวลาแบบแม่นยำ', 'Exact timing'),
                  context.tr('ช่วยให้แจ้งเตือนตรงเวลามากขึ้น', 'Helps reminders arrive on time'),
                  _perm?.exactAlarmsAllowed,
                  () async {
                    final ok = await c.s.gateway.requestExactAlarmPermission();
                    if (!ok) await SystemSettings.openExactAlarmSettings();
                    await _refresh();
                  },
                ),
                permRow(
                  context.tr('ไม่จำกัดการใช้แบตเตอรี่', 'Unrestricted battery'),
                  context.tr(
                    'ป้องกัน Android หน่วงการแจ้งเตือนเพื่อประหยัดแบต',
                    'Stops Android delaying reminders to save battery',
                  ),
                  _batteryUnrestricted,
                  () async {
                    await SystemSettings.openBatterySettings();
                  },
                ),
              ],
            ),
            const SizedBox(height: Space.md),
            LkBanner(
              icon: Icons.battery_alert_rounded,
              color: status.warning,
              background: status.warningContainer,
              title: context.tr('ความแม่นยำขึ้นกับระบบ Android', 'Timing depends on Android'),
              message: context.tr(
                'Android อาจเลื่อนการแจ้งเตือนเมื่อเปิดโหมดประหยัดแบตเตอรี่ หรือผู้ผลิตจำกัดการทำงานเบื้องหลัง แนะนำให้ตั้ง LIFEKEEP เป็น “ไม่จำกัด” ในการตั้งค่าแบตเตอรี่',
                'Android may delay reminders under battery saver or manufacturer restrictions. Set LIFEKEEP to "Unrestricted" in battery settings.',
              ),
            ),
            if (SystemSettings.isAggressiveOem(_oem)) ...[
              const SizedBox(height: Space.md),
              LkBanner(
                icon: Icons.phone_android_rounded,
                color: scheme.primary,
                background: scheme.primaryContainer.withValues(alpha: 0.6),
                title: context.tr('สำหรับมือถือยี่ห้อนี้', 'For this phone brand'),
                message: context.tr(
                  'บางยี่ห้อ (เช่น Xiaomi, Redmi, POCO, OPPO, vivo) ปิดแอปเบื้องหลังเอง ให้เปิด “เริ่มอัตโนมัติ (Autostart)” และตั้ง “ประหยัดแบตเตอรี่” ของ LIFEKEEP เป็น “ไม่มีข้อจำกัด” ในหน้าข้อมูลแอป',
                  'Some brands (e.g. Xiaomi, Redmi, POCO, OPPO, vivo) stop background apps. Enable "Autostart" and set LIFEKEEP\'s battery saver to "No restrictions" in the app info page.',
                ),
                action: TextButton(onPressed: SystemSettings.openAppDetails, child: Text(context.tr('เปิด', 'Open'))),
              ),
            ],
            LkSectionHeader(title: context.tr('เครื่องมือ', 'Tools')),
            LkGroup(
              dividerIndent: 56,
              children: [
                LkRow(
                  leading: Icon(Icons.timer_outlined, color: scheme.primary),
                  title: context.tr('ทดสอบแจ้งเตือนแบบตั้งเวลา (1 นาที)', 'Test scheduled reminder (1 min)'),
                  subtitle: Text(context.tr('ออกจากแอปแล้วรอประมาณ 1 นาที', 'Leave the app and wait about 1 minute')),
                  onTap: () => runAction(
                    context,
                    () async {
                      await c.ensureNotificationPermission();
                      await c.s.gateway.schedule(
                        id: 3,
                        title: 'LIFEKEEP',
                        body: Lang.t('การแจ้งเตือนแบบตั้งเวลาทำงานปกติ ✅', 'Scheduled reminders are working ✅'),
                        at: DateTime.now().add(const Duration(minutes: 1)),
                      );
                    },
                    success: context.tr(
                      'ตั้งแล้ว — ออกจากแอปแล้วรอ 1 นาที',
                      'Scheduled — leave the app and wait 1 minute',
                    ),
                  ),
                ),
                LkRow(
                  leading: Icon(Icons.notifications_active_outlined, color: scheme.primary),
                  title: context.tr('ทดสอบการแจ้งเตือนทันที', 'Send test notification now'),
                  onTap: () => runAction(context, () async {
                    await c.ensureNotificationPermission();
                    await c.s.gateway.showNow(
                      id: 2,
                      title: 'LIFEKEEP',
                      body: Lang.t('การแจ้งเตือนทำงานปกติ', 'Notifications are working'),
                    );
                  }, success: context.tr('ส่งการแจ้งเตือนทดสอบแล้ว', 'Test notification sent')),
                ),
                LkRow(
                  leading: Icon(Icons.sync_rounded, color: scheme.primary),
                  title: context.tr('ตั้งการแจ้งเตือนทั้งหมดใหม่', 'Re-schedule all reminders'),
                  subtitle: Text(context.tr('ตั้งไว้ $_pending รายการ', '$_pending scheduled')),
                  onTap: () => runAction(context, () async {
                    await c.s.scheduler.rescheduleAll();
                    await _refresh();
                  }, success: context.tr('ตั้งการแจ้งเตือนใหม่แล้ว', 'Reminders re-scheduled')),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
