import 'package:flutter/material.dart';

import '../../core/app_info.dart';
import '../../core/l10n/lang.dart';
import '../design/design.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final features = [
      (Icons.wifi_off_rounded, context.tr('ทำงานออฟไลน์ 100%', 'Works 100% offline')),
      (Icons.phonelink_lock_rounded, context.tr('ข้อมูลอยู่ในเครื่องของคุณเท่านั้น', 'Your data stays on your device')),
      (Icons.download_rounded, context.tr('ส่งออกข้อมูลได้ทุกเมื่อ (JSON ใน ZIP)', 'Export anytime (JSON in ZIP)')),
      (Icons.update_disabled_rounded, context.tr('ใช้งานได้ระยะยาวแม้ไม่อัปเดตแอป', 'Keeps working without updates')),
    ];
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('เกี่ยวกับ', 'About'))),
      body: ContentWidth(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(Space.page, Space.sm, Space.page, Space.xxxl),
          children: [
            Center(
              child: Container(
                width: 76,
                height: 76,
                decoration: BoxDecoration(color: scheme.primary, borderRadius: BorderRadius.circular(24)),
                child: Icon(Icons.task_rounded, color: scheme.onPrimary, size: 40),
              ),
            ),
            const SizedBox(height: Space.md),
            Center(child: Text(AppInfo.name, style: t.headlineSmall?.copyWith(letterSpacing: 2))),
            Center(
              child: Text(
                '${context.tr('เวอร์ชัน', 'Version')} ${AppInfo.version} (${AppInfo.build})',
                style: t.bodySmall,
              ),
            ),
            const SizedBox(height: Space.xl),
            LkPanel(
              child: Text(
                context.tr(
                  'LIFEKEEP คือแอปจัดการชีวิตประจำวันแบบออฟไลน์ ช่วยเก็บเอกสารสำคัญ จัดการงาน กำหนดการ และการแจ้งเตือน '
                      'ทุกอย่างทำงานในเครื่องของคุณ ไม่ต้องสมัครบัญชี ไม่ใช้อินเทอร์เน็ต ไม่มีโฆษณา และไม่มีค่าบริการรายเดือน',
                  'LIFEKEEP is an offline organizer for important documents, tasks, schedules and reminders. '
                      'Everything runs on your device — no account, no internet, no ads and no subscription.',
                ),
                style: t.bodyMedium,
              ),
            ),
            const SizedBox(height: Space.md),
            LkGroup(
              dividerIndent: 56,
              children: [
                for (final (icon, label) in features)
                  LkRow(
                    leading: Icon(icon, color: scheme.primary),
                    title: label,
                    dense: true,
                  ),
              ],
            ),
            const SizedBox(height: Space.md),
            LkGroup(
              children: [
                LkRow(
                  leading: Icon(Icons.description_outlined, color: scheme.onSurfaceVariant),
                  title: context.tr('สัญญาอนุญาตโอเพนซอร์ส', 'Open-source licences'),
                  showChevron: true,
                  onTap: () => showLicensePage(
                    context: context,
                    applicationName: AppInfo.name,
                    applicationVersion: AppInfo.version,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class PrivacyScreen extends StatelessWidget {
  const PrivacyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final items = [
      (
        Icons.cloud_off_rounded,
        context.tr('ไม่มีเซิร์ฟเวอร์และไม่มี API', 'No server, no API'),
        context.tr(
          'แอปไม่มีสิทธิ์เข้าถึงอินเทอร์เน็ต ข้อมูลจึงไม่ถูกส่งออกนอกเครื่อง',
          'The app has no internet permission, so data never leaves the device.',
        ),
      ),
      (
        Icons.person_off_outlined,
        context.tr('ไม่ต้องสมัครบัญชี', 'No account'),
        context.tr(
          'ไม่มีการเก็บข้อมูลส่วนตัว ไม่มีการติดตาม ไม่มีโฆษณา',
          'No personal data collection, tracking or ads.',
        ),
      ),
      (
        Icons.storage_rounded,
        context.tr('เก็บข้อมูลในเครื่อง', 'Stored locally'),
        context.tr(
          'ข้อมูลอยู่ใน SQLite และโฟลเดอร์ส่วนตัวของแอป ปิดการสำรองข้อมูลอัตโนมัติไปยังคลาวด์ของ Android',
          'Data lives in SQLite and the app\'s private folder. Android cloud auto-backup is disabled.',
        ),
      ),
      (
        Icons.key_rounded,
        context.tr('PIN ปลอดภัย', 'Secure PIN'),
        context.tr(
          'PIN เก็บเป็นค่าแฮชพร้อม salt ใน Secure Storage ไม่เก็บเป็นข้อความ',
          'The PIN is stored as a salted hash in secure storage, never as text.',
        ),
      ),
      (
        Icons.visibility_off_outlined,
        context.tr('ซ่อนจากหน้าแอปล่าสุด', 'Hidden in Recent Apps'),
        context.tr(
          'เมื่อเปิดล็อกแอป เนื้อหาจะถูกซ่อนในหน้าแอปล่าสุดและป้องกันการจับภาพหน้าจอ',
          'With App Lock on, content is hidden in Recent Apps and screenshots are blocked.',
        ),
      ),
      (
        Icons.backup_outlined,
        context.tr('ไฟล์สำรองเป็นของคุณ', 'Backups are yours'),
        context.tr(
          'ไฟล์สำรองไม่ได้เข้ารหัส กรุณาเก็บไว้ในที่ปลอดภัย',
          'Backup files are not encrypted — keep them somewhere safe.',
        ),
      ),
    ];
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ความเป็นส่วนตัว', 'Privacy'))),
      body: ContentWidth(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
          children: [
            LkGroup(
              dividerIndent: 64,
              children: [
                for (final (icon, title, body) in items)
                  LkRow(
                    leading: IconBadge(icon: icon, color: scheme.primary),
                    title: title,
                    titleMaxLines: 2,
                    subtitle: Text(body, maxLines: 4),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
