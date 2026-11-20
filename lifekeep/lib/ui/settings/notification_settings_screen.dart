import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../data/repositories/settings_repository.dart';
import '../../services/notification_gateway.dart';
import '../../state/app_controller.dart';
import '../widgets/common.dart';
import '../widgets/form_widgets.dart';

class NotificationSettingsScreen extends StatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  State<NotificationSettingsScreen> createState() => _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState extends State<NotificationSettingsScreen> {
  NotificationPermissionState? _perm;
  int _pending = 0;

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    final c = context.read<AppController>();
    try {
      final p = await c.s.gateway.permissionState();
      final n = await c.s.scheduler.pendingCount();
      if (mounted) {
        setState(() {
          _perm = p;
          _pending = n;
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
    final status = context.status;
    Widget permRow(String label, bool? ok, VoidCallback onFix) => ListTile(
          leading: Icon(
            ok == true ? Icons.check_circle_rounded : (ok == false ? Icons.error_outline_rounded : Icons.help_outline_rounded),
            color: ok == true ? status.success : (ok == false ? status.warning : scheme.onSurfaceVariant),
          ),
          title: Text(label),
          subtitle: Text(ok == true
              ? context.tr('อนุญาตแล้ว', 'Allowed')
              : (ok == false ? context.tr('ยังไม่อนุญาต', 'Not allowed') : context.tr('ไม่ทราบสถานะ', 'Unknown'))),
          trailing: ok == true ? null : TextButton(onPressed: onFix, child: Text(context.tr('อนุญาต', 'Allow'))),
        );

    return Scaffold(
      appBar: AppBar(title: Text(context.tr('การแจ้งเตือน', 'Notifications'))),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), children: [
        Card(
          child: Column(children: [
            SwitchListTile(
              title: Text(context.tr('เปิดการแจ้งเตือน', 'Enable reminders')),
              subtitle: Text(context.tr('ปิดเพื่อหยุดการแจ้งเตือนทั้งหมด', 'Turn off to pause all reminders')),
              value: p.enabled,
              onChanged: (v) => _set(SettingKeys.notificationsEnabled, v),
            ),
            const Divider(indent: 16),
            SwitchListTile(
              title: Text(context.tr('งาน', 'Tasks')),
              value: p.tasks,
              onChanged: p.enabled ? (v) => _set(SettingKeys.notifyTasks, v) : null,
            ),
            SwitchListTile(
              title: Text(context.tr('กิจกรรม', 'Events')),
              value: p.events,
              onChanged: p.enabled ? (v) => _set(SettingKeys.notifyEvents, v) : null,
            ),
            SwitchListTile(
              title: Text(context.tr('เอกสารใกล้หมดอายุ', 'Expiring documents')),
              value: p.documents,
              onChanged: p.enabled ? (v) => _set(SettingKeys.notifyDocuments, v) : null,
            ),
            const Divider(indent: 16),
            ListTile(
              leading: const Icon(Icons.schedule_rounded),
              title: Text(context.tr('เวลาแจ้งเตือนเริ่มต้น', 'Default reminder time')),
              subtitle: Text(context.tr('ใช้กับเอกสารและงาน/กิจกรรมที่ไม่ระบุเวลา', 'For documents and items without a time')),
              trailing: Text('${p.hour.toString().padLeft(2, '0')}:${p.minute.toString().padLeft(2, '0')}',
                  style: Theme.of(context).textTheme.titleMedium),
              onTap: () async {
                final t = await pickTime(context, initial: TimeOfDay(hour: p.hour, minute: p.minute));
                if (t == null || !context.mounted) return;
                await runAction(context, () async {
                  await c.setSetting(SettingKeys.reminderHour, '${t.hour}');
                  await c.setSetting(SettingKeys.reminderMinute, '${t.minute}', reschedule: true);
                });
              },
            ),
          ]),
        ),
        const SectionHeader(title: ''),
        Text(context.tr('สิทธิ์ของระบบ Android', 'Android permissions'), style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        Card(
          child: Column(children: [
            permRow(context.tr('แสดงการแจ้งเตือน', 'Show notifications'), _perm?.notificationsAllowed, () async {
              await c.s.gateway.requestNotificationPermission();
              await _refresh();
            }),
            const Divider(indent: 56),
            permRow(context.tr('ตั้งเวลาแบบแม่นยำ (Alarms & reminders)', 'Exact timing (Alarms & reminders)'), _perm?.exactAlarmsAllowed,
                () async {
              await c.s.gateway.requestExactAlarmPermission();
              await _refresh();
              if (context.mounted) await c.setSetting(SettingKeys.notificationsEnabled, p.enabled ? 'true' : 'false', reschedule: true);
            }),
          ]),
        ),
        const SizedBox(height: 12),
        AppCard(
          color: status.warningContainer,
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(Icons.info_outline_rounded, color: status.warning),
            const SizedBox(width: 12),
            Expanded(
              child: Text(context.tr(
                'การแจ้งเตือนทำงานภายในเครื่องทั้งหมด แต่ Android อาจเลื่อนเวลาแจ้งเตือนได้ เมื่อเปิดโหมดประหยัดแบตเตอรี่, '
                    'ไม่ได้อนุญาต “ตั้งเวลาแบบแม่นยำ” หรือผู้ผลิตบางรายจำกัดการทำงานเบื้องหลัง '
                    'หากต้องการความแม่นยำมากขึ้น ให้อนุญาตสิทธิ์ด้านบน และตั้ง LIFEKEEP เป็น “ไม่จำกัด” ในการตั้งค่าแบตเตอรี่ของเครื่อง',
                'Reminders run entirely on this device, but Android may delay them when battery saver is on, '
                    '"exact timing" is not allowed, or the manufacturer restricts background work. '
                    'For better accuracy, allow the permissions above and set LIFEKEEP to "Unrestricted" in battery settings.',
              )),
            ),
          ]),
        ),
        const SizedBox(height: 12),
        Text(
          context.tr('การแจ้งเตือนที่ตั้งไว้: $_pending รายการ', 'Scheduled reminders: $_pending'),
          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          icon: const Icon(Icons.notifications_active_outlined),
          label: Text(context.tr('ทดสอบการแจ้งเตือน', 'Send test notification')),
          onPressed: () => runAction(
            context,
            () async {
              await c.ensureNotificationPermission();
              await c.s.gateway.showNow(
                id: 2,
                title: 'LIFEKEEP',
                body: Lang.t('การแจ้งเตือนทำงานปกติ', 'Notifications are working'),
              );
            },
            success: context.tr('ส่งการแจ้งเตือนทดสอบแล้ว', 'Test notification sent'),
          ),
        ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          icon: const Icon(Icons.sync_rounded),
          label: Text(context.tr('ตั้งการแจ้งเตือนทั้งหมดใหม่', 'Re-schedule all reminders')),
          onPressed: () => runAction(
            context,
            () async {
              await c.s.scheduler.rescheduleAll();
              await _refresh();
            },
            success: context.tr('ตั้งการแจ้งเตือนใหม่แล้ว', 'Reminders re-scheduled'),
          ),
        ),
      ]),
    );
  }
}
