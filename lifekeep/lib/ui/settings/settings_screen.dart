import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/app_info.dart';
import '../../core/l10n/lang.dart';
import '../../core/utils/dates.dart';
import '../../data/repositories/settings_repository.dart';
import '../../state/app_controller.dart';
import '../design/design.dart';
import 'about_screens.dart';
import 'app_lock_screen.dart';
import 'backup_screen.dart';
import 'categories_screen.dart';
import 'delete_all_flow.dart';
import 'notification_settings_screen.dart';
import 'storage_screen.dart';

/// Last successful backup (manual or automatic), if any.
DateTime? lastBackupAt(Map<String, String> s) {
  final a = int.tryParse(s[SettingKeys.lastAutoBackupAt] ?? '');
  final m = int.tryParse(s[SettingKeys.lastManualBackupAt] ?? '');
  final ms = [?a, ?m];
  if (ms.isEmpty) return null;
  ms.sort();
  return DateTime.fromMillisecondsSinceEpoch(ms.last);
}

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    void open(Widget page) => Navigator.push(context, MaterialPageRoute(builder: (_) => page));
    final s = context.status;
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final last = lastBackupAt(c.settings);
    final backupDays = last == null ? null : Dates.daysBetween(last, DateTime.now());
    final backupLabel = last == null
        ? context.tr('ยังไม่เคยสำรอง', 'Never backed up')
        : (backupDays == 0
              ? context.tr('สำรองวันนี้', 'Backed up today')
              : context.tr('สำรอง $backupDays วันก่อน', '$backupDays days ago'));

    Widget badge(IconData i, Color col) => IconBadge(icon: i, color: col, size: 32, filled: true);

    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ตั้งค่า', 'Settings'))),
      body: ContentWidth(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(Space.page, 0, Space.page, Space.xxxl),
          children: [
            // Status header
            LkPanel(
              padding: const EdgeInsets.all(Space.lg),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(color: scheme.primary, borderRadius: BorderRadius.circular(14)),
                        child: Icon(Icons.task_rounded, color: scheme.onPrimary, size: 24),
                      ),
                      const SizedBox(width: Space.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(AppInfo.name, style: t.titleMedium?.copyWith(letterSpacing: 1.2)),
                            Text(
                              context.tr('ข้อมูลทั้งหมดอยู่ในเครื่องนี้', 'All data stays on this device'),
                              style: t.bodySmall,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: Space.md),
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: [
                      Pill(
                        label: context.tr('ออฟไลน์ 100%', '100% offline'),
                        icon: Icons.wifi_off_rounded,
                        color: s.success,
                        background: s.successContainer,
                      ),
                      Pill(
                        label: c.lockEnabled
                            ? context.tr('ล็อกแอปเปิด', 'App lock on')
                            : context.tr('ล็อกแอปปิด', 'App lock off'),
                        icon: c.lockEnabled ? Icons.lock_rounded : Icons.lock_open_rounded,
                        color: c.lockEnabled ? s.success : scheme.onSurfaceVariant,
                        background: c.lockEnabled ? s.successContainer : scheme.surfaceContainerHigh,
                      ),
                      Pill(
                        label: backupLabel,
                        icon: Icons.backup_outlined,
                        color: (backupDays ?? 99) <= 7 ? s.success : s.warning,
                        background: (backupDays ?? 99) <= 7 ? s.successContainer : s.warningContainer,
                      ),
                    ],
                  ),
                ],
              ),
            ),
            LkSectionHeader(title: context.tr('การแสดงผล', 'Appearance')),
            LkGroup(
              dividerIndent: 60,
              children: [
                _SegmentRow<ThemeMode>(
                  leading: badge(Icons.palette_outlined, const Color(0xFF6B5BA8)),
                  title: context.tr('ธีม', 'Theme'),
                  value: c.themeMode,
                  options: {
                    ThemeMode.system: context.tr('ระบบ', 'Auto'),
                    ThemeMode.light: context.tr('สว่าง', 'Light'),
                    ThemeMode.dark: context.tr('มืด', 'Dark'),
                  },
                  onChanged: (m) => runAction(context, () => c.setThemeMode(m)),
                ),
                _SegmentRow<AppLanguage>(
                  leading: badge(Icons.translate_rounded, const Color(0xFF3B6EA8)),
                  title: context.tr('ภาษา', 'Language'),
                  value: c.language,
                  options: const {AppLanguage.th: 'ไทย', AppLanguage.en: 'English'},
                  onChanged: (l) => runAction(context, () => c.setLanguage(l)),
                ),
              ],
            ),
            LkSectionHeader(title: context.tr('การแจ้งเตือนและความปลอดภัย', 'Reminders & security')),
            LkGroup(
              dividerIndent: 60,
              children: [
                LkValueRow(
                  leading: badge(Icons.notifications_none_rounded, const Color(0xFFC0742A)),
                  title: context.tr('การแจ้งเตือน', 'Notifications'),
                  value: c.reminderPrefs.enabled ? context.tr('เปิด', 'On') : context.tr('ปิด', 'Off'),
                  onTap: () => open(const NotificationSettingsScreen()),
                ),
                LkValueRow(
                  leading: badge(Icons.lock_outline_rounded, const Color(0xFF2E7D5B)),
                  title: context.tr('ล็อกแอป', 'App lock'),
                  value: c.lockEnabled ? context.tr('เปิด', 'On') : context.tr('ปิด', 'Off'),
                  onTap: () => open(const AppLockSettingsScreen()),
                ),
              ],
            ),
            LkSectionHeader(title: context.tr('ข้อมูล', 'Data')),
            LkGroup(
              dividerIndent: 60,
              children: [
                LkValueRow(
                  leading: badge(Icons.backup_outlined, const Color(0xFF233B67)),
                  title: context.tr('สำรองและกู้คืนข้อมูล', 'Backup & restore'),
                  value: backupLabel,
                  valueColor: (backupDays ?? 99) <= 7 ? null : s.warning,
                  onTap: () => open(const BackupScreen()),
                ),
                LkValueRow(
                  leading: badge(Icons.label_outline_rounded, const Color(0xFF2E7D7A)),
                  title: context.tr('จัดการหมวดหมู่', 'Manage categories'),
                  value: '${c.categories.length}',
                  onTap: () => open(const CategoriesScreen()),
                ),
                LkValueRow(
                  leading: badge(Icons.storage_rounded, const Color(0xFF64748B)),
                  title: context.tr('พื้นที่จัดเก็บ', 'Storage'),
                  onTap: () => open(const StorageScreen()),
                ),
              ],
            ),
            LkSectionHeader(title: context.tr('เกี่ยวกับ', 'About')),
            LkGroup(
              dividerIndent: 60,
              children: [
                LkValueRow(
                  leading: badge(Icons.info_outline_rounded, const Color(0xFF3B6EA8)),
                  title: context.tr('เกี่ยวกับ LIFEKEEP', 'About LIFEKEEP'),
                  onTap: () => open(const AboutScreen()),
                ),
                LkValueRow(
                  leading: badge(Icons.privacy_tip_outlined, const Color(0xFF2E7D5B)),
                  title: context.tr('ความเป็นส่วนตัว', 'Privacy'),
                  onTap: () => open(const PrivacyScreen()),
                ),
                LkValueRow(
                  leading: badge(Icons.tag_rounded, const Color(0xFF64748B)),
                  title: context.tr('เวอร์ชัน', 'Version'),
                  value: '${AppInfo.version} (${AppInfo.build})',
                ),
              ],
            ),
            const SizedBox(height: Space.xl),
            LkGroup(
              children: [
                LkRow(
                  leading: IconBadge(icon: Icons.delete_forever_outlined, color: s.danger, size: 32),
                  title: context.tr('ลบข้อมูลทั้งหมด', 'Delete all data'),
                  titleStyle: t.bodyLarge?.copyWith(color: s.danger, fontWeight: FontWeight.w600),
                  subtitle: Text(context.tr('ต้องยืนยัน 2 ขั้นตอน', 'Requires 2-step confirmation')),
                  onTap: () => startDeleteAllFlow(context),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Row with a compact inline segmented selector.
class _SegmentRow<T> extends StatelessWidget {
  const _SegmentRow({
    required this.leading,
    required this.title,
    required this.value,
    required this.options,
    required this.onChanged,
  });
  final Widget leading;
  final String title;
  final T value;
  final Map<T, String> options;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final selector = Container(
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(color: scheme.surfaceContainerHigh, borderRadius: Radii.brSm),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final e in options.entries)
            GestureDetector(
              onTap: () => onChanged(e.key),
              child: AnimatedContainer(
                duration: Motion.fast,
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: e.key == value ? scheme.surface : Colors.transparent,
                  borderRadius: BorderRadius.circular(Radii.xs + 1),
                  boxShadow: e.key == value
                      ? [
                          BoxShadow(
                            color: scheme.shadow.withValues(alpha: 0.08),
                            blurRadius: 4,
                            offset: const Offset(0, 1),
                          ),
                        ]
                      : null,
                ),
                child: Text(
                  e.value,
                  style: t.labelMedium?.copyWith(color: e.key == value ? scheme.onSurface : scheme.onSurfaceVariant),
                ),
              ),
            ),
        ],
      ),
    );
    final label = Text(
      title,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: t.bodyLarge?.copyWith(fontWeight: FontWeight.w500),
    );
    return Padding(
      padding: const EdgeInsets.fromLTRB(Space.lg, Space.md, Space.md, Space.md),
      child: LayoutBuilder(
        builder: (context, box) {
          // Side by side when it fits; otherwise stack the selector under the
          // title (narrow phones / large system text) instead of overflowing.
          final textScale = MediaQuery.textScalerOf(context).scale(1);
          final needed = 150 + options.length * 64 * textScale;
          if (box.maxWidth >= needed) {
            return Row(
              children: [
                leading,
                const SizedBox(width: Space.md),
                Expanded(child: label),
                selector,
              ],
            );
          }
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  leading,
                  const SizedBox(width: Space.md),
                  Expanded(child: label),
                ],
              ),
              const SizedBox(height: Space.sm),
              Padding(
                padding: const EdgeInsets.only(left: 44),
                child: FittedBox(fit: BoxFit.scaleDown, alignment: Alignment.centerLeft, child: selector),
              ),
            ],
          );
        },
      ),
    );
  }
}
