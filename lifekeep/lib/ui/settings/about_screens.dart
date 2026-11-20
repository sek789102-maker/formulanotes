import 'package:flutter/material.dart';

import '../../core/app_info.dart';
import '../../core/l10n/lang.dart';
import '../widgets/common.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('เกี่ยวกับ LIFEKEEP', 'About LIFEKEEP'))),
      body: ListView(padding: const EdgeInsets.fromLTRB(20, 8, 20, 32), children: [
        Center(
          child: Container(
            width: 84,
            height: 84,
            decoration: BoxDecoration(color: scheme.primary, borderRadius: BorderRadius.circular(26)),
            child: Icon(Icons.task_rounded, color: scheme.onPrimary, size: 42),
          ),
        ),
        const SizedBox(height: 16),
        Center(child: Text(AppInfo.name, style: text.headlineSmall?.copyWith(letterSpacing: 2))),
        Center(
          child: Text('${context.tr('เวอร์ชัน', 'Version')} ${AppInfo.version} (${AppInfo.build})',
              style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant)),
        ),
        const SizedBox(height: 24),
        AppCard(
          child: Text(
            context.tr(
              'LIFEKEEP คือแอปจัดการชีวิตประจำวันแบบออฟไลน์ ช่วยเก็บเอกสารสำคัญ จัดการงาน กำหนดการ และการแจ้งเตือน '
                  'ทุกอย่างทำงานในเครื่องของคุณ ไม่ต้องสมัครบัญชี ไม่ต้องใช้อินเทอร์เน็ต ไม่มีโฆษณา และไม่มีค่าบริการรายเดือน',
              'LIFEKEEP is an offline life organizer for important documents, tasks, schedules and reminders. '
                  'Everything runs on your device — no account, no internet, no ads and no subscription.',
            ),
            style: text.bodyMedium,
          ),
        ),
        const SizedBox(height: 12),
        for (final (icon, th, en) in const [
          (Icons.wifi_off_rounded, 'ทำงานแบบออฟไลน์ 100%', 'Works 100% offline'),
          (Icons.lock_outline_rounded, 'ข้อมูลอยู่ในเครื่องของคุณเท่านั้น', 'Your data stays on your device'),
          (Icons.download_rounded, 'ส่งออกข้อมูลได้ทุกเมื่อ (JSON ใน ZIP)', 'Export anytime (JSON in ZIP)'),
          (Icons.update_disabled_rounded, 'ใช้งานได้ระยะยาวแม้ไม่อัปเดตแอป', 'Keeps working long-term without updates'),
        ])
          ListTile(leading: Icon(icon, color: scheme.primary), title: Text(context.tr(th, en))),
        const SizedBox(height: 12),
        OutlinedButton(
          onPressed: () => showLicensePage(context: context, applicationName: AppInfo.name, applicationVersion: AppInfo.version),
          child: Text(context.tr('สัญญาอนุญาตโอเพนซอร์ส', 'Open-source licences')),
        ),
      ]),
    );
  }
}

class PrivacyScreen extends StatelessWidget {
  const PrivacyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final items = [
      (Icons.cloud_off_rounded, context.tr('ไม่มีเซิร์ฟเวอร์และไม่มี API', 'No server, no API'),
          context.tr('แอปไม่มีสิทธิ์เข้าถึงอินเทอร์เน็ต (ไม่มี INTERNET permission) ข้อมูลจึงไม่ถูกส่งออกนอกเครื่อง',
              'The app has no internet permission, so data never leaves the device.')),
      (Icons.person_off_outlined, context.tr('ไม่ต้องสมัครบัญชี', 'No account'),
          context.tr('ไม่มีการเก็บข้อมูลส่วนตัว ไม่มีการติดตาม ไม่มีโฆษณา', 'No personal data collection, tracking or ads.')),
      (Icons.storage_rounded, context.tr('เก็บข้อมูลในเครื่อง', 'Stored locally'),
          context.tr('ข้อมูลอยู่ในฐานข้อมูล SQLite และโฟลเดอร์ส่วนตัวของแอป ปิดการสำรองข้อมูลอัตโนมัติไปยังคลาวด์ของ Android',
              'Data lives in a SQLite database and the app\'s private folder. Android cloud auto-backup is disabled.')),
      (Icons.key_rounded, context.tr('PIN ปลอดภัย', 'Secure PIN'),
          context.tr('PIN ถูกเก็บเป็นค่าแฮชพร้อม salt ใน Secure Storage ไม่เก็บเป็นข้อความ',
              'The PIN is stored as a salted hash in secure storage, never as text.')),
      (Icons.visibility_off_outlined, context.tr('ซ่อนจากหน้าแอปล่าสุด', 'Hidden in Recent Apps'),
          context.tr('เมื่อเปิดล็อกแอป เนื้อหาจะถูกซ่อนในหน้าแอปล่าสุดและป้องกันการจับภาพหน้าจอ',
              'With App Lock on, content is hidden in Recent Apps and screenshots are blocked.')),
      (Icons.backup_outlined, context.tr('ไฟล์สำรองเป็นของคุณ', 'Backups are yours'),
          context.tr('ไฟล์สำรองไม่ได้เข้ารหัส กรุณาเก็บไว้ในที่ปลอดภัย', 'Backup files are not encrypted — keep them somewhere safe.')),
    ];
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ความเป็นส่วนตัว', 'Privacy'))),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), children: [
        for (final (icon, title, body) in items)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: AppCard(
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                IconBadge(icon: icon, color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(title, style: text.titleSmall),
                    const SizedBox(height: 4),
                    Text(body, style: text.bodySmall),
                  ]),
                ),
              ]),
            ),
          ),
      ]),
    );
  }
}
